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
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.util.AppleMusicLyricsParser
import chromahub.rhythm.app.util.WordByWordLyricLine
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Represents either a lyrics line or a gap indicator
 */
sealed class LyricsItem {
    data class LyricLine(val line: WordByWordLyricLine, val index: Int) : LyricsItem()
    data class Gap(val duration: Long, val startTime: Long) : LyricsItem()
}

/**
 * Animation presets for word-by-word highlighting
 * TODO: Implement different animation styles for word transitions
 */
enum class WordAnimationPreset {
    DEFAULT,      // Standard fade and scale
    BOUNCE,       // Bouncy spring animation (TODO: implement)
    SLIDE,        // Slide-in from sides (TODO: implement)
    GLOW,         // Glowing highlight effect (TODO: implement)
    KARAOKE,      // Filling bar effect (TODO: implement)
    MINIMAL       // Subtle color change only (TODO: implement)
}

/**
 * Composable for displaying word-by-word synchronized lyrics from Apple Music
 * TODO: Add animation preset system for different word highlighting styles
 */
@Composable
fun WordByWordLyricsView(
    wordByWordLyrics: String,
    currentPlaybackTime: Long,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onSeek: ((Long) -> Unit)? = null,
    syncOffset: Long = 0L, // TODO: Add UI controls for adjusting sync offset in real-time
    animationPreset: WordAnimationPreset = WordAnimationPreset.DEFAULT // TODO: Implement animation presets
) {
    val context = LocalContext.current
    // TODO: Apply syncOffset to all timestamp comparisons for manual sync adjustment
    val adjustedPlaybackTime = currentPlaybackTime + syncOffset
    
    val parsedLyrics = remember(wordByWordLyrics) {
        AppleMusicLyricsParser.parseWordByWordLyrics(wordByWordLyrics)
    }

    // Create items list with gaps for instrumental sections
    val lyricsItems = remember(parsedLyrics) {
        val items = mutableListOf<LyricsItem>()
        parsedLyrics.forEachIndexed { index, line ->
            items.add(LyricsItem.LyricLine(line, index))
            
            // Check for gap to next line
            if (index < parsedLyrics.size - 1) {
                val nextLine = parsedLyrics[index + 1]
                val gapDuration = nextLine.lineTimestamp - line.lineEndtime
                if (gapDuration > 3000) { // 3 seconds threshold
                    items.add(LyricsItem.Gap(gapDuration, line.lineEndtime))
                }
            }
        }
        items
    }

    val coroutineScope = rememberCoroutineScope()
    
    // Find current line index (among lyric lines only) - using adjustedPlaybackTime for sync offset
    val currentLineIndex by remember(adjustedPlaybackTime, parsedLyrics) {
        derivedStateOf {
            parsedLyrics.indexOfLast { line ->
                adjustedPlaybackTime >= line.lineTimestamp && adjustedPlaybackTime <= line.lineEndtime
            }
        }
    }

    // Find current item index (including gaps) - using adjustedPlaybackTime for sync offset
    val currentItemIndex by remember(adjustedPlaybackTime, lyricsItems) {
        derivedStateOf {
            lyricsItems.indexOfFirst { item ->
                when (item) {
                    is LyricsItem.LyricLine -> 
                        adjustedPlaybackTime >= item.line.lineTimestamp && adjustedPlaybackTime <= item.line.lineEndtime
                    is LyricsItem.Gap -> 
                        adjustedPlaybackTime >= item.startTime && adjustedPlaybackTime < item.startTime + item.duration
                }
            }.takeIf { it >= 0 } ?: 0
        }
    }

    // Auto-scroll to current lyric line with elastic spring animation
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && parsedLyrics.isNotEmpty()) {
            // Find the corresponding item index in lyricsItems
            val targetItemIndex = lyricsItems.indexOfFirst { item ->
                item is LyricsItem.LyricLine && item.index == currentLineIndex
            }

            if (targetItemIndex >= 0) {
                val offset = listState.layoutInfo.viewportSize.height / 3

                coroutineScope.launch {
                    // Add staggering delay based on line position for elastic effect
                    val delayMs = when {
                        currentLineIndex == 0 -> 0L
                        currentLineIndex < 3 -> 50L
                        else -> 100L + (currentLineIndex * 20L).coerceAtMost(300L)
                    }

                    if (delayMs > 0) {
                        delay(delayMs)
                    }

                    // Use elastic spring animation for smooth, bouncy scrolling
                    listState.animateScrollToItem(
                        index = targetItemIndex,
                        scrollOffset = -offset
                    )
                }
            }
        }
    }

    if (parsedLyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = context.getString(R.string.word_by_word_unavailable),
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
            itemsIndexed(lyricsItems) { itemIndex, item ->
                when (item) {
                    is LyricsItem.LyricLine -> {
                        val line = item.line
                        val index = item.index
                        val isCurrentLine = currentLineIndex == index
                        val isUpcomingLine = index > currentLineIndex
                        val linesAhead = index - currentLineIndex
                        
                        // Animated scale for current line with elastic spring
                        val scale by animateFloatAsState(
                            targetValue = when {
                                isCurrentLine -> 1.08f
                                isUpcomingLine && linesAhead == 1 -> 1.02f
                                else -> 1f
                            },
                            animationSpec = spring<Float>(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessVeryLow
                            ),
                            label = "lineScale"
                        )

                        // Staggered opacity animation for upcoming lines with elastic effect
                        val opacity by animateFloatAsState(
                            targetValue = when {
                                isCurrentLine -> 1f
                                isUpcomingLine && linesAhead <= 4 -> 0.9f - (linesAhead * 0.1f)
                                else -> 0.3f
                            },
                            animationSpec = if (isUpcomingLine) {
                                spring<Float>(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessVeryLow,
                                    visibilityThreshold = 0.01f
                                )
                            } else {
                                spring<Float>(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            },
                            label = "lineOpacity"
                        )

                        // Staggered translation animation for upcoming lines with elastic bounce
                        val animatedTranslationY by animateFloatAsState(
                            targetValue = when {
                                isUpcomingLine && linesAhead <= 3 -> (linesAhead * 6f)
                                else -> 0f
                            },
                            animationSpec = spring<Float>(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessVeryLow
                            ),
                            label = "lineTranslation"
                        )

                        // Subtle rotation animation for elastic effect
                        val rotationZ by animateFloatAsState(
                            targetValue = if (isCurrentLine) 0.5f else 0f,
                            animationSpec = spring<Float>(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessVeryLow
                            ),
                            label = "lineRotation"
                        )

                        // Calculate distance-based alpha for better readability
                        val distanceFromCurrent = abs(index - currentLineIndex)
                        
                        // Build annotated string with word-level highlighting (using adjustedPlaybackTime)
                        val annotatedText = buildAnnotatedString {
                            line.words.forEachIndexed { wordIndex, word ->
                                // TODO: Apply animation preset here based on animationPreset parameter
                                val isWordActive = isCurrentLine && 
                                    adjustedPlaybackTime >= word.timestamp && 
                                    adjustedPlaybackTime <= word.endtime
                                
                                // Improved alpha values based on distance for better readability
                                val wordAlpha = when {
                                    isWordActive -> 1f
                                    isCurrentLine -> 0.95f // Active line words that haven't been sung yet
                                    distanceFromCurrent == 1 -> 0.75f // Next/previous line
                                    distanceFromCurrent == 2 -> 0.60f
                                    distanceFromCurrent == 3 -> 0.45f
                                    else -> 0.32f // Far away lines
                                }
                                
                                // Apply different colors based on voice tag
                                val baseColor = when (line.voiceTag) {
                                    "v2" -> MaterialTheme.colorScheme.secondary
                                    "v3" -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary // Default/v1
                                }
                                
                                val wordColor = if (isWordActive) {
                                    baseColor // Active word gets voice-specific color
                                } else if (isCurrentLine) {
                                    // Inactive words in current line - use voice color but slightly dimmed
                                    when (line.voiceTag) {
                                        "v2" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                        "v3" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = wordAlpha)
                                }
                                
                                withStyle(
                                    SpanStyle(
                                        color = wordColor,
                                        fontWeight = if (isWordActive) FontWeight.Bold else 
                                            if (isCurrentLine) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                ) {
                                    // Add space before word if it's not a syllable part
                                    if (wordIndex > 0 && !word.isPart) {
                                        append(" ")
                                    }
                                    append(word.text)
                                }
                            }
                        }
                        
                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * 1.4f
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSeek?.invoke(line.lineTimestamp)
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = opacity
                                    translationY = animatedTranslationY
//                                    rotationZ = rotationZ
                                }
                        )
                    }
                    is LyricsItem.Gap -> {
                        // Visual indicator for instrumental gap
                        val isCurrentGap = currentPlaybackTime >= item.startTime && 
                            currentPlaybackTime < item.startTime + item.duration
                        
                        val gapHeight = (item.duration / 1000f).coerceIn(20f, 80f) // 20-80dp based on duration
                        
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(gapHeight.dp)
                                .padding(horizontal = 32.dp)
                        )
                        
                        // Musical note icon or wave indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val iconScale by animateFloatAsState(
                                targetValue = if (isCurrentGap) 1.5f else 1f,
                                animationSpec = spring<Float>(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessVeryLow
                                ),
                                label = "iconScale"
                            )
                            
                            Text(
                                text = "♪",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (isCurrentGap) 0.8f else 0.3f
                                ),
                                modifier = Modifier.graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                            )
                        }
                        
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(gapHeight.dp)
                                .padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        }
    }
}
