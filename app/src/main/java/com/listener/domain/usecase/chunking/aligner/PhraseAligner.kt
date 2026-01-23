package com.listener.domain.usecase.chunking.aligner

import com.listener.domain.model.Word

/**
 * Phrase-Word 정렬 결과
 */
data class AlignResult(
    val phraseIdx: Int?,    // null = phrase에 없음 (word만 있음)
    val wordIdx: Int?,      // null = word에 없음 (phrase만 있음)
    val op: AlignOp
)

enum class AlignOp {
    MATCH,          // phrase와 word 매칭
    PHRASE_ONLY,    // phrase만 있음 (word에 없음)
    WORD_ONLY       // word만 있음 (filler 등)
}

/**
 * 정렬된 토큰 (텍스트 + 타임스탬프)
 */
data class AlignedToken(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

/**
 * 시간 범위
 */
data class TimeRange(
    val start: Double,
    val end: Double
)

/**
 * Phrase-Word 정렬 인터페이스
 *
 * 핵심 원칙:
 * - 텍스트 = Phrases 기준 (구두점 보존)
 * - 타임스탬프 = Words 기준
 */
interface PhraseAligner {
    /**
     * Phrase 토큰들을 Word 토큰들에 정렬
     *
     * @param phrases 정렬할 phrase 토큰들 (구두점 포함)
     * @param words Word 토큰들 (타임스탬프 포함)
     * @return 정렬 결과
     */
    fun align(phrases: List<String>, words: List<Word>): List<AlignResult>

    /**
     * 문자열 정규화 (비교용)
     */
    fun normalize(text: String): String {
        return text.lowercase().replace(Regex("[\\p{P}\\p{S}]"), "")
    }
}
