package com.listener.domain.usecase.chunking

import com.listener.domain.model.Word
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DuplicateRemoverTest {

    private lateinit var remover: DuplicateRemover

    @Before
    fun setup() {
        remover = DuplicateRemover()
    }

    @Test
    fun `remove duplicate words with overlapping timestamps`() {
        val words = listOf(
            Word("hello", 1.0, 1.5),
            Word("hello", 1.0, 1.5)
        )
        val result = remover.removeDuplicates(words)
        assertEquals(1, result.size)
        assertEquals("hello", result[0].word)
    }

    @Test
    fun `keep non-duplicate words`() {
        val words = listOf(
            Word("hello", 1.0, 1.5),
            Word("world", 2.0, 2.5)
        )
        val result = remover.removeDuplicates(words)
        assertEquals(2, result.size)
    }

    @Test
    fun `skip when overlapping and same word`() {
        val words = listOf(
            Word("hello", 1.0, 2.0),
            Word("hello", 1.5, 2.5) // start < prev.end
        )
        val result = remover.removeDuplicates(words)
        assertEquals(1, result.size)
    }

    @Test
    fun `handle empty list`() {
        val result = remover.removeDuplicates(emptyList())
        assertTrue(result.isEmpty())
    }
}
