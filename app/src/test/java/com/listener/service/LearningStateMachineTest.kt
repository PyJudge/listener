package com.listener.service

import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LearningStateMachineTest {

    private lateinit var stateMachine: LearningStateMachine

    @Before
    fun setup() {
        stateMachine = LearningStateMachine()
    }

    // Normal Mode Tests

    @Test
    fun `play in normal mode transitions to Playing`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = false))
        stateMachine.play()
        assertEquals(LearningState.Playing, stateMachine.state.value)
    }

    @Test
    fun `playback complete in Playing transitions to Gap or Recording`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = false, isRecordingEnabled = false))
        stateMachine.play()
        stateMachine.onPlaybackComplete()
        assertEquals(LearningState.Gap, stateMachine.state.value)
    }

    @Test
    fun `playback complete with recording enabled transitions to Recording`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = false, isRecordingEnabled = true))
        stateMachine.play()
        stateMachine.onPlaybackComplete()
        assertEquals(LearningState.Recording, stateMachine.state.value)
    }

    @Test
    fun `gap complete with repeats remaining transitions to Playing`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = false, repeatCount = 2, isRecordingEnabled = false))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // Playing -> Gap
        val result = stateMachine.onPlaybackComplete() // Gap -> Playing (repeat)
        assertEquals(LearningState.Playing, stateMachine.state.value)
        assertEquals(TransitionResult.Continue, result)
    }

    @Test
    fun `gap complete with no repeats remaining returns NextChunk`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = false, repeatCount = 1, isRecordingEnabled = false))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // Playing -> Gap
        val result = stateMachine.onPlaybackComplete() // Gap -> NextChunk
        assertEquals(TransitionResult.NextChunk, result)
    }

    @Test
    fun `pause preserves previous state`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = false))
        stateMachine.play()
        stateMachine.pause()
        assertEquals(LearningState.Paused, stateMachine.state.value)
    }

    @Test
    fun `resume restores previous state`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = false))
        stateMachine.play()
        stateMachine.pause()
        stateMachine.resume()
        assertEquals(LearningState.Playing, stateMachine.state.value)
    }

    // Hard Mode Tests

    @Test
    fun `play in hard mode transitions to PlayingFirst`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = true))
        stateMachine.play()
        assertEquals(LearningState.PlayingFirst, stateMachine.state.value)
    }

    @Test
    fun `hard mode PlayingFirst complete transitions to GapWithRecording`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = true))
        stateMachine.play()
        stateMachine.onPlaybackComplete()
        assertEquals(LearningState.GapWithRecording, stateMachine.state.value)
    }

    @Test
    fun `hard mode GapWithRecording complete transitions to PlayingSecond`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = true))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // PlayingFirst -> GapWithRecording
        stateMachine.onPlaybackComplete() // GapWithRecording -> PlayingSecond
        assertEquals(LearningState.PlayingSecond, stateMachine.state.value)
    }

    @Test
    fun `hard mode PlayingSecond complete transitions to PlaybackRecording`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = true))
        stateMachine.play()
        stateMachine.onPlaybackComplete() // PlayingFirst -> GapWithRecording
        stateMachine.onPlaybackComplete() // GapWithRecording -> PlayingSecond
        stateMachine.onPlaybackComplete() // PlayingSecond -> PlaybackRecording
        assertEquals(LearningState.PlaybackRecording, stateMachine.state.value)
    }

    @Test
    fun `hard mode full cycle with repeats`() {
        stateMachine.updateSettings(LearningSettings(isHardMode = true, repeatCount = 2))
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
}
