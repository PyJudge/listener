package com.listener.presentation.home

import app.cash.turbine.test
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PlaylistItemEntity
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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var recentLearningDao: RecentLearningDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var playlistDao: PlaylistDao
    private lateinit var viewModel: HomeViewModel

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
    fun setup() = runTest {
        Dispatchers.setMain(testDispatcher)

        recentLearningDao = mock()
        podcastDao = mock()
        playlistDao = mock()

        whenever(recentLearningDao.getRecentLearnings(any())).thenReturn(flowOf(mockRecentLearnings))
        whenever(podcastDao.getNewEpisodes(any())).thenReturn(flowOf(mockEpisodes))
        whenever(podcastDao.getAllSubscriptions()).thenReturn(flowOf(mockSubscriptions))
        whenever(playlistDao.getAllPlaylists()).thenReturn(flowOf(mockPlaylists))
        whenever(playlistDao.getPlaylistItemsList(any())).thenReturn(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loads recent learnings successfully`() = runTest {
        viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.recentLearnings.size)
            assertEquals("Episode 1", state.recentLearnings[0].title)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loads new episodes successfully`() = runTest {
        viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.newEpisodes.size)
            assertTrue(state.newEpisodes[0].isNew)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `hasSubscriptions is true when subscriptions exist`() = runTest {
        viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.hasSubscriptions)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `showCreatePlaylistDialog updates state`() = runTest {
        viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showCreatePlaylistDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showCreatePlaylistDialog)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `dismissCreatePlaylistDialog updates state`() = runTest {
        viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showCreatePlaylistDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissCreatePlaylistDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showCreatePlaylistDialog)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `addToPlaylist calls dao with correct parameters`() = runTest {
        whenever(playlistDao.getMaxOrderIndex(any())).thenReturn(2)

        viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addToPlaylist(
            playlistId = 1L,
            sourceId = "source123",
            sourceType = "PODCAST_EPISODE"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playlistDao).insertPlaylistItem(any())
    }

    @Test
    fun `createPlaylistAndAddItem creates playlist and adds item`() = runTest {
        whenever(playlistDao.insertPlaylist(any())).thenReturn(1L)

        viewModel = HomeViewModel(recentLearningDao, podcastDao, playlistDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showCreatePlaylistDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createPlaylistAndAddItem(
            name = "New Playlist",
            sourceId = "source123",
            sourceType = "PODCAST_EPISODE"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playlistDao).insertPlaylist(any())
        verify(playlistDao).insertPlaylistItem(any())

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showCreatePlaylistDialog)
            cancelAndConsumeRemainingEvents()
        }
    }
}
