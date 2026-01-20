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

    @Test
    fun `merge short chunks`() {
        val chunks = listOf(
            Chunk(0, 0, 800, "Hello"),
            Chunk(1, 800, 1400, "world")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(1, result.size)
        assertEquals(0, result[0].startMs)
        assertEquals(1400, result[0].endMs)
        assertEquals("Hello world", result[0].displayText)
    }

    @Test
    fun `keep long chunks separate`() {
        val chunks = listOf(
            Chunk(0, 0, 1500, "Hello"),
            Chunk(1, 1500, 2800, "world")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(2, result.size)
    }

    @Test
    fun `merge last short chunk with previous`() {
        val chunks = listOf(
            Chunk(0, 0, 1500, "Hello"),
            Chunk(1, 1500, 2000, "world")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(1, result.size)
        assertEquals("Hello world", result[0].displayText)
    }

    @Test
    fun `handle all short chunks`() {
        val chunks = listOf(
            Chunk(0, 0, 500, "A"),
            Chunk(1, 500, 1000, "B"),
            Chunk(2, 1000, 1500, "C")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(1, result.size)
        assertEquals("A B C", result[0].displayText)
    }

    @Test
    fun `handle single chunk`() {
        val chunks = listOf(Chunk(0, 0, 800, "Hello"))
        val result = merger.merge(chunks, minChunkMs = 1200)
        assertEquals(1, result.size)
        assertEquals("Hello", result[0].displayText)
    }

    @Test
    fun `reindex after merge`() {
        val chunks = listOf(
            Chunk(0, 0, 1500, "First"),
            Chunk(1, 1500, 1700, "short"),
            Chunk(2, 1700, 3200, "Second")
        )
        val result = merger.merge(chunks, minChunkMs = 1200)
        result.forEachIndexed { index, chunk ->
            assertEquals(index, chunk.orderIndex)
        }
    }
}
