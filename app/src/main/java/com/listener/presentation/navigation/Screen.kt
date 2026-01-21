package com.listener.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(
    val route: String,
    val title: String = "",
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Playlist : Screen(
        route = "playlist",
        title = "Playlist",
        selectedIcon = Icons.AutoMirrored.Filled.QueueMusic,
        unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic
    )

    data object PlaylistDetail : Screen(route = "playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    data object FolderDetail : Screen(route = "folder/{folderId}") {
        fun createRoute(folderId: Long) = "folder/$folderId"
    }

    data object Podcast : Screen(
        route = "podcast",
        title = "Podcast",
        selectedIcon = Icons.Filled.Podcasts,
        unselectedIcon = Icons.Outlined.Podcasts
    )

    data object PodcastSearch : Screen(route = "podcast/search")

    data object PodcastDetail : Screen(route = "podcast/{feedUrl}") {
        fun createRoute(feedUrl: String): String {
            return "podcast/${URLEncoder.encode(feedUrl, "UTF-8")}"
        }

        fun decodeUrl(encodedUrl: String): String {
            return URLDecoder.decode(encodedUrl, "UTF-8")
        }
    }

    data object MediaFile : Screen(
        route = "media",
        title = "Media",
        selectedIcon = Icons.Filled.AudioFile,
        unselectedIcon = Icons.Outlined.AudioFile
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object Transcription : Screen(route = "transcription/{sourceId}") {
        fun createRoute(sourceId: String) = "transcription/$sourceId"
    }

    data object TranscriptionQueue : Screen(route = "transcription_queue")

    data object FullScreenPlayer : Screen(route = "player/{sourceId}") {
        fun createRoute(sourceId: String) = "player/$sourceId"
    }

    companion object {
        val bottomNavItems = listOf(Home, Playlist, Podcast, MediaFile, Settings)
    }
}
