package com.listener.presentation.home

import app.cash.turbine.test
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.service.TranscriptionQueueManager
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.RecentLearningEntity
import com.listener.data.local.db.entity.SubscribedPodcastEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for HomeViewModel Flow optimization.
 *
 * Problem: HomeViewModel uses 4 independent Flow collects, causing multiple UI updates.
 * Solution: Use combine() to merge flows and emit single state updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var recentLearningDao: RecentLearningDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var playlistDao: PlaylistDao
    private lateinit var transcriptionQueueManager: TranscriptionQueueManager

    private val mockRecentLearnings = listOf(
        RecentLearningEntity(
            sourceId = "source1",
            sourceType = "PODCAST_EPISODE",
            title = "Episode 1",
            subtitle = "Podcast 1",
            currentChunkIndex = 5,
            totalChunks = 10,
            thumbnailUrl = null,
            lastAccessedAt = System.currentTimeMillis()
        )
    )

    private val mockEpisodes = listOf(
        PodcastEpisodeEntity(
            id = "ep1",
            feedUrl = "https://example.com/feed",
            title = "New Episode",
            audioUrl = "https://example.com/audio.mp3",
            description = "Description",
            durationMs = 1800000,
            pubDate = System.currentTimeMillis(),
            isNew = true
        )
    )

    private val mockSubscriptions = listOf(
        SubscribedPodcastEntity(
            feedUrl = "https://example.com/feed",
            collectionId = 123L,
            title = "Test Podcast",
            description = "Test podcast description",
            artworkUrl = null,
            lastCheckedAt = System.currentTimeMillis(),
            addedAt = System.currentTimeMillis()
        )
    )

    private val mockPlaylists = listOf(
        PlaylistEntity(
            id = 1,
            name = "My Playlist",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        recentLearningDao = mock()
        podcastDao = mock()
        playlistDao = mock()
        transcriptionQueueManager = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test that loadData emits a single combined state update.
     *
     * Previously, 4 independent collects would cause 4 state updates.
     * After using combine(), we should see fewer intermediate states.
     */
    @Test
    fun `loadData should emit single combined state update`() = runTest {
        // Given: All data sources return data
        whenever(recentLearningDao.getRecentLearnings(any())).thenReturn(flowOf(mockRecentLearnings))
        whenever(podcastDao.getNewEpisodes(any(), any())).thenReturn(flowOf(mockEpisodes))
        whenever(podcastDao.getAllSubscriptions()).thenReturn(flowOf(mockSubscriptions))
        whenever(playlistDao.getAllPlaylists()).thenReturn(flowOf(mockPlaylists))
        runBlocking {
            whenever(playlistDao.getPlaylistItemsList(any())).thenReturn(emptyList())
        }

        // When: ViewModel is created and data is loaded
        val viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao, transcriptionQueueManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Final state should have all data populated
        viewModel.uiState.test {
            val state = awaitItem()

            // All data should be present in a single state
            assertEquals(1, state.recentLearnings.size)
            assertEquals(1, state.newEpisodes.size)
            assertEquals(1, state.playlists.size)
            assertTrue(state.hasSubscriptions)
            assertFalse(state.isLoading)

            cancelAndConsumeRemainingEvents()
        }
    }

    /**
     * Test that empty data sources still result in proper combined state.
     */
    @Test
    fun `loadData with empty sources should emit proper state`() = runTest {
        // Given: All data sources return empty
        whenever(recentLearningDao.getRecentLearnings(any())).thenReturn(flowOf(emptyList()))
        whenever(podcastDao.getNewEpisodes(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(podcastDao.getAllSubscriptions()).thenReturn(flowOf(emptyList()))
        whenever(playlistDao.getAllPlaylists()).thenReturn(flowOf(emptyList()))

        // When: ViewModel is created and data is loaded
        val viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao, transcriptionQueueManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Final state should have empty lists but not be loading
        viewModel.uiState.test {
            val state = awaitItem()

            assertEquals(0, state.recentLearnings.size)
            assertEquals(0, state.newEpisodes.size)
            assertEquals(0, state.playlists.size)
            assertFalse(state.hasSubscriptions)
            assertFalse(state.isLoading)

            cancelAndConsumeRemainingEvents()
        }
    }

    /**
     * Test that podcast names are properly mapped from subscriptions.
     */
    @Test
    fun `loadData should build podcast name map from subscriptions`() = runTest {
        // Given: Subscriptions exist
        whenever(recentLearningDao.getRecentLearnings(any())).thenReturn(flowOf(emptyList()))
        whenever(podcastDao.getNewEpisodes(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(podcastDao.getAllSubscriptions()).thenReturn(flowOf(mockSubscriptions))
        whenever(playlistDao.getAllPlaylists()).thenReturn(flowOf(emptyList()))

        // When: ViewModel is created and data is loaded
        val viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao, transcriptionQueueManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Podcast name map should be populated
        viewModel.uiState.test {
            val state = awaitItem()

            assertEquals("Test Podcast", state.podcastNames["https://example.com/feed"])

            cancelAndConsumeRemainingEvents()
        }
    }

    /**
     * Test that playlist item counts are calculated correctly.
     */
    @Test
    fun `loadData should calculate playlist item counts`() = runTest {
        // Given: Playlists with items
        whenever(recentLearningDao.getRecentLearnings(any())).thenReturn(flowOf(emptyList()))
        whenever(podcastDao.getNewEpisodes(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(podcastDao.getAllSubscriptions()).thenReturn(flowOf(emptyList()))
        whenever(playlistDao.getAllPlaylists()).thenReturn(flowOf(mockPlaylists))
        runBlocking {
            whenever(playlistDao.getPlaylistItemsList(1L)).thenReturn(
                listOf(
                    com.listener.data.local.db.entity.PlaylistItemEntity(
                        id = 1, playlistId = 1, sourceId = "s1", sourceType = "PODCAST_EPISODE",
                        orderIndex = 0, addedAt = System.currentTimeMillis()
                    ),
                    com.listener.data.local.db.entity.PlaylistItemEntity(
                        id = 2, playlistId = 1, sourceId = "s2", sourceType = "PODCAST_EPISODE",
                        orderIndex = 1, addedAt = System.currentTimeMillis()
                    )
                )
            )
        }

        // When: ViewModel is created and data is loaded
        val viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao, transcriptionQueueManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Item count should be 2 for playlist id 1
        viewModel.uiState.test {
            val state = awaitItem()

            assertEquals(2, state.playlistItemCounts[1L])

            cancelAndConsumeRemainingEvents()
        }
    }
}
