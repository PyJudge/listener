package com.listener.data.repository

import android.util.Log
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.SubscribedPodcastEntity
import com.listener.data.remote.RssParser
import com.listener.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepositoryImpl @Inject constructor(
    private val podcastDao: PodcastDao,
    private val rssParser: RssParser
) : PodcastRepository {

    companion object {
        private const val TAG = "PodcastRepository"
    }

    override fun getSubscriptions(): Flow<List<SubscribedPodcastEntity>> {
        return podcastDao.getAllSubscriptions()
    }

    override suspend fun getSubscription(feedUrl: String): SubscribedPodcastEntity? {
        return podcastDao.getSubscription(feedUrl)
    }

    override suspend fun subscribe(podcast: SubscribedPodcastEntity) {
        podcastDao.insertSubscription(podcast)
    }

    override suspend fun unsubscribe(feedUrl: String) {
        podcastDao.deleteSubscriptionByFeedUrl(feedUrl)
    }

    override fun getEpisodes(feedUrl: String): Flow<List<PodcastEpisodeEntity>> {
        return podcastDao.getEpisodes(feedUrl)
    }

    override suspend fun getEpisode(id: String): PodcastEpisodeEntity? {
        return podcastDao.getEpisode(id)
    }

    override suspend fun refreshEpisodes(feedUrl: String): Result<List<PodcastEpisodeEntity>> {
        return rssParser.parseFeed(feedUrl).onFailure { error ->
            Log.e(TAG, "RSS parsing failed for $feedUrl: ${error.message}", error)
        }.map { feedResult ->
            // Update podcast description from RSS channel info
            feedResult.channelDescription?.let { description ->
                podcastDao.updateDescription(feedUrl, description)
            }

            val episodes = feedResult.episodes
            if (episodes.isNotEmpty()) {
                val existingEpisodes = podcastDao.getEpisodes(feedUrl).first()
                val existingEpisodeMap = existingEpisodes.associateBy { it.id }

                val episodesToInsert = episodes.map { episode ->
                    val existing = existingEpisodeMap[episode.id]
                    if (existing != null) {
                        // Keep existing isNew value (preserve unplayed state)
                        episode.copy(isNew = existing.isNew)
                    } else {
                        // New episode - isNew = true by default from parser
                        episode
                    }
                }
                podcastDao.insertEpisodes(episodesToInsert)
            }
            episodes
        }
    }

    override suspend fun markEpisodeAsRead(id: String) {
        podcastDao.markAsRead(id)
    }
}
