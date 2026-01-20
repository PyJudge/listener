package com.listener.presentation.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.dao.FolderDao
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.entity.FolderEntity
import com.listener.data.local.db.entity.FolderItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FolderDetailUiState {
    data object Loading : FolderDetailUiState
    data class Success(
        val folder: FolderEntity,
        val items: List<FolderDetailItem>,
        val totalDurationMs: Long,
        val progress: Float,
        val completedCount: Int
    ) : FolderDetailUiState
    data class Error(val message: String) : FolderDetailUiState
}

data class FolderDetailItem(
    val folderItem: FolderItemEntity,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val durationMs: Long?,
    val progress: Float,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val folderDao: FolderDao,
    private val podcastDao: PodcastDao,
    private val localFileDao: LocalFileDao,
    private val recentLearningDao: RecentLearningDao
) : ViewModel() {

    val folderId: Long = checkNotNull(savedStateHandle["folderId"])

    private val _uiState = MutableStateFlow<FolderDetailUiState>(FolderDetailUiState.Loading)
    val uiState: StateFlow<FolderDetailUiState> = _uiState.asStateFlow()

    init {
        loadFolderDetail()
    }

    private fun loadFolderDetail() {
        viewModelScope.launch {
            val folder = folderDao.getFolder(folderId)
            if (folder == null) {
                _uiState.value = FolderDetailUiState.Error("Folder not found")
                return@launch
            }

            combine(
                folderDao.getFolderItems(folderId),
                recentLearningDao.getRecentLearnings(100)
            ) { folderItems, recentLearnings ->
                Pair(folderItems, recentLearnings)
            }.collect { (folderItems, recentLearnings) ->
                val recentLearningsMap = recentLearnings.associateBy { it.sourceId }

                var totalDurationMs = 0L
                var completedCount = 0
                var foundCurrent = false

                val detailItems = mutableListOf<FolderDetailItem>()
                for (item in folderItems) {
                    val learning = recentLearningsMap[item.sourceId]
                    val itemProgress = if (learning != null && learning.totalChunks > 0) {
                        learning.currentChunkIndex.toFloat() / learning.totalChunks
                    } else {
                        0f
                    }
                    val isCompleted = itemProgress >= 1f
                    if (isCompleted) completedCount++

                    val isCurrent = !isCompleted && !foundCurrent
                    if (isCurrent) foundCurrent = true

                    val (title, subtitle, thumbnailUrl, durationMs) = getItemDetails(item, learning)
                    totalDurationMs += durationMs ?: 0L

                    detailItems.add(
                        FolderDetailItem(
                            folderItem = item,
                            title = title,
                            subtitle = subtitle,
                            thumbnailUrl = thumbnailUrl,
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

                _uiState.value = FolderDetailUiState.Success(
                    folder = folder,
                    items = detailItems,
                    totalDurationMs = totalDurationMs,
                    progress = overallProgress,
                    completedCount = completedCount
                )
            }
        }
    }

    private data class ItemDetails(
        val title: String,
        val subtitle: String,
        val thumbnailUrl: String?,
        val durationMs: Long?
    )

    private suspend fun getItemDetails(
        item: FolderItemEntity,
        learning: com.listener.data.local.db.entity.RecentLearningEntity?
    ): ItemDetails {
        if (learning != null) {
            return ItemDetails(learning.title, learning.subtitle, learning.thumbnailUrl, null)
        }

        return when (item.sourceType) {
            "PODCAST_EPISODE" -> {
                val episode = podcastDao.getEpisode(item.sourceId)
                if (episode != null) {
                    val podcast = podcastDao.getSubscription(episode.feedUrl)
                    ItemDetails(
                        episode.title,
                        podcast?.title ?: "Unknown Podcast",
                        podcast?.artworkUrl,
                        episode.durationMs
                    )
                } else {
                    ItemDetails("Unknown Episode", "Unknown", null, null)
                }
            }
            "LOCAL_FILE" -> {
                val file = localFileDao.getFile(item.sourceId)
                if (file != null) {
                    ItemDetails(file.displayName, "Local File", null, file.durationMs)
                } else {
                    ItemDetails("Unknown File", "Local File", null, null)
                }
            }
            else -> ItemDetails("Unknown", "Unknown", null, null)
        }
    }

    fun removeItem(item: FolderDetailItem) {
        viewModelScope.launch {
            folderDao.deleteFolderItem(item.folderItem)
            reorderAfterRemoval(item.folderItem.orderIndex)
        }
    }

    private suspend fun reorderAfterRemoval(removedIndex: Int) {
        val items = folderDao.getFolderItemsList(folderId)
        items.filter { it.orderIndex > removedIndex }.forEach { item ->
            folderDao.updateFolderItemOrder(item.id, item.orderIndex - 1)
        }
    }

    fun reorderItems(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val items = folderDao.getFolderItemsList(folderId).toMutableList()
            if (fromIndex < 0 || fromIndex >= items.size ||
                toIndex < 0 || toIndex >= items.size) {
                return@launch
            }

            val movedItem = items.removeAt(fromIndex)
            items.add(toIndex, movedItem)

            items.forEachIndexed { index, item ->
                if (item.orderIndex != index) {
                    folderDao.updateFolderItemOrder(item.id, index)
                }
            }
        }
    }

    fun getFirstIncompleteItemIndex(): Int {
        val currentState = _uiState.value
        if (currentState is FolderDetailUiState.Success) {
            return currentState.items.indexOfFirst { !it.isCompleted }.takeIf { it >= 0 } ?: 0
        }
        return 0
    }

    fun updateFolderName(newName: String) {
        viewModelScope.launch {
            val folder = folderDao.getFolder(folderId) ?: return@launch
            folderDao.updateFolder(
                folder.copy(
                    name = newName,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.update { currentState ->
                if (currentState is FolderDetailUiState.Success) {
                    currentState.copy(folder = currentState.folder.copy(name = newName))
                } else {
                    currentState
                }
            }
        }
    }

    fun deleteFolder(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val folder = folderDao.getFolder(folderId) ?: return@launch
            folderDao.deleteFolder(folder)
            onDeleted()
        }
    }
}
