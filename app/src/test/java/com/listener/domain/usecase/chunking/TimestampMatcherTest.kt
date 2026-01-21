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
}
