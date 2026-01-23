package com.listener.presentation.player

import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.entity.PlaylistItemEntity
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.domain.model.Chunk
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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for D1 - Playlist Auto-Advance feature.
 *
 * D1 Feature: When contentComplete is true in PlaybackState,
 * PlayerViewModel should automatically advance to the next playlist item
 * if in playlist mode and there are more items.
 *
 * Implementation:
 * 1. PlaybackState.kt has `contentComplete: Boolean = false` field
 * 2. PlaybackService sets `contentComplete = true` when last chunk completes
 * 3. PlayerViewModel observes `contentComplete` and calls `nextPlaylistItem()` if in playlist mode
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelPlaylistAutoAdvanceTest {

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

    private val testPlaylistItems = listOf(
        PlaylistItemEntity(
            id = 1L,
            playlistId = 100L,
            sourceId = "episode1",
            sourceType = "PODCAST_EPISODE",
            orderIndex = 0,
            addedAt = System.currentTimeMillis()
        ),
        PlaylistItemEntity(
            id = 2L,
            playlistId = 100L,
            sourceId = "episode2",
            sourceType = "PODCAST_EPISODE",
            orderIndex = 1,
            addedAt = System.currentTimeMillis()
        ),
        PlaylistItemEntity(
            id = 3L,
            playlistId = 100L,
            sourceId = "episode3",
            sourceType = "PODCAST_EPISODE",
            orderIndex = 2,
            addedAt = System.currentTimeMillis()
        )
    )

    private val testEpisode1 = PodcastEpisodeEntity(
        id = "episode1",
        feedUrl = "https://example.com/feed.xml",
        title = "Episode 1",
        audioUrl = "https://example.com/ep1.mp3",
        description = "First episode",
        durationMs = 60000L,
        pubDate = System.currentTimeMillis(),
        isNew = false
    )

    private val testEpisode2 = PodcastEpisodeEntity(
        id = "episode2",
        feedUrl = "https://example.com/feed.xml",
        title = "Episode 2",
        audioUrl = "https://example.com/ep2.mp3",
        description = "Second episode",
        durationMs = 60000L,
        pubDate = System.currentTimeMillis(),
        isNew = false
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
     * Test 1: contentComplete가 true이고 플레이리스트 모드면 다음 아이템 로드
     *
     * Scenario: User is playing a playlist. The current item finishes (contentComplete = true).
     * Expected: ViewModel automatically loads the next item in the playlist.
     */
    @Test
    fun `contentComplete true이고 플레이리스트 모드면 다음 아이템 로드`() = runTest {
        // Given: Setup playlist mode with 3 items
        whenever(playlistDao.getPlaylistItemsList(100L)).thenReturn(testPlaylistItems)
        whenever(podcastDao.getEpisode("episode1")).thenReturn(testEpisode1)
        whenever(podcastDao.getEpisode("episode2")).thenReturn(testEpisode2)
        whenever(transcriptionRepository.getChunks(any())).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath(any())).thenReturn("/path/to/audio.mp3")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Start playlist mode (current item = episode1, index = 0)
        viewModel.startWithPlaylist(playlistId = 100L, startIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial state
        assertEquals(0, viewModel.currentPlaylistItemIndex.value)

        // When: contentComplete becomes true (current content finished)
        mockPlaybackState.value = PlaybackState(
            sourceId = "episode1",
            contentComplete = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should advance to next item (index 1)
        assertEquals(1, viewModel.currentPlaylistItemIndex.value)

        // And episode2 should be loaded
        verify(podcastDao).getEpisode("episode2")
    }

    /**
     * Test 2: contentComplete가 true이지만 단독 모드면 다음 아이템 안 로드
     *
     * Scenario: User is playing a single item (not in playlist mode).
     *           Content completes (contentComplete = true).
     * Expected: Nothing happens - no auto-advance since not in playlist mode.
     */
    @Test
    fun `contentComplete true이지만 단독 모드면 다음 아이템 안 로드`() = runTest {
        // Given: Not in playlist mode (standalone playback)
        whenever(transcriptionRepository.getChunks(any())).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath(any())).thenReturn("/path/to/audio.mp3")
        whenever(recentLearningDao.getRecentLearning("source1")).thenReturn(null)
        whenever(podcastDao.getEpisode("source1")).thenReturn(testEpisode1.copy(id = "source1"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Load content directly (not via playlist)
        viewModel.loadBySourceId("source1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify not in playlist mode
        assertFalse(viewModel.isInPlaylistMode())

        // When: contentComplete becomes true
        mockPlaybackState.value = PlaybackState(
            sourceId = "source1",
            contentComplete = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should NOT try to load any playlist items
        verify(playlistDao, never()).getPlaylistItemsList(any())
        // currentPlaylistItemIndex should remain 0 (default)
        assertEquals(0, viewModel.currentPlaylistItemIndex.value)
    }

    /**
     * Test 3: contentComplete가 true이고 마지막 아이템이면 다음 아이템 안 로드
     *
     * Scenario: User is playing the last item in a playlist.
     *           Content completes (contentComplete = true).
     * Expected: Nothing happens - no more items to advance to.
     */
    @Test
    fun `contentComplete true이고 마지막 아이템이면 다음 아이템 안 로드`() = runTest {
        // Given: Setup playlist mode starting at the last item (index 2)
        whenever(playlistDao.getPlaylistItemsList(100L)).thenReturn(testPlaylistItems)
        whenever(podcastDao.getEpisode("episode3")).thenReturn(
            testEpisode1.copy(id = "episode3", title = "Episode 3")
        )
        whenever(transcriptionRepository.getChunks(any())).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath(any())).thenReturn("/path/to/audio.mp3")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Start playlist at the LAST item (index = 2)
        viewModel.startWithPlaylist(playlistId = 100L, startIndex = 2)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify we're at the last item
        assertEquals(2, viewModel.currentPlaylistItemIndex.value)
        assertFalse(viewModel.hasNextPlaylistItem())

        // When: contentComplete becomes true
        mockPlaybackState.value = PlaybackState(
            sourceId = "episode3",
            contentComplete = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Index should remain at 2 (no advance)
        assertEquals(2, viewModel.currentPlaylistItemIndex.value)

        // episode1 and episode2 should NOT be loaded
        verify(podcastDao, never()).getEpisode("episode1")
        verify(podcastDao, never()).getEpisode("episode2")
    }

    /**
     * Test 4: PlaybackState contentComplete 기본값은 false
     *
     * Scenario: Create a new PlaybackState with default values.
     * Expected: contentComplete should default to false.
     */
    @Test
    fun `PlaybackState contentComplete 기본값은 false`() {
        // When: Create a new PlaybackState with defaults
        val defaultState = PlaybackState()

        // Then: contentComplete should be false
        assertFalse(defaultState.contentComplete)
    }
}
