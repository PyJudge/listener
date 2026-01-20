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
    val artworkUrl: String? = null
) {
    val progress: Float
        get() = if (totalChunks > 0) currentChunkIndex.toFloat() / totalChunks else 0f
}
