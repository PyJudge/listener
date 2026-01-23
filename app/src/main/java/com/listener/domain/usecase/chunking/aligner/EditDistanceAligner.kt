package com.listener.domain.usecase.chunking.aligner

import com.listener.domain.model.Word
import javax.inject.Inject

/**
 * 알고리즘 1: Edit Distance (DP 정렬)
 *
 * 두 시퀀스를 동적 프로그래밍으로 최적 정렬.
 * - 장점: 최적 정렬 보장, 누락/추가 단어 완벽 처리
 * - 단점: O(n*m) 시간/공간
 */
class EditDistanceAligner @Inject constructor() : PhraseAligner {

    override fun align(phrases: List<String>, words: List<Word>): List<AlignResult> {
        if (phrases.isEmpty()) return emptyList()
        if (words.isEmpty()) {
            return phrases.mapIndexed { idx, _ -> AlignResult(idx, null, AlignOp.PHRASE_ONLY) }
        }

        val n = phrases.size
        val m = words.size

        // dp[i][j] = phrases[0..i), words[0..j) 정렬 최소 비용
        val dp = Array(n + 1) { IntArray(m + 1) { Int.MAX_VALUE / 2 } }
        val parent = Array(n + 1) { arrayOfNulls<Triple<Int, Int, AlignOp>>(m + 1) }

        // 초기화
        dp[0][0] = 0
        for (i in 1..n) {
            dp[i][0] = i
            parent[i][0] = Triple(i - 1, 0, AlignOp.PHRASE_ONLY)
        }
        for (j in 1..m) {
            dp[0][j] = j
            parent[0][j] = Triple(0, j - 1, AlignOp.WORD_ONLY)
        }

        // DP 채우기
        for (i in 1..n) {
            for (j in 1..m) {
                val pNorm = normalize(phrases[i - 1])
                val wNorm = normalize(words[j - 1].word)
                val matchCost = if (pNorm == wNorm) 0 else 2  // 대체 비용 높게

                // 매칭/대체
                if (dp[i - 1][j - 1] + matchCost < dp[i][j]) {
                    dp[i][j] = dp[i - 1][j - 1] + matchCost
                    parent[i][j] = Triple(i - 1, j - 1, AlignOp.MATCH)
                }

                // phrase만 (word에 없음)
                if (dp[i - 1][j] + 1 < dp[i][j]) {
                    dp[i][j] = dp[i - 1][j] + 1
                    parent[i][j] = Triple(i - 1, j, AlignOp.PHRASE_ONLY)
                }

                // word만 (filler)
                if (dp[i][j - 1] + 1 < dp[i][j]) {
                    dp[i][j] = dp[i][j - 1] + 1
                    parent[i][j] = Triple(i, j - 1, AlignOp.WORD_ONLY)
                }
            }
        }

        // Backtrack
        return backtrack(parent, n, m)
    }

    private fun backtrack(
        parent: Array<Array<Triple<Int, Int, AlignOp>?>>,
        i: Int,
        j: Int
    ): List<AlignResult> {
        if (i == 0 && j == 0) return emptyList()

        val p = parent[i][j] ?: return emptyList()
        val prev = backtrack(parent, p.first, p.second)

        val result = when (p.third) {
            AlignOp.MATCH -> AlignResult(i - 1, j - 1, AlignOp.MATCH)
            AlignOp.PHRASE_ONLY -> AlignResult(i - 1, null, AlignOp.PHRASE_ONLY)
            AlignOp.WORD_ONLY -> AlignResult(null, j - 1, AlignOp.WORD_ONLY)
        }
        return prev + result
    }
}
