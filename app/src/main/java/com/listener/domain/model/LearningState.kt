package com.listener.domain.model

sealed class LearningState {
    data object Idle : LearningState()
    data object Playing : LearningState()
    data object Paused : LearningState()
    data object Gap : LearningState()
    data object Recording : LearningState()

    // HARD 모드 전용 상태
    data object PlayingFirst : LearningState()  // 1차 원문 재생
    data object GapWithRecording : LearningState()  // 공백 + 녹음
    data object PlayingSecond : LearningState()  // 2차 원문 재생
    data object PlaybackRecording : LearningState()  // 내 녹음 재생
}

enum class LearningMode {
    NORMAL,
    HARD
}

data class LearningSettings(
    val repeatCount: Int = 2,
    val gapRatio: Float = 0.4f,
    val isRecordingEnabled: Boolean = true,
    val isHardMode: Boolean = false
) {
    val mode: LearningMode get() = if (isHardMode) LearningMode.HARD else LearningMode.NORMAL
}
