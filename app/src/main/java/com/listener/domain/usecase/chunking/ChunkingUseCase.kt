package com.listener.domain.usecase.chunking

import com.listener.domain.model.Chunk
import com.listener.domain.model.WhisperResult
import javax.inject.Inject

class ChunkingUseCase @Inject constructor(
    private val sentenceSplitter: SentenceSplitter,
    private val timestampMatcher: TimestampMatcher,
    private val chunkMerger: ChunkMerger,
    private val duplicateRemover: DuplicateRemover
) {

    fun process(
        whisperResult: WhisperResult,
        sentenceOnly: Boolean = true,
        minChunkMs: Long = 1200L
    ): List<Chunk> {
        val words = duplicateRemover.removeDuplicates(whisperResult.words)
        val segments = whisperResult.segments

        if (segments.isEmpty()) return emptyList()

        val rawChunks = mutableListOf<Chunk>()
        var wordIndex = 0
        var chunkIndex = 0

        for (segment in segments) {
            val sentences = sentenceSplitter.split(segment.text, sentenceOnly)

            for (sentence in sentences) {
                val sentenceWords = sentence.split(Regex("\\s+")).filter { it.isNotEmpty() }

                val startTime = if (words.isNotEmpty() && wordIndex < words.size) {
                    words[wordIndex].start
                } else {
                    segment.start
                }

                val (endTime, newWordIndex) = if (words.isNotEmpty()) {
                    timestampMatcher.findEndTimestamp(sentenceWords, words, wordIndex)
                        ?: Pair(segment.end, wordIndex + sentenceWords.size)
                } else {
                    Pair(segment.end, wordIndex)
                }

                wordIndex = newWordIndex

                rawChunks.add(
                    Chunk(
                        orderIndex = chunkIndex++,
                        startMs = (startTime * 1000).toLong(),
                        endMs = (endTime * 1000).toLong(),
                        displayText = sentence
                    )
                )
            }
        }

        return chunkMerger.merge(rawChunks, minChunkMs)
    }
}
