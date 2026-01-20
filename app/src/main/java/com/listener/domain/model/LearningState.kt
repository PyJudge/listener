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

enum class PlayMode {
    NORMAL,  // 일반 재생 (공백 없음)
    LR,      // Listen & Repeat (듣고 따라하기)
    LRLR     // Listen & Repeat + 녹음 재생
}

data class LearningSettings(
    val repeatCount: Int = 2,
    val gapRatio: Float = 0.4f,
    val playMode: PlayMode = PlayMode.LR
) {
    // 하위 호환성
    val isHardMode: Boolean get() = playMode == PlayMode.LRLR
    val isRecordingEnabled: Boolean get() = playMode == PlayMode.LRLR
    val isNormalMode: Boolean get() = playMode == PlayMode.NORMAL
}
