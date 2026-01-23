package com.listener.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.listener.domain.model.Chunk
import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import com.listener.domain.model.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.listener.data.repository.TranscriptionRepositoryImpl
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls audio playback by managing the connection to PlaybackService.
 * Acts as a bridge between ViewModels and the actual playback service.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PlaybackController"
    }

    private var playbackService: PlaybackService? = null
    private var isBound = false
    private val mainScope = MainScope()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "Service connected")
            val localBinder = binder as? PlaybackService.LocalBinder
            playbackService = localBinder?.getService()
            isBound = true
            _isServiceConnected.value = true

            // Subscribe to service's playback state
            playbackService?.let { service ->
                mainScope.launch {
                    service.playbackState.collect { state ->
                        _playbackState.value = state
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            playbackService = null
            isBound = false
            _isServiceConnected.value = false
        }
    }

    /**
     * Bind to the PlaybackService. Call this when the app starts or when playback is needed.
     */
    fun bindService() {
        if (isBound) return

        Log.d(TAG, "Binding to PlaybackService")
        val intent = Intent(context, PlaybackService::class.java)
        // MediaSessionService는 bindService만으로 자동 시작됨
        // startService() 제거 - 백그라운드에서 BackgroundServiceStartNotAllowedException 방지
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Unbind from the PlaybackService. Call this when playback is no longer needed.
     */
    fun unbindService() {
        if (!isBound) return

        Log.d(TAG, "Unbinding from PlaybackService")
        context.unbindService(serviceConnection)
        playbackService = null
        isBound = false
        _isServiceConnected.value = false
    }

    /**
     * Set up content for playback.
     * Suspend function that waits for service connection before setting content.
     */
    suspend fun setContent(
        sourceId: String,
        audioUri: String,
        chunks: List<Chunk>,
        settings: LearningSettings,
        title: String = "",
        subtitle: String = "",
        artworkUrl: String? = null
    ) {
        Log.d(TAG, "setContent: sourceId=$sourceId, audioUri=$audioUri, chunks=${chunks.size}")

        // 서비스가 연결될 때까지 대기 (최대 5초)
        if (playbackService == null) {
            Log.d(TAG, "Waiting for service connection...")
            bindService()
            try {
                withTimeout(5000) {
                    isServiceConnected.first { it }
                }
                Log.d(TAG, "Service connected successfully")
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Service connection timeout after 5 seconds")
                return
            }
        }

        playbackService?.setContent(sourceId, audioUri, chunks, settings, title, subtitle, artworkUrl)
    }

    fun play() {
        Log.d(TAG, "play()")
        playbackService?.play()
    }

    fun pause() {
        Log.d(TAG, "pause()")
        playbackService?.pause()
    }

    fun resume() {
        Log.d(TAG, "resume()")
        playbackService?.resume()
    }

    fun togglePlayPause() {
        val state = _playbackState.value
        Log.d(TAG, "togglePlayPause(), isPlaying=${state.isPlaying}, learningState=${state.learningState}")
        if (state.isPlaying) {
            pause()
        } else {
            // Use resume() for Paused state, play() for Idle or other states
            if (state.learningState is LearningState.Paused) {
                resume()
            } else {
                play()
            }
        }
    }

    fun nextChunk() {
        Log.d(TAG, "nextChunk()")
        playbackService?.nextChunk()
    }

    fun previousChunk() {
        Log.d(TAG, "previousChunk()")
        playbackService?.previousChunk()
    }

    fun seekToChunk(index: Int) {
        Log.d(TAG, "seekToChunk($index)")
        playbackService?.seekToChunk(index)
    }

    fun updateSettings(settings: LearningSettings) {
        Log.d(TAG, "updateSettings()")
        playbackService?.updateSettings(settings)
    }

    fun updateChunks(chunks: List<Chunk>) {
        Log.d(TAG, "updateChunks(${chunks.size})")
        playbackService?.updateChunks(chunks)
    }

    fun stop() {
        Log.d(TAG, "stop()")
        playbackService?.let {
            it.pause()
            _playbackState.value = PlaybackState()
        }
    }

    /**
     * Get the cached audio file path for a sourceId.
     */
    fun getAudioFilePath(sourceId: String): String? {
        val cacheDir = File(context.cacheDir, "audio_downloads")
        val fileName = "${TranscriptionRepositoryImpl.safeFileName(sourceId)}.mp3"
        val file = File(cacheDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Check if audio file exists for sourceId.
     */
    fun hasAudioFile(sourceId: String): Boolean {
        return getAudioFilePath(sourceId) != null
    }

    /**
     * Check if the app has RECORD_AUDIO permission.
     */
    fun hasRecordPermission(): Boolean {
        return playbackService?.hasRecordPermission() ?: false
    }

    /**
     * BUG-C4 Fix: 진행 중인 녹음/Gap 작업 취소
     *
     * 플레이리스트 아이템 전환 시 호출하여 녹음 작업을 정리합니다.
     * - 녹음 중이면 녹음 중지 및 파일 삭제
     * - Gap 타이머 취소
     * - 녹음 재생 중지
     */
    suspend fun cancelPendingOperations() {
        Log.d(TAG, "cancelPendingOperations()")
        playbackService?.cancelPendingOperations()
    }
}
