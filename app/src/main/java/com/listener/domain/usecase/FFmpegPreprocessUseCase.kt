package com.listener.domain.usecase

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.StatisticsCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * FFmpeg 기반 오디오 전처리 UseCase
 *
 * Media3 Transformer 대비 10-20x 빠른 처리 속도
 * - 16kHz 모노로 리샘플링
 * - 32kbps AAC 압축
 * - 25MB 초과시 청킹
 */
@Singleton
class FFmpegPreprocessUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FFmpegPreprocess"
        private const val WHISPER_MAX_SIZE_MB = 25
        private const val WHISPER_SAMPLE_RATE = 16000
        private const val TARGET_BITRATE = "32k"
    }

    data class AudioChunk(
        val path: String,
        val startOffsetMs: Long,
        val durationMs: Long
    )

    data class PreprocessResult(
        val chunks: List<AudioChunk>,
        val originalSizeMb: Float,
        val processedSizeMb: Float,
        val skipped: Boolean = false
    )

    /**
     * 전처리가 필요 없는 파일인지 확인
     * - 25MB 미만
     * - 이미 최적화된 포맷 (m4a, aac, mp3)
     */
    fun canSkipPreprocessing(inputFile: File): Boolean {
        val sizeMb = inputFile.length() / (1024f * 1024f)
        val extension = inputFile.extension.lowercase()

        // 이미 작고 호환 가능한 포맷이면 스킵
        if (sizeMb < WHISPER_MAX_SIZE_MB) {
            when (extension) {
                "m4a", "aac" -> return true
                "mp3" -> if (sizeMb < 15) return true // mp3는 좀 더 작아야 스킵
            }
        }
        return false
    }

    /**
     * FFmpeg를 사용한 오디오 전처리 - Media3 Transformer 대비 10-20x 빠름
     */
    suspend fun preprocess(
        inputFile: File,
        onProgress: ((Float) -> Unit)? = null
    ): PreprocessResult = withContext(Dispatchers.IO) {
        val originalSizeMb = inputFile.length() / (1024f * 1024f)
        Log.i(TAG, "Preprocessing audio: ${inputFile.name}, size: ${"%.1f".format(originalSizeMb)}MB")

        // 스킵 가능 여부 체크
        if (canSkipPreprocessing(inputFile)) {
            Log.i(TAG, "Skipping preprocessing - file already optimized")
            onProgress?.invoke(1f)
            return@withContext PreprocessResult(
                chunks = listOf(AudioChunk(
                    path = inputFile.absolutePath,
                    startOffsetMs = 0,
                    durationMs = getAudioDuration(inputFile)
                )),
                originalSizeMb = originalSizeMb,
                processedSizeMb = originalSizeMb,
                skipped = true
            )
        }

        // 출력 디렉토리
        val outputDir = File(context.cacheDir, "preprocessed_audio").apply { mkdirs() }
        val outputFile = File(outputDir, "preprocessed_${UUID.randomUUID().toString().take(8)}.m4a")

        // 입력 파일 길이 (진행률 계산용)
        val inputDurationMs = getAudioDuration(inputFile)

        // FFmpeg 명령어: 16kHz 모노, AAC 32kbps
        // -vn: 비디오 스트림 무시 (MP3 커버 이미지 등)
        val command = buildString {
            append("-i \"${inputFile.absolutePath}\" ")
            append("-vn ")                        // 비디오 무시 (커버 이미지 제외)
            append("-ar $WHISPER_SAMPLE_RATE ")  // Sample rate: 16kHz
            append("-ac 1 ")                      // Mono
            append("-c:a aac ")                   // AAC codec
            append("-b:a $TARGET_BITRATE ")       // Bitrate: 32kbps
            append("-y ")                         // 덮어쓰기
            append("\"${outputFile.absolutePath}\"")
        }

        Log.d(TAG, "FFmpeg command: ffmpeg $command")

        executeFFmpeg(command, inputDurationMs, onProgress)

        if (!outputFile.exists() || outputFile.length() == 0L) {
            throw Exception("FFmpeg processing failed - output file not created")
        }

        val processedSizeMb = outputFile.length() / (1024f * 1024f)
        val compressionRatio = if (originalSizeMb > 0) (1 - processedSizeMb / originalSizeMb) * 100 else 0f
        Log.i(TAG, "Preprocessed: ${"%.1f".format(processedSizeMb)}MB (compression: ${"%.1f".format(compressionRatio)}%)")

        // 25MB 초과시 청킹
        val chunks = if (processedSizeMb > WHISPER_MAX_SIZE_MB) {
            chunkAudio(outputFile)
        } else {
            listOf(AudioChunk(
                path = outputFile.absolutePath,
                startOffsetMs = 0,
                durationMs = getAudioDuration(outputFile)
            ))
        }

        PreprocessResult(
            chunks = chunks,
            originalSizeMb = originalSizeMb,
            processedSizeMb = processedSizeMb
        )
    }

    private suspend fun executeFFmpeg(
        command: String,
        totalDurationMs: Long,
        onProgress: ((Float) -> Unit)?
    ) = suspendCancellableCoroutine { continuation ->
        val statisticsCallback = StatisticsCallback { statistics ->
            if (totalDurationMs > 0 && statistics != null) {
                val progress = (statistics.time.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                onProgress?.invoke(progress)
            }
        }

        FFmpegKitConfig.enableStatisticsCallback(statisticsCallback)

        val session = FFmpegKit.executeAsync(command) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                onProgress?.invoke(1f)
                Log.d(TAG, "FFmpeg completed successfully")
                continuation.resume(Unit)
            } else if (ReturnCode.isCancel(session.returnCode)) {
                Log.w(TAG, "FFmpeg was cancelled")
                continuation.resumeWithException(Exception("FFmpeg processing was cancelled"))
            } else {
                val error = session.failStackTrace ?: "Unknown error"
                Log.e(TAG, "FFmpeg failed: $error")
                continuation.resumeWithException(Exception("FFmpeg failed: $error"))
            }
        }

        continuation.invokeOnCancellation {
            FFmpegKit.cancel(session.sessionId)
        }
    }

    /**
     * 25MB 초과 오디오를 청크로 분할
     */
    private suspend fun chunkAudio(inputFile: File): List<AudioChunk> = withContext(Dispatchers.IO) {
        val fileSizeMb = inputFile.length() / (1024f * 1024f)
        val durationMs = getAudioDuration(inputFile)

        // 필요한 청크 수 계산 (청크당 20MB 목표)
        val numChunks = kotlin.math.ceil(fileSizeMb / 20.0).toInt().coerceAtLeast(2)
        val chunkDurationMs = durationMs / numChunks

        Log.i(TAG, "Splitting ${"%.1f".format(fileSizeMb)}MB into $numChunks chunks")

        val outputDir = File(context.cacheDir, "preprocessed_audio")
        val chunks = mutableListOf<AudioChunk>()

        for (i in 0 until numChunks) {
            val startMs = i * chunkDurationMs
            val startSec = startMs / 1000.0
            val durationSec = chunkDurationMs / 1000.0
            val outputFile = File(outputDir, "chunk_${i}_${UUID.randomUUID().toString().take(8)}.m4a")

            val command = buildString {
                append("-i \"${inputFile.absolutePath}\" ")
                append("-ss $startSec ")
                append("-t $durationSec ")
                append("-c copy ")
                append("-y \"${outputFile.absolutePath}\"")
            }

            val session = FFmpegKit.execute(command)

            if (ReturnCode.isSuccess(session.returnCode) && outputFile.exists()) {
                chunks.add(AudioChunk(
                    path = outputFile.absolutePath,
                    startOffsetMs = startMs,
                    durationMs = chunkDurationMs
                ))
                Log.d(TAG, "Created chunk $i: ${outputFile.name}")
            } else {
                Log.w(TAG, "Failed to create chunk $i")
            }
        }

        chunks
    }

    private fun getAudioDuration(file: File): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get audio duration", e)
            0L
        }
    }

    /**
     * 임시 전처리 파일 정리
     */
    fun cleanupTempFiles() {
        val outputDir = File(context.cacheDir, "preprocessed_audio")
        if (outputDir.exists()) {
            outputDir.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "Cleaned up preprocessed audio files")
        }
    }
}
