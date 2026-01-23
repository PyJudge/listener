package com.listener.domain.usecase.chunking.aligner

import com.listener.domain.model.Word
import javax.inject.Inject

/**
 * 알고리즘 3: Two-Pointer with Fuzzy Score
 *
 * 두 포인터를 동시에 이동하며 유사도 기반 매칭.
 * - 장점: Filler 자동 스킵, Fuzzy 매칭
 * - 단점: 휴리스틱 의존
 */
class TwoPointerAligner @Inject constructor() : PhraseAligner {

    private val fillerWords = setOf(
        "um", "uh", "like", "yeah", "so", "well", "okay", "ok",
        "right", "you know", "i mean", "actually", "basically"
    )

    private val maxLookAhead = 10  // filler가 많은 경우를 위해 확장

    override fun align(phrases: List<String>, words: List<Word>): List<AlignResult> {
        if (phrases.isEmpty()) return emptyList()
        if (words.isEmpty()) {
            return phrases.mapIndexed { idx, _ -> AlignResult(idx, null, AlignOp.PHRASE_ONLY) }
        }

        val results = mutableListOf<AlignResult>()
        var p = 0
        var w = 0

        while (p < phrases.size) {
            if (w >= words.size) {
                // Words 소진 - 남은 phrases는 PHRASE_ONLY
                results.add(AlignResult(p, null, AlignOp.PHRASE_ONLY))
                p++
                continue
            }

            val pNorm = normalize(phrases[p])
            val wNorm = normalize(words[w].word)

            when {
                // 완전 매칭
                pNorm == wNorm -> {
                    results.add(AlignResult(p, w, AlignOp.MATCH))
                    p++
                    w++
                }

                // Fuzzy match (편집거리 1 이하) - 문자열 길이가 최소 2 이상일 때만
                isFuzzyMatch(pNorm, wNorm) -> {
                    results.add(AlignResult(p, w, AlignOp.MATCH))
                    p++
                    w++
                }

                // Filler word → skip
                isFiller(wNorm) -> {
                    results.add(AlignResult(null, w, AlignOp.WORD_ONLY))
                    w++
                }

                // 앞에 더 좋은 매칭이 있는지 확인
                else -> {
                    val lookAhead = findBestMatch(pNorm, words, w, w + maxLookAhead)
                    if (lookAhead != null) {
                        // lookAhead까지의 words는 filler로 스킵
                        for (skipIdx in w until lookAhead) {
                            results.add(AlignResult(null, skipIdx, AlignOp.WORD_ONLY))
                        }
                        results.add(AlignResult(p, lookAhead, AlignOp.MATCH))
                        p++
                        w = lookAhead + 1
                    } else {
                        // 매칭 실패 - phrase 전진
                        results.add(AlignResult(p, null, AlignOp.PHRASE_ONLY))
                        p++
                    }
                }
            }
        }

        // 남은 words
        while (w < words.size) {
            results.add(AlignResult(null, w, AlignOp.WORD_ONLY))
            w++
        }

        return results
    }

    /**
     * Filler word 체크 - 명시적인 filler 단어만 체크
     * 짧은 단어라도 의미 있는 단어일 수 있으므로 길이 기반 체크 제거
     */
    private fun isFiller(word: String): Boolean {
        return word in fillerWords
    }

    /**
     * Fuzzy match 체크 - Levenshtein 거리 1 이하이고 둘 다 길이 2 이상
     */
    private fun isFuzzyMatch(s1: String, s2: String): Boolean {
        if (s1.isEmpty() || s2.isEmpty()) return false
        // 너무 짧은 단어는 fuzzy match 제외 (오탐 방지)
        if (s1.length < 2 || s2.length < 2) return false
        return levenshtein(s1, s2) <= 1
    }

    /**
     * lookAhead 범위에서 최적의 매칭 찾기
     * - 완전 매칭 우선
     * - 없으면 fuzzy 매칭
     */
    private fun findBestMatch(target: String, words: List<Word>, start: Int, end: Int): Int? {
        // 1차: 완전 매칭 찾기
        for (i in start until minOf(end, words.size)) {
            if (normalize(words[i].word) == target) return i
        }

        // 2차: Fuzzy 매칭 찾기 (편집거리 1 이하)
        if (target.length >= 2) {
            for (i in start until minOf(end, words.size)) {
                val wNorm = normalize(words[i].word)
                if (wNorm.length >= 2 && levenshtein(target, wNorm) <= 1) {
                    return i
                }
            }
        }

        return null
    }

    private fun levenshtein(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + if (s1[i - 1] == s2[j - 1]) 0 else 1
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
