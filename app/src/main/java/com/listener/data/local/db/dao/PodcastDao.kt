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
    @Query("SELECT * FROM subscribed_podcasts ORDER BY addedAt DESC")
    fun getAllSubscriptions(): Flow<List<SubscribedPodcastEntity>>

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

    @Query("SELECT * FROM podcast_episodes WHERE isNew = 1 ORDER BY pubDate DESC LIMIT :limit")
    fun getNewEpisodes(limit: Int = 10): Flow<List<PodcastEpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<PodcastEpisodeEntity>)

    @Query("UPDATE podcast_episodes SET isNew = 0 WHERE id = :id")
    suspend fun markAsRead(id: String)
}
