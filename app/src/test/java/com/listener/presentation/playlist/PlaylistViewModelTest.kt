package com.listener.presentation.playlist

import app.cash.turbine.test
import com.listener.data.local.db.dao.FolderDao
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.TranscriptionDao
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PlaylistItemEntity
import com.listener.data.local.db.entity.TranscriptionResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var playlistDao: PlaylistDao
    private lateinit var folderDao: FolderDao
    private lateinit var transcriptionDao: TranscriptionDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var localFileDao: LocalFileDao
    private lateinit var viewModel: PlaylistViewModel

    private val mockPlaylists = listOf(
        PlaylistEntity(
            id = 1,
            name = "Playlist 1",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ),
        PlaylistEntity(
            id = 2,
            name = "Playlist 2",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    )

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(testDispatcher)

        playlistDao = mock()
        folderDao = mock()
        transcriptionDao = mock()
        podcastDao = mock()
        localFileDao = mock()

        whenever(playlistDao.getAllPlaylists()).thenReturn(flowOf(mockPlaylists))
        whenever(playlistDao.getPlaylistItemsList(any())).thenReturn(emptyList())
        whenever(playlistDao.getPlaylistTotalDuration(any())).thenReturn(0L)
        whenever(playlistDao.getPlaylistProgress(any())).thenReturn(0f)

        whenever(folderDao.getAllFolders()).thenReturn(flowOf(emptyList()))
        whenever(transcriptionDao.getAllTranscriptions()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        viewModel = PlaylistViewModel(playlistDao, folderDao, transcriptionDao, podcastDao, localFileDao)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loads playlists successfully`() = runTest {
        viewModel = PlaylistViewModel(playlistDao, folderDao, transcriptionDao, podcastDao, localFileDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.playlists.size)
            assertEquals("Playlist 1", state.playlists[0].playlist.name)
            assertEquals("Playlist 2", state.playlists[1].playlist.name)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `showCreatePlaylistDialog updates state`() = runTest {
        viewModel = PlaylistViewModel(playlistDao, folderDao, transcriptionDao, podcastDao, localFileDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showCreatePlaylistDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showCreatePlaylistDialog)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `dismissCreatePlaylistDialog updates state`() = runTest {
        viewModel = PlaylistViewModel(playlistDao, folderDao, transcriptionDao, podcastDao, localFileDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showCreatePlaylistDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissCreatePlaylistDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showCreatePlaylistDialog)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `createPlaylist calls dao and dismisses dialog`() = runTest {
        whenever(playlistDao.insertPlaylist(any())).thenReturn(1L)

        viewModel = PlaylistViewModel(playlistDao, folderDao, transcriptionDao, podcastDao, localFileDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showCreatePlaylistDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createPlaylist("New Playlist")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playlistDao).insertPlaylist(any())

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showCreatePlaylistDialog)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deletePlaylist calls dao`() = runTest {
        viewModel = PlaylistViewModel(playlistDao, folderDao, transcriptionDao, podcastDao, localFileDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deletePlaylist(mockPlaylists[0])
        testDispatcher.scheduler.advanceUntilIdle()

        verify(playlistDao).deletePlaylist(mockPlaylists[0])
    }

    @Test
    fun `playlists include progress information`() = runTest {
        val mockItems = listOf(
            PlaylistItemEntity(1, 1, "s1", "PODCAST_EPISODE", 0, 0),
            PlaylistItemEntity(2, 1, "s2", "PODCAST_EPISODE", 1, 0),
            PlaylistItemEntity(3, 1, "s3", "PODCAST_EPISODE", 2, 0)
        )
        whenever(playlistDao.getPlaylistItemsList(1L)).thenReturn(mockItems)
        whenever(playlistDao.getPlaylistTotalDuration(1L)).thenReturn(3600000L) // 1 hour
        whenever(playlistDao.getPlaylistProgress(1L)).thenReturn(0.5f) // 50%

        viewModel = PlaylistViewModel(playlistDao, folderDao, transcriptionDao, podcastDao, localFileDao)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            val playlist1 = state.playlists.find { it.playlist.id == 1L }
            assertEquals(3, playlist1?.itemCount)
            assertEquals(3600000L, playlist1?.totalDurationMs)
            assertEquals(0.5f, playlist1?.progress)
            cancelAndConsumeRemainingEvents()
        }
    }
}
