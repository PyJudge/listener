package com.listener.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.listener.data.local.db.entity.ChunkEntity
import com.listener.data.local.db.entity.TranscriptionResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcription_results WHERE sourceId = :sourceId")
    suspend fun getTranscription(sourceId: String): TranscriptionResultEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM transcription_results WHERE sourceId = :sourceId AND language = :language)")
    suspend fun hasTranscription(sourceId: String, language: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscription(transcription: TranscriptionResultEntity)

    @Query("DELETE FROM transcription_results WHERE sourceId = :sourceId")
    suspend fun deleteTranscription(sourceId: String)

    @Query("SELECT * FROM chunks WHERE sourceId = :sourceId ORDER BY orderIndex ASC")
    fun getChunks(sourceId: String): Flow<List<ChunkEntity>>

    @Query("SELECT * FROM chunks WHERE sourceId = :sourceId ORDER BY orderIndex ASC")
    suspend fun getChunksList(sourceId: String): List<ChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    @Query("DELETE FROM chunks WHERE sourceId = :sourceId")
    suspend fun deleteChunks(sourceId: String)

    @Query("SELECT COUNT(*) FROM chunks WHERE sourceId = :sourceId")
    suspend fun getChunkCount(sourceId: String): Int

    @Query("SELECT * FROM transcription_results ORDER BY createdAt DESC")
    fun getAllTranscriptions(): Flow<List<TranscriptionResultEntity>>

    @Query("SELECT * FROM transcription_results ORDER BY createdAt DESC")
    suspend fun getAllTranscriptionsList(): List<TranscriptionResultEntity>

    @Query("SELECT COUNT(*) FROM transcription_results")
    fun getTranscribedCount(): Flow<Int>

    @Query("DELETE FROM transcription_results")
    suspend fun deleteAllTranscriptions()

    @Query("DELETE FROM chunks")
    suspend fun deleteAllChunks()
}
