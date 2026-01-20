package com.listener.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.entity.PlaylistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUiState(
    val playlists: List<PlaylistWithProgress> = emptyList(),
    val isLoading: Boolean = false,
    val showCreateDialog: Boolean = false
)

data class PlaylistWithProgress(
    val playlist: PlaylistEntity,
    val itemCount: Int,
    val totalDurationMs: Long,
    val progress: Float
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistDao: PlaylistDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState(isLoading = true))
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collect { playlists ->
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
                    it.copy(playlists = playlistsWithProgress, isLoading = false)
                }
            }
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
            dismissCreateDialog()
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }
}
