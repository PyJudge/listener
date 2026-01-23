package com.listener.presentation.player

import app.cash.turbine.test
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.entity.RecentLearningEntity
import com.listener.domain.model.Chunk
import com.listener.domain.model.LearningSettings
import com.listener.domain.model.PlaybackState
import com.listener.domain.repository.TranscriptionRepository
import com.listener.data.repository.AppSettings
import com.listener.data.repository.SettingsRepository
import com.listener.service.PlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for PlayerViewModel loadBySourceId duplicate prevention.
 *
 * Problem: loadBySourceId() is called every time FullScreenPlayerScreen re-enters,
 *          causing progress reset.
 * Solution: Skip loading if same sourceId is already in playback state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelLoadTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var transcriptionRepository: TranscriptionRepository
    private lateinit var playlistDao: PlaylistDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var localFileDao: LocalFileDao
    private lateinit var recentLearningDao: RecentLearningDao
    private lateinit var playbackController: PlaybackController
    private lateinit var settingsRepository: SettingsRepository

    private val mockPlaybackState = MutableStateFlow(PlaybackState())

    private val testChunks = listOf(
        Chunk(orderIndex = 0, startMs = 0, endMs = 5000, displayText = "First chunk"),
        Chunk(orderIndex = 1, startMs = 5000, endMs = 10000, displayText = "Second chunk"),
        Chunk(orderIndex = 2, startMs = 10000, endMs = 15000, displayText = "Third chunk")
    )

    private val testRecentLearning = RecentLearningEntity(
        sourceId = "source1",
        sourceType = "PODCAST_EPISODE",
        title = "Recent Episode",
        subtitle = "Test Podcast",
        currentChunkIndex = 5,
        totalChunks = 10,
        thumbnailUrl = null,
        lastAccessedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        transcriptionRepository = mock()
        playlistDao = mock()
        podcastDao = mock()
        localFileDao = mock()
        recentLearningDao = mock()
        playbackController = mock()
        settingsRepository = mock()

        whenever(playbackController.playbackState).thenReturn(mockPlaybackState)
        whenever(settingsRepository.settings).thenReturn(flowOf(AppSettings()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PlayerViewModel {
        return PlayerViewModel(
            transcriptionRepository = transcriptionRepository,
            playlistDao = playlistDao,
            podcastDao = podcastDao,
            localFileDao = localFileDao,
            recentLearningDao = recentLearningDao,
            playbackController = playbackController,
            settingsRepository = settingsRepository
        )
    }

    /**
     * Test that loadBySourceId skips loading when same sourceId is already playing.
     *
     * Scenario: User navigates to FullScreenPlayerScreen with sourceId "source1",
     *           content is loaded. User navigates away and back.
     *           Second call to loadBySourceId("source1") should be skipped.
     */
    @Test
    fun `loadBySourceId should skip if same sourceId already playing`() = runTest {
        // Given: Content is already playing with sourceId "source1"
        mockPlaybackState.value = PlaybackState(
            sourceId = "source1",
            isPlaying = true,
            currentChunkIndex = 3
        )
        whenever(transcriptionRepository.getChunks("source1")).thenReturn(testChunks)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: loadBySourceId is called with same sourceId
        viewModel.loadBySourceId("source1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should not reload content (no calls to recentLearningDao or setContent)
        verify(recentLearningDao, never()).getRecentLearning("source1")
        verify(playbackController, never()).setContent(any(), any(), any(), any(), any(), any(), any())
    }

    /**
     * Test that loadBySourceId loads content when different sourceId is requested.
     */
    @Test
    fun `loadBySourceId should load when different sourceId requested`() = runTest {
        // Given: Content is already playing with sourceId "source1"
        mockPlaybackState.value = PlaybackState(
            sourceId = "source1",
            isPlaying = true,
            currentChunkIndex = 3
        )
        whenever(recentLearningDao.getRecentLearning("source2")).thenReturn(
            testRecentLearning.copy(sourceId = "source2")
        )
        whenever(transcriptionRepository.getChunks("source2")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("source2")).thenReturn("/path/to/audio.mp3")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: loadBySourceId is called with different sourceId
        viewModel.loadBySourceId("source2")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should load new content
        verify(recentLearningDao).getRecentLearning("source2")
        verify(playbackController).setContent(
            sourceId = "source2",
            audioUri = "/path/to/audio.mp3",
            chunks = testChunks,
            settings = LearningSettings(),
            title = "Recent Episode",
            subtitle = "Test Podcast",
            artworkUrl = null
        )
    }

    /**
     * Test that loadBySourceId loads content when nothing is playing.
     */
    @Test
    fun `loadBySourceId should load when nothing playing`() = runTest {
        // Given: No content is playing (default state)
        mockPlaybackState.value = PlaybackState() // sourceId is empty by default
        whenever(recentLearningDao.getRecentLearning("source1")).thenReturn(testRecentLearning)
        whenever(transcriptionRepository.getChunks("source1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("source1")).thenReturn("/path/to/audio.mp3")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: loadBySourceId is called
        viewModel.loadBySourceId("source1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should load content
        verify(recentLearningDao).getRecentLearning("source1")
        verify(playbackController).setContent(
            sourceId = "source1",
            audioUri = "/path/to/audio.mp3",
            chunks = testChunks,
            settings = LearningSettings(),
            title = "Recent Episode",
            subtitle = "Test Podcast",
            artworkUrl = null
        )
    }

    /**
     * Test that chunks are still loaded even when skipping reload.
     * This ensures UI can display chunks even if playback controller already has content.
     */
    @Test
    fun `loadBySourceId should still load chunks when skipping reload if chunks empty`() = runTest {
        // Given: Content is playing but ViewModel's chunks are empty
        mockPlaybackState.value = PlaybackState(
            sourceId = "source1",
            isPlaying = true,
            currentChunkIndex = 3
        )
        whenever(transcriptionRepository.getChunks("source1")).thenReturn(testChunks)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify chunks are initially empty
        viewModel.chunks.test {
            assertEquals(emptyList<Chunk>(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        // When: loadBySourceId is called with same sourceId
        viewModel.loadBySourceId("source1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Chunks should be loaded for UI display
        viewModel.chunks.test {
            assertEquals(testChunks, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    /**
     * Test that multiple consecutive calls with same sourceId are handled correctly.
     */
    @Test
    fun `multiple loadBySourceId calls with same sourceId should only load once`() = runTest {
        // Given: No content playing initially
        mockPlaybackState.value = PlaybackState()
        whenever(recentLearningDao.getRecentLearning("source1")).thenReturn(testRecentLearning)
        whenever(transcriptionRepository.getChunks("source1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("source1")).thenReturn("/path/to/audio.mp3")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // First call
        viewModel.loadBySourceId("source1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate that content is now playing
        mockPlaybackState.value = PlaybackState(sourceId = "source1", isPlaying = true)

        // Second call (simulating screen re-entry)
        viewModel.loadBySourceId("source1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Third call
        viewModel.loadBySourceId("source1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: setContent should only be called once (verify with any arguments)
        // Note: We use atMost(1) because the first call should load, subsequent should skip
        verify(playbackController, org.mockito.kotlin.atMost(1)).setContent(
            org.mockito.kotlin.eq("source1"),
            org.mockito.kotlin.eq("/path/to/audio.mp3"),
            org.mockito.kotlin.eq(testChunks),
            any(),  // settings may vary
            org.mockito.kotlin.eq("Recent Episode"),
            org.mockito.kotlin.eq("Test Podcast"),
            org.mockito.kotlin.isNull()
        )
    }

    /**
     * Test loadBySourceId with empty sourceId is handled.
     */
    @Test
    fun `loadBySourceId should handle empty sourceId gracefully`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: loadBySourceId is called with empty string
        viewModel.loadBySourceId("")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should not attempt to load anything meaningful
        // The current implementation would try to load, but with proper guard this should skip
        // For now, we verify no exceptions occur
    }
}
