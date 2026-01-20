package com.listener.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.listener.data.local.db.entity.LearningProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningProgressDao {
    @Query("SELECT * FROM learning_progress WHERE sourceId = :sourceId")
    suspend fun getProgress(sourceId: String): LearningProgressEntity?

    @Query("SELECT * FROM learning_progress WHERE sourceId = :sourceId")
    fun observeProgress(sourceId: String): Flow<LearningProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: LearningProgressEntity)

    @Query("UPDATE learning_progress SET currentChunkIndex = :index, updatedAt = :updatedAt WHERE sourceId = :sourceId")
    suspend fun updateCurrentChunk(sourceId: String, index: Int, updatedAt: Long)

    @Query("DELETE FROM learning_progress WHERE sourceId = :sourceId")
    suspend fun deleteProgress(sourceId: String)
}
