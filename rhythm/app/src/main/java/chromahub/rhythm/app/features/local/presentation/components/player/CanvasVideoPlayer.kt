package chromahub.rhythm.app.features.local.presentation.components.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.common.M3CircularLoader
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * A composable that displays a looping video player for Spotify Canvas videos with enhanced performance
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
    showGradientOverlay: Boolean = true,
    onVideoReady: (() -> Unit)? = null,
    onVideoError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
    var isVideoLoaded by remember(videoUrl) { mutableStateOf(false) }
    var hasError by remember(videoUrl) { mutableStateOf(false) }
    var retryCount by remember(videoUrl) { mutableStateOf(0) }
    
    // Initialize ExoPlayer with enhanced error handling
    LaunchedEffect(videoUrl, retryCount) {
        Log.d("CanvasVideoPlayer", "Initializing video player for: $videoUrl (retry: $retryCount)")
        isVideoLoaded = false
        hasError = false
        
        // Release previous player if exists
        exoPlayer?.release()
        exoPlayer = null
        
        try {
            val player = ExoPlayer.Builder(context)
                .build()
                .apply {
                    val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                    setMediaItem(mediaItem)
                    prepare()
                    repeatMode = Player.REPEAT_MODE_ONE // Loop the video
                    volume = 0f // Canvas videos are silent
                    
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    Log.d("CanvasVideoPlayer", "Video ready")
                                    isVideoLoaded = true
                                    hasError = false
                                    onVideoReady?.invoke()
                                }
                                Player.STATE_ENDED -> {
                                    Log.d("CanvasVideoPlayer", "Video ended - will loop")
                                }
                                Player.STATE_BUFFERING -> {
                                    Log.d("CanvasVideoPlayer", "Video buffering...")
                                }
                                Player.STATE_IDLE -> {
                                    Log.d("CanvasVideoPlayer", "Video player idle")
                                }
                            }
                        }
                        
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Log.e("CanvasVideoPlayer", "Video playback error: ${error.message}")
                            hasError = true
                            isVideoLoaded = false
                            
                            // Retry logic for network errors
                            if (retryCount < 2 && (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)) {
                                retryCount++
                                Log.d("CanvasVideoPlayer", "Retrying video load (attempt $retryCount)")
                                // Retry will be triggered by the LaunchedEffect dependency on retryCount
                            } else {
                                onVideoError?.invoke()
                            }
                        }
                    })
                }
            
            exoPlayer = player
        } catch (e: Exception) {
            Log.e("CanvasVideoPlayer", "Failed to initialize video player: ${e.message}")
            hasError = true
            onVideoError?.invoke()
        }
    }

    // Control playback based on isPlaying state with improved synchronization
    LaunchedEffect(isPlaying, exoPlayer, isVideoLoaded) {
        exoPlayer?.let { player ->
            // Always keep video playing when loaded, regardless of audio state
            if (isVideoLoaded) {
                Log.d("CanvasVideoPlayer", "Keeping video playing (canvas independent of audio)")
                if (!player.isPlaying) {
                    player.play()
                }
            }
        }
    }
    
    // Auto-start video when loaded
    LaunchedEffect(isVideoLoaded) {
        if (isVideoLoaded) {
            Log.d("CanvasVideoPlayer", "Auto-starting canvas video (independent mode)")
            exoPlayer?.play()
        
            // Delay to avoid UI flicker
            delay(0)

        }
    }

    // Cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            Log.d("CanvasVideoPlayer", "Disposing video player")
            exoPlayer?.release()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Error state
        if (hasError && retryCount >= 2) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (hasError) 1f else 0f,
                            animationSpec = tween(400),
                            label = "error_alpha"
                        ).value
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Canvas unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
                                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.0f),
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.3f),
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.7f),
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.4f)
                                )
                            )
                        )
                )
            }
        }
        
        // Loading state with lyrics-view-style overlay (show during loading or retry, but not when falling back)
        if ((!isVideoLoaded && !hasError) || (hasError && retryCount < 2)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(BottomSheetDefaults.ContainerColor.copy(alpha = 0.1f))
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (isVideoLoaded && !hasError) 0f else 1f,
                            animationSpec = tween(600),
                            label = "loading_alpha"
                        ).value
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Gradient overlays like lyrics view
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.0f),
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.3f),
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.7f),
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.4f)
                                )
                            )
                        )
                )
                
                // Centered loading content like lyrics view
                // Column(
                //     horizontalAlignment = Alignment.CenterHorizontally,
                //     modifier = Modifier
                //         .fillMaxWidth()
                //         .padding(vertical = 32.dp)
                // ) {
                //     chromahub.rhythm.app.ui.components.M3CircularLoader(
                //         modifier = Modifier.size(56.dp),
                //         fourColor = true,
                //         isExpressive = true
                //     )
                //     Spacer(modifier = Modifier.height(16.dp))
                //     Text(
                //         text = if (retryCount > 0) "Retrying canvas..." else "Loading canvas...",
                //         style = MaterialTheme.typography.bodyMedium,
                //         color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                //         textAlign = TextAlign.Center
                //     )
                // }
            }
        }
        
        // Video player with smooth entrance animation
        if (isVideoLoaded && exoPlayer != null && !hasError) {
            val videoAlpha by animateFloatAsState(
                targetValue = if (isVideoLoaded && !hasError) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 800,
                    delayMillis = 200,
                    easing = FastOutSlowInEasing
                ),
                label = "video_alpha"
            )
            val videoScale by animateFloatAsState(
                targetValue = if (isVideoLoaded && !hasError) 1f else 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "video_scale"
            )

            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false // Disable all player controls
                        hideController()      // Explicitly hide controller
                        controllerShowTimeoutMs = 0 // Never show controller
                        controllerHideOnTouch = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER) // Hide buffering indicator
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color.Transparent)
                    .alpha(videoAlpha)
                    .scale(videoScale)
            )

            // Add gradient overlays on top of the video player (controlled by setting)
            if (showGradientOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(cornerRadius))
                        .alpha(videoAlpha)
                        .scale(videoScale)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.6f),
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.9f),
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 1.0f)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(cornerRadius))
                        .alpha(videoAlpha)
                        .scale(videoScale)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.2f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.2f)
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * Simplified canvas player that handles common use cases with enhanced animations, preloader, and fallback support
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasPlayer(
    videoUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
    albumArtUrl: Any? = null,
    albumName: String? = null,
    showGradientOverlay: Boolean = true,
    onCanvasLoaded: (() -> Unit)? = null,
    onCanvasFailed: (() -> Unit)? = null,
    onRetryRequested: (() -> Unit)? = null,
    fallbackContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    var isVisible by remember(videoUrl) { mutableStateOf(false) }
    var showFallback by remember(videoUrl) { mutableStateOf(false) }
    var isLoading by remember(videoUrl) { mutableStateOf(false) }
    var canRetry by remember(videoUrl) { mutableStateOf(false) }
    var showAlbumArt by remember(videoUrl) { mutableStateOf(true) }
    var isCanvasReady by remember(videoUrl) { mutableStateOf(false) }
    // Handle album art to canvas transition with delayed loading state exit
    LaunchedEffect(isCanvasReady) {
        if (isCanvasReady) {
            delay(2000) // Delay exit of loading state for 500ms after canvas is ready
            showAlbumArt = false // Then hide album art for smooth transition
        }
    }
    
    LaunchedEffect(videoUrl) {
        isVisible = !videoUrl.isNullOrBlank()
        showFallback = false
        isLoading = !videoUrl.isNullOrBlank()
        canRetry = false
        showAlbumArt = true
        isCanvasReady = false
    }
    
    // Show fallback content when canvas fails and fallback is provided
    if (showFallback && fallbackContent != null) {
        AnimatedVisibility(
            visible = showFallback,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                )
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutLinearInEasing
                )
            )
        ) {
            fallbackContent()
            // Add gradient overlays to fallback content for consistency (controlled by setting)
            if (showGradientOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.6f),
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.9f),
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 1.0f)
                                )
                            )
                        )
                )
                
                // Horizontal gradient for more depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.2f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    BottomSheetDefaults.ContainerColor.copy(alpha = 0.2f)
                                )
                            )
                        )
                )
            }
        }
        return
    }
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Album art background - always shown first, then transitions out when canvas loads
        AnimatedVisibility(
            visible = showAlbumArt && (albumArtUrl != null || fallbackContent != null),
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutLinearInEasing
                )
            ) + scaleOut(
                targetScale = 1.05f,
                animationSpec = tween(800)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                if (albumArtUrl != null) {
                    // Show album art with loading state and gradients
                    Box(modifier = Modifier.fillMaxSize()) {
                        chromahub.rhythm.app.util.M3ImageUtils.AlbumArt(
                            imageUrl = albumArtUrl,
                            albumName = albumName,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(cornerRadius)
                        )
                        
                        // Add gradient overlays to album art for consistency with canvas (controlled by setting)
                        if (showGradientOverlay) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                BottomSheetDefaults.ContainerColor.copy(alpha = 0.6f),
                                                BottomSheetDefaults.ContainerColor.copy(alpha = 0.9f),
                                                BottomSheetDefaults.ContainerColor.copy(alpha = 1.0f)
                                            )
                                        )
                                    )
                            )
                            
                            // Horizontal gradient for more depth
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                BottomSheetDefaults.ContainerColor.copy(alpha = 0.2f),
                                                Color.Transparent,
                                                Color.Transparent,
                                                BottomSheetDefaults.ContainerColor.copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                                
                            )
                        }
                    }
                } else if (fallbackContent != null) {
                    fallbackContent()
                } else {
                    // Default placeholder with Rhythm logo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        BottomSheetDefaults.ContainerColor.copy(alpha = 0.3f),
                                        BottomSheetDefaults.ContainerColor
                                    ),
                                    radius = 400f
                                ),
                                RoundedCornerShape(cornerRadius)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = chromahub.rhythm.app.R.drawable.rhythm_logo),
                            contentDescription = "Album artwork",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(128.dp)
                        )
                    }
                }

                // Gradient overlay and loading indicator while canvas is being fetched
                // Only show when loading and canvas is not ready yet
                AnimatedVisibility(
                    visible = isLoading && !videoUrl.isNullOrBlank() && !isCanvasReady,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(300)
                    ),
                    exit = fadeOut(animationSpec = tween(800)) + scaleOut(
                        targetScale = 0.9f,
                        animationSpec = tween(600)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        // Card background for loading indicator
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(RoundedCornerShape(50.dp)),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = BottomSheetDefaults.ContainerColor.copy(alpha = 0.95f)
                            ),
                            elevation = androidx.compose.material3.CardDefaults.cardElevation(
                                defaultElevation = 8.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                M3CircularLoader(
                                    modifier = Modifier.size(36.dp),
                                    fourColor = true,
                                    isExpressive = true
                                )
                            }
                                // Spacer(modifier = Modifier.height(12.dp))
                                // Text(
                                //     text = "Preparing Canvas",
                                //     style = MaterialTheme.typography.labelMedium,
                                //     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                //     textAlign = TextAlign.Center
                                // )
                            
                        }
                    }
                }
                    //             isExpressive = true
                    //         )
                    //     }
                    // }
                }
            }
        }

        // Canvas video player with smooth entrance animation  
        // Show CanvasVideoPlayer as soon as URL is available, its internal state will manage loading/gradient
        AnimatedVisibility(
            visible = isVisible, // Show when videoUrl is not blank
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 800,
                    delayMillis = 200,
                    easing = FastOutSlowInEasing
                )
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 500,
                    easing = FastOutLinearInEasing
                )
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(400)
                )
        ) {
            if (!videoUrl.isNullOrBlank()) {
                CanvasVideoPlayer(
                    videoUrl = videoUrl,
                    isPlaying = isPlaying,
                    cornerRadius = cornerRadius,
                    showGradientOverlay = showGradientOverlay,
                    modifier = Modifier.fillMaxSize(),
                    onVideoReady = {
                        Log.d("CanvasPlayer", "Canvas video is ready and animated in")
                        isLoading = false
                        canRetry = false
                        isCanvasReady = true // This will trigger album art to hide
                        onCanvasLoaded?.invoke()
                    },
                    onVideoError = {
                        Log.w("CanvasPlayer", "Canvas video failed to load")
                        isLoading = false
                        canRetry = true
                        isCanvasReady = false
                        onCanvasFailed?.invoke()
                    }
                )
            }
        }

        // Handle fallback logic after a delay if retry is possible
        LaunchedEffect(canRetry) {
            if (canRetry) {
                delay(2000) // Give 2 seconds for potential retry or user interaction
                if (canRetry) { // Check again in case retry was initiated during delay
                    if (fallbackContent != null) {
                        Log.d("CanvasPlayer", "Switching to fallback content (album art)")
                        showFallback = true
                        isVisible = false
                        showAlbumArt = false // Hide the background album art
                    } else {
                        Log.d("CanvasPlayer", "No fallback provided, keeping album art visible")
                        // Keep showing album art as fallback
                        showAlbumArt = true
                        isVisible = false
                    }
                }
            }
        }
        
        // Retry button when canvas fails to load
        AnimatedVisibility(
            visible = canRetry && !showFallback && !showAlbumArt,
            enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(300)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            canRetry = false
                            isLoading = true
                            showAlbumArt = true // Show album art during retry
                            onRetryRequested?.invoke()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.Text("Retry Canvas")
                    }
                    
                    androidx.compose.material3.Text(
                        text = "Canvas couldn't load",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
