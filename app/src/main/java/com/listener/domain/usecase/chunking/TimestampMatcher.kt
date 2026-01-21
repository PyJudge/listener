package com.listener.domain.usecase.chunking

import com.listener.domain.model.Word
import javax.inject.Inject

class TimestampMatcher @Inject constructor() {

    /**
     * 문장의 끝 타임스탬프를 찾습니다.
     *
     * @param sentenceWords 문장의 단어 리스트
     * @param allWords Whisper 단어 배열
     * @param startWordIndex 검색 시작 인덱스
     * @param maxSearchIndex 검색 최대 인덱스 (ankigpt 방식: wordIdx + wordCount * 3 + 100)
     * @param sentenceStartTime 문장 시작 시간 (검증용)
     * @return Pair(끝 시간, 다음 wordIndex) 또는 null
     */
    fun findEndTimestamp(
        sentenceWords: List<String>,
        allWords: List<Word>,
        startWordIndex: Int,
        maxSearchIndex: Int = allWords.size,
        sentenceStartTime: Double = -1.0
    ): Pair<Double, Int>? {
        if (sentenceWords.isEmpty() || allWords.isEmpty()) return null

        val effectiveMaxIndex = minOf(maxSearchIndex, allWords.size)

        // Try matching last 3, 2, 1 words
        for (nWords in 3 downTo 1) {
            if (sentenceWords.size >= nWords) {
                val tailWords = sentenceWords.takeLast(nWords)
                val expandedPattern = expandHyphenated(tailWords)

                val matchResult = findSequence(expandedPattern, startWordIndex, allWords, effectiveMaxIndex)
                if (matchResult != null) {
                    val endWord = allWords[matchResult]
                    // 시간 검증: 찾은 단어가 문장 시작 이후인지 확인 (ankigpt 방식)
                    if (sentenceStartTime < 0 || endWord.start >= sentenceStartTime - 0.1) {
                        return Pair(endWord.end, matchResult + 1)
                    }
                }
            }
        }

        // Fallback: linear assignment (단어 배열 내에서만)
        val estimatedEndIndex = minOf(startWordIndex + sentenceWords.size - 1, effectiveMaxIndex - 1)
        return if (estimatedEndIndex >= startWordIndex && estimatedEndIndex < allWords.size) {
            Pair(allWords[estimatedEndIndex].end, estimatedEndIndex + 1)
        } else null
    }

    private fun expandHyphenated(words: List<String>): List<String> {
        return words.flatMap { word ->
            val clean = normalize(word)
            if (clean.contains("-")) {
                clean.split("-").filter { it.isNotEmpty() }
            } else {
                listOf(clean)
            }
        }
    }

    private fun findSequence(pattern: List<String>, startIdx: Int, words: List<Word>, maxIdx: Int = words.size): Int? {
        if (pattern.isEmpty()) return null

        val effectiveMax = minOf(maxIdx, words.size)
        for (i in startIdx until effectiveMax - pattern.size + 1) {
            var match = true
            for (j in pattern.indices) {
                if (normalize(words[i + j].word) != pattern[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                return i + pattern.size - 1
            }
        }
        return null
    }

    private fun normalize(word: String): String {
        // 다국어 지원: 구두점/기호만 제거, 모든 언어 문자 보존
        return word.lowercase()
            .replace(Regex("[\\p{P}\\p{S}]"), "")
    }
}
