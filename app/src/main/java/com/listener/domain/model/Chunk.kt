package com.listener.domain.model

data class Chunk(
    val orderIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val displayText: String
) {
    val durationMs: Long get() = endMs - startMs
}
