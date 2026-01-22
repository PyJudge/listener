package com.listener.service

import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import com.listener.domain.model.PlayMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LearningStateMachineTest {

    private lateinit var stateMachine: LearningStateMachine

    @Before
    fun setup() {
        stateMachine = LearningStateMachine()
    }

    // Normal Mode Tests (PlayMode.LR)

    @Test
    fun `play in LR mode transitions to Playing`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        assertEquals(LearningState.Playing, stateMachine.state.value)
    }

    @Test
    fun `playback complete in Playing transitions to Gap`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.onPlaybackComplete()
        assertEquals(LearningState.Gap, stateMachine.state.value)
    }

    @Test
    fun `gap complete with repeats remaining transitions to Playing`() {
        stateMachine.updateSettings(LearningSettings(repeatCount = 2, playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // Playing -> Gap
        val result = stateMachine.onPlaybackComplete() // Gap -> Playing (repeat)
        assertEquals(LearningState.Playing, stateMachine.state.value)
        assertEquals(TransitionResult.Continue, result)
    }

    @Test
    fun `gap complete with no repeats remaining returns NextChunk`() {
        stateMachine.updateSettings(LearningSettings(repeatCount = 1, playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // Playing -> Gap
        val result = stateMachine.onPlaybackComplete() // Gap -> NextChunk
        assertEquals(TransitionResult.NextChunk, result)
    }

    @Test
    fun `pause preserves previous state`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.pause()
        assertEquals(LearningState.Paused, stateMachine.state.value)
    }

    @Test
    fun `resume restores previous state`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.pause()
        stateMachine.resume()
        assertEquals(LearningState.Playing, stateMachine.state.value)
    }

    // LRLR Mode Tests (Hard Mode)

    @Test
    fun `play in LRLR mode transitions to PlayingFirst`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        stateMachine.play()
        assertEquals(LearningState.PlayingFirst, stateMachine.state.value)
    }

    @Test
    fun `LRLR mode PlayingFirst complete transitions to GapWithRecording`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        stateMachine.play()
        stateMachine.onPlaybackComplete()
        assertEquals(LearningState.GapWithRecording, stateMachine.state.value)
    }

    @Test
    fun `LRLR mode GapWithRecording complete transitions to PlayingSecond`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // PlayingFirst -> GapWithRecording
        stateMachine.onPlaybackComplete() // GapWithRecording -> PlayingSecond
        assertEquals(LearningState.PlayingSecond, stateMachine.state.value)
    }

    @Test
    fun `LRLR mode PlayingSecond complete transitions to PlaybackRecording`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // PlayingFirst -> GapWithRecording
        stateMachine.onPlaybackComplete() // GapWithRecording -> PlayingSecond
        stateMachine.onPlaybackComplete() // PlayingSecond -> PlaybackRecording
        assertEquals(LearningState.PlaybackRecording, stateMachine.state.value)
    }

    @Test
    fun `LRLR mode full cycle with repeats`() {
        stateMachine.updateSettings(LearningSettings(repeatCount = 2, playMode = PlayMode.LRLR))
        stateMachine.play()

        // First repeat
        stateMachine.onPlaybackComplete() // PlayingFirst -> GapWithRecording
        stateMachine.onPlaybackComplete() // GapWithRecording -> PlayingSecond
        stateMachine.onPlaybackComplete() // PlayingSecond -> PlaybackRecording
        val result1 = stateMachine.onPlaybackComplete() // PlaybackRecording -> PlayingFirst (repeat)
        assertEquals(LearningState.PlayingFirst, stateMachine.state.value)
        assertEquals(TransitionResult.Continue, result1)

        // Second repeat
        stateMachine.onPlaybackComplete()
        stateMachine.onPlaybackComplete()
        stateMachine.onPlaybackComplete()
        val result2 = stateMachine.onPlaybackComplete() // -> NextChunk
        assertEquals(TransitionResult.NextChunk, result2)
    }

    // NORMAL Mode Tests

    @Test
    fun `play in NORMAL mode transitions to Playing`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.NORMAL))
        stateMachine.play()
        assertEquals(LearningState.Playing, stateMachine.state.value)
    }

    @Test
    fun `NORMAL mode playback complete goes directly to NextChunk`() {
        stateMachine.updateSettings(LearningSettings(repeatCount = 1, playMode = PlayMode.NORMAL))
        stateMachine.play()
        val result = stateMachine.onPlaybackComplete()
        assertEquals(TransitionResult.NextChunk, result)
    }

    // === pause 위치 저장 테스트 ===

    @Test
    fun `pause saves current position`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.pause(5000L)
        assertEquals(5000L, stateMachine.getPausedPositionMs())
    }

    @Test
    fun `resume returns saved position`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.pause(5000L)
        val resumePosition = stateMachine.resume()
        assertEquals(5000L, resumePosition)
    }

    @Test
    fun `pause with zero position stores zero`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.pause(0L)
        assertEquals(0L, stateMachine.getPausedPositionMs())
    }

    // === PlayMode 변경 시 상태 정규화 테스트 ===

    @Test
    fun `switching from LR Playing to LRLR normalizes to PlayingFirst`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        assertEquals(LearningState.Playing, stateMachine.state.value)

        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        assertEquals(LearningState.PlayingFirst, stateMachine.state.value)
    }

    @Test
    fun `switching from LRLR PlayingFirst to LR normalizes to Playing`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        stateMachine.play()
        assertEquals(LearningState.PlayingFirst, stateMachine.state.value)

        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        assertEquals(LearningState.Playing, stateMachine.state.value)
    }

    @Test
    fun `switching from LR Gap to LRLR normalizes to GapWithRecording`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // Playing → Gap
        assertEquals(LearningState.Gap, stateMachine.state.value)

        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        assertEquals(LearningState.GapWithRecording, stateMachine.state.value)
    }

    @Test
    fun `switching from LRLR GapWithRecording to LR normalizes to Gap`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // PlayingFirst → GapWithRecording
        assertEquals(LearningState.GapWithRecording, stateMachine.state.value)

        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        assertEquals(LearningState.Gap, stateMachine.state.value)
    }

    @Test
    fun `switching from LR Gap to NORMAL normalizes to Idle`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // Playing → Gap
        assertEquals(LearningState.Gap, stateMachine.state.value)

        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.NORMAL))
        assertEquals(LearningState.Idle, stateMachine.state.value)
    }

    @Test
    fun `switching modes during Idle maintains Idle`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        assertEquals(LearningState.Idle, stateMachine.state.value)

        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        assertEquals(LearningState.Idle, stateMachine.state.value)
    }

    @Test
    fun `switching modes during Paused maintains Paused`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        stateMachine.play()
        stateMachine.pause()
        assertEquals(LearningState.Paused, stateMachine.state.value)

        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        assertEquals(LearningState.Paused, stateMachine.state.value)
    }

    @Test
    fun `switching from LRLR PlayingSecond to LR normalizes to Playing`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // → GapWithRecording
        stateMachine.onPlaybackComplete() // → PlayingSecond
        assertEquals(LearningState.PlayingSecond, stateMachine.state.value)

        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        assertEquals(LearningState.Playing, stateMachine.state.value)
    }

    @Test
    fun `switching from LRLR PlaybackRecording to LR normalizes to Idle`() {
        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LRLR))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // → GapWithRecording
        stateMachine.onPlaybackComplete() // → PlayingSecond
        stateMachine.onPlaybackComplete() // → PlaybackRecording
        assertEquals(LearningState.PlaybackRecording, stateMachine.state.value)

        stateMachine.updateSettings(LearningSettings(playMode = PlayMode.LR))
        assertEquals(LearningState.Idle, stateMachine.state.value)
    }
}
