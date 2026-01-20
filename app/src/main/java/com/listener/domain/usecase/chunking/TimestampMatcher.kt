package com.listener.domain.usecase.chunking

import com.listener.domain.model.Word
import javax.inject.Inject

class TimestampMatcher @Inject constructor() {

    fun findEndTimestamp(
        sentenceWords: List<String>,
        allWords: List<Word>,
        startWordIndex: Int
    ): Pair<Double, Int>? {
        if (sentenceWords.isEmpty() || allWords.isEmpty()) return null

        // Try matching last 3, 2, 1 words
        for (nWords in 3 downTo 1) {
            if (sentenceWords.size >= nWords) {
                val tailWords = sentenceWords.takeLast(nWords)
                val expandedPattern = expandHyphenated(tailWords)

                val matchResult = findSequence(expandedPattern, startWordIndex, allWords)
                if (matchResult != null) {
                    val endWord = allWords[matchResult]
                    return Pair(endWord.end, matchResult + 1)
                }
            }
        }

        // Fallback: linear assignment
        val estimatedEndIndex = minOf(startWordIndex + sentenceWords.size - 1, allWords.size - 1)
        return if (estimatedEndIndex >= startWordIndex) {
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

    private fun findSequence(pattern: List<String>, startIdx: Int, words: List<Word>): Int? {
        if (pattern.isEmpty()) return null

        for (i in startIdx until words.size - pattern.size + 1) {
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
        return word.lowercase()
            .replace(Regex("[^a-z0-9-]"), "")
    }
}
