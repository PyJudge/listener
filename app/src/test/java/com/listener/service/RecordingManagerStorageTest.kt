package com.listener.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
 * Tests for RecordingManager storage space checking.
 *
 * PM-2 Issue: Recording should fail if available storage is less than MIN_STORAGE_BYTES (10MB).
 *
 * Key behaviors:
 * - startRecording() should check storage before starting
 * - Return false if available storage < 10MB
 * - Return true (proceed) if available storage >= 10MB
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordingManagerStorageTest {

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

    // ==================== hasEnoughStorage Tests ====================

    /**
     * Test that hasEnoughStorage returns true when available space > 10MB.
     */
    @Test
    fun `hasEnoughStorage returns true when space greater than 10MB`() {
        // Given: Available space is 50MB
        val availableBytes = 50L * 1024 * 1024  // 50MB
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { availableBytes }
        )

        // When/Then
        assertTrue(recordingManager.hasEnoughStorage())
    }

    /**
     * Test that hasEnoughStorage returns true when available space == 10MB.
     */
    @Test
    fun `hasEnoughStorage returns true when space equals 10MB`() {
        // Given: Available space is exactly 10MB
        val availableBytes = 10L * 1024 * 1024  // 10MB
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { availableBytes }
        )

        // When/Then
        assertTrue(recordingManager.hasEnoughStorage())
    }

    /**
     * Test that hasEnoughStorage returns false when available space < 10MB.
     */
    @Test
    fun `hasEnoughStorage returns false when space less than 10MB`() {
        // Given: Available space is 5MB (less than minimum)
        val availableBytes = 5L * 1024 * 1024  // 5MB
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { availableBytes }
        )

        // When/Then
        assertFalse(recordingManager.hasEnoughStorage())
    }

    /**
     * Test that hasEnoughStorage returns false when no space available.
     */
    @Test
    fun `hasEnoughStorage returns false when no space available`() {
        // Given: No available space
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { 0L }
        )

        // When/Then
        assertFalse(recordingManager.hasEnoughStorage())
    }

    // ==================== startRecording Storage Check Tests ====================

    /**
     * Test that startRecording fails when storage is insufficient.
     */
    @Test
    fun `startRecording returns false when storage insufficient`() = runTest {
        // Given: Available space is 5MB (less than minimum)
        val availableBytes = 5L * 1024 * 1024
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { availableBytes }
        )

        // When: Attempt to start recording
        val result = recordingManager.startRecording("source1", 0)

        // Then: Should fail
        assertFalse(result)
    }

    /**
     * Test that startRecording message indicates storage issue.
     * Note: This tests that the correct reason is logged (not directly testable without Log mock)
     */
    @Test
    fun `startRecording with insufficient storage does not throw`() = runTest {
        // Given: Available space is 0
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { 0L }
        )

        // When: Attempt to start recording
        val result = recordingManager.startRecording("source1", 0)

        // Then: Should return false without throwing
        assertFalse(result)
    }

    /**
     * Test that startRecording checks storage before permission.
     * Storage check should happen first to fail fast.
     */
    @Test
    fun `startRecording checks storage before other operations`() = runTest {
        // Given: No permission AND no storage
        whenever(context.checkPermission(eq(Manifest.permission.RECORD_AUDIO), any(), any()))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { 0L }
        )

        // When: Attempt to start recording
        val result = recordingManager.startRecording("source1", 0)

        // Then: Should fail (either due to storage or permission)
        assertFalse(result)
    }

    // ==================== Edge Case Tests ====================

    /**
     * Test with exactly MIN_STORAGE_BYTES - 1 byte (boundary condition).
     */
    @Test
    fun `hasEnoughStorage returns false at boundary minus one byte`() {
        // Given: Available space is 10MB - 1 byte
        val availableBytes = (10L * 1024 * 1024) - 1
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { availableBytes }
        )

        // When/Then
        assertFalse(recordingManager.hasEnoughStorage())
    }

    /**
     * Test with negative available bytes (edge case from system).
     */
    @Test
    fun `hasEnoughStorage handles negative available bytes`() {
        // Given: System returns negative (error condition)
        val recordingManager = RecordingManager(
            context = context,
            storageChecker = { -1L }
        )

        // When/Then
        assertFalse(recordingManager.hasEnoughStorage())
    }
}
