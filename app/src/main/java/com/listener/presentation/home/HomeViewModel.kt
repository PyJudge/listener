package com.listener.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.service.TranscriptionQueueManager
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PlaylistItemEntity
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.RecentLearningEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentLearnings: List<RecentLearningEntity> = emptyList(),
    val newEpisodes: List<PodcastEpisodeEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val playlistItemCounts: Map<Long, Int> = emptyMap(),
    val podcastNames: Map<String, String> = emptyMap(), // feedUrl -> podcastName
    val isLoading: Boolean = false,
    val hasSubscriptions: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recentLearningDao: RecentLearningDao,
    private val podcastDao: PodcastDao,
    private val playlistDao: PlaylistDao,
    private val transcriptionQueueManager: TranscriptionQueueManager
) : ViewModel() {

    fun markEpisodeAsPlayed(episodeId: String) {
        viewModelScope.launch {
            podcastDao.markAsRead(episodeId)
        }
    }

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)

        viewModelScope.launch {
            // Combine all data sources into a single state update
            combine(
                recentLearningDao.getRecentLearnings(5),
                podcastDao.getNewEpisodes(threeDaysAgo, 10),
                podcastDao.getAllSubscriptions(),
                playlistDao.getAllPlaylists()
            ) { learnings, episodes, subscriptions, playlists ->
                // Build podcast name map
                val nameMap = subscriptions.associate { it.feedUrl to it.title }

                // Calculate playlist item counts
                val itemCounts = playlists.associate { playlist ->
                    playlist.id to playlistDao.getPlaylistItemsList(playlist.id).size
                }

                HomeUiState(
                    recentLearnings = learnings,
                    newEpisodes = episodes,
                    playlists = playlists,
                    playlistItemCounts = itemCounts,
                    podcastNames = nameMap,
                    isLoading = false,
                    hasSubscriptions = subscriptions.isNotEmpty(),
                    showCreatePlaylistDialog = _uiState.value.showCreatePlaylistDialog
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun addToPlaylist(playlistId: Long, sourceId: String, sourceType: String) {
        viewModelScope.launch {
            val maxOrder = playlistDao.getMaxOrderIndex(playlistId) ?: -1
            val item = PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = sourceId,
                sourceType = sourceType,
                orderIndex = maxOrder + 1,
                addedAt = System.currentTimeMillis()
            )
            playlistDao.insertPlaylistItem(item)
        }
    }

    fun showCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = true) }
    }

    fun dismissCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = false) }
    }

    fun createPlaylistAndAddItem(name: String, sourceId: String, sourceType: String) {
        viewModelScope.launch {
            val playlist = PlaylistEntity(
                name = name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val playlistId = playlistDao.insertPlaylist(playlist)
            val item = PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = sourceId,
                sourceType = sourceType,
                orderIndex = 0,
                addedAt = System.currentTimeMillis()
            )
            playlistDao.insertPlaylistItem(item)
            dismissCreatePlaylistDialog()
        }
    }

    fun addToTranscriptionQueue(sourceId: String, sourceType: String) {
        viewModelScope.launch {
            transcriptionQueueManager.addToQueue(sourceId, sourceType)
        }
    }
}
