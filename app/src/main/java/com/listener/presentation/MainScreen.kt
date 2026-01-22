package com.listener.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.listener.domain.repository.TranscriptionRepository
import com.listener.domain.repository.TranscriptionState
import com.listener.presentation.navigation.ListenerBottomBar
import com.listener.presentation.navigation.ListenerNavHost
import com.listener.presentation.navigation.Screen
import com.listener.presentation.player.MiniPlayer
import com.listener.presentation.player.PlayerViewModel

@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    transcriptionRepository: TranscriptionRepository
) {
    val navController = rememberNavController()
    val playbackState by playerViewModel.playbackState.collectAsState()
    val transcriptionState by transcriptionRepository.transcriptionState.collectAsState()

    // FullScreenPlayer 화면에서는 미니 플레이어 숨김
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isFullScreenPlayer = currentRoute?.startsWith("player/") == true

    val showMiniPlayer = (playbackState.isPlaying || playbackState.sourceId.isNotEmpty())
        && !isFullScreenPlayer

    // 전사 진행 중일 때 FAB 위치에 모래시계 표시
    val isTranscribing = transcriptionState is TranscriptionState.InProgress
    val transcribingSourceId = (transcriptionState as? TranscriptionState.InProgress)?.sourceId

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
        },
        floatingActionButton = {
            // 전사 진행 중일 때만 모래시계 FAB 표시
            if (isTranscribing && transcribingSourceId != null) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.Transcription.createRoute(transcribingSourceId))
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = "Transcription in progress"
                    )
                }
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
