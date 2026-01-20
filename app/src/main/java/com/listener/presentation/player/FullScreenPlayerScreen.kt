package com.listener.presentation.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.listener.domain.model.Chunk
import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import com.listener.domain.model.PlaybackState
import com.listener.presentation.theme.ChunkHighlight
import com.listener.presentation.theme.ChunkHighlightAlpha
import com.listener.presentation.theme.Error
import com.listener.presentation.theme.ListenerTheme
import com.listener.presentation.theme.PlayerBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayerScreen(
    sourceId: String = "",
    onNavigateBack: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val chunks by viewModel.chunks.collectAsStateWithLifecycle()
    val playlistId by viewModel.playlistId.collectAsStateWithLifecycle()
    val playlistItems by viewModel.playlistItems.collectAsStateWithLifecycle()
    val currentPlaylistItemIndex by viewModel.currentPlaylistItemIndex.collectAsStateWithLifecycle()

    // Load content when sourceId changes
    LaunchedEffect(sourceId) {
        if (sourceId.isNotEmpty() && playbackState.sourceId != sourceId) {
            viewModel.loadBySourceId(sourceId)
        }
    }

    var isBlindMode by remember { mutableStateOf(false) }
    var revealedChunks by remember { mutableStateOf(setOf<Int>()) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to current chunk
    LaunchedEffect(playbackState.currentChunkIndex) {
        if (chunks.isNotEmpty() && playbackState.currentChunkIndex >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = maxOf(0, playbackState.currentChunkIndex - 1)
                )
            }
        }
    }

    // Reset revealed chunks when blind mode changes or chunk changes
    LaunchedEffect(isBlindMode, playbackState.currentChunkIndex) {
        if (isBlindMode) {
            revealedChunks = setOf()
        }
    }

    Scaffold(
        containerColor = PlayerBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            PlayerHeader(
                title = playbackState.title,
                subtitle = playbackState.subtitle,
                chunkProgress = "${playbackState.currentChunkIndex + 1} / ${playbackState.totalChunks}",
                isBlindMode = isBlindMode,
                onBlindModeToggle = { isBlindMode = !isBlindMode },
                onNavigateBack = onNavigateBack
            )

            // Transcript Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                when {
                                    dragAmount < -50 -> viewModel.nextChunk()
                                    dragAmount > 50 -> viewModel.previousChunk()
                                }
                            }
                        }
                ) {
                    itemsIndexed(
                        items = chunks,
                        key = { _, chunk -> chunk.orderIndex }
                    ) { index, chunk ->
                        ChunkItem(
                            chunk = chunk,
                            isCurrent = index == playbackState.currentChunkIndex,
                            isBlindMode = isBlindMode,
                            isRevealed = revealedChunks.contains(index),
                            hasRecording = viewModel.hasRecording(index),
                            onTap = { viewModel.seekToChunk(index) },
                            onReveal = { revealedChunks = revealedChunks + index },
                            onPlayRecording = { viewModel.playRecording(index) }
                        )
                    }
                }

                // Gradient overlays
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(PlayerBackground, Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, PlayerBackground)
                            )
                        )
                )
            }

            // State indicator
            LearningStateIndicator(
                state = playbackState.learningState,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            PlayerControls(
                isPlaying = playbackState.isPlaying,
                hasPlaylist = playlistId != null,
                hasPreviousItem = currentPlaylistItemIndex > 0,
                hasNextItem = currentPlaylistItemIndex < playlistItems.size - 1,
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.previousChunk() },
                onNext = { viewModel.nextChunk() },
                onPreviousItem = { viewModel.previousPlaylistItem() },
                onNextItem = { viewModel.nextPlaylistItem() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Settings bar
            LearningSettingsBar(
                settings = playbackState.settings,
                onRepeatChange = { viewModel.setRepeatCount(it) },
                onGapRatioChange = { viewModel.setGapRatio(it) },
                onRecordingToggle = { viewModel.toggleRecording() },
                onHardModeToggle = { viewModel.toggleHardMode() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlayerHeader(
    title: String,
    subtitle: String,
    chunkProgress: String,
    isBlindMode: Boolean,
    onBlindModeToggle: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Minimize",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.ifEmpty { "No content" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle.ifEmpty { "Select something to play" },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
        }

        Text(
            text = chunkProgress,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(end = 8.dp)
        )

        IconButton(onClick = onBlindModeToggle) {
            Icon(
                imageVector = if (isBlindMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Toggle blind mode",
                tint = if (isBlindMode) ChunkHighlight else Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ChunkItem(
    chunk: Chunk,
    isCurrent: Boolean,
    isBlindMode: Boolean,
    isRevealed: Boolean,
    hasRecording: Boolean,
    onTap: () -> Unit,
    onReveal: () -> Unit,
    onPlayRecording: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCurrent -> ChunkHighlightAlpha
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isCurrent -> ChunkHighlight
            else -> Color.White.copy(alpha = 0.1f)
        },
        animationSpec = tween(200),
        label = "border"
    )

    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = borderColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isBlindMode && !isRevealed) {
                    // Show time range only
                    Text(
                        text = formatTimeRange(chunk.startMs, chunk.endMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                } else {
                    Text(
                        text = chunk.displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (hasRecording) {
                    IconButton(
                        onClick = onPlayRecording,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Play recording",
                            tint = ChunkHighlight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isBlindMode && !isRevealed) {
                    IconButton(
                        onClick = onReveal,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "Reveal",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningStateIndicator(
    state: LearningState,
    modifier: Modifier = Modifier
) {
    val text = when (state) {
        is LearningState.Idle -> "Ready"
        is LearningState.Playing, is LearningState.PlayingFirst -> "Playing"
        is LearningState.Paused -> "Paused"
        is LearningState.Gap -> "Gap"
        is LearningState.Recording, is LearningState.GapWithRecording -> "Recording"
        is LearningState.PlayingSecond -> "Playing (2nd)"
        is LearningState.PlaybackRecording -> "Your Recording"
    }

    val color = when (state) {
        is LearningState.Recording, is LearningState.GapWithRecording -> Error
        is LearningState.Playing, is LearningState.PlayingFirst, is LearningState.PlayingSecond -> ChunkHighlight
        else -> Color.White.copy(alpha = 0.5f)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state is LearningState.Recording || state is LearningState.GapWithRecording) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    hasPlaylist: Boolean,
    hasPreviousItem: Boolean = false,
    hasNextItem: Boolean = false,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPreviousItem: () -> Unit,
    onNextItem: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousItem,
            enabled = hasPlaylist && hasPreviousItem
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous item",
                tint = if (hasPlaylist && hasPreviousItem) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Default.FastRewind,
                contentDescription = "Previous chunk",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ChunkHighlight
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        IconButton(onClick = onNext) {
            Icon(
                Icons.Default.FastForward,
                contentDescription = "Next chunk",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(
            onClick = onNextItem,
            enabled = hasPlaylist && hasNextItem
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next item",
                tint = if (hasPlaylist && hasNextItem) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun LearningSettingsBar(
    settings: LearningSettings,
    onRepeatChange: (Int) -> Unit,
    onGapRatioChange: (Float) -> Unit,
    onRecordingToggle: () -> Unit,
    onHardModeToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SettingButton(
            icon = Icons.Default.Repeat,
            label = "x${settings.repeatCount}",
            onClick = {
                val next = if (settings.repeatCount >= 5) 1 else settings.repeatCount + 1
                onRepeatChange(next)
            }
        )

        SettingButton(
            icon = Icons.Default.Timer,
            label = "${(settings.gapRatio * 100).toInt()}%",
            onClick = {
                val ratios = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
                val currentIndex = ratios.indexOf(settings.gapRatio)
                val nextIndex = (currentIndex + 1) % ratios.size
                onGapRatioChange(ratios[nextIndex])
            }
        )

        SettingButton(
            icon = Icons.Default.Mic,
            label = if (settings.isRecordingEnabled) "On" else "Off",
            isActive = settings.isRecordingEnabled,
            isEnabled = !settings.isHardMode,
            onClick = onRecordingToggle
        )

        SettingButton(
            icon = Icons.Default.Speed,
            label = if (settings.isHardMode) "Hard" else "Normal",
            isActive = settings.isHardMode,
            onClick = onHardModeToggle
        )
    }
}

@Composable
private fun SettingButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val color = when {
        !isEnabled -> Color.White.copy(alpha = 0.3f)
        isActive -> ChunkHighlight
        else -> Color.White.copy(alpha = 0.7f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private fun formatTimeRange(startMs: Long, endMs: Long): String {
    fun format(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
    return "${format(startMs)} ~ ${format(endMs)}"
}

@Preview(showBackground = true)
@Composable
private fun FullScreenPlayerPreview() {
    ListenerTheme(darkTheme = true) {
        // Preview without ViewModel
    }
}
