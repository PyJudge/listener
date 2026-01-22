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
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.entity.RecentLearningEntity
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

    @Inject
    lateinit var recentLearningDao: RecentLearningDao

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
    private var recordingJob: Job? = null  // startRecordingWithGap의 Job 추적

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // Handler-based position monitoring (replaces busy-loop polling)
    private val positionHandler = Handler(Looper.getMainLooper())
    private var currentChunkEndMs: Long = 0L
    private var isMonitoringActive = false
    private val positionMonitorRunnable = object : Runnable {
        override fun run() {
            if (!isMonitoringActive) return

            val currentPosition = player?.currentPosition ?: 0L
            val isPlaying = player?.isPlaying == true

            if (currentPosition >= currentChunkEndMs && isPlaying) {
                player?.pause()
                onChunkPlaybackComplete()
            } else {
                // 버퍼링 중이어도 계속 모니터링
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

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        // 모든 진행 중인 작업 취소
        gapJob?.cancel()
        recordingJob?.cancel()
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
        recordingJob?.cancel()
        stopPositionMonitoring()

        // 현재 재생 위치 저장 (HIGH 버그 수정)
        val currentPosition = player?.currentPosition ?: 0L
        player?.pause()
        stateMachine.pause(currentPosition)

        updatePlaybackState { copy(isPlaying = false, learningState = LearningState.Paused) }
    }

    fun resume() {
        val resumePosition = stateMachine.resume()
        when (stateMachine.state.value) {
            is LearningState.Gap, is LearningState.GapWithRecording -> startGapTimer()
            else -> {
                // 저장된 위치로 seek 후 재생 (HIGH 버그 수정)
                if (resumePosition > 0) {
                    player?.seekTo(resumePosition)
                }
                player?.play()
                startPositionMonitoring()
            }
        }
        updatePlaybackState { copy(isPlaying = true, learningState = stateMachine.state.value) }
    }

    fun nextChunk() {
        if (currentChunkIndex < chunks.size - 1) {
            // 진행 중인 Gap/Recording 작업 취소
            cancelPendingOperations()

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
            saveProgress()
        }
    }

    fun previousChunk() {
        if (currentChunkIndex > 0) {
            // 진행 중인 Gap/Recording 작업 취소
            cancelPendingOperations()

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
            saveProgress()
        }
    }

    fun updateChunks(newChunks: List<Chunk>) {
        this.chunks = newChunks
        // 현재 인덱스가 범위를 벗어나면 조정
        if (currentChunkIndex >= newChunks.size) {
            currentChunkIndex = maxOf(0, newChunks.size - 1)
        }
        updatePlaybackState {
            copy(
                currentChunkIndex = currentChunkIndex,
                totalChunks = newChunks.size
            )
        }
    }

    fun seekToChunk(index: Int) {
        if (index in chunks.indices) {
            // 진행 중인 Gap/Recording 작업 취소 (CRITICAL 버그 수정)
            cancelPendingOperations()

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
            saveProgress()
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
        isMonitoringActive = true
        positionHandler.postDelayed(positionMonitorRunnable, POSITION_CHECK_INTERVAL_MS)
    }

    private fun stopPositionMonitoring() {
        isMonitoringActive = false
        positionHandler.removeCallbacks(positionMonitorRunnable)
    }

    /**
     * 청크 전환 시 진행 중인 모든 비동기 작업 취소
     * - Gap 타이머 취소
     * - 녹음+Gap 타이머 취소
     * - 진행 중인 녹음 중지
     * - 위치 모니터링 중지
     */
    private fun cancelPendingOperations() {
        // Gap 타이머 취소
        gapJob?.cancel()
        gapJob = null

        // 녹음+Gap 타이머 취소
        recordingJob?.cancel()
        recordingJob = null

        // 진행 중인 녹음 중지
        if (recordingManager.isCurrentlyRecording()) {
            serviceScope.launch {
                recordingManager.stopRecording()
            }
        }

        // 위치 모니터링 중지
        stopPositionMonitoring()
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
                    saveProgress()
                } else {
                    // End of content
                    stateMachine.stop()
                    updatePlaybackState { copy(isPlaying = false, learningState = LearningState.Idle) }
                    saveProgress()
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
        gapJob?.cancel()  // 기존 타이머 취소 (중복 실행 방지)

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

        // Job 저장하여 청크 전환 시 취소 가능하도록 함 (CRITICAL 버그 수정)
        recordingJob = serviceScope.launch {
            val success = recordingManager.startRecording(sourceId, currentChunkIndex)
            if (!success) {
                // 녹음 실패 시 (권한 없음 등) LR 모드로 전환
                val result = stateMachine.onRecordingFailed()
                if (result == TransitionResult.SwitchToLRMode) {
                    // LR 모드로 전환됨 - UI 상태 업데이트
                    updatePlaybackState {
                        copy(
                            learningState = stateMachine.state.value,
                            settings = stateMachine.getSettings()
                        )
                    }
                } else {
                    updatePlaybackState { copy(learningState = stateMachine.state.value) }
                }
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
                Player.STATE_BUFFERING -> { /* buffering */ }
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

    fun hasRecordPermission(): Boolean {
        return recordingManager.hasRecordPermission()
    }

    private fun saveProgress() {
        val state = _playbackState.value
        if (state.sourceId.isEmpty()) return

        serviceScope.launch {
            recentLearningDao.upsertRecentLearning(
                RecentLearningEntity(
                    sourceId = state.sourceId,
                    sourceType = "PODCAST_EPISODE",
                    title = state.title,
                    subtitle = state.subtitle,
                    currentChunkIndex = state.currentChunkIndex,
                    totalChunks = state.totalChunks,
                    thumbnailUrl = state.artworkUrl,
                    lastAccessedAt = System.currentTimeMillis()
                )
            )
        }
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
