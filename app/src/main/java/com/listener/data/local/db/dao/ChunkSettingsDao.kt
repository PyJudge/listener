package com.listener.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.listener.data.local.db.entity.ChunkSettingsEntity

@Dao
interface ChunkSettingsDao {
    @Query("SELECT * FROM chunk_settings WHERE sourceId = :sourceId")
    suspend fun getSettings(sourceId: String): ChunkSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: ChunkSettingsEntity)

    @Query("DELETE FROM chunk_settings WHERE sourceId = :sourceId")
    suspend fun deleteSettings(sourceId: String)
}
