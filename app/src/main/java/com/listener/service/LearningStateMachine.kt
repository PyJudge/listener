package com.listener.service

import com.listener.domain.model.LearningMode
import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 학습 상태 전환을 관리하는 상태 머신
 *
 * 개선사항:
 * 1. 일시정지 후 재개 시 정확한 위치 복원
 * 2. chunk 변경 시 현재 반복 횟수 리셋
 * 3. HARD 모드에서 녹음 실패 시 graceful 처리
 * 4. 상태 변경 이벤트 콜백 추가
 */
@Singleton
class LearningStateMachine @Inject constructor() {

    private val _state = MutableStateFlow<LearningState>(LearningState.Idle)
    val state: StateFlow<LearningState> = _state.asStateFlow()

    private var currentRepeat = 1
    private var settings = LearningSettings()
    private var previousState: LearningState = LearningState.Idle

    // 일시정지 시 재생 위치 저장 (ms)
    private var pausedPositionMs: Long = 0L

    // 현재 chunk 인덱스
    private var currentChunkIndex: Int = 0

    // 상태 변경 콜백
    private var onStateChangeListener: ((oldState: LearningState, newState: LearningState) -> Unit)? = null

    fun updateSettings(newSettings: LearningSettings) {
        settings = newSettings
    }

    /**
     * 상태 변경 리스너 설정
     */
    fun setOnStateChangeListener(listener: ((LearningState, LearningState) -> Unit)?) {
        onStateChangeListener = listener
    }

    private fun setState(newState: LearningState) {
        val oldState = _state.value
        _state.value = newState
        onStateChangeListener?.invoke(oldState, newState)
    }

    fun play() {
        when (settings.mode) {
            LearningMode.NORMAL -> setState(LearningState.Playing)
            LearningMode.HARD -> setState(LearningState.PlayingFirst)
        }
        currentRepeat = 1
        pausedPositionMs = 0L
    }

    /**
     * 일시정지 (현재 재생 위치 저장)
     * @param currentPositionMs 현재 재생 위치 (ms)
     */
    fun pause(currentPositionMs: Long = 0L) {
        previousState = _state.value
        pausedPositionMs = currentPositionMs
        setState(LearningState.Paused)
    }

    /**
     * 재개
     * @return 일시정지 시 저장된 재생 위치 (ms)
     */
    fun resume(): Long {
        if (_state.value == LearningState.Paused) {
            setState(previousState)
        }
        return pausedPositionMs
    }

    fun stop() {
        setState(LearningState.Idle)
        currentRepeat = 1
        pausedPositionMs = 0L
    }

    /**
     * chunk 변경 시 호출 - 반복 횟수 리셋
     * @param newChunkIndex 새로운 chunk 인덱스
     */
    fun onChunkChanged(newChunkIndex: Int) {
        currentChunkIndex = newChunkIndex
        currentRepeat = 1
        pausedPositionMs = 0L
        // 상태는 유지하되 반복 횟수만 리셋
    }

    fun onPlaybackComplete(): TransitionResult {
        return when (settings.mode) {
            LearningMode.NORMAL -> handleNormalModeComplete()
            LearningMode.HARD -> handleHardModeComplete()
        }
    }

    /**
     * HARD 모드에서 녹음 실패 시 graceful 처리
     * 녹음을 건너뛰고 다음 단계로 진행
     */
    fun onRecordingFailed(): TransitionResult {
        return when (_state.value) {
            is LearningState.Recording -> {
                // NORMAL 모드: 녹음 실패 시 Gap으로 전환
                setState(LearningState.Gap)
                TransitionResult.Continue
            }
            is LearningState.GapWithRecording -> {
                // HARD 모드: 녹음 실패 시 2차 재생으로 건너뜀
                setState(LearningState.PlayingSecond)
                TransitionResult.SkipRecordingPlayback
            }
            else -> TransitionResult.Continue
        }
    }

    private fun handleNormalModeComplete(): TransitionResult {
        return when (_state.value) {
            is LearningState.Playing -> {
                setState(
                    if (settings.isRecordingEnabled) {
                        LearningState.Recording
                    } else {
                        LearningState.Gap
                    }
                )
                TransitionResult.Continue
            }
            is LearningState.Gap, is LearningState.Recording -> {
                if (currentRepeat < settings.repeatCount) {
                    currentRepeat++
                    setState(LearningState.Playing)
                    TransitionResult.Continue
                } else {
                    currentRepeat = 1
                    setState(LearningState.Idle)
                    TransitionResult.NextChunk
                }
            }
            else -> TransitionResult.Continue
        }
    }

    private fun handleHardModeComplete(): TransitionResult {
        return when (_state.value) {
            is LearningState.PlayingFirst -> {
                setState(LearningState.GapWithRecording)
                TransitionResult.Continue
            }
            is LearningState.GapWithRecording -> {
                setState(LearningState.PlayingSecond)
                TransitionResult.Continue
            }
            is LearningState.PlayingSecond -> {
                setState(LearningState.PlaybackRecording)
                TransitionResult.Continue
            }
            is LearningState.PlaybackRecording -> {
                if (currentRepeat < settings.repeatCount) {
                    currentRepeat++
                    setState(LearningState.PlayingFirst)
                    TransitionResult.Continue
                } else {
                    currentRepeat = 1
                    setState(LearningState.Idle)
                    TransitionResult.NextChunk
                }
            }
            else -> TransitionResult.Continue
        }
    }

    fun getCurrentRepeat(): Int = currentRepeat
    fun getSettings(): LearningSettings = settings
    fun getCurrentChunkIndex(): Int = currentChunkIndex
    fun getPausedPositionMs(): Long = pausedPositionMs
}

sealed class TransitionResult {
    data object Continue : TransitionResult()
    data object NextChunk : TransitionResult()
    data object PreviousChunk : TransitionResult()
    data object Stop : TransitionResult()
    data object SkipRecordingPlayback : TransitionResult() // 녹음 실패 시 녹음 재생 건너뜀
}
