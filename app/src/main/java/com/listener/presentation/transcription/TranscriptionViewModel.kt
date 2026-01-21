package com.listener.presentation.transcription

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.domain.repository.TranscriptionRepository
import com.listener.domain.repository.TranscriptionState
import com.listener.domain.repository.TranscriptionStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UiTranscriptionStep {
    IDLE, DOWNLOADING, PREPROCESSING, TRANSCRIBING, PROCESSING, COMPLETE, ERROR
}

data class TranscriptionUiState(
    val step: UiTranscriptionStep = UiTranscriptionStep.IDLE,
    val downloadProgress: Float = 0f,
    val preprocessProgress: Float = 0f,
    val transcriptionProgress: Float = 0f,
    val chunkCount: Int = 0,
    val errorMessage: String? = null
) {
    // 진행률 분배: 다운로드 15%, 압축 15%, 전사 55%, 처리 15%
    val overallProgress: Float
        get() = when (step) {
            UiTranscriptionStep.IDLE -> 0f
            UiTranscriptionStep.DOWNLOADING -> downloadProgress * 0.15f
            UiTranscriptionStep.PREPROCESSING -> 0.15f + (preprocessProgress * 0.15f)
            UiTranscriptionStep.TRANSCRIBING -> 0.30f + (transcriptionProgress * 0.55f)
            UiTranscriptionStep.PROCESSING -> 0.85f + 0.10f
            UiTranscriptionStep.COMPLETE -> 1f
            UiTranscriptionStep.ERROR -> 0f
        }
}

@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transcriptionRepository: TranscriptionRepository
) : ViewModel() {

    val sourceId: String = savedStateHandle.get<String>("sourceId") ?: ""

    private val _uiState = MutableStateFlow(TranscriptionUiState())
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()

    init {
        // Repository의 전사 상태 관찰
        viewModelScope.launch {
            transcriptionRepository.transcriptionState.collect { state ->
                // 이 sourceId에 해당하는 상태만 처리
                updateUiState(state)
            }
        }

        // 전사 시작 (이미 진행 중이면 Repository가 무시)
        if (sourceId.isNotBlank()) {
            transcriptionRepository.startTranscription(sourceId)
        }
    }

    private fun updateUiState(state: TranscriptionState) {
        when (state) {
            is TranscriptionState.Idle -> {
                // Idle 상태는 무시 (이 화면이 열리면 이미 전사가 시작됨)
            }

            is TranscriptionState.InProgress -> {
                if (state.sourceId == sourceId) {
                    _uiState.update {
                        it.copy(
                            step = when (state.step) {
                                TranscriptionStep.DOWNLOADING -> UiTranscriptionStep.DOWNLOADING
                                TranscriptionStep.PREPROCESSING -> UiTranscriptionStep.PREPROCESSING
                                TranscriptionStep.TRANSCRIBING -> UiTranscriptionStep.TRANSCRIBING
                                TranscriptionStep.PROCESSING -> UiTranscriptionStep.PROCESSING
                            },
                            downloadProgress = state.downloadProgress,
                            preprocessProgress = state.preprocessProgress,
                            transcriptionProgress = state.transcriptionProgress
                        )
                    }
                }
            }

            is TranscriptionState.Complete -> {
                if (state.sourceId == sourceId) {
                    _uiState.update {
                        it.copy(
                            step = UiTranscriptionStep.COMPLETE,
                            chunkCount = state.chunkCount
                        )
                    }
                }
            }

            is TranscriptionState.Error -> {
                if (state.sourceId == sourceId) {
                    _uiState.update {
                        it.copy(
                            step = UiTranscriptionStep.ERROR,
                            errorMessage = state.message
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        _uiState.update { TranscriptionUiState(step = UiTranscriptionStep.IDLE) }
        transcriptionRepository.startTranscription(sourceId)
    }

    fun cancel() {
        transcriptionRepository.cancelTranscription()
    }
}
