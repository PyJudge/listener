package com.listener.data.repository

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.listener.data.local.db.dao.ChunkSettingsDao
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.TranscriptionDao
import com.listener.data.local.db.entity.ChunkEntity
import com.listener.data.local.db.entity.ChunkSettingsEntity
import com.listener.data.local.db.entity.TranscriptionResultEntity
import com.listener.data.remote.TranscriptionServiceFactory
import com.listener.domain.model.Chunk
import com.listener.domain.model.ChunkSettings
import com.listener.domain.model.Segment
import com.listener.domain.model.WhisperResult
import com.listener.domain.model.Word
import com.listener.domain.repository.PodcastRepository
import com.listener.domain.repository.TranscriptionRepository
import com.listener.domain.repository.TranscriptionState
import com.listener.domain.repository.TranscriptionStep
import com.listener.domain.usecase.FFmpegPreprocessUseCase
import com.listener.domain.usecase.chunking.ChunkingUseCase
import com.listener.service.TranscriptionForegroundService
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.room.withTransaction
import com.listener.data.local.db.ListenerDatabase
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class TranscriptionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ListenerDatabase,
    private val transcriptionDao: TranscriptionDao,
    private val chunkSettingsDao: ChunkSettingsDao,
    private val localFileDao: LocalFileDao,
    private val podcastRepository: PodcastRepository,
    private val transcriptionServiceFactory: TranscriptionServiceFactory,
    private val settingsRepository: SettingsRepository,
    private val chunkingUseCase: ChunkingUseCase,
    private val ffmpegPreprocessUseCase: FFmpegPreprocessUseCase,
    private val gson: Gson
) : TranscriptionRepository {

    companion object {
        private const val TAG = "TranscriptionRepo"
        private const val TRANSCRIPTION_TIMEOUT_MS = 900_000L // 15분 (긴 오디오 처리 시간 고려)
        private val SUPPORTED_LANGUAGES = setOf(
            "en", "ko", "ja", "zh", "es", "fr", "de", "it", "pt", "ru",
            "ar", "hi", "nl", "pl", "tr", "vi", "th", "id", "cs", "sv"
        )

        /**
         * Generate a safe filename from sourceId using SHA-256.
         * This prevents hash collisions that can occur with hashCode().
         */
        fun safeFileName(sourceId: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(sourceId.toByteArray())
            return bytes.take(16).joinToString("") { "%02x".format(it) }
        }
    }

    // Application 레벨 스코프 - 화면 이동해도 유지됨
    private val transcriptionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null

    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    override val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState.asStateFlow()

    private val okHttpClient = OkHttpClient()
    private val cacheDir: File get() = File(context.cacheDir, "audio_downloads")

    override fun startTranscription(sourceId: String, language: String) {
        // 이미 동일 sourceId로 진행 중이면 무시
        val currentState = _transcriptionState.value
        if (currentState is TranscriptionState.InProgress && currentState.sourceId == sourceId) {
            Log.d(TAG, "Transcription already in progress for $sourceId")
            return
        }

        // 이전 작업 취소
        currentJob?.cancel()

        // Start Foreground Service for background protection
        startForegroundService(sourceId, language)

        // Run transcription in protected scope
        currentJob = transcriptionScope.launch {
            runTranscription(sourceId, language)
        }
    }

    private fun startForegroundService(sourceId: String, language: String) {
        try {
            val intent = TranscriptionForegroundService.createStartIntent(context, sourceId, language)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Started TranscriptionForegroundService")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            // Continue without foreground service - transcription will still run
            // but may be killed in background
        }
    }

    private fun stopForegroundService() {
        try {
            val intent = Intent(context, TranscriptionForegroundService::class.java)
            context.stopService(intent)
            Log.d(TAG, "Stopped TranscriptionForegroundService")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop foreground service: ${e.message}")
        }
    }

    override fun cancelTranscription() {
        currentJob?.cancel()
        currentJob = null
        _transcriptionState.value = TranscriptionState.Idle
        stopForegroundService()
    }

    private suspend fun runTranscription(sourceId: String, language: String) {
        try {
            Log.d(TAG, "Starting transcription for $sourceId")

            // Validate language
            if (language !in SUPPORTED_LANGUAGES) {
                _transcriptionState.value = TranscriptionState.Error(
                    sourceId = sourceId,
                    message = "지원하지 않는 언어입니다: $language"
                )
                return
            }

            // 이미 전사된 chunks가 있으면 스킵
            val existingChunks = getChunks(sourceId)
            if (existingChunks.isNotEmpty()) {
                Log.d(TAG, "Already transcribed, skipping: $sourceId")
                _transcriptionState.value = TranscriptionState.Complete(
                    sourceId = sourceId,
                    chunkCount = existingChunks.size
                )
                return
            }

            // 전사 서비스 가져오기 및 API 키 확인
            val transcriptionService = transcriptionServiceFactory.getService()
            if (!transcriptionService.hasApiKey()) {
                _transcriptionState.value = TranscriptionState.Error(
                    sourceId = sourceId,
                    message = "${transcriptionService.providerName} API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요."
                )
                return
            }

            // 설정 가져오기
            val settings = settingsRepository.settings.first()

            // 오디오 소스 찾기
            val audioSource = findAudioSource(sourceId)
            if (audioSource == null) {
                _transcriptionState.value = TranscriptionState.Error(
                    sourceId = sourceId,
                    message = "오디오 소스를 찾을 수 없습니다."
                )
                return
            }

            // Download phase
            _transcriptionState.value = TranscriptionState.InProgress(
                sourceId = sourceId,
                step = TranscriptionStep.DOWNLOADING,
                downloadProgress = 0f
            )

            val audioFile = when (audioSource) {
                is AudioSource.Remote -> downloadAudioFile(sourceId, audioSource.url)
                is AudioSource.Local -> getLocalFile(sourceId, audioSource.uri)
            }

            if (audioFile == null || !audioFile.exists()) {
                cleanupCaches(sourceId)
                _transcriptionState.value = TranscriptionState.Error(
                    sourceId = sourceId,
                    message = "오디오 파일 준비에 실패했습니다."
                )
                return
            }

            // Preprocess audio (FFmpeg 압축 - 스킵 로직 포함)
            _transcriptionState.value = TranscriptionState.InProgress(
                sourceId = sourceId,
                step = TranscriptionStep.PREPROCESSING,
                downloadProgress = 1f,
                preprocessProgress = 0f
            )

            val preprocessResult = try {
                // 스킵 가능 여부 확인
                if (settings.skipPreprocessingForSmallFiles &&
                    ffmpegPreprocessUseCase.canSkipPreprocessing(audioFile)) {
                    Log.i(TAG, "Skipping preprocessing - file already optimized")
                    _transcriptionState.value = TranscriptionState.InProgress(
                        sourceId = sourceId,
                        step = TranscriptionStep.PREPROCESSING,
                        downloadProgress = 1f,
                        preprocessProgress = 1f
                    )
                    FFmpegPreprocessUseCase.PreprocessResult(
                        chunks = listOf(FFmpegPreprocessUseCase.AudioChunk(
                            path = audioFile.absolutePath,
                            startOffsetMs = 0,
                            durationMs = 0
                        )),
                        originalSizeMb = audioFile.length() / (1024f * 1024f),
                        processedSizeMb = audioFile.length() / (1024f * 1024f),
                        skipped = true
                    )
                } else {
                    ffmpegPreprocessUseCase.preprocess(audioFile) { progress ->
                        _transcriptionState.value = TranscriptionState.InProgress(
                            sourceId = sourceId,
                            step = TranscriptionStep.PREPROCESSING,
                            downloadProgress = 1f,
                            preprocessProgress = progress
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Preprocessing failed, using original: ${e.message}")
                null
            }

            val fileToTranscribe = preprocessResult?.chunks?.firstOrNull()?.let {
                File(it.path)
            } ?: audioFile

            // Transcription phase with timeout
            _transcriptionState.value = TranscriptionState.InProgress(
                sourceId = sourceId,
                step = TranscriptionStep.TRANSCRIBING,
                downloadProgress = 1f,
                preprocessProgress = 1f,
                transcriptionProgress = 0f
            )

            Log.i(TAG, "Using ${transcriptionService.providerName} (${transcriptionService.modelName})")

            val whisperResponse = try {
                withTimeout(TRANSCRIPTION_TIMEOUT_MS) {
                    transcriptionService.transcribe(fileToTranscribe, language)
                }
            } catch (e: TimeoutCancellationException) {
                cleanupCaches(sourceId)
                _transcriptionState.value = TranscriptionState.Error(
                    sourceId = sourceId,
                    message = "전사 요청 시간이 초과되었습니다 (15분). 네트워크 상태를 확인해주세요."
                )
                return
            }

            _transcriptionState.value = TranscriptionState.InProgress(
                sourceId = sourceId,
                step = TranscriptionStep.TRANSCRIBING,
                downloadProgress = 1f,
                preprocessProgress = 1f,
                transcriptionProgress = 1f
            )

            // Convert to domain model
            val whisperResult = WhisperResult(
                text = whisperResponse.text,
                segments = whisperResponse.segments?.map {
                    Segment(it.start, it.end, it.text)
                } ?: emptyList(),
                words = whisperResponse.words?.map {
                    Word(it.word, it.start, it.end)
                } ?: emptyList()
            )

            // Save transcription
            saveTranscription(sourceId, language, whisperResult)

            // Processing phase
            _transcriptionState.value = TranscriptionState.InProgress(
                sourceId = sourceId,
                step = TranscriptionStep.PROCESSING,
                downloadProgress = 1f,
                preprocessProgress = 1f,
                transcriptionProgress = 1f
            )

            val chunks = chunkingUseCase.process(
                whisperResult = whisperResult,
                sentenceOnly = ChunkSettings.DEFAULT.sentenceOnly,
                minChunkMs = ChunkSettings.DEFAULT.minChunkMs
            )

            saveChunks(sourceId, chunks)

            // 전사 성공 시 에피소드를 읽음 처리 (new 표시 제거)
            podcastRepository.markEpisodeAsRead(sourceId)

            Log.d(TAG, "Transcription complete: $sourceId, ${chunks.size} chunks")
            _transcriptionState.value = TranscriptionState.Complete(
                sourceId = sourceId,
                chunkCount = chunks.size
            )

        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed: ${e.message}", e)
            cleanupCaches(sourceId)
            _transcriptionState.value = TranscriptionState.Error(
                sourceId = sourceId,
                message = e.message ?: "전사 실패"
            )
        }
    }

    private sealed class AudioSource {
        data class Remote(val url: String) : AudioSource()
        data class Local(val uri: String) : AudioSource()
    }

    private suspend fun findAudioSource(sourceId: String): AudioSource? {
        // 1. 팟캐스트 에피소드에서 찾기
        val episode = podcastRepository.getEpisode(sourceId)
        if (episode != null) {
            return AudioSource.Remote(episode.audioUrl)
        }

        // 2. 로컬 파일에서 찾기
        val localFile = localFileDao.getFile(sourceId)
        if (localFile != null) {
            return AudioSource.Local(localFile.uri)
        }

        return null
    }

    private suspend fun downloadAudioFile(sourceId: String, url: String): File? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "downloadAudioFile: $url")
            cacheDir.mkdirs()
            val fileName = "${safeFileName(sourceId)}.mp3"
            val targetFile = File(cacheDir, fileName)

            // 캐시된 파일 재사용
            if (targetFile.exists() && targetFile.length() > 0) {
                Log.d(TAG, "Using cached file")
                updateDownloadProgress(sourceId, 1f)
                return@withContext targetFile
            }

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: ${response.code}")
                return@withContext null
            }

            val contentLength = response.body?.contentLength() ?: -1L
            var downloadedBytes = 0L

            response.body?.byteStream()?.use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (contentLength > 0) {
                            val progress = downloadedBytes.toFloat() / contentLength
                            updateDownloadProgress(sourceId, progress)
                        }
                    }
                }
            }

            Log.d(TAG, "Download complete: ${targetFile.length()} bytes")
            updateDownloadProgress(sourceId, 1f)
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            null
        }
    }

    private fun updateDownloadProgress(sourceId: String, progress: Float) {
        val current = _transcriptionState.value
        if (current is TranscriptionState.InProgress && current.sourceId == sourceId) {
            _transcriptionState.value = current.copy(downloadProgress = progress)
        }
    }

    private fun getLocalFile(sourceId: String, uri: String): File? {
        return try {
            if (uri.startsWith("content://")) {
                val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(uri))
                    ?: return null

                cacheDir.mkdirs()
                val fileName = "${safeFileName(sourceId)}.mp3"
                val targetFile = File(cacheDir, fileName)

                inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                updateDownloadProgress(sourceId, 1f)
                targetFile
            } else {
                updateDownloadProgress(sourceId, 1f)
                File(uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanupCaches(sourceId: String) {
        try {
            val targetFileName = "${safeFileName(sourceId)}.mp3"
            File(cacheDir, targetFileName).let {
                if (it.exists()) it.delete()
            }
            ffmpegPreprocessUseCase.cleanupTempFiles()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup caches: ${e.message}")
        }
    }

    // 기존 Repository 메서드들

    override suspend fun hasTranscription(sourceId: String, language: String): Boolean {
        return transcriptionDao.hasTranscription(sourceId, language)
    }

    override suspend fun getTranscription(sourceId: String): WhisperResult? {
        val entity = transcriptionDao.getTranscription(sourceId) ?: return null
        return WhisperResult(
            text = entity.fullText,
            segments = gson.fromJson(entity.segmentsJson, Array<Segment>::class.java).toList(),
            words = gson.fromJson(entity.wordsJson, Array<Word>::class.java).toList()
        )
    }

    override suspend fun saveTranscription(sourceId: String, language: String, result: WhisperResult) {
        val entity = TranscriptionResultEntity(
            sourceId = sourceId,
            language = language,
            fullText = result.text,
            segmentsJson = gson.toJson(result.segments),
            wordsJson = gson.toJson(result.words),
            createdAt = System.currentTimeMillis()
        )
        transcriptionDao.insertTranscription(entity)
    }

    override suspend fun getChunks(sourceId: String): List<Chunk> {
        return transcriptionDao.getChunksList(sourceId).map { it.toDomain() }
    }

    override fun observeChunks(sourceId: String): Flow<List<Chunk>> {
        return transcriptionDao.getChunks(sourceId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveChunks(sourceId: String, chunks: List<Chunk>) {
        val entities = chunks.map { chunk ->
            ChunkEntity(
                sourceId = sourceId,
                orderIndex = chunk.orderIndex,
                startMs = chunk.startMs,
                endMs = chunk.endMs,
                displayText = chunk.displayText
            )
        }
        // Use transaction to ensure atomicity - concurrent readers will either see
        // all old chunks or all new chunks, but never an empty list
        database.withTransaction {
            transcriptionDao.deleteChunks(sourceId)
            transcriptionDao.insertChunks(entities)
        }
    }

    override suspend fun deleteChunks(sourceId: String) {
        transcriptionDao.deleteChunks(sourceId)
    }

    override suspend fun getChunkSettings(sourceId: String): ChunkSettings? {
        val entity = chunkSettingsDao.getSettings(sourceId) ?: return null
        return ChunkSettings(
            sentenceOnly = entity.sentenceOnly,
            minChunkMs = entity.minChunkMs
        )
    }

    override suspend fun saveChunkSettings(sourceId: String, settings: ChunkSettings) {
        val entity = ChunkSettingsEntity(
            sourceId = sourceId,
            sentenceOnly = settings.sentenceOnly,
            minChunkMs = settings.minChunkMs,
            updatedAt = System.currentTimeMillis()
        )
        chunkSettingsDao.insertSettings(entity)
    }

    private fun ChunkEntity.toDomain() = Chunk(
        orderIndex = orderIndex,
        startMs = startMs,
        endMs = endMs,
        displayText = displayText
    )
}
