package com.listener.domain.repository

import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.SubscribedPodcastEntity
import kotlinx.coroutines.flow.Flow

interface PodcastRepository {
    fun getSubscriptions(): Flow<List<SubscribedPodcastEntity>>
    suspend fun getSubscription(feedUrl: String): SubscribedPodcastEntity?
    suspend fun subscribe(podcast: SubscribedPodcastEntity)
    suspend fun unsubscribe(feedUrl: String)

    fun getEpisodes(feedUrl: String): Flow<List<PodcastEpisodeEntity>>
    suspend fun getEpisode(id: String): PodcastEpisodeEntity?
    suspend fun refreshEpisodes(feedUrl: String): Result<List<PodcastEpisodeEntity>>
    suspend fun markEpisodeAsRead(id: String)
}
