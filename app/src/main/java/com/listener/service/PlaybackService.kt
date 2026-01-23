package com.listener.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
import com.listener.domain.model.PlayMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private var userRecordingJob: Job? = null  // 녹음 재생 Job 추적

    // B1: Gap 타이머 시작 시간 및 전체 시간 (pause/resume용)
    private var gapStartTimeMs: Long = 0L
    private var gapTotalDurationMs: Long = 0L

    // Navigation mutex to serialize chunk navigation operations
    // Prevents race conditions when rapidly calling nextChunk/previousChunk/seekToChunk
    private val navigationMutex = Mutex()

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

        // C1: 오디오 포커스 설정 (ExoPlayer Best Practice)
        // https://medium.com/google-exoplayer/easy-audio-focus-with-exoplayer-a2dcbbe4640e
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)  // 학습용 음성 콘텐츠
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)  // true = 오디오 포커스 자동 관리
            .setHandleAudioBecomingNoisy(true)          // 이어폰 뽑힘 시 자동 일시정지
            .build()
            .apply {
                addListener(playerListener)
            }

        // C2: MediaSession 콜백 설정 (알림 컨트롤 지원)
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(mediaSessionCallback)
            .build()
    }

    /**
     * C2: MediaSession 콜백 - 알림/잠금화면 컨트롤 처리
     *
     * Media3 MediaSession.Callback은 다음을 처리:
     * - onPlaybackResumption: 재생 재개 시 호출
     * - onMediaButtonEvent: 미디어 버튼 이벤트 처리
     *
     * 참고: 기본적인 play/pause는 Player가 자동 처리
     * skip next/previous는 Player.Commands에 포함되면 자동 처리됨
     */
    private val mediaSessionCallback = object : MediaSession.Callback {
        // 기본 구현 사용 (play/pause 자동 처리)
        // 추가 커스텀 동작이 필요하면 여기에 오버라이드
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        Log.d("PlaybackService", "onDestroy: Cleaning up service")

        // A3: 진행 상황 저장 (serviceScope 취소 전에 실행)
        saveProgressSync()

        // 모든 진행 중인 작업 취소
        gapJob?.cancel()
        recordingJob?.cancel()
        userRecordingJob?.cancel()
        stopPositionMonitoring()

        // BUG-H4 Fix: 녹음 중이면 강제 중지 및 정리
        // forceStopRecording()은 동기적으로 실행되어 onDestroy 완료 전 정리됨
        recordingManager.forceStopRecording()

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
                artworkUrl = artworkUrl,
                error = null,  // A4: 새 콘텐츠 로드 시 에러 초기화
                contentComplete = false  // D1: 새 콘텐츠 로드 시 완료 플래그 리셋
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
        // B1: Gap 타이머 중이면 남은 시간 계산
        val remainingGapTime = if (gapJob?.isActive == true && gapStartTimeMs > 0) {
            val elapsed = System.currentTimeMillis() - gapStartTimeMs
            maxOf(0L, gapTotalDurationMs - elapsed)
        } else {
            0L
        }

        gapJob?.cancel()
        recordingJob?.cancel()
        stopPositionMonitoring()

        // 현재 재생 위치 저장 (HIGH 버그 수정)
        val currentPosition = player?.currentPosition ?: 0L
        player?.pause()

        // B1: Gap 상태면 남은 시간 저장, 아니면 재생 위치 저장
        val isInGap = stateMachine.state.value == LearningState.Gap ||
                      stateMachine.state.value == LearningState.GapWithRecording
        if (isInGap) {
            stateMachine.pauseWithGapTime(remainingGapTime)
        } else {
            stateMachine.pause(currentPosition)
        }

        updatePlaybackState { copy(isPlaying = false, learningState = LearningState.Paused) }
    }

    fun resume() {
        // B1: Gap 정보와 함께 재개
        val resumeResult = stateMachine.resumeWithGapInfo()

        when (stateMachine.state.value) {
            is LearningState.Gap, is LearningState.GapWithRecording -> {
                // B1: 남은 Gap 시간이 있으면 해당 시간부터 타이머 시작
                val remainingTime = if (resumeResult.wasInGap && resumeResult.remainingGapTimeMs > 0) {
                    resumeResult.remainingGapTimeMs
                } else {
                    null  // 처음부터 시작
                }
                startGapTimer(remainingTime)
            }
            else -> {
                val resumePosition = stateMachine.getPausedPositionMs()
                // BUG-H2 Fix: 청크 변경 후 resume 시 위치 검증
                // 저장된 위치가 현재 청크 범위 내에 있는지 확인
                val currentChunk = chunks.getOrNull(currentChunkIndex)
                val validPosition = if (currentChunk != null && resumePosition > 0) {
                    // 위치가 현재 청크 범위 내에 있는지 검증
                    if (resumePosition >= currentChunk.startMs && resumePosition <= currentChunk.endMs) {
                        resumePosition
                    } else {
                        // 범위 밖이면 청크 시작점부터 (PR-7 요구사항)
                        Log.d("PlaybackService", "resume: Position $resumePosition out of chunk bounds [${currentChunk.startMs}, ${currentChunk.endMs}], starting from beginning")
                        currentChunk.startMs
                    }
                } else {
                    currentChunk?.startMs ?: 0L
                }

                if (validPosition > 0) {
                    player?.seekTo(validPosition)
                }
                player?.play()
                startPositionMonitoring()
            }
        }
        updatePlaybackState { copy(isPlaying = true, learningState = stateMachine.state.value) }
    }

    fun nextChunk() {
        serviceScope.launch {
            navigationMutex.withLock {
                if (currentChunkIndex < chunks.size - 1) {
                    cancelPendingOperations()
                    currentChunkIndex++
                    stateMachine.onChunkChanged(currentChunkIndex)  // 반복 카운터 리셋
                    stateMachine.stop()
                    stateMachine.play()
                    playCurrentChunk()
                    updatePlaybackState {
                        copy(
                            currentChunkIndex = currentChunkIndex,
                            learningState = stateMachine.state.value
                        )
                    }
                    saveProgress()
                }
            }
        }
    }

    fun previousChunk() {
        serviceScope.launch {
            navigationMutex.withLock {
                if (currentChunkIndex > 0) {
                    cancelPendingOperations()
                    currentChunkIndex--
                    stateMachine.onChunkChanged(currentChunkIndex)  // 반복 카운터 리셋
                    stateMachine.stop()
                    stateMachine.play()
                    playCurrentChunk()
                    updatePlaybackState {
                        copy(
                            currentChunkIndex = currentChunkIndex,
                            learningState = stateMachine.state.value
                        )
                    }
                    saveProgress()
                }
            }
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
        serviceScope.launch {
            navigationMutex.withLock {
                if (index in chunks.indices) {
                    cancelPendingOperations()
                    currentChunkIndex = index
                    stateMachine.onChunkChanged(currentChunkIndex)  // 반복 카운터 리셋
                    stateMachine.stop()
                    stateMachine.play()
                    playCurrentChunk()
                    updatePlaybackState {
                        copy(
                            currentChunkIndex = currentChunkIndex,
                            learningState = stateMachine.state.value
                        )
                    }
                    saveProgress()
                }
            }
        }
    }

    fun updateSettings(settings: LearningSettings) {
        val oldMode = stateMachine.getSettings().playMode
        val newMode = settings.playMode

        // LRLR에서 다른 모드로 전환 시 녹음 관련 작업 취소
        if (oldMode == PlayMode.LRLR && newMode != PlayMode.LRLR) {
            serviceScope.launch {
                recordingJob?.cancel()
                recordingJob = null
                userRecordingJob?.cancel()
                userRecordingJob = null
                if (recordingManager.isCurrentlyRecording()) {
                    recordingManager.stopRecording()
                    recordingManager.deleteRecording(sourceId, currentChunkIndex)
                }
            }
        }

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
     * 청크 전환 시 진행 중인 모든 비동기 작업 취소 및 대기
     * - Gap 타이머 취소
     * - 녹음+Gap 타이머 취소 + 완료 대기 (join)
     * - 녹음 재생 Job 취소 + 완료 대기 (join)
     * - 진행 중인 녹음 중지 (동기적 대기)
     * - 위치 모니터링 중지
     *
     * CRITICAL: recordingJob?.join()을 통해 취소된 Job이 완전히 종료될 때까지 대기
     * 이를 통해 경합 조건 방지 (5-10% 확률로 발생하던 녹음 저장 오류 해결)
     *
     * BUG-C4 Fix: PlaybackController에서 호출 가능하도록 suspend fun으로 노출
     */
    suspend fun cancelPendingOperations() {
        // Gap 타이머 취소
        gapJob?.cancel()
        gapJob = null

        // 녹음+Gap 타이머 취소 후 완료 대기 (CRITICAL: join으로 경합 방지)
        recordingJob?.let { job ->
            job.cancel()
            try {
                job.join()  // Job이 완전히 종료될 때까지 대기
            } catch (e: CancellationException) {
                // 정상 취소 - 무시
            }
        }
        recordingJob = null

        // 녹음 재생 Job 취소 후 완료 대기
        userRecordingJob?.let { job ->
            job.cancel()
            try {
                job.join()
            } catch (e: CancellationException) {
                // 정상 취소 - 무시
            }
        }
        userRecordingJob = null

        // 위치 모니터링 중지
        stopPositionMonitoring()

        // 진행 중인 녹음 동기적으로 중지 대기
        if (recordingManager.isCurrentlyRecording()) {
            recordingManager.stopRecording()  // suspend - 완료까지 대기
            // 불완전 녹음 삭제
            recordingManager.deleteRecording(sourceId, currentChunkIndex)
        }
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
                    updatePlaybackState { copy(currentChunkIndex = newIndex, contentComplete = false) }
                    saveProgress()
                } else {
                    // D1: End of content - 플레이리스트 자동 전환을 위해 contentComplete 설정
                    stateMachine.stop()
                    updatePlaybackState {
                        copy(
                            isPlaying = false,
                            learningState = LearningState.Idle,
                            contentComplete = true  // D1: 콘텐츠 완료 시그널
                        )
                    }
                    saveProgress()
                    Log.d("PlaybackService", "Content complete for sourceId=$sourceId")
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

    /**
     * B1: Gap 타이머 시작
     * @param remainingTimeMs 남은 시간 (null이면 전체 시간으로 시작)
     */
    private fun startGapTimer(remainingTimeMs: Long? = null) {
        gapJob?.cancel()  // 기존 타이머 취소 (중복 실행 방지)

        val chunk = chunks.getOrNull(currentChunkIndex) ?: return
        val fullGapDuration = (chunk.durationMs * (1 + stateMachine.getSettings().gapRatio)).toLong()
        val gapDuration = remainingTimeMs ?: fullGapDuration

        // B1: 타이머 시작 시간 및 전체 시간 기록
        gapStartTimeMs = System.currentTimeMillis()
        gapTotalDurationMs = gapDuration

        Log.d("PlaybackService", "startGapTimer: duration=${gapDuration}ms (remaining=${remainingTimeMs != null})")

        gapJob = serviceScope.launch {
            delay(gapDuration)
            onChunkPlaybackComplete()
        }
    }

    private fun startRecordingWithGap() {
        val chunk = chunks.getOrNull(currentChunkIndex) ?: return
        val gapDuration = (chunk.durationMs * (1 + stateMachine.getSettings().gapRatio)).toLong()
        val targetChunkIndex = currentChunkIndex  // 현재 청크 인덱스 캡처

        // Job 저장하여 청크 전환 시 취소 가능하도록 함 (CRITICAL 버그 수정)
        recordingJob = serviceScope.launch {
            val success = recordingManager.startRecording(sourceId, targetChunkIndex)
            if (!success) {
                // 녹음 실패 시 불완전 파일 삭제 (MEDIUM 버그 수정)
                recordingManager.deleteRecording(sourceId, targetChunkIndex)
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
            // 청크가 변경되지 않았을 때만 완료 처리
            if (currentChunkIndex == targetChunkIndex) {
                onChunkPlaybackComplete()
            }
        }
    }

    private fun playUserRecording() {
        val recordingPath = recordingManager.getRecordingPath(sourceId, currentChunkIndex)
        if (recordingPath != null) {
            Log.d("PlaybackService", "playUserRecording: $recordingPath")
            val originalAudioUri = audioUri
            val targetChunkIndex = currentChunkIndex  // 현재 청크 캡처

            player?.setMediaItem(MediaItem.fromUri(recordingPath))
            player?.prepare()

            userRecordingJob = serviceScope.launch {
                try {
                    // Wait for player to be ready (prepare is async)
                    var waitCount = 0
                    while (player?.playbackState != Player.STATE_READY && waitCount < 100) {
                        delay(50)
                        waitCount++
                    }

                    if (player?.playbackState == Player.STATE_READY) {
                        player?.play()

                        // Wait for playback to start
                        var startWaitCount = 0
                        while (player?.isPlaying != true && startWaitCount < 50) {
                            delay(20)
                            startWaitCount++
                        }

                        // Wait for playback to complete, checking for chunk change
                        while (player?.isPlaying == true) {
                            delay(50)
                            // 청크 변경 체크
                            if (currentChunkIndex != targetChunkIndex) {
                                Log.d("PlaybackService", "Chunk changed during recording playback, aborting")
                                return@launch
                            }
                        }
                    }

                    // 같은 청크일 때만 원본 미디어 복원
                    if (currentChunkIndex == targetChunkIndex) {
                        player?.setMediaItem(MediaItem.fromUri(originalAudioUri))
                        player?.prepare()
                        onChunkPlaybackComplete()
                    }
                } finally {
                    userRecordingJob = null
                }
            }
        } else {
            Log.d("PlaybackService", "playUserRecording: no recording found")
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
            // A4: 에러 메시지를 PlaybackState에 저장하여 UI에 표시
            val errorMessage = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "오디오 파일을 찾을 수 없습니다"
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "네트워크 연결에 실패했습니다"
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "네트워크 연결 시간이 초과되었습니다"
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "오디오 디코더 초기화에 실패했습니다"
                PlaybackException.ERROR_CODE_DECODING_FAILED -> "오디오 파일이 손상되었거나 지원하지 않는 형식입니다"
                else -> "오디오 재생 오류: ${error.message ?: "알 수 없는 오류"}"
            }
            Log.e("PlaybackService", "onPlayerError: $errorMessage (code=${error.errorCode})", error)
            updatePlaybackState { copy(isPlaying = false, error = errorMessage) }
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

    /**
     * A3: 동기적으로 진행 상황 저장 (onDestroy에서 사용)
     *
     * onDestroy에서 serviceScope 취소 전에 호출하여
     * 재생 진행 상황이 확실히 저장되도록 함
     */
    private fun saveProgressSync() {
        val state = _playbackState.value
        if (state.sourceId.isEmpty()) {
            Log.d("PlaybackService", "saveProgressSync: No content to save")
            return
        }

        try {
            runBlocking(Dispatchers.IO) {
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
                Log.d("PlaybackService", "saveProgressSync: Saved progress - sourceId=${state.sourceId}, chunkIndex=${state.currentChunkIndex}")
            }
        } catch (e: Exception) {
            Log.e("PlaybackService", "saveProgressSync: Failed to save progress", e)
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
