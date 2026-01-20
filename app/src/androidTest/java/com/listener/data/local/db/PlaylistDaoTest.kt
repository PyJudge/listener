package com.listener.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var database: ListenerDatabase
    private lateinit var playlistDao: PlaylistDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ListenerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        playlistDao = database.playlistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertPlaylist_returnsId() = runBlocking {
        val playlist = PlaylistEntity(
            name = "Test Playlist",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val id = playlistDao.insertPlaylist(playlist)

        assertTrue(id > 0)
    }

    @Test
    fun getAllPlaylists_returnsInsertedPlaylists() = runBlocking {
        val playlist1 = PlaylistEntity(
            name = "Playlist 1",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val playlist2 = PlaylistEntity(
            name = "Playlist 2",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        playlistDao.insertPlaylist(playlist1)
        playlistDao.insertPlaylist(playlist2)

        val playlists = playlistDao.getAllPlaylists().first()

        assertEquals(2, playlists.size)
    }

    @Test
    fun deletePlaylist_removesPlaylist() = runBlocking {
        val playlist = PlaylistEntity(
            name = "To Delete",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = playlistDao.insertPlaylist(playlist)
        val insertedPlaylist = playlist.copy(id = id)

        playlistDao.deletePlaylist(insertedPlaylist)

        val playlists = playlistDao.getAllPlaylists().first()
        assertTrue(playlists.isEmpty())
    }

    @Test
    fun insertPlaylistItem_addsItemToPlaylist() = runBlocking {
        val playlist = PlaylistEntity(
            name = "Test Playlist",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val playlistId = playlistDao.insertPlaylist(playlist)

        val item = PlaylistItemEntity(
            playlistId = playlistId,
            sourceId = "source123",
            sourceType = "PODCAST_EPISODE",
            orderIndex = 0,
            addedAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylistItem(item)

        val items = playlistDao.getPlaylistItemsList(playlistId)
        assertEquals(1, items.size)
        assertEquals("source123", items[0].sourceId)
    }

    @Test
    fun getMaxOrderIndex_returnsCorrectValue() = runBlocking {
        val playlist = PlaylistEntity(
            name = "Test Playlist",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val playlistId = playlistDao.insertPlaylist(playlist)

        val item1 = PlaylistItemEntity(
            playlistId = playlistId,
            sourceId = "source1",
            sourceType = "PODCAST_EPISODE",
            orderIndex = 0,
            addedAt = System.currentTimeMillis()
        )
        val item2 = PlaylistItemEntity(
            playlistId = playlistId,
            sourceId = "source2",
            sourceType = "PODCAST_EPISODE",
            orderIndex = 5,
            addedAt = System.currentTimeMillis()
        )

        playlistDao.insertPlaylistItem(item1)
        playlistDao.insertPlaylistItem(item2)

        val maxOrder = playlistDao.getMaxOrderIndex(playlistId)
        assertEquals(5, maxOrder)
    }

    @Test
    fun updatePlaylist_updatesName() = runBlocking {
        val playlist = PlaylistEntity(
            name = "Original Name",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = playlistDao.insertPlaylist(playlist)

        val updatedPlaylist = playlist.copy(
            id = id,
            name = "Updated Name",
            updatedAt = System.currentTimeMillis()
        )
        playlistDao.updatePlaylist(updatedPlaylist)

        val playlists = playlistDao.getAllPlaylists().first()
        assertEquals("Updated Name", playlists[0].name)
    }

    @Test
    fun deletePlaylistItem_removesItem() = runBlocking {
        val playlist = PlaylistEntity(
            name = "Test Playlist",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val playlistId = playlistDao.insertPlaylist(playlist)

        val item = PlaylistItemEntity(
            playlistId = playlistId,
            sourceId = "source123",
            sourceType = "PODCAST_EPISODE",
            orderIndex = 0,
            addedAt = System.currentTimeMillis()
        )
        val itemId = playlistDao.insertPlaylistItem(item)
        val insertedItem = item.copy(id = itemId)

        playlistDao.deletePlaylistItem(insertedItem)

        val items = playlistDao.getPlaylistItemsList(playlistId)
        assertTrue(items.isEmpty())
    }

    @Test
    fun updateItemOrders_reordersItems() = runBlocking {
        val playlist = PlaylistEntity(
            name = "Test Playlist",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val playlistId = playlistDao.insertPlaylist(playlist)

        val item1Id = playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = "source1",
                sourceType = "PODCAST_EPISODE",
                orderIndex = 0,
                addedAt = System.currentTimeMillis()
            )
        )
        val item2Id = playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                sourceId = "source2",
                sourceType = "PODCAST_EPISODE",
                orderIndex = 1,
                addedAt = System.currentTimeMillis()
            )
        )

        // Swap order
        playlistDao.updateItemOrder(item1Id, 1)
        playlistDao.updateItemOrder(item2Id, 0)

        val items = playlistDao.getPlaylistItemsList(playlistId)
        assertEquals("source2", items[0].sourceId)
        assertEquals("source1", items[1].sourceId)
    }
}
