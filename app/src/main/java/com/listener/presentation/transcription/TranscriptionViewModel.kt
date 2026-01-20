package com.listener.presentation.transcription

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.remote.OpenAiService
import com.listener.domain.model.ChunkSettings
import com.listener.domain.model.Segment
import com.listener.domain.model.WhisperResult
import com.listener.domain.model.Word
import com.listener.domain.repository.TranscriptionRepository
import com.listener.domain.usecase.chunking.ChunkingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val openAiService: OpenAiService,
    private val transcriptionRepository: TranscriptionRepository,
    private val chunkingUseCase: ChunkingUseCase
) : ViewModel() {

    val sourceId: String = savedStateHandle.get<String>("sourceId") ?: ""

    private val _uiState = MutableStateFlow(TranscriptionUiState())
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()

    fun startTranscription(audioFilePath: String, language: String = "en") {
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

                // Download phase (simulate for local files)
                _uiState.update { it.copy(step = TranscriptionStep.DOWNLOADING) }
                for (i in 1..10) {
                    kotlinx.coroutines.delay(100)
                    _uiState.update { it.copy(downloadProgress = i / 10f) }
                }

                // Transcription phase
                _uiState.update { it.copy(step = TranscriptionStep.TRANSCRIBING) }
                val audioFile = File(audioFilePath)
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

    fun retry() {
        _uiState.update {
            TranscriptionUiState(step = TranscriptionStep.IDLE)
        }
    }

    fun cancel() {
        // Cancel ongoing transcription if needed
    }
}
