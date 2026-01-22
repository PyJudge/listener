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

    /**
     * Whisper 결과를 청크로 분할합니다 (ankigpt 방식).
     *
     * 알고리즘:
     * 1. 전체 세그먼트 텍스트를 합쳐서 한번에 처리
     * 2. 문장별로 검색 범위 제한 (wordIdx + wordCount * 3 + 100)
     * 3. 시간 검증으로 역방향 매칭 방지
     * 4. Fallback: 세그먼트 타임스탬프 대신 단어 배열에서 계산
     */
    fun process(
        whisperResult: WhisperResult,
        sentenceOnly: Boolean = true,
        minChunkMs: Long = 1200L
    ): List<Chunk> {
        val words = duplicateRemover.removeDuplicates(whisperResult.words)
        val segments = whisperResult.segments

        if (segments.isEmpty()) return emptyList()

        // 1. 전체 세그먼트 텍스트 합치기
        val fullText = segments.joinToString(" ") { it.text.trim() }

        // 2. 문장 분리
        val sentences = sentenceSplitter.split(fullText, sentenceOnly)

        if (sentences.isEmpty()) return emptyList()
        if (words.isEmpty()) {
            // 단어 타임스탬프 없으면 세그먼트 타임스탬프 사용
            val segmentStart = segments.first().start
            val segmentEnd = segments.last().end
            return sentences.mapIndexed { index, sentence ->
                Chunk(
                    orderIndex = index,
                    startMs = (segmentStart * 1000).toLong(),
                    endMs = (segmentEnd * 1000).toLong(),
                    displayText = sentence
                )
            }
        }

        // 3. 각 문장에 타임스탬프 매칭 (ankigpt 방식)
        val rawChunks = mutableListOf<Chunk>()
        var wordIdx = 0
        var prevEndMs = 0L  // 시간순 보장용

        for ((index, sentence) in sentences.withIndex()) {
            // [FIX] words 소진 시 루프 종료 - 중복 청크 방지
            if (wordIdx >= words.size) break

            val sentenceWords = sentence.split(Regex("\\s+")).filter { it.isNotEmpty() }
            val wordCount = sentenceWords.size

            // 시작 인덱스 (이 chunk가 사용할 word 범위의 시작)
            val startWordIdx = wordIdx

            // 검색 범위 제한 (ankigpt 핵심)
            val maxSearch = minOf(wordIdx + wordCount * 3 + 100, words.size)

            // 마지막 N개 단어로 매칭하여 끝 인덱스 찾기
            val matchResult = timestampMatcher.findEndTimestamp(
                sentenceWords = sentenceWords,
                allWords = words,
                startWordIndex = wordIdx,
                maxSearchIndex = maxSearch,
                sentenceStartTime = words.getOrNull(startWordIdx)?.start ?: 0.0
            )

            // 끝 인덱스 결정 (newWordIdx는 다음 chunk의 시작이므로 -1)
            val endWordIdx = if (matchResult != null) {
                matchResult.second - 1
            } else {
                // Fallback: 단어 수만큼 전진
                minOf(wordIdx + wordCount - 1, words.size - 1).coerceAtLeast(startWordIdx)
            }

            // 다음 chunk의 시작 인덱스
            wordIdx = endWordIdx + 1

            // 타임스탬프는 words에서, displayText는 원본 sentence에서
            // Whisper가 겹치는 타임스탬프를 출력할 수 있으므로 시간순 보장
            val startMs = maxOf((words[startWordIdx].start * 1000).toLong(), prevEndMs)
            val endMs = maxOf((words[endWordIdx].end * 1000).toLong(), startMs)
            prevEndMs = endMs

            rawChunks.add(
                Chunk(
                    orderIndex = index,
                    startMs = startMs,
                    endMs = endMs,
                    displayText = sentence  // 원본 sentence 사용 (구두점 보존)
                )
            )
        }

        return chunkMerger.merge(rawChunks, minChunkMs)
    }
}
