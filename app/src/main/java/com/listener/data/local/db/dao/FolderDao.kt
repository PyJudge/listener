package com.listener.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.listener.data.local.db.entity.FolderEntity
import com.listener.data.local.db.entity.FolderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY updatedAt DESC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolder(id: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("SELECT * FROM folder_items WHERE folderId = :folderId ORDER BY orderIndex ASC")
    fun getFolderItems(folderId: Long): Flow<List<FolderItemEntity>>

    @Query("SELECT * FROM folder_items WHERE folderId = :folderId ORDER BY orderIndex ASC")
    suspend fun getFolderItemsList(folderId: Long): List<FolderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderItem(item: FolderItemEntity)

    @Delete
    suspend fun deleteFolderItem(item: FolderItemEntity)

    @Query("UPDATE folder_items SET orderIndex = :newIndex WHERE id = :itemId")
    suspend fun updateFolderItemOrder(itemId: Long, newIndex: Int)

    @Query("SELECT MAX(orderIndex) FROM folder_items WHERE folderId = :folderId")
    suspend fun getMaxOrderIndex(folderId: Long): Int?

    @Query("SELECT COUNT(*) FROM folder_items WHERE folderId = :folderId")
    suspend fun getFolderItemCount(folderId: Long): Int

    @Query("""
        SELECT COALESCE(SUM(pe.durationMs), 0)
        FROM folder_items fi
        LEFT JOIN podcast_episodes pe ON fi.sourceId = pe.id
        WHERE fi.folderId = :folderId
    """)
    suspend fun getFolderTotalDuration(folderId: Long): Long

    @Query("""
        SELECT COALESCE(AVG(
            CASE WHEN rl.totalChunks > 0
            THEN CAST(rl.currentChunkIndex AS FLOAT) / rl.totalChunks
            ELSE 0.0 END
        ), 0.0)
        FROM folder_items fi
        LEFT JOIN recent_learnings rl ON fi.sourceId = rl.sourceId
        WHERE fi.folderId = :folderId
    """)
    suspend fun getFolderProgress(folderId: Long): Float
}
