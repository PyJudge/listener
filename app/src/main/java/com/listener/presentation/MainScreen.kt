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
    val showMiniPlayer = playbackState.isPlaying || playbackState.sourceId.isNotEmpty()

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
                            navController.navigate(Screen.FullScreenPlayer.route)
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
            onNavigateToPlayer = {
                navController.navigate(Screen.FullScreenPlayer.route)
            }
        )
    }
}
