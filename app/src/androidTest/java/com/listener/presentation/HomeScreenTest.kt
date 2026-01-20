package com.listener.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.RecentLearningEntity
import com.listener.presentation.components.PlaylistSelectDialog
import com.listener.presentation.theme.ListenerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playlistSelectDialog_displaysPlaylists() {
        val playlists = listOf(
            PlaylistEntity(
                id = 1,
                name = "My Playlist",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            PlaylistEntity(
                id = 2,
                name = "Another Playlist",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        composeTestRule.setContent {
            ListenerTheme {
                PlaylistSelectDialog(
                    playlists = playlists,
                    onDismiss = {},
                    onPlaylistSelected = {},
                    onCreateNewPlaylist = {},
                    itemCounts = mapOf(1L to 5, 2L to 10)
                )
            }
        }

        composeTestRule.onNodeWithText("My Playlist").assertIsDisplayed()
        composeTestRule.onNodeWithText("Another Playlist").assertIsDisplayed()
        composeTestRule.onNodeWithText("5개 항목").assertIsDisplayed()
        composeTestRule.onNodeWithText("10개 항목").assertIsDisplayed()
    }

    @Test
    fun playlistSelectDialog_showsCreateOption() {
        composeTestRule.setContent {
            ListenerTheme {
                PlaylistSelectDialog(
                    playlists = emptyList(),
                    onDismiss = {},
                    onPlaylistSelected = {},
                    onCreateNewPlaylist = {}
                )
            }
        }

        composeTestRule.onNodeWithText("새 플레이리스트 만들기").assertIsDisplayed()
    }

    @Test
    fun playlistSelectDialog_emptyState() {
        composeTestRule.setContent {
            ListenerTheme {
                PlaylistSelectDialog(
                    playlists = emptyList(),
                    onDismiss = {},
                    onPlaylistSelected = {},
                    onCreateNewPlaylist = {}
                )
            }
        }

        composeTestRule.onNodeWithText("플레이리스트가 없습니다.").assertIsDisplayed()
    }

    @Test
    fun playlistSelectDialog_callsOnCreateNewPlaylist() {
        var createCalled = false

        composeTestRule.setContent {
            ListenerTheme {
                PlaylistSelectDialog(
                    playlists = emptyList(),
                    onDismiss = {},
                    onPlaylistSelected = {},
                    onCreateNewPlaylist = { createCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithText("새 플레이리스트 만들기").performClick()

        assert(createCalled) { "onCreateNewPlaylist should be called" }
    }

    @Test
    fun playlistSelectDialog_callsOnPlaylistSelected() {
        var selectedId: Long? = null
        val playlists = listOf(
            PlaylistEntity(
                id = 1,
                name = "My Playlist",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        composeTestRule.setContent {
            ListenerTheme {
                PlaylistSelectDialog(
                    playlists = playlists,
                    onDismiss = {},
                    onPlaylistSelected = { selectedId = it },
                    onCreateNewPlaylist = {},
                    itemCounts = mapOf(1L to 5)
                )
            }
        }

        composeTestRule.onNodeWithText("My Playlist").performClick()

        assert(selectedId == 1L) { "Selected playlist ID should be 1" }
    }

    @Test
    fun playlistSelectDialog_callsOnDismiss() {
        var dismissed = false

        composeTestRule.setContent {
            ListenerTheme {
                PlaylistSelectDialog(
                    playlists = emptyList(),
                    onDismiss = { dismissed = true },
                    onPlaylistSelected = {},
                    onCreateNewPlaylist = {}
                )
            }
        }

        composeTestRule.onNodeWithText("취소").performClick()

        assert(dismissed) { "onDismiss should be called" }
    }
}
