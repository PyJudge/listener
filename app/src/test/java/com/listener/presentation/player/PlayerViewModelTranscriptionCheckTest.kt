package com.listener.presentation.player

import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.RecentLearningEntity
import com.listener.data.repository.AppSettings
import com.listener.data.repository.SettingsRepository
import com.listener.domain.model.Chunk
import com.listener.domain.model.PlaybackState
import com.listener.domain.repository.TranscriptionRepository
import com.listener.service.PlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for PlayerViewModel transcription check on content load.
 *
 * D2 Issue (ET-5): 전사 미완료 에피소드 선택 시 자동으로 TranscriptionScreen으로 이동
 *
 * Key behaviors:
 * - 전사 완료 (chunks > 0): 플레이어 정상 진입
 * - 전사 미완료 (chunks == 0): TranscriptionScreen으로 이동 이벤트 발행
 * - RecentLearning에서 찾지 못한 에피소드 (chunks == 0): TranscriptionScreen으로 이동
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTranscriptionCheckTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: PlayerViewModel
    private lateinit var transcriptionRepository: TranscriptionRepository
    private lateinit var playlistDao: PlaylistDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var localFileDao: LocalFileDao
    private lateinit var recentLearningDao: RecentLearningDao
    private lateinit var playbackController: PlaybackController
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var playbackStateFlow: MutableStateFlow<PlaybackState>

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

        playbackStateFlow = MutableStateFlow(PlaybackState())
        whenever(playbackController.playbackState).thenReturn(playbackStateFlow)
        whenever(settingsRepository.settings).thenReturn(flowOf(AppSettings()))

        viewModel = PlayerViewModel(
            transcriptionRepository = transcriptionRepository,
            playlistDao = playlistDao,
            podcastDao = podcastDao,
            localFileDao = localFileDao,
            recentLearningDao = recentLearningDao,
            playbackController = playbackController,
            settingsRepository = settingsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 전사 완료 케이스 ====================

    /**
     * 전사 완료된 에피소드 선택 시 플레이어에 정상 진입.
     * (chunks > 0)
     */
    @Test
    fun `loadBySourceId with transcription complete should not emit navigation event`() = runTest {
        // Given: 전사 완료된 콘텐츠 (chunks 존재)
        val sourceId = "episode-123"
        val chunks = listOf(
            Chunk(orderIndex = 0, startMs = 0, endMs = 1000, displayText = "Hello")
        )
        val recentLearning = RecentLearningEntity(
            sourceId = sourceId,
            title = "Test Episode",
            subtitle = "Test Subtitle",
            thumbnailUrl = null,
            sourceType = "PODCAST_EPISODE",
            currentChunkIndex = 0,
            totalChunks = 1,
            lastAccessedAt = System.currentTimeMillis()
        )

        whenever(recentLearningDao.getRecentLearning(sourceId)).thenReturn(recentLearning)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(chunks)
        whenever(playbackController.getAudioFilePath(sourceId)).thenReturn("/path/to/audio.mp3")

        // When: 콘텐츠 로드
        viewModel.loadBySourceId(sourceId)
        advanceUntilIdle()

        // Then: 청크가 로드되고, 네비게이션 이벤트 없음
        assertEquals(1, viewModel.chunks.value.size)

        // NavigationEvent가 TranscriptionScreen으로 발행되지 않음
        val navEvent = viewModel.navigationEvent.first()
        assertTrue(
            "Should be None when transcription is complete, but got: $navEvent",
            navEvent is PlayerNavigationEvent.None
        )
    }

    /**
     * RecentLearning에서 찾은 콘텐츠가 청크 있으면 플레이어 진입.
     */
    @Test
    fun `loadBySourceId from recent learning with chunks should load player`() = runTest {
        // Given
        val sourceId = "recent-episode"
        val chunks = listOf(
            Chunk(orderIndex = 0, startMs = 0, endMs = 2000, displayText = "Test"),
            Chunk(orderIndex = 1, startMs = 2000, endMs = 4000, displayText = "Text")
        )
        val recentLearning = RecentLearningEntity(
            sourceId = sourceId,
            title = "Recent Episode",
            subtitle = "Subtitle",
            thumbnailUrl = null,
            sourceType = "PODCAST_EPISODE",
            currentChunkIndex = 1,
            totalChunks = 2,
            lastAccessedAt = System.currentTimeMillis()
        )

        whenever(recentLearningDao.getRecentLearning(sourceId)).thenReturn(recentLearning)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(chunks)
        whenever(playbackController.getAudioFilePath(sourceId)).thenReturn("/path/to/audio.mp3")

        // When
        viewModel.loadBySourceId(sourceId)
        advanceUntilIdle()

        // Then: 플레이어 정상 로드
        assertEquals(2, viewModel.chunks.value.size)

        // NavigationEvent가 None (전사 완료이므로 이동 안 함)
        val navEvent = viewModel.navigationEvent.first()
        assertTrue(navEvent is PlayerNavigationEvent.None)
    }

    // ==================== 전사 미완료 케이스 ====================

    /**
     * 전사 미완료 에피소드 선택 시 TranscriptionScreen으로 이동.
     * (chunks == 0)
     */
    @Test
    fun `loadBySourceId with no transcription should emit navigation to transcription`() = runTest {
        // Given: 전사 미완료 콘텐츠 (chunks 없음)
        val sourceId = "episode-no-transcription"
        val episode = PodcastEpisodeEntity(
            id = sourceId,
            feedUrl = "https://example.com/feed.rss",
            title = "Episode Without Transcription",
            description = "Test",
            pubDate = System.currentTimeMillis(),
            durationMs = 60000,
            audioUrl = "https://example.com/audio.mp3",
            isNew = false
        )

        whenever(recentLearningDao.getRecentLearning(sourceId)).thenReturn(null)
        whenever(podcastDao.getEpisode(sourceId)).thenReturn(episode)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(emptyList())
        whenever(playbackController.getAudioFilePath(sourceId)).thenReturn(null)

        // When: 콘텐츠 로드
        viewModel.loadBySourceId(sourceId)
        advanceUntilIdle()

        // Then: TranscriptionScreen으로 네비게이션 이벤트 발행
        val navEvent = viewModel.navigationEvent.first()
        assertTrue(
            "Expected NavigateToTranscription event but got: $navEvent",
            navEvent is PlayerNavigationEvent.NavigateToTranscription
        )
        assertEquals(sourceId, (navEvent as PlayerNavigationEvent.NavigateToTranscription).sourceId)
    }

    /**
     * RecentLearning에 있지만 청크가 0인 경우 TranscriptionScreen으로 이동.
     */
    @Test
    fun `loadBySourceId from recent learning with zero chunks should navigate to transcription`() = runTest {
        // Given: RecentLearning 존재하지만 청크 없음
        val sourceId = "recent-no-chunks"
        val recentLearning = RecentLearningEntity(
            sourceId = sourceId,
            title = "Episode Title",
            subtitle = "Subtitle",
            thumbnailUrl = null,
            sourceType = "PODCAST_EPISODE",
            currentChunkIndex = 0,
            totalChunks = 0,  // 청크 없음
            lastAccessedAt = System.currentTimeMillis()
        )

        whenever(recentLearningDao.getRecentLearning(sourceId)).thenReturn(recentLearning)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(emptyList())

        // When
        viewModel.loadBySourceId(sourceId)
        advanceUntilIdle()

        // Then: TranscriptionScreen으로 이동
        val navEvent = viewModel.navigationEvent.first()
        assertTrue(navEvent is PlayerNavigationEvent.NavigateToTranscription)
    }

    /**
     * Podcast에서 찾은 에피소드이지만 청크가 0인 경우 TranscriptionScreen으로 이동.
     */
    @Test
    fun `loadBySourceId from podcast with zero chunks should navigate to transcription`() = runTest {
        // Given
        val sourceId = "podcast-no-chunks"
        val episode = PodcastEpisodeEntity(
            id = sourceId,
            feedUrl = "https://test.com/feed.rss",
            title = "Podcast Episode",
            description = "Description",
            pubDate = System.currentTimeMillis(),
            durationMs = 120000,
            audioUrl = "https://test.com/audio.mp3",
            isNew = true
        )

        whenever(recentLearningDao.getRecentLearning(sourceId)).thenReturn(null)
        whenever(podcastDao.getEpisode(sourceId)).thenReturn(episode)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(emptyList())

        // When
        viewModel.loadBySourceId(sourceId)
        advanceUntilIdle()

        // Then
        val navEvent = viewModel.navigationEvent.first()
        assertTrue(navEvent is PlayerNavigationEvent.NavigateToTranscription)
        assertEquals(sourceId, (navEvent as PlayerNavigationEvent.NavigateToTranscription).sourceId)
    }

    // ==================== loadContent 직접 호출 케이스 ====================

    /**
     * loadContent() 직접 호출 시 청크가 0이면 TranscriptionScreen으로 이동.
     */
    @Test
    fun `loadContent with zero chunks should emit navigation to transcription`() = runTest {
        // Given: 청크 없음
        val sourceId = "direct-load-no-chunks"
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(emptyList())

        // When
        viewModel.loadContent(
            sourceId = sourceId,
            title = "Test Title",
            subtitle = "Test Subtitle",
            artworkUrl = null
        )
        advanceUntilIdle()

        // Then: 전사 화면으로 이동
        val navEvent = viewModel.navigationEvent.first()
        assertTrue(navEvent is PlayerNavigationEvent.NavigateToTranscription)
    }

    /**
     * loadContent() 호출 시 청크가 있으면 정상 로드.
     */
    @Test
    fun `loadContent with chunks should load player normally`() = runTest {
        // Given: 청크 존재
        val sourceId = "direct-load-with-chunks"
        val chunks = listOf(
            Chunk(orderIndex = 0, startMs = 0, endMs = 1000, displayText = "Test")
        )
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(chunks)
        whenever(playbackController.getAudioFilePath(sourceId)).thenReturn("/path/to/audio.mp3")

        // When
        viewModel.loadContent(
            sourceId = sourceId,
            title = "Test Title",
            subtitle = "Test Subtitle",
            artworkUrl = null
        )
        advanceUntilIdle()

        // Then: 플레이어 정상 로드, 네비게이션 이벤트 없음
        assertEquals(1, viewModel.chunks.value.size)

        // NavigationEvent가 None (전사 완료이므로 이동 안 함)
        val navEvent = viewModel.navigationEvent.first()
        assertTrue(navEvent is PlayerNavigationEvent.None)
    }

    // ==================== 네비게이션 이벤트 소비 ====================

    /**
     * consumeNavigationEvent() 호출 시 이벤트가 None으로 리셋됨.
     */
    @Test
    fun `consumeNavigationEvent should reset event to None`() = runTest {
        // Given: TranscriptionScreen으로 이동 이벤트 발행된 상태
        val sourceId = "episode-to-consume"
        whenever(recentLearningDao.getRecentLearning(sourceId)).thenReturn(null)
        whenever(podcastDao.getEpisode(sourceId)).thenReturn(null)
        whenever(localFileDao.getFile(sourceId)).thenReturn(null)
        whenever(transcriptionRepository.getChunks(sourceId)).thenReturn(emptyList())

        viewModel.loadBySourceId(sourceId)
        advanceUntilIdle()

        // When: 이벤트 소비
        viewModel.consumeNavigationEvent()

        // Then: None으로 리셋
        val navEvent = viewModel.navigationEvent.first()
        assertEquals(PlayerNavigationEvent.None, navEvent)
    }
}
