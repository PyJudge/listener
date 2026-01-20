package com.listener.domain.usecase.chunking

import com.listener.domain.model.Chunk
import javax.inject.Inject

class ChunkMerger @Inject constructor() {

    private val sentenceEndPattern = Regex("[.!?][\"'\"']?\\s*$")

    fun merge(chunks: List<Chunk>, minChunkMs: Long = 1200L): List<Chunk> {
        if (chunks.isEmpty()) return emptyList()
        if (chunks.size == 1) return chunks

        // First pass: merge chunks that don't end with sentence-ending punctuation
        val sentenceMerged = mergeBySentenceBoundary(chunks)

        // Second pass: merge short chunks
        val result = mergeByDuration(sentenceMerged, minChunkMs)

        // Reindex
        return result.mapIndexed { index, chunk ->
            chunk.copy(orderIndex = index)
        }
    }

    private fun mergeBySentenceBoundary(chunks: List<Chunk>): List<Chunk> {
        val result = mutableListOf<Chunk>()
        var buffer: Chunk? = null

        for (chunk in chunks) {
            buffer = if (buffer == null) {
                chunk
            } else {
                mergeTwo(buffer, chunk)
            }

            // Only flush if text ends with sentence-ending punctuation
            if (endsWithSentence(buffer.displayText)) {
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

        return result
    }

    private fun mergeByDuration(chunks: List<Chunk>, minChunkMs: Long): List<Chunk> {
        if (chunks.isEmpty()) return emptyList()

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

        return result
    }

    private fun endsWithSentence(text: String): Boolean {
        return sentenceEndPattern.containsMatchIn(text)
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
