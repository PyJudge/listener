package com.listener.presentation.player

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.listener.domain.model.Chunk
import com.listener.domain.model.LearningSettings
import com.listener.domain.model.LearningState
import com.listener.domain.model.PlayMode
import com.listener.presentation.theme.ErrorRed
import com.listener.presentation.theme.ListenerTheme
import com.listener.presentation.theme.PlayerBackground
import com.listener.presentation.theme.PlayerTypography
import com.listener.presentation.theme.PurpleAlpha
import com.listener.presentation.theme.PurplePrimary
import com.listener.presentation.theme.SurfaceContainer
import com.listener.presentation.theme.SurfaceDark
import com.listener.presentation.theme.SurfaceElevated
import com.listener.presentation.theme.TextMuted
import com.listener.presentation.theme.TextPrimary
import com.listener.presentation.theme.TextSecondary
import kotlin.math.roundToInt

// 색상 별칭 (테마 색상 사용)
private val AccentColor = PurplePrimary
private val AccentColorDim = PurpleAlpha
private val SurfaceLight = SurfaceElevated
private val SurfaceDarkBg = SurfaceDark

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

    // BUG-C3 Fix: Context for Toast
    val context = LocalContext.current

    LaunchedEffect(sourceId) {
        if (sourceId.isNotEmpty()) {
            viewModel.loadBySourceId(sourceId)
        }
    }

    // Permission launcher for RECORD_AUDIO
    // BUG-C3 Fix: 권한 거부 시 사용자에게 피드백 제공
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onRecordPermissionGranted()
        } else {
            // BUG-C3 Fix: 권한 거부 시 토스트 표시하고 LR 모드 유지
            Toast.makeText(
                context,
                "Recording permission denied. Staying in L/R mode.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var isBlindMode by remember { mutableStateOf(false) }
    var peekingChunkIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    // A4: 오디오 에러 발생 시 토스트 표시
    LaunchedEffect(playbackState.error) {
        playbackState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    // Auto-scroll to current chunk (청크 변경, 일시정지, 또는 숨김 모드 전환 시)
    LaunchedEffect(playbackState.currentChunkIndex, playbackState.isPlaying, isBlindMode) {
        if (chunks.isNotEmpty() && playbackState.currentChunkIndex >= 0) {
            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val targetOffset = -(viewportHeight / 3)
            listState.animateScrollToItem(
                index = playbackState.currentChunkIndex,
                scrollOffset = targetOffset
            )
        }
    }

    LaunchedEffect(isBlindMode, playbackState.currentChunkIndex) {
        peekingChunkIndex = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDarkBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            ModernHeader(
                title = playbackState.title,
                subtitle = playbackState.subtitle,
                currentChunk = playbackState.currentChunkIndex + 1,
                totalChunks = playbackState.totalChunks,
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
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    when {
                                        totalDrag < -80 -> viewModel.nextChunk()
                                        totalDrag > 80 -> viewModel.previousChunk()
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                }
                            )
                        }
                ) {
                    itemsIndexed(
                        items = chunks,
                        key = { _, chunk -> chunk.orderIndex }
                    ) { index, chunk ->
                        ModernChunkItem(
                            chunk = chunk,
                            index = index + 1,
                            isCurrent = index == playbackState.currentChunkIndex,
                            isBlindMode = isBlindMode,
                            isPeeking = peekingChunkIndex == index,
                            hasRecording = viewModel.hasRecording(index),
                            onTap = { viewModel.seekToChunk(index) },
                            onPeekStart = { peekingChunkIndex = index },
                            onPeekEnd = { peekingChunkIndex = null },
                            onPlayRecording = { viewModel.playRecording(index) }
                        )
                    }
                }

                // Gradient overlays
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(SurfaceDarkBg, Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, SurfaceDarkBg)
                            )
                        )
                )
            }

            // Modern Seek Bar
            ModernSeekBar(
                currentIndex = playbackState.currentChunkIndex,
                totalCount = playbackState.totalChunks,
                onSeek = { viewModel.seekToChunk(it) },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // State indicator
            ModernStateIndicator(
                state = playbackState.learningState,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Controls
            ModernControls(
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
            ModernSettingsBar(
                settings = playbackState.settings,
                onRepeatChange = { viewModel.setRepeatCount(it) },
                onGapRatioChange = { viewModel.setGapRatio(it) },
                onPlayModeToggle = {
                    val nextMode = viewModel.getNextPlayMode()
                    if (nextMode == PlayMode.LRLR && !viewModel.hasRecordPermission()) {
                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.togglePlayMode()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModernHeader(
    title: String,
    subtitle: String,
    currentChunk: Int,
    totalChunks: Int,
    isBlindMode: Boolean,
    onBlindModeToggle: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Close",
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Chunk counter pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceLight)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$currentChunk / $totalChunks",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onBlindModeToggle) {
                Icon(
                    imageVector = if (isBlindMode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = "Toggle blind mode",
                    tint = if (isBlindMode) AccentColor else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = title.ifEmpty { "No content" },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Subtitle
        Text(
            text = subtitle.ifEmpty { "Select something to play" },
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ModernChunkItem(
    chunk: Chunk,
    index: Int,
    isCurrent: Boolean,
    isBlindMode: Boolean,
    isPeeking: Boolean,
    hasRecording: Boolean,
    onTap: () -> Unit,
    onPeekStart: () -> Unit,
    onPeekEnd: () -> Unit,
    onPlayRecording: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.97f,
        animationSpec = tween(200),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.6f,
        animationSpec = tween(200),
        label = "alpha"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isCurrent) SurfaceLight else Color.Transparent,
        animationSpec = tween(200),
        label = "bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(alpha)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onTap)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Index indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) AccentColor else TextMuted.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) Color.White else TextMuted
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                if (isBlindMode && !isPeeking) {
                    Text(
                        text = formatTimeRange(chunk.startMs, chunk.endMs),
                        fontSize = 14.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = chunk.displayText,
                        fontSize = 16.sp,
                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                        color = if (isCurrent) TextPrimary else TextSecondary,
                        lineHeight = 24.sp
                    )
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (hasRecording) {
                    IconButton(
                        onClick = onPlayRecording,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Play recording",
                            tint = AccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isBlindMode) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onPeekStart()
                                        tryAwaitRelease()
                                        onPeekEnd()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Visibility,
                            contentDescription = "Peek",
                            tint = if (isPeeking) AccentColor else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSeekBar(
    currentIndex: Int,
    totalCount: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalCount <= 0) return

    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var trackWidth by remember { mutableFloatStateOf(0f) }

    val progress = if (isDragging) {
        dragProgress
    } else {
        if (totalCount > 1) currentIndex.toFloat() / (totalCount - 1) else 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(if (isDragging) 0 else 200),
        label = "progress"
    )

    Column(modifier = modifier) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .onSizeChanged { trackWidth = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / trackWidth).coerceIn(0f, 1f)
                        val targetIndex = (newProgress * (totalCount - 1)).roundToInt()
                        onSeek(targetIndex)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / trackWidth).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            dragProgress = (change.position.x / trackWidth).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val targetIndex = (dragProgress * (totalCount - 1)).roundToInt()
                            onSeek(targetIndex)
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextMuted.copy(alpha = 0.2f))
            )

            // Progress track
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(AccentColor.copy(alpha = 0.7f), AccentColor)
                        )
                    )
                    .align(Alignment.CenterStart)
            )

            // Thumb
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((trackWidth * animatedProgress) - with(density) { 8.dp.toPx() }).toInt(),
                            y = 0
                        )
                    }
                    .size(16.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(AccentColor)
                    .align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
private fun ModernStateIndicator(
    state: LearningState,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (state) {
        is LearningState.Idle -> "Ready" to TextMuted
        is LearningState.Playing, is LearningState.PlayingFirst -> "Playing" to AccentColor
        is LearningState.Paused -> "Paused" to TextSecondary
        is LearningState.Gap -> "Gap" to TextSecondary
        is LearningState.Recording, is LearningState.GapWithRecording -> "Recording" to ErrorRed
        is LearningState.PlayingSecond -> "Replay" to AccentColor
        is LearningState.PlaybackRecording -> "Your Voice" to AccentColor
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
                    .background(ErrorRed.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ModernControls(
    isPlaying: Boolean,
    hasPlaylist: Boolean,
    hasPreviousItem: Boolean,
    hasNextItem: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPreviousItem: () -> Unit,
    onNextItem: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous item
        IconButton(
            onClick = onPreviousItem,
            enabled = hasPlaylist && hasPreviousItem,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Previous item",
                tint = if (hasPlaylist && hasPreviousItem) TextSecondary else TextMuted,
                modifier = Modifier.size(28.dp)
            )
        }

        // Previous chunk
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Rounded.FastRewind,
                contentDescription = "Previous chunk",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        // Play/Pause
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(AccentColor)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        // Next chunk
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Rounded.FastForward,
                contentDescription = "Next chunk",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        // Next item
        IconButton(
            onClick = onNextItem,
            enabled = hasPlaylist && hasNextItem,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Next item",
                tint = if (hasPlaylist && hasNextItem) TextSecondary else TextMuted,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ModernSettingsBar(
    settings: LearningSettings,
    onRepeatChange: (Int) -> Unit,
    onGapRatioChange: (Float) -> Unit,
    onPlayModeToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 1. 모드 버튼 (Normal / L/R / L/R/LR)
        ModernSettingChip(
            icon = Icons.Rounded.Speed,
            label = when (settings.playMode) {
                PlayMode.NORMAL -> "Normal"
                PlayMode.LR -> "L/R"
                PlayMode.LRLR -> "L/R/LR"
            },
            isActive = settings.playMode != PlayMode.NORMAL,
            onClick = onPlayModeToggle
        )

        // 2. 반복 횟수 (항상 활성화)
        ModernSettingChip(
            icon = Icons.Rounded.Repeat,
            label = "x${settings.repeatCount}",
            onClick = {
                val next = if (settings.repeatCount >= 5) 1 else settings.repeatCount + 1
                onRepeatChange(next)
            }
        )

        // 3. 공백 비율 (Normal일 때 비활성화)
        ModernSettingChip(
            icon = Icons.Rounded.Timer,
            label = "+${(settings.gapRatio * 100).toInt()}%",
            isEnabled = !settings.isNormalMode,
            onClick = {
                val ratios = listOf(0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
                val currentIndex = ratios.indexOf(settings.gapRatio)
                val nextIndex = (currentIndex + 1) % ratios.size
                onGapRatioChange(ratios[nextIndex])
            }
        )
    }
}

@Composable
private fun ModernSettingChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val chipColor = when {
        !isEnabled -> TextMuted.copy(alpha = 0.3f)
        isActive -> AccentColor
        else -> TextSecondary
    }

    val bgColor = when {
        isActive -> AccentColorDim
        else -> SurfaceLight
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = chipColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = chipColor
            )
        }
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
