package com.listener.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.listener.presentation.home.HomeScreen
import com.listener.presentation.media.MediaFileScreen
import com.listener.presentation.player.FullScreenPlayerScreen
import com.listener.presentation.playlist.FolderDetailScreen
import com.listener.presentation.playlist.PlaylistDetailScreen
import com.listener.presentation.playlist.PlaylistScreen
import com.listener.presentation.podcast.PodcastDetailScreen
import com.listener.presentation.podcast.PodcastScreen
import com.listener.presentation.podcast.PodcastSearchScreen
import com.listener.presentation.settings.SettingsScreen
import com.listener.presentation.transcription.TranscriptionScreen

@Composable
fun ListenerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToPlayer: (sourceId: String) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { it / 5 },
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                targetOffsetX = { it / 5 },
                animationSpec = tween(200)
            )
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPlayer = { sourceId ->
                    navController.navigate(Screen.FullScreenPlayer.createRoute(sourceId))
                },
                onNavigateToPodcast = { navController.navigate(Screen.Podcast.route) },
                onNavigateToContinueLearning = { navController.navigate(Screen.Playlist.route) },
                onNavigateToTranscription = { sourceId ->
                    navController.navigate(Screen.Transcription.createRoute(sourceId))
                }
            )
        }

        composable(Screen.Playlist.route) {
            PlaylistScreen(
                onNavigateToPlaylistDetail = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                },
                onNavigateToFolderDetail = { folderId ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderId))
                },
                onNavigateToPlayer = { sourceId ->
                    navController.navigate(Screen.FullScreenPlayer.createRoute(sourceId))
                },
                onNavigateToTranscription = { sourceId ->
                    navController.navigate(Screen.Transcription.createRoute(sourceId))
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            PlaylistDetailScreen(
                playlistId = playlistId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { sourceId ->
                    navController.navigate(Screen.FullScreenPlayer.createRoute(sourceId))
                },
                onNavigateToTranscription = { sourceId ->
                    navController.navigate(Screen.Transcription.createRoute(sourceId))
                }
            )
        }

        composable(
            route = Screen.FolderDetail.route,
            arguments = listOf(navArgument("folderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: 0L
            FolderDetailScreen(
                folderId = folderId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { sourceId ->
                    navController.navigate(Screen.FullScreenPlayer.createRoute(sourceId))
                },
                onNavigateToTranscription = { sourceId ->
                    navController.navigate(Screen.Transcription.createRoute(sourceId))
                }
            )
        }

        composable(Screen.Podcast.route) {
            PodcastScreen(
                onNavigateToSearch = { navController.navigate(Screen.PodcastSearch.route) },
                onNavigateToDetail = { feedUrl ->
                    navController.navigate(Screen.PodcastDetail.createRoute(feedUrl))
                }
            )
        }

        composable(Screen.PodcastSearch.route) {
            PodcastSearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { feedUrl ->
                    navController.navigate(Screen.PodcastDetail.createRoute(feedUrl))
                }
            )
        }

        composable(
            route = Screen.PodcastDetail.route,
            arguments = listOf(navArgument("feedUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val feedUrl = Screen.PodcastDetail.decodeUrl(
                backStackEntry.arguments?.getString("feedUrl") ?: ""
            )
            PodcastDetailScreen(
                feedUrl = feedUrl,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTranscription = { sourceId ->
                    navController.navigate(Screen.Transcription.createRoute(sourceId))
                }
            )
        }

        composable(Screen.MediaFile.route) {
            MediaFileScreen(
                onNavigateToTranscription = { sourceId ->
                    navController.navigate(Screen.Transcription.createRoute(sourceId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = Screen.Transcription.route,
            arguments = listOf(navArgument("sourceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getString("sourceId") ?: ""
            TranscriptionScreen(
                sourceId = sourceId,
                onNavigateBack = { navController.popBackStack() },
                onTranscriptionComplete = {
                    navController.popBackStack()
                    onNavigateToPlayer(sourceId)
                }
            )
        }

        composable(
            route = Screen.FullScreenPlayer.route,
            arguments = listOf(navArgument("sourceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getString("sourceId") ?: ""
            FullScreenPlayerScreen(
                sourceId = sourceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
