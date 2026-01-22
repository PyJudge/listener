package com.listener.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.listener.data.local.db.entity.UserRecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM user_recordings WHERE sourceId = :sourceId")
    fun getRecordings(sourceId: String): Flow<List<UserRecordingEntity>>

    @Query("SELECT * FROM user_recordings WHERE sourceId = :sourceId AND chunkIndex = :chunkIndex")
    suspend fun getRecording(sourceId: String, chunkIndex: Int): UserRecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: UserRecordingEntity)

    @Query("DELETE FROM user_recordings WHERE sourceId = :sourceId AND chunkIndex = :chunkIndex")
    suspend fun deleteRecording(sourceId: String, chunkIndex: Int)

    @Query("DELETE FROM user_recordings WHERE sourceId = :sourceId")
    suspend fun deleteAllRecordings(sourceId: String)

    @Query("DELETE FROM user_recordings")
    suspend fun deleteAll()
}
