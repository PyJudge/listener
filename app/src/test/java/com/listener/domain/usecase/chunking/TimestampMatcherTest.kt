package com.listener.domain.usecase.chunking

import com.listener.domain.model.Word
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TimestampMatcherTest {

    private lateinit var matcher: TimestampMatcher

    @Before
    fun setup() {
        matcher = TimestampMatcher()
    }

    // ===== 버그 C 수정 테스트: 다국어 정규화 지원 =====

    @Test
    fun `normalize should preserve Korean characters`() {
        // 버그 C: "안녕!" → 기존: "", 수정 후: "안녕"
        val words = listOf(
            Word("안녕하세요!", 0.0, 1.0)
        )
        val sentenceWords = listOf("안녕하세요!")

        val result = matcher.findEndTimestamp(
            sentenceWords = sentenceWords,
            allWords = words,
            startWordIndex = 0
        )

        assertNotNull("Korean word should be matched", result)
        assertEquals(1.0, result!!.first, 0.01)
    }

    @Test
    fun `normalize should preserve Japanese characters`() {
        val words = listOf(
            Word("こんにちは!", 0.0, 1.0)
        )
        val sentenceWords = listOf("こんにちは!")

        val result = matcher.findEndTimestamp(
            sentenceWords = sentenceWords,
            allWords = words,
            startWordIndex = 0
        )

        assertNotNull("Japanese word should be matched", result)
    }

    @Test
    fun `normalize should preserve Chinese characters`() {
        val words = listOf(
            Word("你好!", 0.0, 1.0)
        )
        val sentenceWords = listOf("你好!")

        val result = matcher.findEndTimestamp(
            sentenceWords = sentenceWords,
            allWords = words,
            startWordIndex = 0
        )

        assertNotNull("Chinese word should be matched", result)
    }

    @Test
    fun `normalize should still work with English`() {
        val words = listOf(
            Word("Hello", 0.0, 0.5),
            Word("world!", 0.5, 1.0)
        )
        val sentenceWords = listOf("Hello", "world!")

        val result = matcher.findEndTimestamp(
            sentenceWords = sentenceWords,
            allWords = words,
            startWordIndex = 0
        )

        assertNotNull("English words should be matched", result)
        assertEquals(1.0, result!!.first, 0.01)
    }

    @Test
    fun `normalize should remove punctuation from all languages`() {
        val words = listOf(
            Word("test,", 0.0, 0.5),
            Word("word.", 0.5, 1.0)
        )
        val sentenceWords = listOf("test,", "word.")

        val result = matcher.findEndTimestamp(
            sentenceWords = sentenceWords,
            allWords = words,
            startWordIndex = 0
        )

        assertNotNull("Punctuation should be stripped for matching", result)
    }

    @Test
    fun `normalize should handle mixed language content`() {
        val words = listOf(
            Word("Hello", 0.0, 0.5),
            Word("안녕", 0.5, 1.0),
            Word("world!", 1.0, 1.5)
        )
        val sentenceWords = listOf("Hello", "안녕", "world!")

        val result = matcher.findEndTimestamp(
            sentenceWords = sentenceWords,
            allWords = words,
            startWordIndex = 0
        )

        assertNotNull("Mixed language should be matched", result)
        assertEquals(1.5, result!!.first, 0.01)
    }

    // ===== 기본 동작 테스트 =====

    @Test
    fun `findEndTimestamp should return null for empty inputs`() {
        val result = matcher.findEndTimestamp(
            sentenceWords = emptyList(),
            allWords = emptyList(),
            startWordIndex = 0
        )
        assertNull(result)
    }

    @Test
    fun `findEndTimestamp should match last word sequence`() {
        val words = listOf(
            Word("The", 0.0, 0.2),
            Word("quick", 0.2, 0.5),
            Word("brown", 0.5, 0.8),
            Word("fox", 0.8, 1.0)
        )
        val sentenceWords = listOf("The", "quick", "brown", "fox")

        val result = matcher.findEndTimestamp(
            sentenceWords = sentenceWords,
            allWords = words,
            startWordIndex = 0
        )

        assertNotNull(result)
        assertEquals(1.0, result!!.first, 0.01)
        assertEquals(4, result.second)
    }

    @Test
    fun `findEndTimestamp should respect maxSearchIndex`() {
        val words = listOf(
            Word("word1", 0.0, 0.5),
            Word("word2", 0.5, 1.0),
            Word("target", 1.0, 1.5),  // This is beyond maxSearchIndex
            Word("end", 1.5, 2.0)
        )
        val sentenceWords = listOf("target", "end")

        val result = matcher.findEndTimestamp(
            sentenceWords = sentenceWords,
            allWords = words,
            startWordIndex = 0,
            maxSearchIndex = 2  // Only search first 2 words
        )

        // Should fallback since target is at index 2
        assertNotNull(result)
    }

    // ===== Chunk-Sound 동기화 버그 테스트 (Essential 에피소드 실제 데이터) =====

    @Test
    fun `should match that-dot not that - essential episode chunk 14`() {
        // 실제 essential 에피소드의 word timestamps
        val words = listOf(
            Word("or", 59.88, 60.08),           // index 0
            Word("experienced", 60.08, 60.44),  // index 1
            Word("something,", 60.44, 61.18),   // index 2
            Word("our", 61.18, 61.72),          // index 3
            Word("brains", 61.72, 62.0),        // index 4
            Word("are", 62.0, 62.18),           // index 5
            Word("drawn", 62.18, 62.44),        // index 6
            Word("to", 62.44, 62.68),           // index 7
            Word("that.", 62.26, 62.98),        // index 8 - 첫 번째 "that."
            Word("Our", 62.98, 63.04),          // index 9
            Word("attentional", 63.04, 63.56),  // index 10
            Word("systems", 63.56, 63.9),       // index 11
            Word("draw", 63.9, 64.28),          // index 12
            Word("us", 64.28, 64.48),           // index 13
            Word("to", 64.48, 64.6),            // index 14
            Word("that.", 64.6, 64.94),         // index 15 - 두 번째 "that." ← 정답!
            Word("And", 64.94, 65.44),          // index 16
            Word("when", 65.44, 65.82),         // index 17
            Word("you", 65.82, 66.1),           // index 18
            Word("are", 66.1, 66.24),           // index 19
            Word("paying", 66.24, 66.42),       // index 20
            Word("attention", 66.42, 66.7),     // index 21
            Word("to", 66.7, 66.98),            // index 22
            Word("something,", 66.98, 67.38),   // index 23
            Word("that's", 66.88, 67.92),       // index 24
            Word("part", 67.92, 68.58),         // index 25
            Word("of", 68.58, 68.84),           // index 26
            Word("what", 68.84, 69.06),         // index 27
            Word("makes", 69.06, 69.42),        // index 28
            Word("things", 69.42, 69.68),       // index 29
            Word("memorable.", 69.68, 70.06),   // index 30
            Word("Second", 70.06, 71.06),       // index 31
            Word("is", 71.06, 71.28),           // index 32
            Word("repetition.", 71.28, 71.64),  // index 33
            Word("Third", 71.64, 72.92),        // index 34
            Word("is", 72.92, 73.56),           // index 35
            Word("association.", 73.56, 74.28), // index 36
            Word("So", 75.38, 75.68),           // index 37
            Word("if", 75.68, 76.52),           // index 38
            Word("you", 76.52, 76.96),          // index 39
            Word("meet", 76.96, 78.08),         // index 40
            Word("somebody", 78.08, 78.5),      // index 41
            Word("new", 78.5, 79.1),            // index 42
            Word("that", 79.1, 79.9),           // index 43 - "that" (마침표 없음) ← 오답!
            Word("knows", 79.9, 80.44),         // index 44
        )

        // 문장: "Our attentional systems draw us to that."
        // 매칭할 마지막 단어들: ["draw", "us", "to", "that."]
        val sentenceWords = listOf("Our", "attentional", "systems", "draw", "us", "to", "that.")

        val result = matcher.findEndTimestamp(
            sentenceWords = sentenceWords,
            allWords = words,
            startWordIndex = 9,  // "Our"부터 시작
            maxSearchIndex = words.size,
            sentenceStartTime = 62.98  // "Our"의 시작 시간
        )

        // "that."(index 15, end=64.94)를 매칭해야 함
        // "that"(index 43, end=79.9)를 매칭하면 안 됨!
        assertNotNull("Should find matching words", result)
        assertEquals("Should match 'that.' at index 15, not 'that' at index 43",
            64.94, result!!.first, 0.01)
    }

    @Test
    fun `should distinguish that-dot from that without dot`() {
        // 간단한 케이스: "that."과 "that"이 모두 있을 때 "that."만 매칭해야 함
        val words = listOf(
            Word("to", 0.0, 0.1),
            Word("that.", 0.1, 0.2),    // 마침표 있음 ← 정답
            Word("And", 0.2, 0.3),
            Word("that", 0.3, 0.4),     // 마침표 없음 ← 오답
        )

        val sentenceWords = listOf("to", "that.")

        val result = matcher.findEndTimestamp(
            sentenceWords = sentenceWords,
            allWords = words,
            startWordIndex = 0
        )

        assertNotNull(result)
        // "that."(index 1, end=0.2)를 매칭해야 함
        assertEquals(0.2, result!!.first, 0.01)
        // "that"(index 3, end=0.4)를 매칭하면 안 됨
        assertNotEquals(0.4, result.first, 0.01)
    }
}
