package com.listener.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.listener.presentation.navigation.ListenerBottomBar
import com.listener.presentation.navigation.ListenerNavHost
import com.listener.presentation.navigation.Screen
import com.listener.presentation.player.MiniPlayer
import com.listener.presentation.player.PlayerViewModel

@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val playbackState by playerViewModel.playbackState.collectAsState()

    // FullScreenPlayer 화면에서는 미니 플레이어 숨김
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isFullScreenPlayer = currentRoute?.startsWith("player/") == true

    val showMiniPlayer = (playbackState.isPlaying || playbackState.sourceId.isNotEmpty())
        && !isFullScreenPlayer

    Scaffold(
        bottomBar = {
            Column {
                // Mini Player
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    MiniPlayer(
                        state = playbackState,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.nextChunk() },
                        onPrevious = { playerViewModel.previousChunk() },
                        onExpand = {
                            if (playbackState.sourceId.isNotEmpty()) {
                                navController.navigate(Screen.FullScreenPlayer.createRoute(playbackState.sourceId))
                            }
                        }
                    )
                }

                // Bottom Navigation
                ListenerBottomBar(navController = navController)
            }
        }
    ) { paddingValues ->
        ListenerNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            onNavigateToPlayer = { sourceId ->
                navController.navigate(Screen.FullScreenPlayer.createRoute(sourceId))
            }
        )
    }
}
