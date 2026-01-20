package com.listener.domain.usecase

import com.listener.domain.model.Chunk
import com.listener.domain.model.ChunkSettings
import com.listener.domain.model.Segment
import com.listener.domain.model.WhisperResult
import com.listener.domain.repository.TranscriptionRepository
import com.listener.domain.usecase.chunking.ChunkingUseCase
import com.listener.service.RecordingManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RechunkUseCaseTest {

    private lateinit var transcriptionRepository: TranscriptionRepository
    private lateinit var chunkingUseCase: ChunkingUseCase
    private lateinit var recordingManager: RecordingManager
    private lateinit var rechunkUseCase: RechunkUseCase

    @Before
    fun setup() {
        transcriptionRepository = mock()
        chunkingUseCase = mock()
        recordingManager = mock()
        rechunkUseCase = RechunkUseCase(
            transcriptionRepository,
            chunkingUseCase,
            recordingManager
        )
    }

    @Test
    fun `same settings returns existing chunks without rechunk`() = runTest {
        val sourceId = "test-source"
        val settings = ChunkSettings(sentenceOnly = true, minChunkMs = 1200)
        val existingChunks = listOf(
            Chunk(0, 0, 1500, "Hello."),
            Chunk(1, 1500, 3000, "World.")
        )

        whenever(transcriptionRepository.getChunkSettings(sourceId)).thenReturn(settings)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(existingChunks)

        val result = rechunkUseCase.execute(sourceId, settings)

        assertTrue(result is RechunkResult.Success)
        assertEquals(existingChunks, (result as RechunkResult.Success).chunks)
        verify(chunkingUseCase, never()).process(any(), any(), any())
    }

    @Test
    fun `recordings exist returns RecordingsExist when not confirmed`() = runTest {
        val sourceId = "test-source"
        val oldSettings = ChunkSettings(sentenceOnly = true, minChunkMs = 1200)
        val newSettings = ChunkSettings(sentenceOnly = false, minChunkMs = 1000)
        val existingChunks = listOf(
            Chunk(0, 0, 1500, "Hello."),
            Chunk(1, 1500, 3000, "World.")
        )

        whenever(transcriptionRepository.getChunkSettings(sourceId)).thenReturn(oldSettings)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(existingChunks)
        whenever(recordingManager.hasRecording(sourceId, 0)).thenReturn(true)
        whenever(recordingManager.hasRecording(sourceId, 1)).thenReturn(false)

        val result = rechunkUseCase.execute(sourceId, newSettings, deleteRecordings = false)

        assertTrue(result is RechunkResult.RecordingsExist)
        assertEquals(1, (result as RechunkResult.RecordingsExist).recordingCount)
    }

    @Test
    fun `deletes recordings and rechunks when confirmed`() = runTest {
        val sourceId = "test-source"
        val oldSettings = ChunkSettings(sentenceOnly = true, minChunkMs = 1200)
        val newSettings = ChunkSettings(sentenceOnly = false, minChunkMs = 1000)
        val existingChunks = listOf(Chunk(0, 0, 1500, "Hello."))
        val whisperResult = WhisperResult(
            text = "Hello.",
            segments = listOf(Segment(0.0, 1.5, "Hello.")),
            words = emptyList()
        )
        val newChunks = listOf(Chunk(0, 0, 1000, "Hello,"), Chunk(1, 1000, 1500, "world."))

        whenever(transcriptionRepository.getChunkSettings(sourceId)).thenReturn(oldSettings)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(existingChunks)
        whenever(recordingManager.hasRecording(sourceId, 0)).thenReturn(true)
        whenever(transcriptionRepository.getTranscription(sourceId)).thenReturn(whisperResult)
        whenever(chunkingUseCase.process(whisperResult, false, 1000)).thenReturn(newChunks)

        val result = rechunkUseCase.execute(sourceId, newSettings, deleteRecordings = true)

        verify(recordingManager).deleteAllRecordings(sourceId)
        verify(transcriptionRepository).saveChunks(sourceId, newChunks)
        assertTrue(result is RechunkResult.Success)
        assertEquals(newChunks, (result as RechunkResult.Success).chunks)
    }

    @Test
    fun `returns error when transcription not found`() = runTest {
        val sourceId = "test-source"
        val oldSettings = ChunkSettings(sentenceOnly = true, minChunkMs = 1200)
        val newSettings = ChunkSettings(sentenceOnly = false, minChunkMs = 1000)
        val existingChunks = emptyList<Chunk>()

        whenever(transcriptionRepository.getChunkSettings(sourceId)).thenReturn(oldSettings)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(existingChunks)
        whenever(transcriptionRepository.getTranscription(sourceId)).thenReturn(null)

        val result = rechunkUseCase.execute(sourceId, newSettings, deleteRecordings = true)

        assertTrue(result is RechunkResult.Error)
        assertEquals("전사 결과를 찾을 수 없습니다", (result as RechunkResult.Error).message)
    }

    @Test
    fun `rechunks when no recordings exist`() = runTest {
        val sourceId = "test-source"
        val oldSettings = ChunkSettings(sentenceOnly = true, minChunkMs = 1200)
        val newSettings = ChunkSettings(sentenceOnly = false, minChunkMs = 1000)
        val existingChunks = listOf(Chunk(0, 0, 1500, "Hello."))
        val whisperResult = WhisperResult(
            text = "Hello.",
            segments = listOf(Segment(0.0, 1.5, "Hello.")),
            words = emptyList()
        )
        val newChunks = listOf(Chunk(0, 0, 1500, "Hello."))

        whenever(transcriptionRepository.getChunkSettings(sourceId)).thenReturn(oldSettings)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(existingChunks)
        whenever(recordingManager.hasRecording(sourceId, 0)).thenReturn(false)
        whenever(transcriptionRepository.getTranscription(sourceId)).thenReturn(whisperResult)
        whenever(chunkingUseCase.process(whisperResult, false, 1000)).thenReturn(newChunks)

        val result = rechunkUseCase.execute(sourceId, newSettings, deleteRecordings = false)

        verify(recordingManager, never()).deleteAllRecordings(sourceId)
        assertTrue(result is RechunkResult.Success)
    }

    @Test
    fun `hasAnyRecordings returns true when recordings exist`() = runTest {
        val sourceId = "test-source"
        val existingChunks = listOf(
            Chunk(0, 0, 1500, "Hello."),
            Chunk(1, 1500, 3000, "World.")
        )

        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(existingChunks)
        whenever(recordingManager.hasRecording(sourceId, 0)).thenReturn(false)
        whenever(recordingManager.hasRecording(sourceId, 1)).thenReturn(true)

        val result = rechunkUseCase.hasAnyRecordings(sourceId)

        assertTrue(result)
    }

    @Test
    fun `getRecordingCount returns correct count`() = runTest {
        val sourceId = "test-source"
        val existingChunks = listOf(
            Chunk(0, 0, 1500, "Hello."),
            Chunk(1, 1500, 3000, "World."),
            Chunk(2, 3000, 4500, "Test.")
        )

        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(existingChunks)
        whenever(recordingManager.hasRecording(sourceId, 0)).thenReturn(true)
        whenever(recordingManager.hasRecording(sourceId, 1)).thenReturn(false)
        whenever(recordingManager.hasRecording(sourceId, 2)).thenReturn(true)

        val result = rechunkUseCase.getRecordingCount(sourceId)

        assertEquals(2, result)
    }
}
