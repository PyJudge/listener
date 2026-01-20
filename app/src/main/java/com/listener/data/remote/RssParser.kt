package com.listener.data.remote

import android.util.Xml
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class RssFeedResult(
    val channelTitle: String?,
    val channelDescription: String?,
    val episodes: List<PodcastEpisodeEntity>
)

@Singleton
class RssParser @Inject constructor() {

    private val rssClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val PARSE_TIMEOUT_MS = 60_000L
        private val RFC_2822_FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        private val RFC_2822_FORMAT_ALT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
        private val ISO_8601_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
    }

    suspend fun parseEpisodes(feedUrl: String): Result<List<PodcastEpisodeEntity>> =
        withContext(Dispatchers.IO) {
            runCatching {
                withTimeout(PARSE_TIMEOUT_MS) {
                    val xmlContent = fetchRssFeed(feedUrl)
                    parseXml(xmlContent, feedUrl).episodes
                }
            }
        }

    suspend fun parseFeed(feedUrl: String): Result<RssFeedResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                withTimeout(PARSE_TIMEOUT_MS) {
                    val xmlContent = fetchRssFeed(feedUrl)
                    parseXml(xmlContent, feedUrl)
                }
            }
        }

    private fun fetchRssFeed(feedUrl: String): String {
        val request = Request.Builder()
            .url(feedUrl)
            .header("User-Agent", "Listener-Android/1.0")
            .build()

        val response = rssClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RssParseException("Failed to fetch RSS feed: ${response.code}")
        }

        return response.body?.string()
            ?: throw RssParseException("Empty response body")
    }

    private fun parseXml(xml: String, feedUrl: String): List<PodcastEpisodeEntity> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val episodes = mutableListOf<PodcastEpisodeEntity>()
        var eventType = parser.eventType
        var currentEpisode: EpisodeBuilder? = null
        var insideItem = false
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    when {
                        currentTag.equals("item", ignoreCase = true) -> {
                            insideItem = true
                            currentEpisode = EpisodeBuilder()
                        }
                        insideItem && currentTag.equals("enclosure", ignoreCase = true) -> {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (type.startsWith("audio/") || type.isEmpty()) {
                                currentEpisode?.audioUrl = parser.getAttributeValue(null, "url")
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideItem && currentEpisode != null) {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            when {
                                currentTag.equals("title", ignoreCase = true) ->
                                    currentEpisode.title = text
                                currentTag.equals("guid", ignoreCase = true) ->
                                    currentEpisode.guid = text
                                currentTag.equals("description", ignoreCase = true) ->
                                    currentEpisode.description = text
                                currentTag.equals("pubDate", ignoreCase = true) ->
                                    currentEpisode.pubDate = text
                                currentTag.equals("itunes:duration", ignoreCase = true) ||
                                currentTag.equals("duration", ignoreCase = true) ->
                                    currentEpisode.duration = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true)) {
                        insideItem = false
                        currentEpisode?.let { builder ->
                            builder.toEntity(feedUrl)?.let { entity ->
                                episodes.add(entity)
                            }
                        }
                        currentEpisode = null
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return episodes
    }

    private class EpisodeBuilder {
        var guid: String? = null
        var title: String? = null
        var audioUrl: String? = null
        var description: String? = null
        var pubDate: String? = null
        var duration: String? = null

        fun toEntity(feedUrl: String): PodcastEpisodeEntity? {
            val episodeTitle = title ?: return null
            val episodeAudioUrl = audioUrl ?: return null

            val id = guid ?: generateId(feedUrl, episodeAudioUrl)

            return PodcastEpisodeEntity(
                id = id,
                feedUrl = feedUrl,
                title = episodeTitle,
                audioUrl = episodeAudioUrl,
                description = description?.take(2000),
                durationMs = parseDuration(duration),
                pubDate = parseDate(pubDate),
                isNew = true
            )
        }

        private fun generateId(feedUrl: String, audioUrl: String): String {
            val input = "$feedUrl|$audioUrl"
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(input.toByteArray())
                .take(16)
                .joinToString("") { "%02x".format(it) }
        }

        private fun parseDuration(duration: String?): Long? {
            if (duration == null) return null

            return try {
                when {
                    duration.contains(":") -> {
                        val parts = duration.split(":")
                        when (parts.size) {
                            3 -> {
                                val hours = parts[0].toLongOrNull() ?: 0
                                val minutes = parts[1].toLongOrNull() ?: 0
                                val seconds = parts[2].toLongOrNull() ?: 0
                                (hours * 3600 + minutes * 60 + seconds) * 1000
                            }
                            2 -> {
                                val minutes = parts[0].toLongOrNull() ?: 0
                                val seconds = parts[1].toLongOrNull() ?: 0
                                (minutes * 60 + seconds) * 1000
                            }
                            else -> null
                        }
                    }
                    else -> {
                        duration.toLongOrNull()?.times(1000)
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun parseDate(dateStr: String?): Long {
            if (dateStr == null) return System.currentTimeMillis()

            return try {
                RFC_2822_FORMAT.parse(dateStr)?.time
                    ?: RFC_2822_FORMAT_ALT.parse(dateStr)?.time
                    ?: ISO_8601_FORMAT.parse(dateStr)?.time
                    ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}

class RssParseException(message: String) : Exception(message)
