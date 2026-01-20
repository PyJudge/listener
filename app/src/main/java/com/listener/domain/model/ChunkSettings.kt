package com.listener.domain.model

data class ChunkSettings(
    val sentenceOnly: Boolean = true,
    val minChunkMs: Long = 1200L
) {
    companion object {
        val DEFAULT = ChunkSettings()
    }
}
