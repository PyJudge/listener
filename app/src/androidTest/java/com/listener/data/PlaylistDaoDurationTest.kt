package com.listener.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.listener.data.local.db.ListenerDatabase
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.entity.LocalAudioFileEntity
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PlaylistItemEntity
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.SubscribedPodcastEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for playlist duration calculation.
 *
 * Problem: The current getPlaylistTotalDuration query only JOINs with podcast_episodes,
 * missing local_audio_files duration. This causes duration to be calculated as 0
 * for playlists containing local files.
 *
 * Solution: Use UNION ALL to combine durations from both podcast_episodes and local_audio_files.
 */
@RunWith(AndroidJUnit4::class)
class PlaylistDaoDurationTest {

    private lateinit var database: ListenerDatabase
    private lateinit var playlistDao: PlaylistDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var localFileDao: LocalFileDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ListenerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        playlistDao = database.playlistDao()
        podcastDao = database.podcastDao()
        localFileDao = database.localFileDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Test that playlist duration includes ONLY podcast episodes.
     * This is the baseline test that should pass with current implementation.
     */
    @Test
    fun getPlaylistTotalDuration_withOnlyPodcastEpisodes_returnsCorrectDuration() = runBlocking {
        // Setup: Create a subscribed podcast (required for foreign key)
        val podcast = SubscribedPodcastEntity(
            feedUrl = "https://example.com/feed.xml",
            collectionId = 123L,
            title = "Test Podcast",
            description = "Description",
            artworkUrl = null,
            lastCheckedAt = System.currentTimeMillis(),
            addedAt = System.currentTimeMillis()
        )
        podcastDao.insertSubscription(podcast)

        // Setup: Create podcast episode with known duration
        val episode = PodcastEpisodeEntity(
            id = "episode-1",
            feedUrl = "https://example.com/feed.xml",
            title = "Episode 1",
            audioUrl = "https://example.com/audio.mp3",
            description = null,
            durationMs = 60000L, // 1 minute
            pubDate = System.currentTimeMillis(),
            isNew = true
        )
        podcastDao.insertEpisodes(listOf(episode))

        // Setup: Create playlist and add episode
        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(
                name = "Test Playlist",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = "episode-1",
                sourceType = "PODCAST_EPISODE",
                orderIndex = 0,
                addedAt = System.currentTimeMillis()
            )
        )

        // Act
        val duration = playlistDao.getPlaylistTotalDuration(playlistId)

        // Assert
        assertEquals("Podcast episode duration should be 60000ms", 60000L, duration)
    }

    /**
     * Test that playlist duration includes local audio files.
     * This test will FAIL until the query is fixed to include local_audio_files.
     */
    @Test
    fun getPlaylistTotalDuration_withOnlyLocalFiles_returnsCorrectDuration() = runBlocking {
        // Setup: Create local file with known duration
        val localFile = LocalAudioFileEntity(
            contentHash = "local-file-1",
            uri = "content://media/audio/123",
            displayName = "My Recording.mp3",
            durationMs = 120000L, // 2 minutes
            addedAt = System.currentTimeMillis()
        )
        localFileDao.insertFile(localFile)

        // Setup: Create playlist and add local file
        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(
                name = "Local Files Playlist",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = "local-file-1",
                sourceType = "LOCAL_FILE",
                orderIndex = 0,
                addedAt = System.currentTimeMillis()
            )
        )

        // Act
        val duration = playlistDao.getPlaylistTotalDuration(playlistId)

        // Assert - This should be 120000, but with current implementation it returns 0
        assertEquals(
            "Local file duration should be included (120000ms)",
            120000L,
            duration
        )
    }

    /**
     * Test that playlist duration correctly sums both podcast episodes AND local files.
     * This is the main test that verifies the fix works for mixed playlists.
     */
    @Test
    fun getPlaylistTotalDuration_withMixedContent_returnsTotalDuration() = runBlocking {
        // Setup: Create podcast
        val podcast = SubscribedPodcastEntity(
            feedUrl = "https://example.com/feed.xml",
            collectionId = 456L,
            title = "Mixed Podcast",
            description = null,
            artworkUrl = null,
            lastCheckedAt = System.currentTimeMillis(),
            addedAt = System.currentTimeMillis()
        )
        podcastDao.insertSubscription(podcast)

        // Setup: Create podcast episode
        val episode = PodcastEpisodeEntity(
            id = "episode-mixed",
            feedUrl = "https://example.com/feed.xml",
            title = "Mixed Episode",
            audioUrl = "https://example.com/mixed.mp3",
            description = null,
            durationMs = 30000L, // 30 seconds
            pubDate = System.currentTimeMillis(),
            isNew = true
        )
        podcastDao.insertEpisodes(listOf(episode))

        // Setup: Create local file
        val localFile = LocalAudioFileEntity(
            contentHash = "local-mixed",
            uri = "content://media/audio/456",
            displayName = "Recording.mp3",
            durationMs = 45000L, // 45 seconds
            addedAt = System.currentTimeMillis()
        )
        localFileDao.insertFile(localFile)

        // Setup: Create playlist with both items
        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(
                name = "Mixed Playlist",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = "episode-mixed",
                sourceType = "PODCAST_EPISODE",
                orderIndex = 0,
                addedAt = System.currentTimeMillis()
            )
        )

        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = "local-mixed",
                sourceType = "LOCAL_FILE",
                orderIndex = 1,
                addedAt = System.currentTimeMillis()
            )
        )

        // Act
        val duration = playlistDao.getPlaylistTotalDuration(playlistId)

        // Assert: 30000 (episode) + 45000 (local file) = 75000ms
        assertEquals(
            "Total duration should include both podcast and local file (75000ms)",
            75000L,
            duration
        )
    }

    /**
     * Test that playlist with null duration items handles gracefully.
     */
    @Test
    fun getPlaylistTotalDuration_withNullDuration_returnsPartialSum() = runBlocking {
        // Setup: Create local file with null duration
        val localFileWithNull = LocalAudioFileEntity(
            contentHash = "null-duration-file",
            uri = "content://media/audio/789",
            displayName = "Unknown Duration.mp3",
            durationMs = null, // Duration unknown
            addedAt = System.currentTimeMillis()
        )
        localFileDao.insertFile(localFileWithNull)

        // Setup: Create local file with known duration
        val localFileWithDuration = LocalAudioFileEntity(
            contentHash = "known-duration-file",
            uri = "content://media/audio/101",
            displayName = "Known Duration.mp3",
            durationMs = 90000L, // 1.5 minutes
            addedAt = System.currentTimeMillis()
        )
        localFileDao.insertFile(localFileWithDuration)

        // Setup: Create playlist with both files
        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(
                name = "Null Duration Playlist",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = "null-duration-file",
                sourceType = "LOCAL_FILE",
                orderIndex = 0,
                addedAt = System.currentTimeMillis()
            )
        )

        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = "known-duration-file",
                sourceType = "LOCAL_FILE",
                orderIndex = 1,
                addedAt = System.currentTimeMillis()
            )
        )

        // Act
        val duration = playlistDao.getPlaylistTotalDuration(playlistId)

        // Assert: null + 90000 = 90000 (COALESCE handles null)
        assertEquals(
            "Duration should handle null values gracefully",
            90000L,
            duration
        )
    }

    /**
     * Test empty playlist returns 0 duration.
     */
    @Test
    fun getPlaylistTotalDuration_emptyPlaylist_returnsZero() = runBlocking {
        // Setup: Create empty playlist
        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(
                name = "Empty Playlist",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Act
        val duration = playlistDao.getPlaylistTotalDuration(playlistId)

        // Assert
        assertEquals("Empty playlist should have 0 duration", 0L, duration)
    }
}
