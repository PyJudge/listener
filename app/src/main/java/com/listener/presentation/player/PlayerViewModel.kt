package com.listener.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.entity.PlaylistItemEntity
import com.listener.domain.model.Chunk
import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import com.listener.domain.model.PlaybackState
import com.listener.domain.repository.TranscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository,
    private val playlistDao: PlaylistDao,
    private val podcastDao: PodcastDao,
    private val localFileDao: LocalFileDao,
    private val recentLearningDao: RecentLearningDao
) : ViewModel() {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

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

    fun loadContent(
        sourceId: String,
        title: String,
        subtitle: String,
        artworkUrl: String? = null
    ) {
        viewModelScope.launch {
            val loadedChunks = transcriptionRepository.getChunks(sourceId)
            _chunks.value = loadedChunks

            _playbackState.update {
                PlaybackState(
                    sourceId = sourceId,
                    title = title,
                    subtitle = subtitle,
                    artworkUrl = artworkUrl,
                    totalChunks = loadedChunks.size,
                    currentChunkIndex = 0,
                    learningState = LearningState.Idle,
                    isPlaying = false
                )
            }
        }
    }

    fun observeChunks(sourceId: String) {
        viewModelScope.launch {
            transcriptionRepository.observeChunks(sourceId).collect { loadedChunks ->
                _chunks.value = loadedChunks
                _playbackState.update { it.copy(totalChunks = loadedChunks.size) }
            }
        }
    }

    /**
     * Load content by sourceId, fetching metadata from recent learnings or searching in podcasts/local files.
     */
    fun loadBySourceId(sourceId: String) {
        viewModelScope.launch {
            // First try to find in recent learnings for quick metadata
            val recentLearning = recentLearningDao.getRecentLearning(sourceId)
            if (recentLearning != null) {
                loadContent(
                    sourceId = recentLearning.sourceId,
                    title = recentLearning.title,
                    subtitle = recentLearning.subtitle,
                    artworkUrl = recentLearning.thumbnailUrl
                )
                // Seek to the last position
                _playbackState.update { it.copy(currentChunkIndex = recentLearning.currentChunkIndex) }
                return@launch
            }

            // Try podcast episodes
            val episode = podcastDao.getEpisode(sourceId)
            if (episode != null) {
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
                loadContent(
                    sourceId = localFile.contentHash,
                    title = localFile.displayName,
                    subtitle = "Local file",
                    artworkUrl = null
                )
                return@launch
            }

            // Fallback: just load chunks without metadata
            loadContent(
                sourceId = sourceId,
                title = "Unknown",
                subtitle = "",
                artworkUrl = null
            )
        }
    }

    fun togglePlayPause() {
        _playbackState.update { state ->
            state.copy(
                isPlaying = !state.isPlaying,
                learningState = if (state.isPlaying) LearningState.Paused else LearningState.Playing
            )
        }
    }

    fun nextChunk() {
        _playbackState.update { state ->
            val nextIndex = (state.currentChunkIndex + 1).coerceAtMost(state.totalChunks - 1)
            state.copy(currentChunkIndex = nextIndex)
        }
    }

    fun previousChunk() {
        _playbackState.update { state ->
            val prevIndex = (state.currentChunkIndex - 1).coerceAtLeast(0)
            state.copy(currentChunkIndex = prevIndex)
        }
    }

    fun seekToChunk(index: Int) {
        _playbackState.update { state ->
            val clampedIndex = index.coerceIn(0, maxOf(0, state.totalChunks - 1))
            state.copy(currentChunkIndex = clampedIndex)
        }
    }

    fun setRepeatCount(count: Int) {
        _playbackState.update { state ->
            state.copy(
                settings = state.settings.copy(repeatCount = count.coerceIn(1, 5))
            )
        }
    }

    fun setGapRatio(ratio: Float) {
        _playbackState.update { state ->
            state.copy(
                settings = state.settings.copy(gapRatio = ratio.coerceIn(0.2f, 1.0f))
            )
        }
    }

    fun toggleRecording() {
        _playbackState.update { state ->
            if (state.settings.isHardMode) return@update state
            state.copy(
                settings = state.settings.copy(isRecordingEnabled = !state.settings.isRecordingEnabled)
            )
        }
    }

    fun toggleHardMode() {
        _playbackState.update { state ->
            val newHardMode = !state.settings.isHardMode
            state.copy(
                settings = state.settings.copy(
                    isHardMode = newHardMode,
                    // Hard mode always has recording enabled
                    isRecordingEnabled = if (newHardMode) true else state.settings.isRecordingEnabled
                )
            )
        }
    }

    fun updateSettings(settings: LearningSettings) {
        _playbackState.update { state ->
            state.copy(settings = settings)
        }
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
        _playbackState.update { it.copy(learningState = LearningState.PlaybackRecording) }
    }

    fun deleteRecording(chunkIndex: Int) {
        recordings.remove(chunkIndex)
    }

    fun stop() {
        _playbackState.update { PlaybackState() }
        _chunks.value = emptyList()
        recordings.clear()
        _playlistId.value = null
        _playlistItems.value = emptyList()
        _currentPlaylistItemIndex.value = 0
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
