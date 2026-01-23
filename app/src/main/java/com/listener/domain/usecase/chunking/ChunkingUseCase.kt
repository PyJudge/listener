package com.listener.domain.usecase.chunking

import com.listener.domain.model.Chunk
import com.listener.domain.model.WhisperResult
import com.listener.domain.model.Word
import com.listener.domain.usecase.chunking.aligner.TwoPointerAligner
import com.listener.domain.usecase.chunking.aligner.AlignOp
import com.listener.domain.usecase.chunking.aligner.TimestampAssigner
import javax.inject.Inject

/**
 * 타임스탬프가 붙은 조각 (내부 사용)
 */
private data class TimestampedFragment(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

class ChunkingUseCase @Inject constructor(
    private val sentenceSplitter: SentenceSplitter,
    private val chunkMerger: ChunkMerger,
    private val duplicateRemover: DuplicateRemover,
    private val aligner: TwoPointerAligner,
    private val timestampAssigner: TimestampAssigner
) {

    /**
     * Whisper 결과를 청크로 분할합니다.
     *
     * 2단계 파이프라인 알고리즘:
     * 1단계: 세그먼트별 타임스탬프 매칭 → 타임스탬프가 붙은 조각 생성
     * 2단계: 조각 병합하여 완전한 문장(Chunk) 생성
     */
    fun process(
        whisperResult: WhisperResult,
        sentenceOnly: Boolean = true,
        minChunkMs: Long = 1200L
    ): List<Chunk> {
        val allWords = duplicateRemover.removeDuplicates(whisperResult.words)
        val segments = whisperResult.segments

        if (segments.isEmpty()) return emptyList()

        // ===== 1단계: 세그먼트별 타임스탬프 매칭 =====
        val allFragments = mutableListOf<TimestampedFragment>()

        for (segment in segments) {
            // 이 세그먼트 시간 범위 내의 words (0.5초 여유)
            val segmentWords = allWords.filter {
                it.start >= segment.start - 0.5 && it.start <= segment.end + 0.5
            }

            // 이 세그먼트의 조각들 (sentenceOnly=true로 문장 경계에서만 분리)
            val pieces = sentenceSplitter.split(segment.text.trim(), sentenceOnly)
            if (pieces.isEmpty()) continue

            if (segmentWords.isEmpty()) {
                // 단어 없으면 세그먼트 타임스탬프 사용
                for (piece in pieces) {
                    allFragments.add(
                        TimestampedFragment(
                            text = piece,
                            startMs = (segment.start * 1000).toLong(),
                            endMs = (segment.end * 1000).toLong()
                        )
                    )
                }
                continue
            }

            // 세그먼트 내에서 word 커서
            var localWordCursor = 0

            for (piece in pieces) {
                val phrases = piece.split(Regex("\\s+")).filter { it.isNotEmpty() }
                if (phrases.isEmpty()) continue

                if (localWordCursor >= segmentWords.size) {
                    // 남은 단어 없으면 마지막 단어 시간 사용
                    val lastWord = segmentWords.last()
                    allFragments.add(
                        TimestampedFragment(
                            text = piece,
                            startMs = (lastWord.start * 1000).toLong(),
                            endMs = (lastWord.end * 1000).toLong()
                        )
                    )
                    continue
                }

                // 첫 2개 단어로 정확한 위치 찾기
                val firstPhraseNorm = aligner.normalize(phrases.first())
                val secondPhraseNorm = if (phrases.size > 1) aligner.normalize(phrases[1]) else null

                val exactIdx = findMatchingWordPairInSegment(
                    segmentWords, localWordCursor, firstPhraseNorm, secondPhraseNorm
                )
                val matchStartIdx = exactIdx ?: localWordCursor

                // 윈도우 설정
                val windowEnd = minOf(matchStartIdx + phrases.size * 2 + 10, segmentWords.size)
                val windowWords = segmentWords.subList(matchStartIdx, windowEnd)

                // 정렬 실행
                val alignResults = aligner.align(phrases, windowWords)
                val matchedCount = alignResults.count { it.op == AlignOp.MATCH }

                // 타임스탬프 결정
                val startMs: Long
                var endMs: Long

                val lastMatchedRelativeIdx = alignResults
                    .filter { it.op == AlignOp.MATCH && it.wordIdx != null }
                    .maxByOrNull { it.wordIdx!! }
                    ?.wordIdx

                if (exactIdx != null) {
                    startMs = (segmentWords[exactIdx].start * 1000).toLong()
                    endMs = if (lastMatchedRelativeIdx != null && lastMatchedRelativeIdx < windowWords.size) {
                        (windowWords[lastMatchedRelativeIdx].end * 1000).toLong()
                    } else {
                        val fallbackIdx = minOf(phrases.size - 1, windowWords.size - 1)
                        (windowWords[fallbackIdx].end * 1000).toLong()
                    }
                } else if (windowWords.isNotEmpty()) {
                    startMs = (windowWords.first().start * 1000).toLong()
                    endMs = if (lastMatchedRelativeIdx != null && lastMatchedRelativeIdx < windowWords.size) {
                        (windowWords[lastMatchedRelativeIdx].end * 1000).toLong()
                    } else {
                        val fallbackIdx = minOf(phrases.size - 1, windowWords.size - 1)
                        (windowWords[fallbackIdx].end * 1000).toLong()
                    }
                } else {
                    val lastWord = segmentWords.last()
                    startMs = (lastWord.start * 1000).toLong()
                    endMs = (lastWord.end * 1000).toLong()
                }

                if (endMs <= startMs) {
                    endMs = startMs + 500
                }

                allFragments.add(
                    TimestampedFragment(
                        text = piece,
                        startMs = startMs,
                        endMs = endMs
                    )
                )

                // 커서 전진
                val minAdvance = maxOf(1, phrases.size / 2)
                localWordCursor = matchStartIdx + maxOf(minAdvance, matchedCount)
            }
        }

        // ===== 2단계: 조각 병합하여 Chunk 생성 =====
        val rawChunks = mutableListOf<Chunk>()
        val buffer = StringBuilder()
        var chunkStartMs = 0L
        var chunkEndMs = 0L
        var chunkIndex = 0

        for (frag in allFragments) {
            if (buffer.isEmpty()) {
                chunkStartMs = frag.startMs
            }
            if (buffer.isNotEmpty()) buffer.append(" ")
            buffer.append(frag.text)
            chunkEndMs = frag.endMs

            // 구분자로 끝나면 Chunk 완성
            if (frag.text.endsWithDelimiter()) {
                rawChunks.add(
                    Chunk(
                        orderIndex = chunkIndex++,
                        startMs = chunkStartMs,
                        endMs = chunkEndMs,
                        displayText = buffer.toString()
                    )
                )
                buffer.clear()
            }
        }

        // 마지막 남은 조각
        if (buffer.isNotEmpty()) {
            rawChunks.add(
                Chunk(
                    orderIndex = chunkIndex++,
                    startMs = chunkStartMs,
                    endMs = chunkEndMs,
                    displayText = buffer.toString()
                )
            )
        }

        // Post-processing: endMs 조정 (다음 chunk와 겹치지 않도록)
        for (i in 0 until rawChunks.size - 1) {
            val current = rawChunks[i]
            val next = rawChunks[i + 1]
            if (current.endMs > next.startMs) {
                rawChunks[i] = current.copy(endMs = next.startMs)
            }
        }

        return chunkMerger.merge(rawChunks, minChunkMs)
    }

    /**
     * 문자열이 문장 구분자로 끝나는지 확인
     */
    private fun String.endsWithDelimiter(): Boolean {
        val trimmed = this.trimEnd()
        return trimmed.endsWith('.') || trimmed.endsWith('?') || trimmed.endsWith('!')
    }

    /**
     * 세그먼트 내에서 첫 2개 단어가 연속으로 매칭되는 위치를 찾습니다.
     */
    private fun findMatchingWordPairInSegment(
        words: List<Word>,
        startIdx: Int,
        firstNorm: String,
        secondNorm: String?
    ): Int? {
        // 세그먼트 내 검색 (startIdx부터)
        for (i in startIdx until words.size) {
            if (aligner.normalize(words[i].word) != firstNorm) continue

            if (secondNorm == null) return i

            // 두 번째 단어 확인
            for (j in i + 1..minOf(i + 3, words.size - 1)) {
                if (aligner.normalize(words[j].word) == secondNorm) {
                    return i
                }
            }
        }

        // 2단어 매칭 실패 시 첫 단어만으로
        for (i in startIdx until words.size) {
            if (aligner.normalize(words[i].word) == firstNorm) {
                return i
            }
        }
        return null
    }

}
