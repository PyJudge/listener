package com.listener.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.listener.data.local.db.entity.TranscriptionQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionQueueDao {

    @Query("SELECT * FROM transcription_queue ORDER BY orderIndex ASC")
    fun observeAll(): Flow<List<TranscriptionQueueEntity>>

    @Query("SELECT * FROM transcription_queue ORDER BY orderIndex ASC")
    suspend fun getAll(): List<TranscriptionQueueEntity>

    @Query("SELECT * FROM transcription_queue WHERE id = :id")
    suspend fun getById(id: Long): TranscriptionQueueEntity?

    @Query("SELECT * FROM transcription_queue WHERE sourceId = :sourceId LIMIT 1")
    suspend fun getBySourceId(sourceId: String): TranscriptionQueueEntity?

    @Query("SELECT * FROM transcription_queue WHERE status = :status ORDER BY orderIndex ASC")
    suspend fun getByStatus(status: String): List<TranscriptionQueueEntity>

    @Query("SELECT * FROM transcription_queue WHERE status = 'PENDING' ORDER BY orderIndex ASC LIMIT 1")
    suspend fun getNextPending(): TranscriptionQueueEntity?

    @Query("SELECT * FROM transcription_queue WHERE status IN ('DOWNLOADING', 'TRANSCRIBING', 'PROCESSING') LIMIT 1")
    suspend fun getCurrentProcessing(): TranscriptionQueueEntity?

    @Query("SELECT MAX(orderIndex) FROM transcription_queue")
    suspend fun getMaxOrderIndex(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TranscriptionQueueEntity): Long

    @Update
    suspend fun update(entity: TranscriptionQueueEntity)

    @Delete
    suspend fun delete(entity: TranscriptionQueueEntity)

    @Query("DELETE FROM transcription_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transcription_queue WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()

    @Query("UPDATE transcription_queue SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, progress: Float)

    @Query("UPDATE transcription_queue SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateError(id: Long, status: String, errorMessage: String)

    @Query("UPDATE transcription_queue SET orderIndex = :newIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, newIndex: Int)

    @Query("DELETE FROM transcription_queue")
    suspend fun deleteAll()
}
