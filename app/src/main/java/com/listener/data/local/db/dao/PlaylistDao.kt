package com.listener.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getPlaylistItemsList(playlistId: Long): List<PlaylistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Delete
    suspend fun deletePlaylistItem(item: PlaylistItemEntity)

    @Query("UPDATE playlist_items SET orderIndex = :newIndex WHERE id = :itemId")
    suspend fun updateItemOrder(itemId: Long, newIndex: Int)

    @Query("SELECT MAX(orderIndex) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?

    @Query("""
        SELECT COALESCE(SUM(pe.durationMs), 0)
        FROM playlist_items pi
        LEFT JOIN podcast_episodes pe ON pi.sourceId = pe.id
        WHERE pi.playlistId = :playlistId
    """)
    suspend fun getPlaylistTotalDuration(playlistId: Long): Long

    @Query("""
        SELECT COALESCE(AVG(
            CASE WHEN rl.totalChunks > 0
            THEN CAST(rl.currentChunkIndex AS FLOAT) / rl.totalChunks
            ELSE 0.0 END
        ), 0.0)
        FROM playlist_items pi
        LEFT JOIN recent_learnings rl ON pi.sourceId = rl.sourceId
        WHERE pi.playlistId = :playlistId
    """)
    suspend fun getPlaylistProgress(playlistId: Long): Float
}
