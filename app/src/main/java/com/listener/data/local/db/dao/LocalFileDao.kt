package com.listener.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.listener.data.local.db.entity.LocalAudioFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalFileDao {
    @Query("SELECT * FROM local_audio_files ORDER BY addedAt DESC")
    fun getAllFiles(): Flow<List<LocalAudioFileEntity>>

    @Query("SELECT * FROM local_audio_files WHERE contentHash = :hash")
    suspend fun getFile(hash: String): LocalAudioFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: LocalAudioFileEntity)

    @Delete
    suspend fun deleteFile(file: LocalAudioFileEntity)

    @Query("DELETE FROM local_audio_files WHERE contentHash = :hash")
    suspend fun deleteFileByHash(hash: String)
}
