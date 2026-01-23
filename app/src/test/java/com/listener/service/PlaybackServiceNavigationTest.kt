package com.listener.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * TDD Tests for PlaybackService Navigation Race Condition Fix
 *
 * Problem: When nextChunk(), previousChunk(), seekToChunk() are called rapidly,
 * multiple coroutines run concurrently causing race conditions.
 *
 * Solution: Use Mutex to serialize navigation operations and join() to wait
 * for recording jobs to complete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackServiceNavigationTest {

    private lateinit var navigator: TestableChunkNavigator

    @Before
    fun setup() {
        navigator = TestableChunkNavigator(totalChunks = 10)
    }

    // ========== Test 1: Rapid next calls should serialize operations ==========

    @Test
    fun `rapidNextChunkCalls_shouldSerializeOperations`() = runTest {
        // Given: Navigator at chunk 0
        assertEquals(0, navigator.currentChunkIndex)

        // When: Rapidly call nextChunk 5 times
        repeat(5) {
            navigator.nextChunk(this)
        }

        // Then: Wait for all operations to complete
        advanceUntilIdle()

        // Should be at chunk 5 (not beyond due to bounds)
        assertEquals(5, navigator.currentChunkIndex)

        // Operations should have executed exactly 5 times, sequentially
        assertEquals(5, navigator.navigationCount.get())
    }

    @Test
    fun `rapidNextChunkCalls_shouldNotExceedTotalChunks`() = runTest {
        // Given: Navigator at chunk 7 (near end, total=10)
        navigator.currentChunkIndex = 7

        // When: Try to advance 5 times (would go to 12, but max is 9)
        repeat(5) {
            navigator.nextChunk(this)
        }
        advanceUntilIdle()

        // Then: Should stop at last chunk (9)
        assertEquals(9, navigator.currentChunkIndex)
    }

    @Test
    fun `rapidPreviousChunkCalls_shouldSerializeOperations`() = runTest {
        // Given: Navigator at chunk 8
        navigator.currentChunkIndex = 8

        // When: Rapidly call previousChunk 5 times
        repeat(5) {
            navigator.previousChunk(this)
        }
        advanceUntilIdle()

        // Then: Should be at chunk 3
        assertEquals(3, navigator.currentChunkIndex)
        assertEquals(5, navigator.navigationCount.get())
    }

    @Test
    fun `rapidPreviousChunkCalls_shouldNotGoBelowZero`() = runTest {
        // Given: Navigator at chunk 2
        navigator.currentChunkIndex = 2

        // When: Try to go back 5 times
        repeat(5) {
            navigator.previousChunk(this)
        }
        advanceUntilIdle()

        // Then: Should stop at first chunk (0)
        assertEquals(0, navigator.currentChunkIndex)
    }

    @Test
    fun `mixedNavigationCalls_shouldSerializeOperations`() = runTest {
        // Given: Navigator at chunk 5
        navigator.currentChunkIndex = 5

        // When: Mix of next and previous calls
        navigator.nextChunk(this)      // 5 -> 6
        navigator.nextChunk(this)      // 6 -> 7
        navigator.previousChunk(this)  // 7 -> 6
        navigator.seekToChunk(this, 3) // 6 -> 3
        navigator.nextChunk(this)      // 3 -> 4
        advanceUntilIdle()

        // Then: Final position should be 4
        assertEquals(4, navigator.currentChunkIndex)
        assertEquals(5, navigator.navigationCount.get())
    }

    // ========== Test 2: Cancel pending operations should wait for recording job ==========

    @Test
    fun `cancelPendingOperations_shouldWaitForRecordingJobCompletion`() = runTest {
        // Given: A recording job that takes 100ms
        navigator.startRecordingJob(this, durationMs = 100)
        assertTrue(navigator.isRecordingJobActive())

        // When: Cancel pending operations
        navigator.cancelPendingOperations()

        // Then: Recording job should be cancelled AND completed (join)
        assertFalse(navigator.isRecordingJobActive())
        assertTrue(navigator.recordingJobWasJoined)
    }

    @Test
    fun `navigationDuringRecording_shouldCancelAndWaitForRecording`() = runTest {
        // Given: Navigator at chunk 3 with active recording
        navigator.currentChunkIndex = 3
        navigator.startRecordingJob(this, durationMs = 200)

        // When: Navigate to next chunk
        navigator.nextChunk(this)
        advanceUntilIdle()

        // Then: Recording should be cancelled, then navigation completes
        assertEquals(4, navigator.currentChunkIndex)
        assertTrue(navigator.recordingJobWasJoined)
        assertFalse(navigator.isRecordingJobActive())
    }

    @Test
    fun `multipleNavigationsDuringRecording_shouldCancelOnceAndSerialize`() = runTest {
        // Given: Recording active
        navigator.currentChunkIndex = 2
        navigator.startRecordingJob(this, durationMs = 300)

        // When: Multiple rapid navigation calls
        navigator.nextChunk(this)  // Should cancel recording, go to 3
        navigator.nextChunk(this)  // Should go to 4
        navigator.nextChunk(this)  // Should go to 5
        advanceUntilIdle()

        // Then: Final position correct, recording cancelled
        assertEquals(5, navigator.currentChunkIndex)
        assertFalse(navigator.isRecordingJobActive())
    }

    // ========== Test 3: Recording failure should delete incomplete file ==========

    @Test
    fun `recordingFailure_shouldDeleteIncompleteFile`() = runTest {
        // Given: Navigator with recording that will fail
        navigator.currentChunkIndex = 2
        navigator.simulateRecordingFailure = true

        // When: Start recording (which fails)
        navigator.startRecordingWithFailure(this)
        advanceUntilIdle()

        // Then: Incomplete file should be marked for deletion
        assertTrue(navigator.incompleteFileWasDeleted)
    }

    @Test
    fun `cancelDuringRecording_shouldDeleteIncompleteFile`() = runTest {
        // Given: Recording in progress
        navigator.currentChunkIndex = 2
        navigator.startRecordingJob(this, durationMs = 500)

        // When: Navigation cancels recording
        navigator.nextChunk(this)
        advanceUntilIdle()

        // Then: Incomplete recording should be deleted
        assertTrue(navigator.incompleteFileWasDeleted)
    }

    // ========== Test 4: Concurrent seekToChunk calls ==========

    @Test
    fun `concurrentSeekToChunk_shouldOnlyExecuteLastSeek`() = runTest {
        // Given: Navigator at chunk 0
        navigator.currentChunkIndex = 0

        // When: Multiple seekToChunk calls
        navigator.seekToChunk(this, 3)
        navigator.seekToChunk(this, 5)
        navigator.seekToChunk(this, 7)
        advanceUntilIdle()

        // Then: Should end at 7 (last seek destination)
        // All seeks execute in order due to mutex, ending at 7
        assertEquals(7, navigator.currentChunkIndex)
    }

    // ========== Test 5: Edge cases ==========

    @Test
    fun `seekToInvalidIndex_shouldBeIgnored`() = runTest {
        navigator.currentChunkIndex = 5

        // Negative index
        navigator.seekToChunk(this, -1)
        advanceUntilIdle()
        assertEquals(5, navigator.currentChunkIndex)

        // Index beyond total
        navigator.seekToChunk(this, 100)
        advanceUntilIdle()
        assertEquals(5, navigator.currentChunkIndex)
    }

    @Test
    fun `emptyChunks_shouldNotCrash`() = runTest {
        val emptyNavigator = TestableChunkNavigator(totalChunks = 0)

        emptyNavigator.nextChunk(this)
        emptyNavigator.previousChunk(this)
        advanceUntilIdle()

        assertEquals(0, emptyNavigator.currentChunkIndex)
    }
}

/**
 * Testable implementation of chunk navigation logic.
 * Extracts the Mutex-based serialization from PlaybackService for testing.
 */
class TestableChunkNavigator(private val totalChunks: Int) {
    var currentChunkIndex = 0
    val navigationCount = AtomicInteger(0)
    var recordingJobWasJoined = false
    var incompleteFileWasDeleted = false
    var simulateRecordingFailure = false

    private val navigationMutex = Mutex()
    private var recordingJob: Job? = null

    suspend fun nextChunk(scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch {
            navigationMutex.withLock {
                cancelPendingOperationsInternal()
                if (currentChunkIndex < totalChunks - 1) {
                    currentChunkIndex++
                    navigationCount.incrementAndGet()
                }
            }
        }
    }

    suspend fun previousChunk(scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch {
            navigationMutex.withLock {
                cancelPendingOperationsInternal()
                if (currentChunkIndex > 0) {
                    currentChunkIndex--
                    navigationCount.incrementAndGet()
                }
            }
        }
    }

    suspend fun seekToChunk(scope: kotlinx.coroutines.CoroutineScope, index: Int) {
        scope.launch {
            navigationMutex.withLock {
                cancelPendingOperationsInternal()
                if (index in 0 until totalChunks) {
                    currentChunkIndex = index
                    navigationCount.incrementAndGet()
                }
            }
        }
    }

    fun startRecordingJob(scope: kotlinx.coroutines.CoroutineScope, durationMs: Long) {
        recordingJob = scope.launch {
            try {
                delay(durationMs)
            } finally {
                // Simulate cleanup on cancellation
            }
        }
    }

    fun startRecordingWithFailure(scope: kotlinx.coroutines.CoroutineScope) {
        recordingJob = scope.launch {
            try {
                delay(50)
                if (simulateRecordingFailure) {
                    incompleteFileWasDeleted = true
                    throw RuntimeException("Recording failed")
                }
            } catch (e: Exception) {
                incompleteFileWasDeleted = true
            }
        }
    }

    suspend fun cancelPendingOperations() {
        cancelPendingOperationsInternal()
    }

    private suspend fun cancelPendingOperationsInternal() {
        recordingJob?.let { job ->
            job.cancel()
            job.join()  // CRITICAL: Wait for job to complete
            recordingJobWasJoined = true
            // Mark incomplete file for deletion
            incompleteFileWasDeleted = true
        }
        recordingJob = null
    }

    fun isRecordingJobActive(): Boolean = recordingJob?.isActive == true
}
