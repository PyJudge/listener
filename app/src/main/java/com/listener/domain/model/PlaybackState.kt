package com.listener.domain.model

data class PlaybackState(
    val sourceId: String = "",
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val learningState: LearningState = LearningState.Idle,
    val settings: LearningSettings = LearningSettings(),
    val currentRepeat: Int = 1,
    val isPlaying: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val artworkUrl: String? = null,
    // A4: 오디오 에러 메시지 (null이면 에러 없음)
    val error: String? = null,
    // D1: 현재 콘텐츠 완료 여부 (플레이리스트 자동 전환용)
    val contentComplete: Boolean = false
) {
    val progress: Float
        get() = if (totalChunks > 0) currentChunkIndex.toFloat() / totalChunks else 0f
}
