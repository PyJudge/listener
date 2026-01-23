package com.listener.domain.usecase.chunking.aligner

import com.listener.domain.model.Word
import javax.inject.Inject

/**
 * 알고리즘 2: Greedy Sequential Matching
 *
 * Phrase를 순차 처리하며 가장 가까운 Word 매칭.
 * - 장점: O(n) 시간, 간단한 구현
 * - 단점: 최적 정렬 아님, lookAhead 밖의 매칭 놓칠 수 있음
 */
class GreedyAligner @Inject constructor() : PhraseAligner {

    private val maxLookAhead = 5  // 최대 몇 개 word까지 탐색

    override fun align(phrases: List<String>, words: List<Word>): List<AlignResult> {
        if (phrases.isEmpty()) return emptyList()
        if (words.isEmpty()) {
            return phrases.mapIndexed { idx, _ -> AlignResult(idx, null, AlignOp.PHRASE_ONLY) }
        }

        val results = mutableListOf<AlignResult>()
        var wordCursor = 0

        for ((phraseIdx, phrase) in phrases.withIndex()) {
            val pNorm = normalize(phrase)

            // wordCursor부터 maxLookAhead 범위에서 매칭 찾기
            var matched = false

            for (offset in 0 until maxLookAhead) {
                val wIdx = wordCursor + offset
                if (wIdx >= words.size) break

                if (normalize(words[wIdx].word) == pNorm) {
                    // 스킵된 words는 WORD_ONLY로 추가
                    for (skipIdx in wordCursor until wIdx) {
                        results.add(AlignResult(null, skipIdx, AlignOp.WORD_ONLY))
                    }

                    results.add(AlignResult(phraseIdx, wIdx, AlignOp.MATCH))
                    wordCursor = wIdx + 1
                    matched = true
                    break
                }
            }

            if (!matched) {
                // 매칭 실패 - phrase만 있음 (보간 필요)
                results.add(AlignResult(phraseIdx, null, AlignOp.PHRASE_ONLY))
            }
        }

        // 남은 words는 WORD_ONLY
        for (wIdx in wordCursor until words.size) {
            results.add(AlignResult(null, wIdx, AlignOp.WORD_ONLY))
        }

        return results
    }
}
