package com.listener.presentation.player

import app.cash.turbine.test
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.entity.LocalAudioFileEntity
import com.listener.data.local.db.entity.PlaylistItemEntity
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.RecentLearningEntity
import com.listener.data.repository.AppSettings
import com.listener.data.repository.SettingsRepository
import com.listener.domain.model.Chunk
import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import com.listener.domain.model.PlaybackState
import com.listener.domain.model.PlayMode
import com.listener.domain.repository.TranscriptionRepository
import com.listener.domain.usecase.RechunkUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var transcriptionRepository: TranscriptionRepository
    private lateinit var playlistDao: PlaylistDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var localFileDao: LocalFileDao
    private lateinit var recentLearningDao: RecentLearningDao
    private lateinit var playbackController: PlaybackController
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var rechunkUseCase: RechunkUseCase
    private lateinit var viewModel: PlayerViewModel

    private val mockPlaybackState = MutableStateFlow(PlaybackState())
    private val mockAppSettings = MutableStateFlow(AppSettings())

    private val testChunks = listOf(
        Chunk(orderIndex = 0, startMs = 0, endMs = 5000, displayText = "First chunk"),
        Chunk(orderIndex = 1, startMs = 5000, endMs = 10000, displayText = "Second chunk"),
        Chunk(orderIndex = 2, startMs = 10000, endMs = 15000, displayText = "Third chunk")
    )

    private val testEpisode = PodcastEpisodeEntity(
        id = "ep1",
        feedUrl = "https://example.com/feed",
        title = "Test Episode",
        audioUrl = "https://example.com/audio.mp3",
        description = "Test description",
        durationMs = 15000,
        pubDate = System.currentTimeMillis(),
        isNew = true
    )

    private val testLocalFile = LocalAudioFileEntity(
        contentHash = "hash123",
        uri = "content://test/file",
        displayName = "Test File.mp3",
        durationMs = 15000L,
        addedAt = System.currentTimeMillis()
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

    private val testPlaylistItems = listOf(
        PlaylistItemEntity(
            id = 1,
            playlistId = 1,
            sourceId = "ep1",
            sourceType = "PODCAST_EPISODE",
            orderIndex = 0,
            addedAt = System.currentTimeMillis()
        ),
        PlaylistItemEntity(
            id = 2,
            playlistId = 1,
            sourceId = "ep2",
            sourceType = "PODCAST_EPISODE",
            orderIndex = 1,
            addedAt = System.currentTimeMillis()
        )
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
        rechunkUseCase = mock()

        whenever(playbackController.playbackState).thenReturn(mockPlaybackState)
        whenever(settingsRepository.settings).thenReturn(mockAppSettings)
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
            settingsRepository = settingsRepository,
            rechunkUseCase = rechunkUseCase
        )
    }

    // Initial State Tests

    @Test
    fun `initial chunks state is empty`() = runTest {
        viewModel = createViewModel()

        viewModel.chunks.test {
            assertEquals(emptyList<Chunk>(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `initial playlistId is null`() = runTest {
        viewModel = createViewModel()

        viewModel.playlistId.test {
            assertEquals(null, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `binds to PlaybackService on init`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).bindService()
    }

    // Load Content Tests

    @Test
    fun `loadContent loads chunks from repository`() = runTest {
        whenever(transcriptionRepository.getChunks("source1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("source1")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.loadContent(
            sourceId = "source1",
            title = "Test Title",
            subtitle = "Test Subtitle"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.chunks.test {
            assertEquals(testChunks, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loadContent sets up playback when audio file exists`() = runTest {
        whenever(transcriptionRepository.getChunks("source1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("source1")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.loadContent(
            sourceId = "source1",
            title = "Test Title",
            subtitle = "Test Subtitle"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).setContent(
            sourceId = "source1",
            audioUri = "/path/to/audio.mp3",
            chunks = testChunks,
            settings = LearningSettings(),
            title = "Test Title",
            subtitle = "Test Subtitle",
            artworkUrl = null
        )
    }

    @Test
    fun `loadContent does not set content when audio file missing`() = runTest {
        whenever(transcriptionRepository.getChunks("source1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("source1")).thenReturn(null)

        viewModel = createViewModel()
        viewModel.loadContent(
            sourceId = "source1",
            title = "Test Title",
            subtitle = "Test Subtitle"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController, never()).setContent(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `loadContent does not set content when chunks empty`() = runTest {
        whenever(transcriptionRepository.getChunks("source1")).thenReturn(emptyList())
        whenever(playbackController.getAudioFilePath("source1")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.loadContent(
            sourceId = "source1",
            title = "Test Title",
            subtitle = "Test Subtitle"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController, never()).setContent(any(), any(), any(), any(), any(), any(), any())
    }

    // Load By SourceId Tests

    @Test
    fun `loadBySourceId finds recent learning first`() = runTest {
        whenever(recentLearningDao.getRecentLearning("source1")).thenReturn(testRecentLearning)
        whenever(transcriptionRepository.getChunks("source1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("source1")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.loadBySourceId("source1")
        testDispatcher.scheduler.advanceUntilIdle()

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

    @Test
    fun `loadBySourceId finds podcast episode when recent not found`() = runTest {
        whenever(recentLearningDao.getRecentLearning("ep1")).thenReturn(null)
        whenever(podcastDao.getEpisode("ep1")).thenReturn(testEpisode)
        whenever(transcriptionRepository.getChunks("ep1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("ep1")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.loadBySourceId("ep1")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(podcastDao).getEpisode("ep1")
    }

    @Test
    fun `loadBySourceId finds local file when podcast not found`() = runTest {
        whenever(recentLearningDao.getRecentLearning("hash123")).thenReturn(null)
        whenever(podcastDao.getEpisode("hash123")).thenReturn(null)
        whenever(localFileDao.getFile("hash123")).thenReturn(testLocalFile)
        whenever(transcriptionRepository.getChunks("hash123")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("hash123")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.loadBySourceId("hash123")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(localFileDao).getFile("hash123")
    }

    // Playback Control Tests

    @Test
    fun `togglePlayPause calls controller`() = runTest {
        viewModel = createViewModel()
        viewModel.togglePlayPause()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).togglePlayPause()
    }

    @Test
    fun `nextChunk calls controller`() = runTest {
        viewModel = createViewModel()
        viewModel.nextChunk()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).nextChunk()
    }

    @Test
    fun `previousChunk calls controller`() = runTest {
        viewModel = createViewModel()
        viewModel.previousChunk()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).previousChunk()
    }

    @Test
    fun `seekToChunk calls controller`() = runTest {
        viewModel = createViewModel()
        viewModel.seekToChunk(5)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).seekToChunk(5)
    }

    // Settings Tests

    @Test
    fun `setRepeatCount updates settings`() = runTest {
        mockPlaybackState.value = PlaybackState(settings = LearningSettings(repeatCount = 2))

        viewModel = createViewModel()
        viewModel.setRepeatCount(3)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).updateSettings(LearningSettings(repeatCount = 3))
    }

    @Test
    fun `setRepeatCount clamps value between 1 and 5`() = runTest {
        mockPlaybackState.value = PlaybackState(settings = LearningSettings(repeatCount = 2))

        viewModel = createViewModel()

        viewModel.setRepeatCount(10)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(playbackController).updateSettings(LearningSettings(repeatCount = 5))
    }

    @Test
    fun `setGapRatio updates settings`() = runTest {
        mockPlaybackState.value = PlaybackState(settings = LearningSettings(gapRatio = 0.4f))

        viewModel = createViewModel()
        viewModel.setGapRatio(0.6f)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).updateSettings(LearningSettings(gapRatio = 0.6f))
    }

    @Test
    fun `togglePlayMode cycles through play modes`() = runTest {
        mockPlaybackState.value = PlaybackState(settings = LearningSettings(playMode = PlayMode.NORMAL))

        viewModel = createViewModel()
        viewModel.togglePlayMode()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).updateSettings(LearningSettings(playMode = PlayMode.LR))
    }

    @Test
    fun `togglePlayMode from LR to LRLR`() = runTest {
        mockPlaybackState.value = PlaybackState(settings = LearningSettings(playMode = PlayMode.LR))

        viewModel = createViewModel()
        viewModel.togglePlayMode()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).updateSettings(LearningSettings(playMode = PlayMode.LRLR))
    }

    @Test
    fun `togglePlayMode from LRLR to NORMAL`() = runTest {
        mockPlaybackState.value = PlaybackState(settings = LearningSettings(playMode = PlayMode.LRLR))

        viewModel = createViewModel()
        viewModel.togglePlayMode()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).updateSettings(LearningSettings(playMode = PlayMode.NORMAL))
    }

    // Playlist Tests

    @Test
    fun `startWithPlaylist loads playlist items`() = runTest {
        whenever(playlistDao.getPlaylistItemsList(1L)).thenReturn(testPlaylistItems)
        whenever(podcastDao.getEpisode("ep1")).thenReturn(testEpisode)
        whenever(transcriptionRepository.getChunks("ep1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("ep1")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.startWithPlaylist(1L, 0)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.playlistId.test {
            assertEquals(1L, awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        viewModel.playlistItems.test {
            assertEquals(testPlaylistItems, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `hasNextPlaylistItem returns true when more items exist`() = runTest {
        whenever(playlistDao.getPlaylistItemsList(1L)).thenReturn(testPlaylistItems)
        whenever(podcastDao.getEpisode("ep1")).thenReturn(testEpisode)
        whenever(transcriptionRepository.getChunks("ep1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("ep1")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.startWithPlaylist(1L, 0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.hasNextPlaylistItem())
    }

    @Test
    fun `hasNextPlaylistItem returns false at last item`() = runTest {
        whenever(playlistDao.getPlaylistItemsList(1L)).thenReturn(testPlaylistItems)
        whenever(podcastDao.getEpisode("ep2")).thenReturn(testEpisode.copy(id = "ep2"))
        whenever(transcriptionRepository.getChunks("ep2")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("ep2")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.startWithPlaylist(1L, 1) // Start at last item
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.hasNextPlaylistItem())
    }

    @Test
    fun `hasPreviousPlaylistItem returns false at first item`() = runTest {
        whenever(playlistDao.getPlaylistItemsList(1L)).thenReturn(testPlaylistItems)
        whenever(podcastDao.getEpisode("ep1")).thenReturn(testEpisode)
        whenever(transcriptionRepository.getChunks("ep1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("ep1")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.startWithPlaylist(1L, 0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.hasPreviousPlaylistItem())
    }

    @Test
    fun `isInPlaylistMode returns true when playlist is set`() = runTest {
        whenever(playlistDao.getPlaylistItemsList(1L)).thenReturn(testPlaylistItems)
        whenever(podcastDao.getEpisode("ep1")).thenReturn(testEpisode)
        whenever(transcriptionRepository.getChunks("ep1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("ep1")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.startWithPlaylist(1L, 0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isInPlaylistMode())
    }

    @Test
    fun `isInPlaylistMode returns false when no playlist`() = runTest {
        viewModel = createViewModel()

        assertFalse(viewModel.isInPlaylistMode())
    }

    // Recording Tests

    @Test
    fun `hasRecording returns false initially`() = runTest {
        viewModel = createViewModel()

        assertFalse(viewModel.hasRecording(0))
    }

    @Test
    fun `saveRecording and hasRecording work correctly`() = runTest {
        viewModel = createViewModel()

        viewModel.saveRecording(0, "/path/to/recording.mp3")

        assertTrue(viewModel.hasRecording(0))
    }

    @Test
    fun `deleteRecording removes recording`() = runTest {
        viewModel = createViewModel()

        viewModel.saveRecording(0, "/path/to/recording.mp3")
        assertTrue(viewModel.hasRecording(0))

        viewModel.deleteRecording(0)
        assertFalse(viewModel.hasRecording(0))
    }

    // Stop Tests

    @Test
    fun `stop resets state`() = runTest {
        whenever(playlistDao.getPlaylistItemsList(1L)).thenReturn(testPlaylistItems)
        whenever(podcastDao.getEpisode("ep1")).thenReturn(testEpisode)
        whenever(transcriptionRepository.getChunks("ep1")).thenReturn(testChunks)
        whenever(playbackController.getAudioFilePath("ep1")).thenReturn("/path/to/audio.mp3")

        viewModel = createViewModel()
        viewModel.startWithPlaylist(1L, 0)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.stop()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController).stop()

        viewModel.chunks.test {
            assertEquals(emptyList<Chunk>(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        viewModel.playlistId.test {
            assertEquals(null, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }
}
