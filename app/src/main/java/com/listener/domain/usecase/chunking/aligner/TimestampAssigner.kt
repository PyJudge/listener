package com.listener.domain.usecase.chunking.aligner

import com.listener.domain.model.Word
import javax.inject.Inject

/**
 * 타임스탬프 할당기
 *
 * 정렬 결과를 받아서 각 phrase 토큰에 타임스탬프를 할당.
 * 매칭 안 된 토큰은 인접 매칭에서 보간.
 */
class TimestampAssigner @Inject constructor() {

    /**
     * 정렬 결과를 AlignedToken 리스트로 변환
     *
     * @param phrases 원본 phrase 토큰들
     * @param words Word 토큰들
     * @param alignResults 정렬 결과
     * @return 타임스탬프가 할당된 토큰 리스트
     */
    fun assign(
        phrases: List<String>,
        words: List<Word>,
        alignResults: List<AlignResult>
    ): List<AlignedToken> {
        if (phrases.isEmpty()) return emptyList()

        val timestamps = mutableMapOf<Int, TimeRange>()

        // 1. 매칭된 phrase에 타임스탬프 할당
        for (result in alignResults) {
            if (result.op == AlignOp.MATCH && result.phraseIdx != null && result.wordIdx != null) {
                val w = words[result.wordIdx]
                timestamps[result.phraseIdx] = TimeRange(w.start, w.end)
            }
        }

        // 2. 매칭 안 된 phrase는 보간
        for (i in phrases.indices) {
            if (i !in timestamps) {
                timestamps[i] = interpolate(i, timestamps, phrases.size)
            }
        }

        // 3. AlignedToken 리스트 생성
        return phrases.mapIndexed { idx, text ->
            val ts = timestamps[idx]!!
            AlignedToken(
                text = text,
                startMs = (ts.start * 1000).toLong(),
                endMs = (ts.end * 1000).toLong()
            )
        }
    }

    /**
     * 인접 매칭에서 타임스탬프 보간
     */
    private fun interpolate(idx: Int, matched: Map<Int, TimeRange>, total: Int): TimeRange {
        val prev = (idx - 1 downTo 0).firstOrNull { it in matched }
        val next = (idx + 1 until total).firstOrNull { it in matched }

        return when {
            prev != null && next != null -> {
                // 선형 보간
                val ratio = (idx - prev).toDouble() / (next - prev)
                val start = matched[prev]!!.end + (matched[next]!!.start - matched[prev]!!.end) * ratio
                TimeRange(start, start + 0.1)
            }
            prev != null -> {
                // 이전 기준 연장
                TimeRange(matched[prev]!!.end, matched[prev]!!.end + 0.2)
            }
            next != null -> {
                // 다음 기준 역방향
                TimeRange(matched[next]!!.start - 0.2, matched[next]!!.start)
            }
            else -> TimeRange(0.0, 0.1)  // fallback
        }
    }
}
