package com.listener.domain.usecase.chunking

import com.listener.domain.model.Chunk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChunkMergerTest {

    private lateinit var merger: ChunkMerger

    @Before
    fun setup() {
        merger = ChunkMerger()
    }

    // ========== Sentence Boundary Tests ==========

    @Test
    fun `merge chunks without sentence ending punctuation`() {
        // "but even now we still" + "don't know for sure who did it."
        val chunks = listOf(
            Chunk(0, 0, 1500, "but even now we still"),
            Chunk(1, 1500, 3000, "don't know for sure who did it.")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(1, result.size)
        assertEquals("but even now we still don't know for sure who did it.", result[0].displayText)
    }

    @Test
    fun `keep chunks separate when both end with period`() {
        val chunks = listOf(
            Chunk(0, 0, 1500, "Hello world."),
            Chunk(1, 1500, 3000, "How are you?")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(2, result.size)
        assertEquals("Hello world.", result[0].displayText)
        assertEquals("How are you?", result[1].displayText)
    }

    @Test
    fun `merge multiple incomplete sentences then flush on period`() {
        val chunks = listOf(
            Chunk(0, 0, 500, "In 1999,"),
            Chunk(1, 500, 1000, "four Russian apartment buildings"),
            Chunk(2, 1000, 1500, "were bombed."),
            Chunk(3, 1500, 2000, "Hundreds killed.")
        )
        val result = merger.merge(chunks, minChunkMs = 500)
        assertEquals(2, result.size)
        assertEquals("In 1999, four Russian apartment buildings were bombed.", result[0].displayText)
        assertEquals("Hundreds killed.", result[1].displayText)
    }

    @Test
    fun `handle exclamation and question marks`() {
        val chunks = listOf(
            Chunk(0, 0, 1000, "What did they miss"),
            Chunk(1, 1000, 2000, "the first time?"),
            Chunk(2, 2000, 3000, "That's incredible!")
        )
        val result = merger.merge(chunks, minChunkMs = 500)
        assertEquals(2, result.size)
        assertEquals("What did they miss the first time?", result[0].displayText)
        assertEquals("That's incredible!", result[1].displayText)
    }

    @Test
    fun `handle sentence ending with quote after punctuation`() {
        val chunks = listOf(
            Chunk(0, 0, 1000, "He said"),
            Chunk(1, 1000, 2000, "\"Hello.\""),
            Chunk(2, 2000, 3000, "Then left.")
        )
        val result = merger.merge(chunks, minChunkMs = 500)
        assertEquals(2, result.size)
        assertEquals("He said \"Hello.\"", result[0].displayText)
        assertEquals("Then left.", result[1].displayText)
    }

    // ========== Duration Merge Tests (with sentence endings) ==========

    @Test
    fun `merge short sentences by duration`() {
        val chunks = listOf(
            Chunk(0, 0, 500, "Hi."),
            Chunk(1, 500, 1000, "Yes.")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(1, result.size)
        assertEquals("Hi. Yes.", result[0].displayText)
    }

    @Test
    fun `keep long sentences separate`() {
        val chunks = listOf(
            Chunk(0, 0, 1500, "This is a complete sentence."),
            Chunk(1, 1500, 3000, "This is another complete sentence.")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(2, result.size)
    }

    @Test
    fun `merge last short chunk with previous`() {
        val chunks = listOf(
            Chunk(0, 0, 1500, "Hello world."),
            Chunk(1, 1500, 1800, "Yes.")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(1, result.size)
        assertEquals("Hello world. Yes.", result[0].displayText)
    }

    // ========== Edge Cases ==========

    @Test
    fun `handle empty list`() {
        val result = merger.merge(emptyList(), minChunkMs = 1200)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `handle single chunk without sentence ending`() {
        val chunks = listOf(Chunk(0, 0, 800, "Hello"))
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(1, result.size)
        assertEquals("Hello", result[0].displayText)
    }

    @Test
    fun `handle single chunk with sentence ending`() {
        val chunks = listOf(Chunk(0, 0, 800, "Hello."))
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(1, result.size)
        assertEquals("Hello.", result[0].displayText)
    }

    @Test
    fun `reindex after merge`() {
        val chunks = listOf(
            Chunk(5, 0, 1500, "First sentence."),
            Chunk(10, 1500, 1700, "Short."),
            Chunk(15, 1700, 3200, "Second sentence.")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        result.forEachIndexed { index, chunk ->
            assertEquals(index, chunk.orderIndex)
        }
    }

    @Test
    fun `complex real world scenario`() {
        // Simulating Whisper output where segment boundaries don't match sentence boundaries
        val chunks = listOf(
            Chunk(0, 0, 3500, "This BBC podcast is supported by ads outside the UK."),
            Chunk(1, 5500, 10500, "If journalism is the first draft of history, what happens if that draft is flawed?"),
            Chunk(2, 11000, 18500, "In 1999, four Russian apartment buildings were bombed, hundreds killed, but even now we still"),
            Chunk(3, 18500, 20500, "don't know for sure who did it.")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)

        assertEquals(3, result.size)
        assertEquals("This BBC podcast is supported by ads outside the UK.", result[0].displayText)
        assertEquals("If journalism is the first draft of history, what happens if that draft is flawed?", result[1].displayText)
        assertEquals("In 1999, four Russian apartment buildings were bombed, hundreds killed, but even now we still don't know for sure who did it.", result[2].displayText)
    }
}
