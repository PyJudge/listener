package com.listener.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.dao.FolderDao
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.TranscriptionDao
import com.listener.data.local.db.entity.FolderEntity
import com.listener.data.local.db.entity.FolderItemEntity
import com.listener.data.local.db.entity.PlaylistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUiState(
    // Transcribed Section
    val transcribedItems: List<TranscribedContentItem> = emptyList(),
    val transcribedCount: Int = 0,
    val isTranscribedExpanded: Boolean = false,

    // Custom Section
    val folders: List<FolderWithItems> = emptyList(),
    val playlists: List<PlaylistWithProgress> = emptyList(),

    // UI State
    val isLoading: Boolean = false,
    val showCreateFolderDialog: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false
)

data class TranscribedContentItem(
    val sourceId: String,
    val sourceType: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val durationMs: Long?,
    val transcribedAt: Long
)

data class FolderWithItems(
    val folder: FolderEntity,
    val items: List<FolderContentItem>,
    val itemCount: Int,
    val totalDurationMs: Long,
    val progress: Float
)

data class FolderContentItem(
    val folderItem: FolderItemEntity,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val durationMs: Long?
)

data class PlaylistWithProgress(
    val playlist: PlaylistEntity,
    val itemCount: Int,
    val totalDurationMs: Long,
    val progress: Float
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val folderDao: FolderDao,
    private val transcriptionDao: TranscriptionDao,
    private val podcastDao: PodcastDao,
    private val localFileDao: LocalFileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState(isLoading = true))
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                transcriptionDao.getAllTranscriptions(),
                folderDao.getAllFolders(),
                playlistDao.getAllPlaylists()
            ) { transcriptions, folders, playlists ->
                Triple(transcriptions, folders, playlists)
            }.collect { (transcriptions, folders, playlists) ->
                // Load transcribed items
                val transcribedItems = transcriptions.mapNotNull { transcription ->
                    getTranscribedContentItem(transcription.sourceId, transcription.createdAt)
                }

                // Load folders with items
                val foldersWithItems = folders.map { folder ->
                    val items = folderDao.getFolderItemsList(folder.id)
                    val folderItems = items.mapNotNull { item ->
                        getFolderContentItem(item)
                    }
                    val totalDuration = folderDao.getFolderTotalDuration(folder.id)
                    val progress = folderDao.getFolderProgress(folder.id).coerceIn(0f, 1f)
                    FolderWithItems(
                        folder = folder,
                        items = folderItems,
                        itemCount = items.size,
                        totalDurationMs = totalDuration,
                        progress = progress
                    )
                }

                // Load playlists with progress
                val playlistsWithProgress = playlists.map { playlist ->
                    val items = playlistDao.getPlaylistItemsList(playlist.id)
                    val totalDuration = playlistDao.getPlaylistTotalDuration(playlist.id)
                    val progress = playlistDao.getPlaylistProgress(playlist.id).coerceIn(0f, 1f)
                    PlaylistWithProgress(
                        playlist = playlist,
                        itemCount = items.size,
                        totalDurationMs = totalDuration,
                        progress = progress
                    )
                }

                _uiState.update {
                    it.copy(
                        transcribedItems = transcribedItems,
                        transcribedCount = transcribedItems.size,
                        folders = foldersWithItems,
                        playlists = playlistsWithProgress,
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun getTranscribedContentItem(sourceId: String, createdAt: Long): TranscribedContentItem? {
        // Try podcast episode first
        val episode = podcastDao.getEpisode(sourceId)
        if (episode != null) {
            val podcast = podcastDao.getSubscription(episode.feedUrl)
            return TranscribedContentItem(
                sourceId = sourceId,
                sourceType = "PODCAST_EPISODE",
                title = episode.title,
                subtitle = podcast?.title ?: "",
                thumbnailUrl = podcast?.artworkUrl,
                durationMs = episode.durationMs,
                transcribedAt = createdAt
            )
        }

        // Try local file
        val localFile = localFileDao.getFile(sourceId)
        if (localFile != null) {
            return TranscribedContentItem(
                sourceId = sourceId,
                sourceType = "LOCAL_FILE",
                title = localFile.displayName,
                subtitle = localFile.uri,
                thumbnailUrl = null,
                durationMs = localFile.durationMs,
                transcribedAt = createdAt
            )
        }

        return null
    }

    private suspend fun getFolderContentItem(item: FolderItemEntity): FolderContentItem? {
        return when (item.sourceType) {
            "PODCAST_EPISODE" -> {
                val episode = podcastDao.getEpisode(item.sourceId)
                if (episode != null) {
                    val podcast = podcastDao.getSubscription(episode.feedUrl)
                    FolderContentItem(
                        folderItem = item,
                        title = episode.title,
                        subtitle = podcast?.title ?: "",
                        thumbnailUrl = podcast?.artworkUrl,
                        durationMs = episode.durationMs
                    )
                } else null
            }
            "LOCAL_FILE" -> {
                val localFile = localFileDao.getFile(item.sourceId)
                if (localFile != null) {
                    FolderContentItem(
                        folderItem = item,
                        title = localFile.displayName,
                        subtitle = localFile.uri,
                        thumbnailUrl = null,
                        durationMs = localFile.durationMs
                    )
                } else null
            }
            else -> null
        }
    }

    fun toggleTranscribedSection() {
        _uiState.update { it.copy(isTranscribedExpanded = !it.isTranscribedExpanded) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val folder = FolderEntity(
                name = name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            folderDao.insertFolder(folder)
            dismissCreateFolderDialog()
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            folderDao.deleteFolder(folder)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val playlist = PlaylistEntity(
                name = name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            playlistDao.insertPlaylist(playlist)
            dismissCreatePlaylistDialog()
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun showCreateFolderDialog() {
        _uiState.update { it.copy(showCreateFolderDialog = true) }
    }

    fun dismissCreateFolderDialog() {
        _uiState.update { it.copy(showCreateFolderDialog = false) }
    }

    fun showCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = true) }
    }

    fun dismissCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = false) }
    }
}
