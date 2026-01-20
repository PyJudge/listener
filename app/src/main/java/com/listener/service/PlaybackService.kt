package com.listener.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.listener.domain.model.Chunk
import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import com.listener.domain.model.PlaybackState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var stateMachine: LearningStateMachine

    @Inject
    lateinit var recordingManager: RecordingManager

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Binder for local service binding
    private val binder = LocalBinder()

    inner class LocalBinder : android.os.Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: android.content.Intent?): android.os.IBinder {
        super.onBind(intent)
        return binder
    }

    private var chunks: List<Chunk> = emptyList()
    private var currentChunkIndex = 0
    private var sourceId = ""
    private var audioUri = ""
    private var gapJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // Handler-based position monitoring (replaces busy-loop polling)
    private val positionHandler = Handler(Looper.getMainLooper())
    private var currentChunkEndMs: Long = 0L
    private val positionMonitorRunnable = object : Runnable {
        override fun run() {
            val currentPosition = player?.currentPosition ?: 0L
            if (currentPosition >= currentChunkEndMs && player?.isPlaying == true) {
                player?.pause()
                onChunkPlaybackComplete()
            } else if (player?.isPlaying == true) {
                positionHandler.postDelayed(this, POSITION_CHECK_INTERVAL_MS)
            }
        }
    }

    companion object {
        private const val POSITION_CHECK_INTERVAL_MS = 30L
        const val CHANNEL_ID = "listener_playback"
        const val NOTIFICATION_ID = 1
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        player = ExoPlayer.Builder(this).build().apply {
            addListener(playerListener)
        }

        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        stopPositionMonitoring()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    fun setContent(
        sourceId: String,
        audioUri: String,
        chunks: List<Chunk>,
        settings: LearningSettings,
        title: String = "",
        subtitle: String = "",
        artworkUrl: String? = null
    ) {
        this.sourceId = sourceId
        this.audioUri = audioUri
        this.chunks = chunks
        this.currentChunkIndex = 0
        stateMachine.updateSettings(settings)

        updatePlaybackState {
            copy(
                sourceId = sourceId,
                currentChunkIndex = 0,
                totalChunks = chunks.size,
                settings = settings,
                title = title,
                subtitle = subtitle,
                artworkUrl = artworkUrl
            )
        }

        player?.setMediaItem(MediaItem.fromUri(audioUri))
        player?.prepare()
    }

    fun play() {
        if (chunks.isEmpty()) return
        stateMachine.play()
        playCurrentChunk()
        updatePlaybackState { copy(isPlaying = true, learningState = stateMachine.state.value) }
    }

    fun pause() {
        gapJob?.cancel()
        stopPositionMonitoring()
        player?.pause()
        stateMachine.pause()
        updatePlaybackState { copy(isPlaying = false, learningState = LearningState.Paused) }
    }

    fun resume() {
        stateMachine.resume()
        when (stateMachine.state.value) {
            is LearningState.Gap, is LearningState.GapWithRecording -> startGapTimer()
            else -> player?.play()
        }
        updatePlaybackState { copy(isPlaying = true, learningState = stateMachine.state.value) }
    }

    fun nextChunk() {
        if (currentChunkIndex < chunks.size - 1) {
            currentChunkIndex++
            val newIndex = currentChunkIndex  // 로컬 변수로 캡처
            stateMachine.stop()
            stateMachine.play()
            playCurrentChunk()
            updatePlaybackState {
                copy(
                    currentChunkIndex = newIndex,
                    learningState = stateMachine.state.value
                )
            }
        }
    }

    fun previousChunk() {
        if (currentChunkIndex > 0) {
            currentChunkIndex--
            val newIndex = currentChunkIndex  // 로컬 변수로 캡처
            stateMachine.stop()
            stateMachine.play()
            playCurrentChunk()
            updatePlaybackState {
                copy(
                    currentChunkIndex = newIndex,
                    learningState = stateMachine.state.value
                )
            }
        }
    }

    fun seekToChunk(index: Int) {
        if (index in chunks.indices) {
            currentChunkIndex = index
            val newIndex = currentChunkIndex  // 로컬 변수로 캡처
            stateMachine.stop()
            stateMachine.play()
            playCurrentChunk()
            updatePlaybackState {
                copy(
                    currentChunkIndex = newIndex,
                    learningState = stateMachine.state.value
                )
            }
        }
    }

    fun updateSettings(settings: LearningSettings) {
        stateMachine.updateSettings(settings)
        updatePlaybackState { copy(settings = settings) }
    }

    private fun playCurrentChunk() {
        val chunk = chunks.getOrNull(currentChunkIndex) ?: return
        currentChunkEndMs = chunk.endMs
        player?.seekTo(chunk.startMs)
        player?.play()
        startPositionMonitoring()
    }

    private fun startPositionMonitoring() {
        stopPositionMonitoring()
        positionHandler.postDelayed(positionMonitorRunnable, POSITION_CHECK_INTERVAL_MS)
    }

    private fun stopPositionMonitoring() {
        positionHandler.removeCallbacks(positionMonitorRunnable)
    }

    private fun onChunkPlaybackComplete() {
        val result = stateMachine.onPlaybackComplete()
        updatePlaybackState { copy(learningState = stateMachine.state.value) }

        when (result) {
            is TransitionResult.Continue -> handleContinue()
            is TransitionResult.NextChunk -> {
                if (currentChunkIndex < chunks.size - 1) {
                    currentChunkIndex++
                    val newIndex = currentChunkIndex  // 로컬 변수로 캡처
                    stateMachine.play()
                    playCurrentChunk()
                    updatePlaybackState { copy(currentChunkIndex = newIndex) }
                } else {
                    // End of content
                    stateMachine.stop()
                    updatePlaybackState { copy(isPlaying = false, learningState = LearningState.Idle) }
                }
            }
            else -> {}
        }
    }

    private fun handleContinue() {
        when (stateMachine.state.value) {
            is LearningState.Gap -> startGapTimer()
            is LearningState.Recording -> startRecordingWithGap()
            is LearningState.GapWithRecording -> startRecordingWithGap()
            is LearningState.Playing, is LearningState.PlayingFirst, is LearningState.PlayingSecond -> {
                playCurrentChunk()
            }
            is LearningState.PlaybackRecording -> playUserRecording()
            else -> {}
        }
    }

    private fun startGapTimer() {
        val chunk = chunks.getOrNull(currentChunkIndex) ?: return
        val gapDuration = (chunk.durationMs * (1 + stateMachine.getSettings().gapRatio)).toLong()

        gapJob = serviceScope.launch {
            delay(gapDuration)
            onChunkPlaybackComplete()
        }
    }

    private fun startRecordingWithGap() {
        val chunk = chunks.getOrNull(currentChunkIndex) ?: return
        val gapDuration = (chunk.durationMs * (1 + stateMachine.getSettings().gapRatio)).toLong()

        // ExoPlayer 일시정지 (오디오 포커스 해제)
        player?.pause()

        serviceScope.launch {
            val success = recordingManager.startRecording(sourceId, currentChunkIndex)
            if (!success) {
                // 녹음 실패 시 (권한 없음 등) Gap으로 전환하여 정상 진행
                stateMachine.onRecordingFailed()
                updatePlaybackState { copy(learningState = stateMachine.state.value) }
            }
            delay(gapDuration)
            if (success) {
                recordingManager.stopRecording()
            }
            onChunkPlaybackComplete()
        }
    }

    private fun playUserRecording() {
        val recordingPath = recordingManager.getRecordingPath(sourceId, currentChunkIndex)
        if (recordingPath != null) {
            val currentMedia = player?.currentMediaItem
            player?.setMediaItem(MediaItem.fromUri(recordingPath))
            player?.prepare()
            player?.play()

            serviceScope.launch {
                while (player?.isPlaying == true) {
                    delay(50)
                }
                // Restore original media
                if (currentMedia != null) {
                    player?.setMediaItem(currentMedia)
                    player?.prepare()
                }
                onChunkPlaybackComplete()
            }
        } else {
            onChunkPlaybackComplete()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> updatePlaybackState { copy(isPlaying = false) }
                Player.STATE_READY -> { /* ready */ }
                Player.STATE_ENDED -> {
                    stopPositionMonitoring()
                    updatePlaybackState { copy(isPlaying = false, learningState = LearningState.Idle) }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState { copy(isPlaying = isPlaying) }
        }

        override fun onPlayerError(error: PlaybackException) {
            updatePlaybackState { copy(isPlaying = false) }
        }
    }

    private fun updatePlaybackState(update: PlaybackState.() -> PlaybackState) {
        _playbackState.value = _playbackState.value.update()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audio playback controls"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
