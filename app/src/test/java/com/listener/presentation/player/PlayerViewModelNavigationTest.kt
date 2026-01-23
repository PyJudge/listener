package com.listener.presentation.player

import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.domain.model.PlaybackState
import com.listener.domain.repository.TranscriptionRepository
import com.listener.data.repository.AppSettings
import com.listener.data.repository.SettingsRepository
import com.listener.service.PlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Tests for PlayerViewModel navigation behavior.
 *
 * CN-1 Issue: seekToChunk() should apply debounce (300ms) to process only the last request
 *             when called multiple times rapidly.
 *
 * Key behaviors:
 * - nextChunk(): ALL requests should be processed sequentially (5 calls = 5 chunk moves)
 * - previousChunk(): ALL requests should be processed sequentially
 * - seekToChunk(): Only LAST request within 300ms should be processed (debounce)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelNavigationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var transcriptionRepository: TranscriptionRepository
    private lateinit var playlistDao: PlaylistDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var localFileDao: LocalFileDao
    private lateinit var recentLearningDao: RecentLearningDao
    private lateinit var playbackController: PlaybackController
    private lateinit var settingsRepository: SettingsRepository

    private val mockPlaybackState = MutableStateFlow(PlaybackState())

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

    // ==================== nextChunk() Tests ====================

    /**
     * Test that 5 rapid nextChunk() calls result in 5 chunk moves.
     * This verifies the current correct behavior should be preserved.
     *
     * Scenario: User rapidly taps the "next" button 5 times.
     * Expected: All 5 requests are processed, moving 5 chunks forward.
     */
    @Test
    fun `nextChunk 5회 연타 시 5개 청크 이동`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: nextChunk() called 5 times rapidly
        repeat(5) {
            viewModel.nextChunk()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: playbackController.nextChunk() should be called 5 times
        verify(playbackController, times(5)).nextChunk()
    }

    /**
     * Test that consecutive nextChunk() calls are processed sequentially.
     */
    @Test
    fun `nextChunk 연속 호출은 순차적으로 처리됨`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: nextChunk() called 3 times
        viewModel.nextChunk()
        viewModel.nextChunk()
        viewModel.nextChunk()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: All 3 calls should be forwarded to controller
        verify(playbackController, times(3)).nextChunk()
    }

    // ==================== previousChunk() Tests ====================

    /**
     * Test that 5 rapid previousChunk() calls result in 5 chunk moves backward.
     */
    @Test
    fun `previousChunk 5회 연타 시 5개 청크 뒤로 이동`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: previousChunk() called 5 times rapidly
        repeat(5) {
            viewModel.previousChunk()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: playbackController.previousChunk() should be called 5 times
        verify(playbackController, times(5)).previousChunk()
    }

    // ==================== seekToChunk() Debounce Tests ====================

    /**
     * Test that rapid seekToChunk() calls only process the last request.
     *
     * Scenario: User drags seekbar rapidly through chunks 3 → 5 → 7 (100ms intervals).
     * Expected: Only seekToChunk(7) is processed after 300ms debounce.
     */
    @Test
    fun `seekToChunk 연타 시 마지막 요청만 처리 (debounce 300ms)`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: seekToChunk() called rapidly with different indices
        viewModel.seekToChunk(3)
        advanceTimeBy(100)  // 100ms later
        viewModel.seekToChunk(5)
        advanceTimeBy(100)  // 200ms total
        viewModel.seekToChunk(7)
        advanceTimeBy(100)  // 300ms total - not yet triggered

        // Then: No calls yet (within debounce window)
        verify(playbackController, never()).seekToChunk(3)
        verify(playbackController, never()).seekToChunk(5)
        verify(playbackController, never()).seekToChunk(7)

        // After debounce period completes
        advanceTimeBy(300)  // 600ms total
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Only the last request (7) should be processed
        verify(playbackController, never()).seekToChunk(3)
        verify(playbackController, never()).seekToChunk(5)
        verify(playbackController, times(1)).seekToChunk(7)
    }

    /**
     * Test that a single seekToChunk() call is processed after debounce delay.
     */
    @Test
    fun `seekToChunk 단일 호출은 debounce 후 처리됨`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Single seekToChunk() call
        viewModel.seekToChunk(5)

        // Advance time but not past debounce
        advanceTimeBy(200)
        verify(playbackController, never()).seekToChunk(5)

        // Advance past debounce (300ms total)
        advanceTimeBy(150)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should be processed
        verify(playbackController, times(1)).seekToChunk(5)
    }

    /**
     * Test that seekToChunk() debounce resets with each new call.
     */
    @Test
    fun `seekToChunk 호출마다 debounce 타이머 리셋`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: seekToChunk(3) at 0ms
        viewModel.seekToChunk(3)
        advanceTimeBy(250)  // 250ms - almost at debounce limit

        // New call at 250ms - should reset timer
        viewModel.seekToChunk(5)
        advanceTimeBy(250)  // 500ms total, 250ms since last call

        // Still within debounce window from last call
        verify(playbackController, never()).seekToChunk(3)
        verify(playbackController, never()).seekToChunk(5)

        // Advance to complete debounce (300ms from last call)
        advanceTimeBy(100)  // 600ms total, 350ms since seekToChunk(5)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Only seekToChunk(5) should be called
        verify(playbackController, never()).seekToChunk(3)
        verify(playbackController, times(1)).seekToChunk(5)
    }

    /**
     * Test that seekToChunk() calls with sufficient gap are both processed.
     */
    @Test
    fun `seekToChunk 호출 사이 충분한 간격이면 모두 처리됨`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: First seekToChunk(3)
        viewModel.seekToChunk(3)
        advanceTimeBy(400)  // Wait past debounce
        testDispatcher.scheduler.advanceUntilIdle()

        // Second seekToChunk(7) after first completes
        viewModel.seekToChunk(7)
        advanceTimeBy(400)  // Wait past debounce
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Both should be processed
        verify(playbackController, times(1)).seekToChunk(3)
        verify(playbackController, times(1)).seekToChunk(7)
    }

    // ==================== Mixed Navigation Tests ====================

    /**
     * Test that seekToChunk debounce doesn't affect nextChunk/previousChunk.
     */
    @Test
    fun `seekToChunk debounce는 nextChunk에 영향 없음`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Mixed calls
        viewModel.seekToChunk(3)
        advanceTimeBy(100)
        viewModel.nextChunk()  // Should be immediate
        viewModel.nextChunk()  // Should be immediate
        advanceTimeBy(100)
        viewModel.seekToChunk(7)  // Replaces seekToChunk(3)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then: nextChunk should be called twice immediately
        verify(playbackController, times(2)).nextChunk()

        // Wait for seekToChunk debounce
        advanceTimeBy(300)
        testDispatcher.scheduler.advanceUntilIdle()

        // Only seekToChunk(7) should be called (not 3)
        verify(playbackController, never()).seekToChunk(3)
        verify(playbackController, times(1)).seekToChunk(7)
    }

    /**
     * Test rapid mixed navigation sequence.
     *
     * Scenario: User rapidly presses next, next, then taps chunk 10 on seekbar.
     * Expected: Both nextChunk() calls processed, seekToChunk(10) processed after debounce.
     */
    @Test
    fun `혼합 네비게이션 시퀀스 테스트`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Rapid mixed navigation
        viewModel.nextChunk()
        viewModel.nextChunk()
        viewModel.seekToChunk(10)
        viewModel.previousChunk()

        // Advance just enough to process channel but not debounce (100ms < 300ms)
        advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        // Then: nextChunk and previousChunk are processed immediately (via channel)
        verify(playbackController, times(2)).nextChunk()
        verify(playbackController, times(1)).previousChunk()

        // seekToChunk is still pending (within debounce window)
        verify(playbackController, never()).seekToChunk(10)

        // After debounce completes (300ms total)
        advanceTimeBy(300)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playbackController, times(1)).seekToChunk(10)
    }
}
