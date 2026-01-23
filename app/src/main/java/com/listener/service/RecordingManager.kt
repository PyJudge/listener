package com.listener.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Functional interface for checking available storage space.
 * Allows injection of mock implementation for testing.
 */
fun interface StorageChecker {
    fun getAvailableBytes(): Long
}

/**
 * RecordingManager - 녹음 관리자
 *
 * Android WakeLock 베스트 프랙티스 적용:
 * - PARTIAL_WAKE_LOCK으로 화면 꺼짐 시에도 CPU 유지
 * - try/finally 패턴으로 안전한 해제
 * - 하드코딩된 태그 사용 (Proguard 호환)
 *
 * 동시성 보호:
 * - Mutex로 startRecording/stopRecording 직렬화
 * - 동시 녹음 요청 시 파일 손상 방지
 *
 * @see https://developer.android.com/develop/background-work/background-tasks/awake/wakelock/best-practices
 */
@Singleton
class RecordingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // A2: 저장공간 체크를 위한 인터페이스 (테스트 시 override 가능)
    // 기본값: StatFs를 사용한 실제 저장공간 체크
    internal var storageChecker: StorageChecker = StorageChecker {
        try {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.availableBytes
        } catch (e: Exception) {
            0L
        }
    }

    // 테스트용 생성자
    internal constructor(
        context: Context,
        storageChecker: StorageChecker
    ) : this(context) {
        this.storageChecker = storageChecker
    }
    companion object {
        private const val TAG = "RecordingManager"
        private const val WAKELOCK_TAG = "com.listener.service.RecordingManager:Recording"
        private const val WAKELOCK_TIMEOUT_MS = 10 * 60 * 1000L  // 10분 타임아웃

        // A2: 녹음에 필요한 최소 저장공간 (10MB)
        const val MIN_STORAGE_BYTES = 10L * 1024 * 1024
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var isRecording = false
    private var isPaused = false  // B2: 녹음 일시정지 상태

    // BUG-H3 Fix: Mutex로 동시성 보호
    private val recordingMutex = Mutex()

    // BUG-C2 Fix: WakeLock으로 화면 꺼짐 시 CPU 유지
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * A2: 저장공간 체크
     *
     * @return 녹음에 필요한 최소 저장공간(10MB) 이상 있으면 true
     */
    fun hasEnoughStorage(): Boolean {
        val availableBytes = storageChecker.getAvailableBytes()
        val hasEnough = availableBytes >= MIN_STORAGE_BYTES
        if (!hasEnough) {
            Log.w(TAG, "hasEnoughStorage: Insufficient storage. Available: ${availableBytes / 1024 / 1024}MB, Required: ${MIN_STORAGE_BYTES / 1024 / 1024}MB")
        }
        return hasEnough
    }

    fun getRecordingFile(sourceId: String, chunkIndex: Int): File {
        val dir = File(context.filesDir, "recordings/$sourceId")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "chunk_$chunkIndex.m4a")
    }

    /**
     * 녹음 시작
     *
     * BUG-C2 Fix: WakeLock 획득하여 화면 꺼짐 시에도 CPU 유지
     * BUG-H3 Fix: Mutex로 동시 요청 직렬화
     *
     * @param sourceId 소스 ID
     * @param chunkIndex 청크 인덱스
     * @return 녹음 시작 성공 여부
     */
    suspend fun startRecording(sourceId: String, chunkIndex: Int): Boolean {
        // A2: 저장공간 체크 (fail fast)
        if (!hasEnoughStorage()) {
            Log.w(TAG, "startRecording: Insufficient storage space")
            return false
        }

        // 권한이 없으면 녹음 시도하지 않음
        if (!hasRecordPermission()) {
            Log.w(TAG, "startRecording: No RECORD_AUDIO permission")
            return false
        }

        // BUG-H3 Fix: Mutex로 동시성 보호
        return recordingMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    // 이전 녹음이 진행 중이면 먼저 정리
                    if (isRecording) {
                        Log.d(TAG, "startRecording: Cleaning up previous recording")
                        try {
                            mediaRecorder?.stop()
                        } catch (e: Exception) {
                            // stop() 실패해도 release는 해야 함
                        }
                        mediaRecorder?.release()
                        mediaRecorder = null
                        isRecording = false
                        releaseWakeLockSafely()
                    }

                    val file = getRecordingFile(sourceId, chunkIndex)
                    if (file.exists()) file.delete()

                    currentFilePath = file.absolutePath

                    // BUG-C2 Fix: WakeLock 획득 (녹음 시작 전)
                    acquireWakeLock()

                    mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(context)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioSamplingRate(22050)
                        setAudioEncodingBitRate(64000)
                        setAudioChannels(1)
                        setOutputFile(currentFilePath)
                        prepare()
                        start()
                    }

                    isRecording = true
                    Log.d(TAG, "startRecording: Started recording to $currentFilePath")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "startRecording: Failed", e)
                    // 에러 발생 시에도 정리
                    mediaRecorder?.release()
                    mediaRecorder = null
                    isRecording = false
                    releaseWakeLockSafely()  // BUG-C2 Fix: 실패 시 WakeLock 해제
                    false
                }
            }
        }
    }

    /**
     * 녹음 중지
     *
     * BUG-C2 Fix: WakeLock 해제 (try/finally 패턴)
     * BUG-H3 Fix: Mutex로 동시 요청 직렬화
     *
     * @return 녹음 파일 경로 (실패 시 null)
     */
    suspend fun stopRecording(): String? {
        // BUG-H3 Fix: Mutex로 동시성 보호
        return recordingMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                    mediaRecorder = null
                    isRecording = false
                    isPaused = false  // B2: 일시정지 상태 리셋
                    Log.d(TAG, "stopRecording: Stopped recording, file: $currentFilePath")
                    currentFilePath
                } catch (e: Exception) {
                    Log.e(TAG, "stopRecording: Failed", e)
                    mediaRecorder?.release()
                    mediaRecorder = null
                    isRecording = false
                    isPaused = false  // B2: 일시정지 상태 리셋
                    null
                } finally {
                    // BUG-C2 Fix: 항상 WakeLock 해제 (try/finally 패턴)
                    releaseWakeLockSafely()
                }
            }
        }
    }

    /**
     * 강제 녹음 중지 (onDestroy용)
     *
     * BUG-H4 Fix: 서비스 종료 시 안전한 정리
     * Mutex 없이 동기적으로 실행 (onDestroy에서 호출)
     */
    fun forceStopRecording() {
        try {
            if (isRecording || isPaused) {  // B2: 일시정지 상태도 체크
                Log.d(TAG, "forceStopRecording: Force stopping recording")
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "forceStopRecording: stop() failed", e)
                    }
                    release()
                }
                mediaRecorder = null
                isRecording = false
                isPaused = false  // B2: 일시정지 상태 리셋
            }
        } catch (e: Exception) {
            Log.e(TAG, "forceStopRecording: Failed", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            isPaused = false  // B2: 일시정지 상태 리셋
        } finally {
            releaseWakeLockSafely()
        }
    }

    /**
     * WakeLock 획득
     *
     * Android 베스트 프랙티스:
     * - PARTIAL_WAKE_LOCK 사용 (화면 꺼도 CPU 유지)
     * - 타임아웃 설정 (10분)
     * - 하드코딩된 태그 사용
     */
    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    WAKELOCK_TAG
                )
            }
            wakeLock?.let { lock ->
                if (!lock.isHeld) {
                    lock.acquire(WAKELOCK_TIMEOUT_MS)
                    Log.d(TAG, "acquireWakeLock: WakeLock acquired with ${WAKELOCK_TIMEOUT_MS}ms timeout")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "acquireWakeLock: Failed to acquire WakeLock", e)
        }
    }

    /**
     * WakeLock 안전하게 해제
     *
     * Android 베스트 프랙티스:
     * - isHeld 체크 후 해제
     * - 예외 처리로 안전성 확보
     */
    private fun releaseWakeLockSafely() {
        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Log.d(TAG, "releaseWakeLockSafely: WakeLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "releaseWakeLockSafely: Failed to release WakeLock", e)
        }
    }

    fun hasRecording(sourceId: String, chunkIndex: Int): Boolean {
        return getRecordingFile(sourceId, chunkIndex).exists()
    }

    fun getRecordingPath(sourceId: String, chunkIndex: Int): String? {
        val file = getRecordingFile(sourceId, chunkIndex)
        return if (file.exists()) file.absolutePath else null
    }

    suspend fun deleteRecording(sourceId: String, chunkIndex: Int): Boolean {
        return withContext(Dispatchers.IO) {
            getRecordingFile(sourceId, chunkIndex).delete()
        }
    }

    suspend fun deleteAllRecordings(sourceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "recordings/$sourceId")
            dir.deleteRecursively()
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    /**
     * B2: 녹음 일시정지 상태 확인
     */
    fun isPaused(): Boolean = isPaused

    /**
     * B2: MediaRecorder.pause() 지원 여부 (API 24+)
     */
    fun supportsRecordingPause(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    /**
     * B2: 녹음 일시정지
     *
     * @return 성공 여부 (API < 24 또는 녹음 중 아니면 false)
     */
    fun pauseRecording(): Boolean {
        if (!supportsRecordingPause()) {
            Log.w(TAG, "pauseRecording: API ${Build.VERSION.SDK_INT} < 24, pause not supported")
            return false
        }
        if (!isRecording || isPaused) {
            Log.w(TAG, "pauseRecording: Not recording or already paused")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                isPaused = true
                Log.d(TAG, "pauseRecording: Recording paused")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "pauseRecording: Failed", e)
            false
        }
    }

    /**
     * B2: 녹음 재개
     *
     * @return 성공 여부 (API < 24 또는 일시정지 중 아니면 false)
     */
    fun resumeRecording(): Boolean {
        if (!supportsRecordingPause()) {
            Log.w(TAG, "resumeRecording: API ${Build.VERSION.SDK_INT} < 24, resume not supported")
            return false
        }
        if (!isPaused) {
            Log.w(TAG, "resumeRecording: Not paused")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                isPaused = false
                Log.d(TAG, "resumeRecording: Recording resumed")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "resumeRecording: Failed", e)
            false
        }
    }
}
