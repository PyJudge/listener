package com.listener.presentation.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.dao.TranscriptionDao
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PlaylistItemEntity
import com.listener.service.RecordingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState
    data class Success(
        val playlist: PlaylistEntity,
        val items: List<PlaylistDetailItem>,
        val totalDurationMs: Long,
        val progress: Float,
        val completedCount: Int,
        val showAddContentDialog: Boolean = false,
        val availableContent: List<AvailableContentItem> = emptyList()
    ) : PlaylistDetailUiState
    data class Error(val message: String) : PlaylistDetailUiState
}

data class AvailableContentItem(
    val sourceId: String,
    val sourceType: String,
    val title: String,
    val subtitle: String,
    val durationMs: Long?,
    val isAlreadyAdded: Boolean
)

data class PlaylistDetailItem(
    val playlistItem: PlaylistItemEntity,
    val title: String,
    val subtitle: String,
    val durationMs: Long?,
    val progress: Float,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistDao: PlaylistDao,
    private val podcastDao: PodcastDao,
    private val localFileDao: LocalFileDao,
    private val recentLearningDao: RecentLearningDao,
    private val transcriptionDao: TranscriptionDao,
    private val recordingManager: RecordingManager
) : ViewModel() {

    val playlistId: Long = checkNotNull(savedStateHandle["playlistId"])

    private val _uiState = MutableStateFlow<PlaylistDetailUiState>(PlaylistDetailUiState.Loading)
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        loadPlaylistDetail()
    }

    // H1: Retry function for error recovery
    fun retry() {
        _uiState.value = PlaylistDetailUiState.Loading
        loadPlaylistDetail()
    }

    private fun loadPlaylistDetail() {
        viewModelScope.launch {
            val playlist = playlistDao.getPlaylist(playlistId)
            if (playlist == null) {
                _uiState.value = PlaylistDetailUiState.Error("Playlist not found")
                return@launch
            }

            combine(
                playlistDao.getPlaylistItems(playlistId),
                recentLearningDao.getRecentLearnings(100)
            ) { playlistItems, recentLearnings ->
                Pair(playlistItems, recentLearnings)
            }.collect { (playlistItems, recentLearnings) ->
                val recentLearningsMap = recentLearnings.associateBy { it.sourceId }

                var totalDurationMs = 0L
                var completedCount = 0
                var foundCurrent = false

                val detailItems = mutableListOf<PlaylistDetailItem>()
                for (item in playlistItems) {
                    val learning = recentLearningsMap[item.sourceId]
                    val itemProgress = if (learning != null && learning.totalChunks > 0) {
                        learning.currentChunkIndex.toFloat() / learning.totalChunks
                    } else {
                        0f
                    }
                    val isCompleted = itemProgress >= 1f
                    if (isCompleted) completedCount++

                    // Determine if this is the current item (first incomplete)
                    val isCurrent = !isCompleted && !foundCurrent
                    if (isCurrent) foundCurrent = true

                    val (title, subtitle, durationMs) = getItemDetails(item, learning)
                    totalDurationMs += durationMs ?: 0L

                    detailItems.add(
                        PlaylistDetailItem(
                            playlistItem = item,
                            title = title,
                            subtitle = subtitle,
                            durationMs = durationMs,
                            progress = itemProgress,
                            isCompleted = isCompleted,
                            isCurrent = isCurrent
                        )
                    )
                }

                val overallProgress = if (detailItems.isNotEmpty()) {
                    completedCount.toFloat() / detailItems.size
                } else {
                    0f
                }

                _uiState.value = PlaylistDetailUiState.Success(
                    playlist = playlist,
                    items = detailItems,
                    totalDurationMs = totalDurationMs,
                    progress = overallProgress,
                    completedCount = completedCount
                )
            }
        }
    }

    private suspend fun getItemDetails(
        item: PlaylistItemEntity,
        learning: com.listener.data.local.db.entity.RecentLearningEntity?
    ): Triple<String, String, Long?> {
        // If we have recent learning info, use it
        if (learning != null) {
            return Triple(learning.title, learning.subtitle, null)
        }

        // Otherwise, fetch from source
        return when (item.sourceType) {
            "PODCAST_EPISODE" -> {
                val episode = podcastDao.getEpisode(item.sourceId)
                if (episode != null) {
                    val podcast = podcastDao.getSubscription(episode.feedUrl)
                    Triple(
                        episode.title,
                        podcast?.title ?: "Unknown Podcast",
                        episode.durationMs
                    )
                } else {
                    Triple("Unknown Episode", "Unknown", null)
                }
            }
            "LOCAL_FILE" -> {
                val file = localFileDao.getFile(item.sourceId)
                if (file != null) {
                    Triple(file.displayName, "Local File", file.durationMs)
                } else {
                    Triple("Unknown File", "Local File", null)
                }
            }
            else -> Triple("Unknown", "Unknown", null)
        }
    }

    fun removeItem(item: PlaylistDetailItem) {
        viewModelScope.launch {
            val sourceId = item.playlistItem.sourceId
            playlistDao.deletePlaylistItem(item.playlistItem)
            // Reorder remaining items
            reorderAfterRemoval(item.playlistItem.orderIndex)
            // H3: Clean up recordings for removed item
            recordingManager.deleteAllRecordings(sourceId)
        }
    }

    private suspend fun reorderAfterRemoval(removedIndex: Int) {
        val items = playlistDao.getPlaylistItemsList(playlistId)
        items.filter { it.orderIndex > removedIndex }.forEach { item ->
            // C5: Prevent negative orderIndex
            val newIndex = (item.orderIndex - 1).coerceAtLeast(0)
            playlistDao.updateItemOrder(item.id, newIndex)
        }
    }

    fun reorderItems(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val items = playlistDao.getPlaylistItemsList(playlistId).toMutableList()
            if (fromIndex < 0 || fromIndex >= items.size ||
                toIndex < 0 || toIndex >= items.size) {
                return@launch
            }

            // Move item in list
            val movedItem = items.removeAt(fromIndex)
            items.add(toIndex, movedItem)

            // Update all order indices
            items.forEachIndexed { index, item ->
                if (item.orderIndex != index) {
                    playlistDao.updateItemOrder(item.id, index)
                }
            }
        }
    }

    fun getFirstIncompleteItemIndex(): Int {
        val currentState = _uiState.value
        if (currentState is PlaylistDetailUiState.Success) {
            return currentState.items.indexOfFirst { !it.isCompleted }.takeIf { it >= 0 } ?: 0
        }
        return 0
    }

    fun updatePlaylistName(newName: String) {
        viewModelScope.launch {
            val playlist = playlistDao.getPlaylist(playlistId) ?: return@launch
            playlistDao.updatePlaylist(
                playlist.copy(
                    name = newName,
                    updatedAt = System.currentTimeMillis()
                )
            )
            // Reload to update UI
            _uiState.update { currentState ->
                if (currentState is PlaylistDetailUiState.Success) {
                    currentState.copy(playlist = currentState.playlist.copy(name = newName))
                } else {
                    currentState
                }
            }
        }
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val playlist = playlistDao.getPlaylist(playlistId) ?: return@launch
            playlistDao.deletePlaylist(playlist)
            onDeleted()
        }
    }

    fun showAddContentDialog() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is PlaylistDetailUiState.Success) return@launch

            val existingSourceIds = currentState.items.map { it.playlistItem.sourceId }.toSet()
            val availableContent = loadAvailableContent(existingSourceIds)

            _uiState.update { state ->
                if (state is PlaylistDetailUiState.Success) {
                    state.copy(
                        showAddContentDialog = true,
                        availableContent = availableContent
                    )
                } else state
            }
        }
    }

    fun dismissAddContentDialog() {
        _uiState.update { state ->
            if (state is PlaylistDetailUiState.Success) {
                state.copy(showAddContentDialog = false)
            } else state
        }
    }

    private suspend fun loadAvailableContent(existingSourceIds: Set<String>): List<AvailableContentItem> {
        val availableItems = mutableListOf<AvailableContentItem>()

        // Get all transcribed content
        val transcriptions = transcriptionDao.getAllTranscriptionsList()

        for (transcription in transcriptions) {
            val sourceId = transcription.sourceId
            val isAlreadyAdded = existingSourceIds.contains(sourceId)

            // Try to get episode info
            val episode = podcastDao.getEpisode(sourceId)
            if (episode != null) {
                val podcast = podcastDao.getSubscription(episode.feedUrl)
                availableItems.add(
                    AvailableContentItem(
                        sourceId = sourceId,
                        sourceType = "PODCAST_EPISODE",
                        title = episode.title,
                        subtitle = podcast?.title ?: "Unknown Podcast",
                        durationMs = episode.durationMs,
                        isAlreadyAdded = isAlreadyAdded
                    )
                )
                continue
            }

            // Try to get local file info
            val localFile = localFileDao.getFile(sourceId)
            if (localFile != null) {
                availableItems.add(
                    AvailableContentItem(
                        sourceId = sourceId,
                        sourceType = "LOCAL_FILE",
                        title = localFile.displayName,
                        subtitle = "Local File",
                        durationMs = localFile.durationMs,
                        isAlreadyAdded = isAlreadyAdded
                    )
                )
            }
        }

        return availableItems
    }

    fun addContentToPlaylist(item: AvailableContentItem) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is PlaylistDetailUiState.Success) return@launch

            val maxOrder = playlistDao.getMaxOrderIndex(playlistId) ?: -1
            val newItem = PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = item.sourceId,
                sourceType = item.sourceType,
                orderIndex = maxOrder + 1,
                addedAt = System.currentTimeMillis()
            )
            playlistDao.insertPlaylistItem(newItem)

            // Update available content to mark as added
            _uiState.update { state ->
                if (state is PlaylistDetailUiState.Success) {
                    state.copy(
                        availableContent = state.availableContent.map {
                            if (it.sourceId == item.sourceId) it.copy(isAlreadyAdded = true)
                            else it
                        }
                    )
                } else state
            }
        }
    }
}
