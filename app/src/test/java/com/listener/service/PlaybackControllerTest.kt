package com.listener.service

import android.content.Context
import com.listener.domain.model.Chunk
import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import com.listener.domain.model.PlaybackState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackControllerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var playbackController: PlaybackController

    @Before
    fun setup() {
        context = mock()
        whenever(context.cacheDir).thenReturn(tempFolder.root)
        playbackController = PlaybackController(context)
    }

    // Initial State Tests

    @Test
    fun `initial playback state is empty`() = runTest {
        val state = playbackController.playbackState.value
        assertEquals("", state.sourceId)
        assertEquals(0, state.currentChunkIndex)
        assertEquals(0, state.totalChunks)
        assertEquals(LearningState.Idle, state.learningState)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `initial service connection state is false`() {
        assertFalse(playbackController.isServiceConnected.value)
    }

    // Toggle Play/Pause Tests

    @Test
    fun `togglePlayPause does not throw when service not bound`() = runTest {
        // When PlaybackService is not bound, togglePlayPause should not throw
        playbackController.togglePlayPause()
        // Test passes if no exception
    }

    @Test
    fun `play does not throw when service not bound`() = runTest {
        playbackController.play()
        // Test passes if no exception
    }

    @Test
    fun `pause does not throw when service not bound`() = runTest {
        playbackController.pause()
        // Test passes if no exception
    }

    @Test
    fun `resume does not throw when service not bound`() = runTest {
        playbackController.resume()
        // Test passes if no exception
    }

    @Test
    fun `nextChunk does not throw when service not bound`() = runTest {
        playbackController.nextChunk()
        // Test passes if no exception
    }

    @Test
    fun `previousChunk does not throw when service not bound`() = runTest {
        playbackController.previousChunk()
        // Test passes if no exception
    }

    @Test
    fun `seekToChunk does not throw when service not bound`() = runTest {
        playbackController.seekToChunk(5)
        // Test passes if no exception
    }

    @Test
    fun `stop does not throw when service not bound`() = runTest {
        playbackController.stop()
        // Test passes if no exception
    }

    // Audio File Path Tests

    @Test
    fun `getAudioFilePath returns null when file does not exist`() {
        val result = playbackController.getAudioFilePath("nonexistent")
        assertNull(result)
    }

    @Test
    fun `hasAudioFile returns false when file does not exist`() {
        assertFalse(playbackController.hasAudioFile("nonexistent"))
    }

    @Test
    fun `getAudioFilePath returns path when file exists`() {
        // Create the audio_downloads directory and a test file
        val audioDownloadsDir = File(tempFolder.root, "audio_downloads")
        audioDownloadsDir.mkdirs()

        val sourceId = "test_source"
        val fileName = "${sourceId.hashCode()}.mp3"
        val audioFile = File(audioDownloadsDir, fileName)
        audioFile.createNewFile()

        val result = playbackController.getAudioFilePath(sourceId)
        assertEquals(audioFile.absolutePath, result)
    }

    @Test
    fun `hasAudioFile returns true when file exists`() {
        // Create the audio_downloads directory and a test file
        val audioDownloadsDir = File(tempFolder.root, "audio_downloads")
        audioDownloadsDir.mkdirs()

        val sourceId = "test_source"
        val fileName = "${sourceId.hashCode()}.mp3"
        val audioFile = File(audioDownloadsDir, fileName)
        audioFile.createNewFile()

        assertTrue(playbackController.hasAudioFile(sourceId))
    }

    // setContent Tests

    @Test
    fun `setContent does not throw when service not bound`() = runTest {
        val chunks = createTestChunks(5)
        // setContent should bind the service if not bound, then retry
        // This test verifies it doesn't crash
        playbackController.setContent(
            sourceId = "test",
            audioUri = "/path/to/audio.mp3",
            chunks = chunks,
            settings = LearningSettings(),
            title = "Test",
            subtitle = "Test subtitle"
        )
        // Test passes if no exception
    }

    // Test Helpers

    private fun createTestChunks(count: Int): List<Chunk> {
        return (0 until count).map { index ->
            Chunk(
                orderIndex = index,
                startMs = (index * 5000).toLong(),
                endMs = ((index + 1) * 5000).toLong(),
                displayText = "Test chunk $index"
            )
        }
    }
}
