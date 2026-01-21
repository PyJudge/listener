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

    // ===== 약어 테스트 =====

    @Test
    fun `do not split on Mr`() {
        val result = splitter.split("Mr. Smith went home.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Mr. Smith went home.", result[0])
    }

    @Test
    fun `do not split on Dr`() {
        val result = splitter.split("Dr. Jones is here.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Dr. Jones is here.", result[0])
    }

    @Test
    fun `do not split on Mrs`() {
        val result = splitter.split("Mrs. Brown called.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Mrs. Brown called.", result[0])
    }

    @Test
    fun `do not split on Ms`() {
        val result = splitter.split("Ms. Lee arrived.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Ms. Lee arrived.", result[0])
    }

    @Test
    fun `do not split on Prof`() {
        val result = splitter.split("Prof. Kim teaches.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Prof. Kim teaches.", result[0])
    }

    @Test
    fun `do not split on etc`() {
        val result = splitter.split("Apples, oranges, etc. are fruits.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Apples, oranges, etc. are fruits.", result[0])
    }

    @Test
    fun `do not split on vs`() {
        val result = splitter.split("Red vs. Blue is a show.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Red vs. Blue is a show.", result[0])
    }

    @Test
    fun `do not split on eg`() {
        val result = splitter.split("Fruits, e.g. apples, are healthy.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Fruits, e.g. apples, are healthy.", result[0])
    }

    @Test
    fun `do not split on ie`() {
        val result = splitter.split("The best one, i.e. the red one, won.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("The best one, i.e. the red one, won.", result[0])
    }

    @Test
    fun `do not split on initials`() {
        val result = splitter.split("John F. Kennedy was president.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("John F. Kennedy was president.", result[0])
    }

    @Test
    fun `do not split on multiple initials`() {
        val result = splitter.split("J. R. R. Tolkien wrote books.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("J. R. R. Tolkien wrote books.", result[0])
    }

    @Test
    fun `do not split on USA pattern`() {
        val result = splitter.split("The U.S.A. is large.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("The U.S.A. is large.", result[0])
    }

    @Test
    fun `do not split on US pattern`() {
        val result = splitter.split("The U.S. economy grew.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("The U.S. economy grew.", result[0])
    }

    @Test
    fun `do not split on PhD`() {
        val result = splitter.split("She has a Ph.D. in physics.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("She has a Ph.D. in physics.", result[0])
    }

    @Test
    fun `do not split on Inc`() {
        val result = splitter.split("Apple Inc. makes phones.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Apple Inc. makes phones.", result[0])
    }

    @Test
    fun `do not split on Corp`() {
        val result = splitter.split("Microsoft Corp. is big.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Microsoft Corp. is big.", result[0])
    }

    @Test
    fun `do not split on Jr`() {
        val result = splitter.split("Martin Luther King Jr. was a leader.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Martin Luther King Jr. was a leader.", result[0])
    }

    @Test
    fun `do not split on Sr`() {
        val result = splitter.split("John Smith Sr. retired.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("John Smith Sr. retired.", result[0])
    }

    @Test
    fun `do not split on St for street`() {
        val result = splitter.split("123 Main St. is nearby.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("123 Main St. is nearby.", result[0])
    }

    @Test
    fun `do not split on Ave`() {
        val result = splitter.split("5th Ave. has shops.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("5th Ave. has shops.", result[0])
    }

    @Test
    fun `do not split on months`() {
        val result = splitter.split("On Jan. 1st we celebrate.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("On Jan. 1st we celebrate.", result[0])
    }

    @Test
    fun `do not split on No`() {
        val result = splitter.split("Item No. 5 is sold.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Item No. 5 is sold.", result[0])
    }

    // ===== 복합 테스트 =====

    @Test
    fun `handle abbreviation followed by real sentence end`() {
        val result = splitter.split("Mr. Smith left. He was tired.", sentenceOnly = true)
        assertEquals(2, result.size)
        assertEquals("Mr. Smith left.", result[0])
        assertEquals("He was tired.", result[1])
    }

    @Test
    fun `handle multiple abbreviations in one sentence`() {
        val result = splitter.split("Dr. Smith and Prof. Jones met.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Dr. Smith and Prof. Jones met.", result[0])
    }

    @Test
    fun `handle abbreviation at end of text`() {
        val result = splitter.split("He works for Apple Inc.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("He works for Apple Inc.", result[0])
    }

    @Test
    fun `complex sentence with multiple abbreviations`() {
        val result = splitter.split("Dr. J. Smith Jr. of the U.S. met Prof. Kim.", sentenceOnly = true)
        assertEquals(1, result.size)
        assertEquals("Dr. J. Smith Jr. of the U.S. met Prof. Kim.", result[0])
    }

    @Test
    fun `normal sentence should still split`() {
        val result = splitter.split("Hello world. Goodbye world.", sentenceOnly = true)
        assertEquals(2, result.size)
        assertEquals("Hello world.", result[0])
        assertEquals("Goodbye world.", result[1])
    }

    // ===== 버그 A 수정 테스트: remaining을 새 문장으로 추가 (ankigpt 방식) =====

    @Test
    fun `remaining text should be new sentence not appended`() {
        // 버그 A: "Hello. Test" → 기존: ["Hello. Test"], 수정 후: ["Hello.", "Test"]
        val result = splitter.split("Hello. Test", sentenceOnly = true)
        assertEquals(2, result.size)
        assertEquals("Hello.", result[0])
        assertEquals("Test", result[1])
    }

    @Test
    fun `remaining text without punctuation should be separate`() {
        val result = splitter.split("First sentence. Second sentence. Remaining part", sentenceOnly = true)
        assertEquals(3, result.size)
        assertEquals("First sentence.", result[0])
        assertEquals("Second sentence.", result[1])
        assertEquals("Remaining part", result[2])
    }

    @Test
    fun `multiple sentences with remaining`() {
        val result = splitter.split("One. Two. Three", sentenceOnly = true)
        assertEquals(3, result.size)
        assertEquals("One.", result[0])
        assertEquals("Two.", result[1])
        assertEquals("Three", result[2])
    }
}
