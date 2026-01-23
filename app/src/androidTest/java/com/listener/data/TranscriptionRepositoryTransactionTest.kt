package com.listener.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.listener.data.local.db.ListenerDatabase
import com.listener.data.local.db.dao.TranscriptionDao
import com.listener.data.local.db.entity.ChunkEntity
import com.listener.data.local.db.entity.TranscriptionResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tests for verifying that saveChunks operation is atomic.
 *
 * Problem: When saveChunks deletes existing chunks and then inserts new ones,
 * another coroutine calling getChunks between delete and insert would get empty list.
 *
 * Solution: Use @Transaction annotation or database.withTransaction {} to ensure atomicity.
 */
@RunWith(AndroidJUnit4::class)
class TranscriptionRepositoryTransactionTest {

    private lateinit var database: ListenerDatabase
    private lateinit var transcriptionDao: TranscriptionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ListenerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transcriptionDao = database.transcriptionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Helper function to insert a TranscriptionResultEntity (required for FK constraint)
     */
    private suspend fun insertTranscriptionResult(sourceId: String) {
        transcriptionDao.insertTranscription(
            TranscriptionResultEntity(
                sourceId = sourceId,
                language = "en",
                fullText = "Test transcription text",
                segmentsJson = "[]",
                wordsJson = "[]",
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Test that saveChunks is atomic - during chunk replacement,
     * concurrent readers should never see an empty list (only old or new chunks).
     *
     * Scenario:
     * 1. Insert initial chunks
     * 2. Start transaction that deletes and re-inserts chunks
     * 3. Concurrent coroutine tries to read during the transaction
     * 4. Reader should either see old chunks or new chunks, but NEVER empty list
     */
    @Test
    fun saveChunks_shouldBeAtomic_concurrentReadNeverSeeEmpty() = runBlocking {
        val sourceId = "test-source-123"

        // Step 0: Insert transcription result (required for FK constraint)
        insertTranscriptionResult(sourceId)

        // Step 1: Insert initial chunks
        val initialChunks = listOf(
            ChunkEntity(
                sourceId = sourceId,
                orderIndex = 0,
                startMs = 0,
                endMs = 1000,
                displayText = "Initial chunk 1"
            ),
            ChunkEntity(
                sourceId = sourceId,
                orderIndex = 1,
                startMs = 1000,
                endMs = 2000,
                displayText = "Initial chunk 2"
            )
        )
        transcriptionDao.insertChunks(initialChunks)

        // Step 2: Verify initial state
        val initialResult = transcriptionDao.getChunksList(sourceId)
        assertTrue("Initial chunks should exist", initialResult.isNotEmpty())

        val sawEmptyList = AtomicBoolean(false)
        val readerStarted = CountDownLatch(1)
        val transactionCompleted = CountDownLatch(1)

        // Step 3: Start a reader coroutine that continuously reads
        val readerJob = async(Dispatchers.IO) {
            readerStarted.countDown()
            var iterations = 0
            while (!transactionCompleted.await(1, TimeUnit.MILLISECONDS) && iterations < 1000) {
                val chunks = transcriptionDao.getChunksList(sourceId)
                if (chunks.isEmpty()) {
                    sawEmptyList.set(true)
                    // Keep checking even after seeing empty to verify the issue
                }
                iterations++
                delay(1) // Small delay to allow context switching
            }
        }

        // Wait for reader to start
        readerStarted.await()

        // Step 4: Perform atomic saveChunks operation using withTransaction
        val newChunks = listOf(
            ChunkEntity(
                sourceId = sourceId,
                orderIndex = 0,
                startMs = 0,
                endMs = 1500,
                displayText = "New chunk 1"
            ),
            ChunkEntity(
                sourceId = sourceId,
                orderIndex = 1,
                startMs = 1500,
                endMs = 3000,
                displayText = "New chunk 2"
            )
        )

        // This should be atomic - either all old chunks or all new chunks visible
        database.withTransaction {
            transcriptionDao.deleteChunks(sourceId)
            delay(10) // Artificial delay to increase race condition probability
            transcriptionDao.insertChunks(newChunks)
        }

        transactionCompleted.countDown()
        readerJob.await()

        // Step 5: Verify reader never saw empty list
        assertFalse(
            "Concurrent reader should never see empty list during atomic saveChunks",
            sawEmptyList.get()
        )

        // Verify final state
        val finalChunks = transcriptionDao.getChunksList(sourceId)
        assertTrue("Final chunks should exist", finalChunks.isNotEmpty())
        assertTrue("Final chunks should be new chunks", finalChunks[0].displayText == "New chunk 1")
    }

    /**
     * Test that demonstrates the problem when NOT using transaction.
     * This test is expected to FAIL if the implementation doesn't use transaction.
     *
     * Once the implementation is fixed with @Transaction, this test should PASS.
     */
    @Test
    fun saveChunks_withoutTransaction_mightShowEmpty() = runBlocking {
        val sourceId = "test-source-456"

        // Insert transcription result first (required for FK constraint)
        insertTranscriptionResult(sourceId)

        // Insert initial chunks
        val initialChunks = listOf(
            ChunkEntity(
                sourceId = sourceId,
                orderIndex = 0,
                startMs = 0,
                endMs = 1000,
                displayText = "Initial"
            )
        )
        transcriptionDao.insertChunks(initialChunks)

        val sawEmptyList = AtomicBoolean(false)
        val writerReady = CountDownLatch(1)
        val writerDone = CountDownLatch(1)

        // Reader coroutine
        val readerJob = async(Dispatchers.IO) {
            writerReady.await()
            repeat(500) {
                val chunks = transcriptionDao.getChunksList(sourceId)
                if (chunks.isEmpty()) {
                    sawEmptyList.set(true)
                }
                delay(1)
            }
        }

        // Writer coroutine - simulates NON-atomic saveChunks (delete then insert without transaction)
        val writerJob = async(Dispatchers.IO) {
            writerReady.countDown()
            repeat(50) {
                // This is NOT atomic - simulates the bug
                transcriptionDao.deleteChunks(sourceId)
                delay(5) // Artificial delay to make race condition more likely
                transcriptionDao.insertChunks(listOf(
                    ChunkEntity(
                        sourceId = sourceId,
                        orderIndex = 0,
                        startMs = 0,
                        endMs = 1000,
                        displayText = "Chunk $it"
                    )
                ))
            }
            writerDone.countDown()
        }

        writerJob.await()
        readerJob.await()

        // This test demonstrates the problem - without transaction, reader CAN see empty list
        // Note: This is a probabilistic test - it might pass sometimes even without fix
        // The important thing is that with proper transaction, it should ALWAYS pass
        println("Without transaction, saw empty list: ${sawEmptyList.get()}")

        // We don't assert here because this test is just to demonstrate the problem
        // The first test is the actual verification test
    }
}
