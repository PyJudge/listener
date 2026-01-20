package com.listener.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.listener.data.local.db.entity.RecentLearningEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentLearningDao {
    @Query("SELECT * FROM recent_learnings ORDER BY lastAccessedAt DESC LIMIT :limit")
    fun getRecentLearnings(limit: Int = 5): Flow<List<RecentLearningEntity>>

    @Query("SELECT * FROM recent_learnings WHERE sourceId = :sourceId")
    suspend fun getRecentLearning(sourceId: String): RecentLearningEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentLearning(learning: RecentLearningEntity)

    @Query("DELETE FROM recent_learnings WHERE sourceId = :sourceId")
    suspend fun deleteRecentLearning(sourceId: String)
}
