package com.listener.domain.usecase.chunking

import com.listener.domain.model.Chunk
import javax.inject.Inject

class ChunkMerger @Inject constructor() {

    fun merge(chunks: List<Chunk>, minChunkMs: Long = 1200L): List<Chunk> {
        if (chunks.isEmpty()) return emptyList()
        if (chunks.size == 1) return chunks

        val result = mutableListOf<Chunk>()
        var buffer: Chunk? = null

        for (chunk in chunks) {
            buffer = if (buffer == null) {
                chunk
            } else {
                mergeTwo(buffer, chunk)
            }

            if (buffer.durationMs >= minChunkMs) {
                result.add(buffer)
                buffer = null
            }
        }

        // Handle remaining buffer
        if (buffer != null) {
            if (result.isEmpty()) {
                result.add(buffer)
            } else {
                val last = result.removeAt(result.lastIndex)
                result.add(mergeTwo(last, buffer))
            }
        }

        // Reindex
        return result.mapIndexed { index, chunk ->
            chunk.copy(orderIndex = index)
        }
    }

    private fun mergeTwo(a: Chunk, b: Chunk): Chunk {
        return Chunk(
            orderIndex = a.orderIndex,
            startMs = a.startMs,
            endMs = b.endMs,
            displayText = "${a.displayText} ${b.displayText}".trim()
        )
    }
}
