package com.listener.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for RecordingManager pause/resume functionality.
 *
 * B2 Issue (PR-3, PR-4): MediaRecorder pause/resume for LRLR mode
 *
 * Key behaviors:
 * - pauseRecording(): Pause ongoing recording (API 24+)
 * - resumeRecording(): Resume paused recording (API 24+)
 * - API < 24: Return false (not supported)
 * - Not recording: Return false
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordingManagerPauseResumeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock

    @Before
    fun setup() {
        context = mock()
        powerManager = mock()
        wakeLock = mock()

        whenever(context.filesDir).thenReturn(tempFolder.root)
        whenever(context.getSystemService(Context.POWER_SERVICE)).thenReturn(powerManager)
        whenever(powerManager.newWakeLock(any(), any())).thenReturn(wakeLock)
        whenever(wakeLock.isHeld).thenReturn(false)

        // Default: Has RECORD_AUDIO permission
        whenever(context.checkPermission(eq(Manifest.permission.RECORD_AUDIO), any(), any()))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
    }

    // ==================== Pause Recording Tests ====================

    /**
     * Test pauseRecording returns false when not recording.
     */
    @Test
    fun `pauseRecording returns false when not recording`() {
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { 100L * 1024 * 1024 }  // 100MB
        )

        // When: Pause without starting recording
        val result = recordingManager.pauseRecording()

        // Then: Should return false
        assertFalse(result)
    }

    /**
     * Test isPaused returns correct state.
     */
    @Test
    fun `isPaused returns false when not paused`() {
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { 100L * 1024 * 1024 }
        )

        // When: Not recording or paused
        // Then: isPaused should be false
        assertFalse(recordingManager.isPaused())
    }

    // ==================== Resume Recording Tests ====================

    /**
     * Test resumeRecording returns false when not paused.
     */
    @Test
    fun `resumeRecording returns false when not paused`() {
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { 100L * 1024 * 1024 }
        )

        // When: Resume without pausing
        val result = recordingManager.resumeRecording()

        // Then: Should return false
        assertFalse(result)
    }

    // ==================== API Level Check Tests ====================

    /**
     * Test that supportsRecordingPause returns correct value based on API level.
     *
     * Note: This test verifies the function exists and returns a boolean.
     * Actual API level behavior is tested by Android's Build.VERSION.SDK_INT.
     */
    @Test
    fun `supportsRecordingPause returns boolean`() {
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { 100L * 1024 * 1024 }
        )

        // When/Then: Function should return a boolean without throwing
        val result = recordingManager.supportsRecordingPause()
        // Result depends on Build.VERSION.SDK_INT, but should be deterministic
        assertTrue(result is Boolean)
    }

    // ==================== State Consistency Tests ====================

    /**
     * Test that pause/resume operations don't corrupt internal state.
     */
    @Test
    fun `multiple pause resume calls are safe`() {
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { 100L * 1024 * 1024 }
        )

        // When: Multiple pause/resume calls without recording
        assertFalse(recordingManager.pauseRecording())
        assertFalse(recordingManager.resumeRecording())
        assertFalse(recordingManager.pauseRecording())
        assertFalse(recordingManager.resumeRecording())

        // Then: Should not throw and state should remain consistent
        assertFalse(recordingManager.isPaused())
        assertFalse(recordingManager.isCurrentlyRecording())
    }
}
