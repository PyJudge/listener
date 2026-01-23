package com.listener.service

import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import com.listener.domain.model.PlayMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for LearningStateMachine Gap timer resume functionality.
 *
 * B1 Issue (LR-1, PR-2): Gap 중 pause 후 resume 시 남은 시간부터 재개
 *
 * Key behaviors:
 * - pause() during Gap: Should save remaining gap time
 * - resume() after Gap pause: Should return remaining gap time
 * - Non-Gap pause: Gap time should be 0
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LearningStateMachineGapTest {

    private lateinit var stateMachine: LearningStateMachine

    @Before
    fun setup() {
        stateMachine = LearningStateMachine()
    }

    // ==================== Gap Pause/Resume Tests ====================

    /**
     * Test that pausing during Gap saves the remaining gap time.
     *
     * Scenario: Gap 5000ms 중 2000ms 경과 후 pause → resume
     * Expected: remainingGapTimeMs == 3000ms
     */
    @Test
    fun `Gap 중 pause 후 resume 시 남은 시간 반환`() {
        // Given: LR mode, currently in Gap state
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR, gapRatio = 1.0f))
        stateMachine.play()

        // Simulate: Playing → Gap
        stateMachine.onPlaybackComplete()
        assertEquals(LearningState.Gap, stateMachine.state.value)

        // When: Pause during Gap with 3000ms remaining (2000ms elapsed of 5000ms gap)
        val remainingGapMs = 3000L
        stateMachine.pauseWithGapTime(remainingGapMs)
        assertEquals(LearningState.Paused, stateMachine.state.value)

        // Then: Resume should return the remaining gap time
        val resumeResult = stateMachine.resumeWithGapInfo()
        assertEquals(LearningState.Gap, stateMachine.state.value)
        assertTrue(resumeResult.wasInGap)
        assertEquals(remainingGapMs, resumeResult.remainingGapTimeMs)
    }

    /**
     * Test that pausing during non-Gap state returns 0 gap time.
     */
    @Test
    fun `Playing 중 pause 후 resume 시 gap 시간은 0`() {
        // Given: LR mode, currently playing
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        assertEquals(LearningState.Playing, stateMachine.state.value)

        // When: Pause during Playing
        stateMachine.pauseWithGapTime(0L)
        assertEquals(LearningState.Paused, stateMachine.state.value)

        // Then: Resume should indicate it was not in Gap
        val resumeResult = stateMachine.resumeWithGapInfo()
        assertEquals(LearningState.Playing, stateMachine.state.value)
        assertTrue(!resumeResult.wasInGap)
        assertEquals(0L, resumeResult.remainingGapTimeMs)
    }

    /**
     * Test that pausing during GapWithRecording (LRLR mode) saves remaining time.
     */
    @Test
    fun `GapWithRecording 중 pause 후 resume 시 남은 시간 반환`() {
        // Given: LRLR mode, currently in GapWithRecording state
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR, gapRatio = 1.0f))
        stateMachine.play()

        // Simulate: PlayingFirst → GapWithRecording
        stateMachine.onPlaybackComplete()
        assertEquals(LearningState.GapWithRecording, stateMachine.state.value)

        // When: Pause during GapWithRecording with 4000ms remaining
        val remainingGapMs = 4000L
        stateMachine.pauseWithGapTime(remainingGapMs)
        assertEquals(LearningState.Paused, stateMachine.state.value)

        // Then: Resume should return the remaining gap time
        val resumeResult = stateMachine.resumeWithGapInfo()
        assertEquals(LearningState.GapWithRecording, stateMachine.state.value)
        assertTrue(resumeResult.wasInGap)
        assertEquals(remainingGapMs, resumeResult.remainingGapTimeMs)
    }

    /**
     * Test that pause at Gap completion boundary immediately triggers next state.
     *
     * Edge case: Gap almost complete (remaining <= 0)
     */
    @Test
    fun `Gap 완료 직전 pause 후 resume 시 즉시 전이`() {
        // Given: LR mode, in Gap state
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.onPlaybackComplete()
        assertEquals(LearningState.Gap, stateMachine.state.value)

        // When: Pause with 0 remaining (gap was about to complete)
        stateMachine.pauseWithGapTime(0L)
        assertEquals(LearningState.Paused, stateMachine.state.value)

        // Then: Resume indicates Gap was complete
        val resumeResult = stateMachine.resumeWithGapInfo()
        assertEquals(LearningState.Gap, stateMachine.state.value)
        assertTrue(resumeResult.wasInGap)
        assertEquals(0L, resumeResult.remainingGapTimeMs)
    }

    /**
     * Test that resuming from non-Paused state returns empty result.
     */
    @Test
    fun `Paused 아닌 상태에서 resume 시 기존 동작 유지`() {
        // Given: Currently Playing (not paused)
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        assertEquals(LearningState.Playing, stateMachine.state.value)

        // When: Try to resume (but not paused)
        val resumeResult = stateMachine.resumeWithGapInfo()

        // Then: State unchanged, no gap info
        assertEquals(LearningState.Playing, stateMachine.state.value)
        assertTrue(!resumeResult.wasInGap)
        assertEquals(0L, resumeResult.remainingGapTimeMs)
    }

    // ==================== Backward Compatibility Tests ====================

    /**
     * Test that existing pause() without gap time still works.
     */
    @Test
    fun `기존 pause() 함수 하위 호환성`() {
        // Given: Playing
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()

        // When: Use existing pause() without gap time parameter
        stateMachine.pause(1000L)  // Only position, no gap time
        assertEquals(LearningState.Paused, stateMachine.state.value)

        // Then: Resume should work
        val position = stateMachine.resume()
        assertEquals(1000L, position)
        assertEquals(LearningState.Playing, stateMachine.state.value)
    }

    /**
     * Test normal playback flow isn't affected.
     */
    @Test
    fun `일반 재생 플로우 영향 없음`() {
        // Given: LR mode
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR, repeatCount = 2))
        stateMachine.play()

        // When: Normal flow Playing → Gap → Playing → Gap → NextChunk
        // First repeat
        assertEquals(LearningState.Playing, stateMachine.state.value)
        var result = stateMachine.onPlaybackComplete()
        assertEquals(LearningState.Gap, stateMachine.state.value)
        assertEquals(TransitionResult.Continue, result)

        result = stateMachine.onPlaybackComplete()  // Gap complete
        assertEquals(LearningState.Playing, stateMachine.state.value)
        assertEquals(TransitionResult.Continue, result)

        // Second repeat
        result = stateMachine.onPlaybackComplete()
        assertEquals(LearningState.Gap, stateMachine.state.value)

        result = stateMachine.onPlaybackComplete()  // Gap complete
        assertEquals(TransitionResult.NextChunk, result)
    }
}
