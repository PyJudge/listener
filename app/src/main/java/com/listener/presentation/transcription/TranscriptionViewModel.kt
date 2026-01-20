package com.listener.presentation.transcription

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.remote.OpenAiService
import com.listener.domain.model.ChunkSettings
import com.listener.domain.model.Segment
import com.listener.domain.model.WhisperResult
import com.listener.domain.model.Word
import com.listener.domain.repository.PodcastRepository
import com.listener.domain.repository.TranscriptionRepository
import com.listener.domain.usecase.chunking.ChunkingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

enum class TranscriptionStep {
    IDLE, DOWNLOADING, TRANSCRIBING, PROCESSING, COMPLETE, ERROR
}

data class TranscriptionUiState(
    val step: TranscriptionStep = TranscriptionStep.IDLE,
    val downloadProgress: Float = 0f,
    val transcriptionProgress: Float = 0f,
    val chunkCount: Int = 0,
    val errorMessage: String? = null
) {
    val overallProgress: Float
        get() = when (step) {
            TranscriptionStep.IDLE -> 0f
            TranscriptionStep.DOWNLOADING -> downloadProgress * 0.2f
            TranscriptionStep.TRANSCRIBING -> 0.2f + (transcriptionProgress * 0.6f)
            TranscriptionStep.PROCESSING -> 0.8f + 0.15f
            TranscriptionStep.COMPLETE -> 1f
            TranscriptionStep.ERROR -> 0f
        }
}

@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val openAiService: OpenAiService,
    private val transcriptionRepository: TranscriptionRepository,
    private val podcastRepository: PodcastRepository,
    private val localFileDao: LocalFileDao,
    private val chunkingUseCase: ChunkingUseCase
) : ViewModel() {

    val sourceId: String = savedStateHandle.get<String>("sourceId") ?: ""

    private val _uiState = MutableStateFlow(TranscriptionUiState())
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()

    private val okHttpClient = OkHttpClient()
    private val cacheDir: File get() = File(context.cacheDir, "audio_downloads")

    init {
        // 자동으로 전사 시작
        if (sourceId.isNotBlank()) {
            startTranscriptionForSource()
        }
    }

    private fun startTranscriptionForSource(language: String = "en") {
        viewModelScope.launch {
            try {
                // Check API key first before any processing
                if (!openAiService.hasApiKey()) {
                    _uiState.update {
                        it.copy(
                            step = TranscriptionStep.ERROR,
                            errorMessage = "OpenAI API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요."
                        )
                    }
                    return@launch
                }

                // Check if already transcribed
                if (transcriptionRepository.hasTranscription(sourceId, language)) {
                    val chunks = transcriptionRepository.getChunks(sourceId)
                    _uiState.update {
                        it.copy(
                            step = TranscriptionStep.COMPLETE,
                            chunkCount = chunks.size
                        )
                    }
                    return@launch
                }

                // sourceId로 오디오 소스 찾기
                val audioSource = findAudioSource()
                if (audioSource == null) {
                    _uiState.update {
                        it.copy(
                            step = TranscriptionStep.ERROR,
                            errorMessage = "오디오 소스를 찾을 수 없습니다."
                        )
                    }
                    return@launch
                }

                // Download phase
                _uiState.update { it.copy(step = TranscriptionStep.DOWNLOADING) }
                val audioFile = when (audioSource) {
                    is AudioSource.Remote -> downloadAudioFile(audioSource.url)
                    is AudioSource.Local -> getLocalFile(audioSource.uri)
                }

                if (audioFile == null || !audioFile.exists()) {
                    _uiState.update {
                        it.copy(
                            step = TranscriptionStep.ERROR,
                            errorMessage = "오디오 파일 준비에 실패했습니다."
                        )
                    }
                    return@launch
                }

                // Transcription phase
                _uiState.update { it.copy(step = TranscriptionStep.TRANSCRIBING) }
                val whisperResponse = openAiService.transcribe(audioFile, language)
                _uiState.update { it.copy(transcriptionProgress = 1f) }

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
                transcriptionRepository.saveTranscription(sourceId, language, whisperResult)

                // Processing - Create chunks
                _uiState.update { it.copy(step = TranscriptionStep.PROCESSING) }
                val chunks = chunkingUseCase.process(
                    whisperResult = whisperResult,
                    sentenceOnly = ChunkSettings.DEFAULT.sentenceOnly,
                    minChunkMs = ChunkSettings.DEFAULT.minChunkMs
                )

                transcriptionRepository.saveChunks(sourceId, chunks)

                _uiState.update {
                    it.copy(
                        step = TranscriptionStep.COMPLETE,
                        chunkCount = chunks.size
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        step = TranscriptionStep.ERROR,
                        errorMessage = e.message ?: "Transcription failed"
                    )
                }
            }
        }
    }

    private sealed class AudioSource {
        data class Remote(val url: String) : AudioSource()
        data class Local(val uri: String) : AudioSource()
    }

    private suspend fun findAudioSource(): AudioSource? {
        android.util.Log.d("TranscriptionVM", "findAudioSource for sourceId: $sourceId")

        // 1. 팟캐스트 에피소드에서 찾기
        val episode = podcastRepository.getEpisode(sourceId)
        android.util.Log.d("TranscriptionVM", "getEpisode result: $episode")
        if (episode != null) {
            android.util.Log.d("TranscriptionVM", "Found episode, audioUrl: ${episode.audioUrl}")
            return AudioSource.Remote(episode.audioUrl)
        }

        // 2. 로컬 파일에서 찾기
        val localFile = localFileDao.getFile(sourceId)
        android.util.Log.d("TranscriptionVM", "getFile result: $localFile")
        if (localFile != null) {
            return AudioSource.Local(localFile.uri)
        }

        android.util.Log.e("TranscriptionVM", "Audio source not found for sourceId: $sourceId")
        return null
    }

    private suspend fun downloadAudioFile(url: String): File? = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("TranscriptionVM", "downloadAudioFile start: $url")
            cacheDir.mkdirs()
            val fileName = "${sourceId.hashCode()}.mp3"
            val targetFile = File(cacheDir, fileName)
            android.util.Log.d("TranscriptionVM", "Target file: ${targetFile.absolutePath}")

            // 이미 캐시된 파일이 있으면 재사용
            if (targetFile.exists() && targetFile.length() > 0) {
                android.util.Log.d("TranscriptionVM", "Using cached file, size: ${targetFile.length()}")
                _uiState.update { it.copy(downloadProgress = 1f) }
                return@withContext targetFile
            }

            val request = Request.Builder().url(url).build()
            android.util.Log.d("TranscriptionVM", "Executing request...")
            val response = okHttpClient.newCall(request).execute()
            android.util.Log.d("TranscriptionVM", "Response code: ${response.code}")

            if (!response.isSuccessful) {
                android.util.Log.e("TranscriptionVM", "Download failed with code: ${response.code}")
                return@withContext null
            }

            val contentLength = response.body?.contentLength() ?: -1L
            android.util.Log.d("TranscriptionVM", "Content length: $contentLength")
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
                            _uiState.update { it.copy(downloadProgress = progress) }
                        }
                    }
                }
            }

            android.util.Log.d("TranscriptionVM", "Download complete, size: ${targetFile.length()}")
            _uiState.update { it.copy(downloadProgress = 1f) }
            targetFile
        } catch (e: Exception) {
            android.util.Log.e("TranscriptionVM", "Download error: ${e.message}", e)
            null
        }
    }

    private fun getLocalFile(uri: String): File? {
        return try {
            // content:// URI의 경우 임시 파일로 복사
            if (uri.startsWith("content://")) {
                val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(uri))
                    ?: return null

                cacheDir.mkdirs()
                val fileName = "${sourceId.hashCode()}.mp3"
                val targetFile = File(cacheDir, fileName)

                inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                _uiState.update { it.copy(downloadProgress = 1f) }
                targetFile
            } else {
                _uiState.update { it.copy(downloadProgress = 1f) }
                File(uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    @Deprecated("Use automatic transcription via sourceId lookup", ReplaceWith(""))
    fun startTranscription(audioFilePath: String, language: String = "en") {
        // 레거시 호환성을 위해 유지
        startTranscriptionForSource(language)
    }

    fun retry() {
        _uiState.update {
            TranscriptionUiState(step = TranscriptionStep.IDLE)
        }
        startTranscriptionForSource()
    }

    fun cancel() {
        // Cancel ongoing transcription if needed
    }
}
