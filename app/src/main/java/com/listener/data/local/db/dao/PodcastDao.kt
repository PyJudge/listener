package com.listener.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.SubscribedPodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM subscribed_podcasts ORDER BY orderIndex ASC, addedAt DESC")
    fun getAllSubscriptions(): Flow<List<SubscribedPodcastEntity>>

    @Query("UPDATE subscribed_podcasts SET orderIndex = :orderIndex WHERE feedUrl = :feedUrl")
    suspend fun updateOrderIndex(feedUrl: String, orderIndex: Int)

    @Query("SELECT MAX(orderIndex) FROM subscribed_podcasts")
    suspend fun getMaxOrderIndex(): Int?

    @Query("SELECT * FROM subscribed_podcasts WHERE feedUrl = :feedUrl")
    suspend fun getSubscription(feedUrl: String): SubscribedPodcastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(podcast: SubscribedPodcastEntity)

    @Delete
    suspend fun deleteSubscription(podcast: SubscribedPodcastEntity)

    @Query("DELETE FROM subscribed_podcasts WHERE feedUrl = :feedUrl")
    suspend fun deleteSubscriptionByFeedUrl(feedUrl: String)

    @Query("SELECT * FROM podcast_episodes WHERE feedUrl = :feedUrl ORDER BY pubDate DESC")
    fun getEpisodes(feedUrl: String): Flow<List<PodcastEpisodeEntity>>

    @Query("SELECT * FROM podcast_episodes WHERE id = :id")
    suspend fun getEpisode(id: String): PodcastEpisodeEntity?

    @Query("""
        SELECT * FROM podcast_episodes
        WHERE isNew = 1
          AND pubDate >= :threeDaysAgo
        ORDER BY pubDate DESC
        LIMIT :limit
    """)
    fun getNewEpisodes(threeDaysAgo: Long, limit: Int = 10): Flow<List<PodcastEpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<PodcastEpisodeEntity>)

    @Query("UPDATE podcast_episodes SET isNew = 0 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE subscribed_podcasts SET description = :description WHERE feedUrl = :feedUrl")
    suspend fun updateDescription(feedUrl: String, description: String?)

    @Query("DELETE FROM podcast_episodes")
    suspend fun deleteAllEpisodes()
}
