package chromahub.rhythm.app.features.local.presentation.components.lyrics

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.util.LyricsParser
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SyncedLyricsView(
    lyrics: String,
    currentPlaybackTime: Long,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onSeek: ((Long) -> Unit)? = null,
    syncOffset: Long = 0L // TODO: Add UI controls for adjusting sync offset in real-time
) {
    val context = LocalContext.current
    // TODO: Apply syncOffset to all timestamp comparisons for manual sync adjustment
    val adjustedPlaybackTime = currentPlaybackTime + syncOffset
    
    val parsedLyrics = remember(lyrics) {
        LyricsParser.parseLyrics(lyrics)
    }

    val coroutineScope = rememberCoroutineScope()
    
    // Track previous line for smooth transitions
    val previousLineIndex = remember { mutableIntStateOf(-1) }
    
    // Find current line index more efficiently (using adjustedPlaybackTime for sync offset)
    val currentLineIndex by remember(adjustedPlaybackTime, parsedLyrics) {
        derivedStateOf {
            parsedLyrics.indexOfLast { it.timestamp <= adjustedPlaybackTime }
        }
    }

    // Enhanced auto-scroll with spring animation
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && parsedLyrics.isNotEmpty() && currentLineIndex != previousLineIndex.intValue) {
            previousLineIndex.intValue = currentLineIndex
            val offset = listState.layoutInfo.viewportSize.height / 3
            coroutineScope.launch {
                listState.animateScrollToItem(currentLineIndex, scrollOffset = -offset)
            }
        }
    }

    if (parsedLyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = context.getString(R.string.synced_lyrics_unavailable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 30.dp)
        ) {
            itemsIndexed(parsedLyrics) { index, line ->
                SyncedLyricItem(
                    line = line,
                    index = index,
                    currentLineIndex = currentLineIndex,
                    currentPlaybackTime = adjustedPlaybackTime, // Use adjusted time for progress calculation
                    parsedLyrics = parsedLyrics,
                    onSeek = onSeek
                )
            }
        }
    }
}

/**
 * Individual synced lyric line with enhanced animations
 */
@Composable
private fun SyncedLyricItem(
    line: chromahub.rhythm.app.util.LyricLine,
    index: Int,
    currentLineIndex: Int,
    currentPlaybackTime: Long,
    parsedLyrics: List<chromahub.rhythm.app.util.LyricLine>,
    onSeek: ((Long) -> Unit)?
) {
    val isCurrentLine = currentLineIndex == index
    val isPreviousLine = currentLineIndex == index + 1
    val isNextLine = currentLineIndex == index - 1
    
    // Distance-based effects
    val distanceFromCurrent = abs(index - currentLineIndex)
    
    // Calculate progress through current line
    val progressToNextLine = if (isCurrentLine && index + 1 < parsedLyrics.size) {
        val nextLineTimestamp = parsedLyrics[index + 1].timestamp
        val timeDiff = nextLineTimestamp - line.timestamp
        if (timeDiff > 0) {
            ((currentPlaybackTime - line.timestamp).toFloat() / timeDiff).coerceIn(0f, 1f)
        } else 0f
    } else 0f
    
    // Smooth scale animation with spring physics - Apple Music style
    val scale by animateFloatAsState(
        targetValue = when {
            isCurrentLine -> 1.10f
            isNextLine -> 1.03f + (0.07f * progressToNextLine)
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "lineScale_$index"
    )
    
    // Enhanced alpha with distance-based gradual fade
    val alpha by animateFloatAsState(
        targetValue = when {
            isCurrentLine -> 1f
            distanceFromCurrent == 1 -> 0.75f
            distanceFromCurrent == 2 -> 0.55f
            distanceFromCurrent == 3 -> 0.40f
            distanceFromCurrent == 4 -> 0.30f
            else -> 0.22f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lineAlpha_$index"
    )
    
    // Vertical translation for flowing effect
    val verticalTranslation by animateFloatAsState(
        targetValue = if (isCurrentLine) 0f else if (isPreviousLine) -8f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lineTranslationY_$index"
    )
    
    // Color transition for active line with voice-specific colors
    val textColor = when {
        isCurrentLine -> {
            // Apply different colors based on voice tag
            when (line.voiceTag) {
                "v2" -> MaterialTheme.colorScheme.secondary // Different color for second voice
                "v3" -> MaterialTheme.colorScheme.tertiary  // Third voice
                else -> MaterialTheme.colorScheme.primary   // Default/v1
            }
        }
        else -> {
            // Inactive lines also get subtle voice coloring (alpha applied via modifier)
            when (line.voiceTag) {
                "v2" -> MaterialTheme.colorScheme.secondary
                "v3" -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurface
            }
        }
    }
    
    // Dynamic font weight based on position
    val fontWeight = when {
        isCurrentLine -> FontWeight.ExtraBold
        distanceFromCurrent <= 1 -> FontWeight.SemiBold
        distanceFromCurrent <= 2 -> FontWeight.Medium
        else -> FontWeight.Normal
    }
    
    // Subtle letter spacing for emphasis
    val letterSpacing = if (isCurrentLine) 0.05.sp else 0.sp

    Text(
        text = line.text,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = fontWeight,
            lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * 1.5f,
            letterSpacing = letterSpacing
        ),
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSeek?.invoke(line.timestamp)
            }
            .padding(vertical = 14.dp, horizontal = 20.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = verticalTranslation
            }
            .alpha(alpha)
    )
}
