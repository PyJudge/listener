package com.listener.presentation.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.entity.PlaylistItemEntity
import com.listener.domain.model.Chunk
import com.listener.domain.model.LearningSettings
import com.listener.domain.model.PlaybackState
import com.listener.domain.model.PlayMode
import com.listener.domain.repository.TranscriptionRepository
import com.listener.service.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val error: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository,
    private val playlistDao: PlaylistDao,
    private val podcastDao: PodcastDao,
    private val localFileDao: LocalFileDao,
    private val recentLearningDao: RecentLearningDao,
    private val playbackController: PlaybackController
) : ViewModel() {

    companion object {
        private const val TAG = "PlayerViewModel"
    }

    // Use playback state from controller
    val playbackState: StateFlow<PlaybackState> = playbackController.playbackState

    // UI state for errors
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _chunks = MutableStateFlow<List<Chunk>>(emptyList())
    val chunks: StateFlow<List<Chunk>> = _chunks.asStateFlow()

    // Playlist state
    private val _playlistId = MutableStateFlow<Long?>(null)
    val playlistId: StateFlow<Long?> = _playlistId.asStateFlow()

    private val _playlistItems = MutableStateFlow<List<PlaylistItemEntity>>(emptyList())
    val playlistItems: StateFlow<List<PlaylistItemEntity>> = _playlistItems.asStateFlow()

    private val _currentPlaylistItemIndex = MutableStateFlow(0)
    val currentPlaylistItemIndex: StateFlow<Int> = _currentPlaylistItemIndex.asStateFlow()

    // Track recordings per chunk index
    private val recordings = mutableMapOf<Int, String>()

    // Current content metadata
    private var currentSourceId: String = ""
    private var currentTitle: String = ""
    private var currentSubtitle: String = ""
    private var currentArtworkUrl: String? = null

    init {
        // Bind to PlaybackService when ViewModel is created
        playbackController.bindService()
    }

    override fun onCleared() {
        super.onCleared()
        // Note: We don't unbind here since the controller is a singleton
        // and might be used by other ViewModels
    }

    fun loadContent(
        sourceId: String,
        title: String,
        subtitle: String,
        artworkUrl: String? = null
    ) {
        Log.d(TAG, "loadContent: sourceId=$sourceId, title=$title")
        viewModelScope.launch {
            currentSourceId = sourceId
            currentTitle = title
            currentSubtitle = subtitle
            currentArtworkUrl = artworkUrl

            // 앱 시작 시 이미 rechunk되었으므로 단순히 로드만 함
            val loadedChunks = transcriptionRepository.getChunks(sourceId)
            _chunks.value = loadedChunks
            Log.d(TAG, "Loaded ${loadedChunks.size} chunks")

            // Get the audio file path
            val audioFilePath = playbackController.getAudioFilePath(sourceId)
            Log.d(TAG, "Audio file path: $audioFilePath")

            if (audioFilePath != null && loadedChunks.isNotEmpty()) {
                val currentSettings = playbackState.value.settings
                playbackController.setContent(
                    sourceId = sourceId,
                    audioUri = audioFilePath,
                    chunks = loadedChunks,
                    settings = currentSettings,
                    title = title,
                    subtitle = subtitle,
                    artworkUrl = artworkUrl
                )
            } else {
                Log.e(TAG, "Cannot set content: audioFilePath=$audioFilePath, chunks=${loadedChunks.size}")
            }
        }
    }

    fun observeChunks(sourceId: String) {
        viewModelScope.launch {
            transcriptionRepository.observeChunks(sourceId).collect { loadedChunks ->
                _chunks.value = loadedChunks
            }
        }
    }

    /**
     * Load content by sourceId, fetching metadata from recent learnings or searching in podcasts/local files.
     */
    fun loadBySourceId(sourceId: String) {
        Log.d(TAG, "loadBySourceId: $sourceId")

        // 이미 같은 sourceId가 재생 중이면 스킵
        if (playbackController.playbackState.value.sourceId == sourceId) {
            Log.d(TAG, "Same sourceId already playing, skipping")
            if (_chunks.value.isEmpty()) {
                viewModelScope.launch {
                    _chunks.value = transcriptionRepository.getChunks(sourceId)
                }
            }
            return
        }

        viewModelScope.launch {
            // First try to find in recent learnings for quick metadata
            val recentLearning = recentLearningDao.getRecentLearning(sourceId)
            if (recentLearning != null) {
                Log.d(TAG, "Found in recent learnings: ${recentLearning.title}, chunkIndex=${recentLearning.currentChunkIndex}")
                loadContent(
                    sourceId = recentLearning.sourceId,
                    title = recentLearning.title,
                    subtitle = recentLearning.subtitle,
                    artworkUrl = recentLearning.thumbnailUrl
                )
                // 저장된 위치로 복원
                if (recentLearning.currentChunkIndex > 0) {
                    kotlinx.coroutines.delay(500)
                    seekToChunk(recentLearning.currentChunkIndex)
                }
                return@launch
            }

            // Try podcast episodes
            val episode = podcastDao.getEpisode(sourceId)
            if (episode != null) {
                Log.d(TAG, "Found episode: ${episode.title}")
                loadContent(
                    sourceId = episode.id,
                    title = episode.title,
                    subtitle = episode.description ?: "",
                    artworkUrl = null
                )
                return@launch
            }

            // Try local files
            val localFile = localFileDao.getFile(sourceId)
            if (localFile != null) {
                Log.d(TAG, "Found local file: ${localFile.displayName}")
                loadContent(
                    sourceId = localFile.contentHash,
                    title = localFile.displayName,
                    subtitle = "Local file",
                    artworkUrl = null
                )
                return@launch
            }

            // Fallback: just load chunks without metadata
            Log.d(TAG, "No metadata found, loading with sourceId only")
            loadContent(
                sourceId = sourceId,
                title = "Unknown",
                subtitle = "",
                artworkUrl = null
            )
        }
    }

    fun togglePlayPause() {
        Log.d(TAG, "togglePlayPause()")
        playbackController.togglePlayPause()
    }

    fun nextChunk() {
        Log.d(TAG, "nextChunk()")
        playbackController.nextChunk()
    }

    fun previousChunk() {
        Log.d(TAG, "previousChunk()")
        playbackController.previousChunk()
    }

    fun seekToChunk(index: Int) {
        Log.d(TAG, "seekToChunk($index)")
        playbackController.seekToChunk(index)
    }

    fun setRepeatCount(count: Int) {
        Log.d(TAG, "setRepeatCount($count)")
        val newSettings = playbackState.value.settings.copy(repeatCount = count.coerceIn(1, 5))
        playbackController.updateSettings(newSettings)
    }

    fun setGapRatio(ratio: Float) {
        Log.d(TAG, "setGapRatio($ratio)")
        val newSettings = playbackState.value.settings.copy(gapRatio = ratio.coerceIn(0.2f, 1.0f))
        playbackController.updateSettings(newSettings)
    }

    fun togglePlayMode() {
        Log.d(TAG, "togglePlayMode()")
        val currentSettings = playbackState.value.settings
        val nextMode = when (currentSettings.playMode) {
            PlayMode.NORMAL -> PlayMode.LR
            PlayMode.LR -> PlayMode.LRLR
            PlayMode.LRLR -> PlayMode.NORMAL
        }
        val newSettings = currentSettings.copy(playMode = nextMode)
        playbackController.updateSettings(newSettings)
    }

    fun updateSettings(settings: LearningSettings) {
        Log.d(TAG, "updateSettings()")
        playbackController.updateSettings(settings)
    }

    fun hasRecording(chunkIndex: Int): Boolean {
        return recordings.containsKey(chunkIndex)
    }

    fun saveRecording(chunkIndex: Int, filePath: String) {
        recordings[chunkIndex] = filePath
    }

    fun playRecording(chunkIndex: Int) {
        val filePath = recordings[chunkIndex] ?: return
        // Actual playback implementation would go here
        Log.d(TAG, "playRecording: $filePath")
    }

    fun deleteRecording(chunkIndex: Int) {
        recordings.remove(chunkIndex)
    }

    fun stop() {
        playbackController.stop()
        _chunks.value = emptyList()
        recordings.clear()
        _playlistId.value = null
        _playlistItems.value = emptyList()
        _currentPlaylistItemIndex.value = 0
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Start playback with a playlist context.
     * @param playlistId The playlist ID
     * @param startIndex The index of the item to start with (default 0)
     */
    fun startWithPlaylist(playlistId: Long, startIndex: Int = 0) {
        _playlistId.value = playlistId
        viewModelScope.launch {
            val items = playlistDao.getPlaylistItemsList(playlistId)
            _playlistItems.value = items
            val safeIndex = startIndex.coerceIn(0, maxOf(0, items.size - 1))
            _currentPlaylistItemIndex.value = safeIndex
            if (items.isNotEmpty()) {
                loadPlaylistItem(items[safeIndex])
            }
        }
    }

    /**
     * Navigate to the next item in the playlist.
     */
    fun nextPlaylistItem() {
        val items = _playlistItems.value
        val currentIndex = _currentPlaylistItemIndex.value
        if (currentIndex < items.size - 1) {
            val nextIndex = currentIndex + 1
            _currentPlaylistItemIndex.value = nextIndex
            viewModelScope.launch {
                loadPlaylistItem(items[nextIndex])
            }
        }
    }

    /**
     * Navigate to the previous item in the playlist.
     */
    fun previousPlaylistItem() {
        val items = _playlistItems.value
        val currentIndex = _currentPlaylistItemIndex.value
        if (currentIndex > 0) {
            val prevIndex = currentIndex - 1
            _currentPlaylistItemIndex.value = prevIndex
            viewModelScope.launch {
                loadPlaylistItem(items[prevIndex])
            }
        }
    }

    /**
     * Load content for a playlist item based on its source type.
     */
    private suspend fun loadPlaylistItem(item: PlaylistItemEntity) {
        // Clear previous recordings when switching items
        recordings.clear()

        when (item.sourceType) {
            "PODCAST_EPISODE" -> {
                val episode = podcastDao.getEpisode(item.sourceId)
                if (episode != null) {
                    loadContent(
                        sourceId = episode.id,
                        title = episode.title,
                        subtitle = episode.description ?: "",
                        artworkUrl = null
                    )
                }
            }
            "LOCAL_FILE" -> {
                val file = localFileDao.getFile(item.sourceId)
                if (file != null) {
                    loadContent(
                        sourceId = file.contentHash,
                        title = file.displayName,
                        subtitle = "Local file",
                        artworkUrl = null
                    )
                }
            }
        }
    }

    /**
     * Check if there is a next item in the playlist.
     */
    fun hasNextPlaylistItem(): Boolean {
        return _currentPlaylistItemIndex.value < _playlistItems.value.size - 1
    }

    /**
     * Check if there is a previous item in the playlist.
     */
    fun hasPreviousPlaylistItem(): Boolean {
        return _currentPlaylistItemIndex.value > 0
    }

    /**
     * Check if currently playing from a playlist.
     */
    fun isInPlaylistMode(): Boolean {
        return _playlistId.value != null
    }
}
