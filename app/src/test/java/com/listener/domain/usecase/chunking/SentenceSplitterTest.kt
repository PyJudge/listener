package com.listener.domain.usecase.chunking

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SentenceSplitterTest {

    private lateinit var splitter: SentenceSplitter

    @Before
    fun setup() {
        splitter = SentenceSplitter()
    }

    @Test
    fun `split by period`() {
        val result = splitter.split("Hello. World.", sentenceOnly = true)
        assertEquals(2, result.size)
        assertEquals("Hello.", result[0])
        assertEquals("World.", result[1])
    }

    @Test
    fun `split by exclamation`() {
        val result = splitter.split("Great! Thanks.", sentenceOnly = true)
        assertEquals(2, result.size)
        assertEquals("Great!", result[0])
        assertEquals("Thanks.", result[1])
    }

    @Test
    fun `split by question mark`() {
        val result = splitter.split("How? Why?", sentenceOnly = true)
        assertEquals(2, result.size)
        assertEquals("How?", result[0])
        assertEquals("Why?", result[1])
    }

    @Test
    fun `split by comma when sentenceOnly is false`() {
        val result = splitter.split("First, second.", sentenceOnly = false)
        assertEquals(2, result.size)
        assertEquals("First,", result[0])
        assertEquals("second.", result[1])
    }

    @Test
    fun `keep comma when sentenceOnly is true`() {
        val result = splitter.split("First, second.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("First, second.", result[0])
    }

    @Test
    fun `handle consecutive punctuation`() {
        val result = splitter.split("Really?!", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Really?!", result[0])
    }
}
