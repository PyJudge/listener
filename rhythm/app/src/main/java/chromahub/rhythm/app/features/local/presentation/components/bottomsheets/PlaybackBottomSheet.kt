package chromahub.rhythm.app.features.local.presentation.components.bottomsheets

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.PlaybackLocation
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.LyricsSourcePreference
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackBottomSheet(
    locations: List<PlaybackLocation>,
    currentLocation: PlaybackLocation?,
    volume: Float,
    isMuted: Boolean,
    musicViewModel: MusicViewModel,
    onLocationSelect: (PlaybackLocation) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onMaxVolume: () -> Unit,
    onRefreshDevices: () -> Unit,
    onDismiss: () -> Unit,
    appSettings: AppSettings,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    
    // System volume state
    var systemVolume by remember { mutableFloatStateOf(0.5f) }
    var systemMaxVolume by remember { mutableStateOf(15) }
    
    // Collect settings
    val playbackSpeed by musicViewModel.playbackSpeed.collectAsState()
    val gaplessPlayback by appSettings.gaplessPlayback.collectAsState()
    val shuffleUsesExoplayer by appSettings.shuffleUsesExoplayer.collectAsState()
    val autoAddToQueue by appSettings.autoAddToQueue.collectAsState()
    val shuffleModePersistence by appSettings.shuffleModePersistence.collectAsState()
    val repeatModePersistence by appSettings.repeatModePersistence.collectAsState()
    val lyricsSourcePreference by appSettings.lyricsSourcePreference.collectAsState()
    val clearQueueOnNewSong by appSettings.clearQueueOnNewSong.collectAsState()
    val queuePersistenceEnabled by appSettings.queuePersistenceEnabled.collectAsState()
    val useSystemVolume by appSettings.useSystemVolume.collectAsState()
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentAlpha"
    )
    
    val contentTranslation by animateFloatAsState(
        targetValue = if (showContent) 0f else 30f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentTranslation"
    )

    // Initialize system volume and monitor for changes
    LaunchedEffect(Unit) {
        delay(100) // Reduced delay for faster appearance
        showContent = true
        
        // Get system volume
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        systemVolume = currentVolume.toFloat() / maxVolume.toFloat()
        systemMaxVolume = maxVolume
    }
    
    // Monitor system volume changes
    LaunchedEffect(Unit) {
        while (true) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val newSystemVolume = currentVolume.toFloat() / maxVolume.toFloat()
            
            if (newSystemVolume != systemVolume) {
                systemVolume = newSystemVolume
                systemMaxVolume = maxVolume
            }
            
            delay(800) // Check every 500ms
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { 
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header - Fixed at top, doesn't scroll
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                PlaybackHeader(
                    haptics = haptics
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Scrollable content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Active Device Card
                item {
                    AnimateIn {
                        ActiveDeviceCard(
                            location = currentLocation,
                            onSwitchDevice = {
                                // Use native Android output switcher
                                musicViewModel.showOutputSwitcherDialog()
                            },
                            onRefreshDevices = onRefreshDevices,
                            haptics = haptics
                        )
                    }
                }
                
                // Volume Control Section
                item {
                    AnimateIn {
                        VolumeControlCard(
                            volume = volume,
                            isMuted = isMuted,
                            systemVolume = systemVolume,
                            systemMaxVolume = systemMaxVolume,
                            appSettings = appSettings,
                            context = context,
                            onVolumeChange = onVolumeChange,
                            onToggleMute = onToggleMute,
                            onMaxVolume = onMaxVolume,
                            onSystemVolumeChange = { newVolume ->
                                systemVolume = newVolume
                            },
                            haptics = haptics
                        )
                    }
                }
                
                // Playback Speed Section
                item {
                    AnimateIn {
                        PlaybackSpeedCard(
                            currentSpeed = playbackSpeed,
                            onSpeedChange = { speed ->
                                musicViewModel.setPlaybackSpeed(speed)
                            },
                            haptics = haptics,
                            context = context
                        )
                    }
                }
                
                // Playback Settings Section
                item {
                    AnimateIn {
                        PlaybackSettingsCard(
                            gaplessPlayback = gaplessPlayback,
                            shuffleUsesExoplayer = shuffleUsesExoplayer,
                            autoAddToQueue = autoAddToQueue,
                            clearQueueOnNewSong = clearQueueOnNewSong,
                            shuffleModePersistence = shuffleModePersistence,
                            repeatModePersistence = repeatModePersistence,
                            lyricsSourcePreference = lyricsSourcePreference,
                            useSystemVolume = useSystemVolume,
                            queuePersistenceEnabled = queuePersistenceEnabled,
                            onGaplessPlaybackChange = {
                                musicViewModel.setGaplessPlayback(it)
                            },
                            onShuffleUsesExoplayerChange = { appSettings.setShuffleUsesExoplayer(it) },
                            onAutoAddToQueueChange = { appSettings.setAutoAddToQueue(it) },
                            onClearQueueOnNewSongChange = { appSettings.setClearQueueOnNewSong(it) },
                            onShuffleModePersistenceChange = { appSettings.setShuffleModePersistence(it) },
                            onRepeatModePersistenceChange = { appSettings.setRepeatModePersistence(it) },
                            onLyricsSourcePreferenceChange = { appSettings.setLyricsSourcePreference(it) },
                            onUseSystemVolumeChange = { appSettings.setUseSystemVolume(it) },
                            onQueuePersistenceEnabledChange = { appSettings.setQueuePersistenceEnabled(it) },
                            haptics = haptics,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackHeader(
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = context.getString(R.string.bottomsheet_playback),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape
                    )
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    text = context.getString(R.string.audio_settings),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ActiveDeviceCard(
    location: PlaybackLocation?,
    onSwitchDevice: () -> Unit,
    onRefreshDevices: () -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Subtle pulse animation for connected device
    val infiniteTransition = rememberInfiniteTransition(label = "devicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Card(
        onClick = onSwitchDevice,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
            // .graphicsLayer {
            //     if (location != null) {
            //         scaleX = pulseScale
            //         scaleY = pulseScale
            //     }
            // },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.active_device),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Refresh button with rotation animation
                var isRefreshing by remember { mutableStateOf(false) }
                val rotation by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isRefreshing) 360f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 500,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    ),
                    finishedListener = {
                        isRefreshing = false
                    },
                    label = "rotation"
                )
                
                IconButton(
                    onClick = {
                        isRefreshing = true
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        onRefreshDevices()
                    },
                    modifier = Modifier
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Refresh,
                        contentDescription = "Refresh devices",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                rotationZ = rotation
                            }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Device info
            if (location != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Device icon background
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getDeviceIcon(location),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Device details
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val typeDescription = when {
                            location.id.startsWith("bt_") -> "Bluetooth device"
                            location.id == "wired_headset" -> "Wired headphones"
                            location.id == "speaker" -> "Phone speaker"
                            else -> "Audio device"
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = typeDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "• Tap to switch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    // Active indicator with switch icon
                    Icon(
                        imageVector = Icons.Rounded.SyncAlt,
                        contentDescription = "Switch device",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // No device state
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = RhythmIcons.Speaker,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = context.getString(R.string.bottomsheet_no_device),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        
                        Text(
                            text = context.getString(R.string.tap_select_output),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeControlCard(
    volume: Float,
    isMuted: Boolean,
    systemVolume: Float,
    systemMaxVolume: Int,
    appSettings: AppSettings,
    context: Context,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onMaxVolume: () -> Unit,
    onSystemVolumeChange: (Float) -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val useSystemVolume by appSettings.useSystemVolume.collectAsState()
    
    // Remember previous volume before muting for system volume
    var previousSystemVolume by remember { mutableFloatStateOf(0.5f) }
    
    // Current volume values based on setting
    val currentVolume = if (useSystemVolume) systemVolume else volume
    val currentIsMuted = if (useSystemVolume) (systemVolume == 0f) else isMuted
    
    // Animated volume for smooth transitions
    val animatedVolume by animateFloatAsState(
        targetValue = if (currentIsMuted) 0f else currentVolume,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "animatedVolume"
    )
    
    // System volume control functions
    val setSystemVolume = { newVolume: Float ->
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val targetVolume = (newVolume * systemMaxVolume).toInt().coerceIn(0, systemMaxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        onSystemVolumeChange(newVolume)
    }
    
    val toggleSystemMute = {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (systemVolume > 0f) {
            // Mute - save current volume
            previousSystemVolume = systemVolume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            onSystemVolumeChange(0f)
        } else {
            // Unmute - restore previous volume
            val volumeToRestore = if (previousSystemVolume > 0f) previousSystemVolume else 0.5f
            val targetVolume = (volumeToRestore * systemMaxVolume).toInt().coerceIn(1, systemMaxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
            onSystemVolumeChange(volumeToRestore)
        }
    }
    
    val setSystemMaxVolume = {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, systemMaxVolume, 0)
        onSystemVolumeChange(1f)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Volume header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated icon based on volume level
                val iconAlpha by animateFloatAsState(
                    targetValue = if (currentIsMuted) 0.5f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "iconAlpha"
                )
                
                Icon(
                    imageVector = if (currentIsMuted) RhythmIcons.VolumeOff else 
                                if (animatedVolume < 0.3f) RhythmIcons.VolumeMute else 
                                if (animatedVolume < 0.7f) RhythmIcons.VolumeDown else 
                                RhythmIcons.VolumeUp,
                    contentDescription = "Volume",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = iconAlpha),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (useSystemVolume) "System Volume" else "App Volume",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Text(
                    //     text = "Tap to switch between app and system volume",
                    //     style = MaterialTheme.typography.bodySmall,
                    //     color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    // )
                }
                
                // Volume percentage
                Text(
                    text = "${(animatedVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Volume controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Volume down button
                IconButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        if (useSystemVolume) {
                            val newVolume = (systemVolume - 0.1f).coerceAtLeast(0f)
                            setSystemVolume(newVolume)
                        } else {
                            val newVolume = (volume - 0.1f).coerceAtLeast(0f)
                            onVolumeChange(newVolume)
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = RhythmIcons.Remove,
                        contentDescription = "Decrease volume",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Volume slider
                Slider(
                    value = currentVolume,
                    onValueChange = { newVolume ->
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                        if (useSystemVolume) {
                            setSystemVolume(newVolume)
                        } else {
                            onVolumeChange(newVolume)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                )
                
                // Volume up button
                IconButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        if (useSystemVolume) {
                            val newVolume = (systemVolume + 0.1f).coerceAtMost(1f)
                            setSystemVolume(newVolume)
                        } else {
                            val newVolume = (volume + 0.1f).coerceAtMost(1f)
                            onVolumeChange(newVolume)
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = RhythmIcons.Add,
                        contentDescription = "Increase volume",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mute and Max Volume buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mute button
                val muteButtonColor by animateColorAsState(
                    targetValue = if (currentIsMuted) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    label = "muteButtonColor"
                )
                val muteContentColor by animateColorAsState(
                    targetValue = if (currentIsMuted) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "muteContentColor"
                )
                
                Surface(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        if (useSystemVolume) {
                            toggleSystemMute()
                        } else {
                            onToggleMute()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = muteButtonColor,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentIsMuted) RhythmIcons.VolumeOff else RhythmIcons.VolumeMute,
                            contentDescription = if (currentIsMuted) "Unmute" else "Mute",
                            tint = muteContentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentIsMuted) "Unmute" else "Mute",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = muteContentColor
                        )
                    }
                }
                
                // Max volume button
                Surface(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        if (useSystemVolume) {
                            setSystemMaxVolume()
                        } else {
                            onMaxVolume()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = RhythmIcons.VolumeUp,
                            contentDescription = context.getString(R.string.bottomsheet_max),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = context.getString(R.string.bottomsheet_max),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackSpeedCard(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: Context,
    modifier: Modifier = Modifier
) {
    var selectedSpeed by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "Playback Speed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = context.getString(R.string.playback_speed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Current speed display
                Text(
                    text = "${String.format("%.2f", selectedSpeed)}x",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Slider with labels
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "0.25x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "2.0x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = selectedSpeed,
                    onValueChange = { newValue ->
                        selectedSpeed = newValue
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                    },
                    onValueChangeFinished = {
                        onSpeedChange(selectedSpeed)
                    },
                    valueRange = 0.25f..2.0f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quick preset buttons
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)) { presetSpeed ->
                    AssistChip(
                        onClick = {
                            selectedSpeed = presetSpeed
                            onSpeedChange(presetSpeed)
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        },
                        label = {
                            Text(
                                text = "${String.format("%.2f", presetSpeed)}x",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedSpeed == presetSpeed)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (selectedSpeed == presetSpeed)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackSettingsCard(
    gaplessPlayback: Boolean,
    shuffleUsesExoplayer: Boolean,
    autoAddToQueue: Boolean,
    clearQueueOnNewSong: Boolean,
    shuffleModePersistence: Boolean,
    repeatModePersistence: Boolean,
    lyricsSourcePreference: LyricsSourcePreference,
    useSystemVolume: Boolean,
    queuePersistenceEnabled: Boolean,
    onGaplessPlaybackChange: (Boolean) -> Unit,
    onShuffleUsesExoplayerChange: (Boolean) -> Unit,
    onAutoAddToQueueChange: (Boolean) -> Unit,
    onClearQueueOnNewSongChange: (Boolean) -> Unit,
    onShuffleModePersistenceChange: (Boolean) -> Unit,
    onRepeatModePersistenceChange: (Boolean) -> Unit,
    onLyricsSourcePreferenceChange: (LyricsSourcePreference) -> Unit,
    onUseSystemVolumeChange: (Boolean) -> Unit,
    onQueuePersistenceEnabledChange: (Boolean) -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: Context,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = RhythmIcons.Settings,
                    contentDescription = "Playback Settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = context.getString(R.string.playback_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Spacer(modifier = Modifier.height(20.dp))

            // // ExoPlayer Settings Section
            // Text(
            //     text = "ExoPlayer Settings",
            //     style = MaterialTheme.typography.bodyLarge,
            //     fontWeight = FontWeight.Medium,
            //     color = MaterialTheme.colorScheme.onSurface
            // )

            Spacer(modifier = Modifier.height(16.dp))

            // Use System Volume
            AudioSettingRow(
                title = "Use System Volume",
                description = "Control system volume instead of app volume",
                enabled = useSystemVolume,
                onToggle = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onUseSystemVolumeChange(it)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Gapless Playback
            AudioSettingRow(
                title = "Gapless Playback",
                description = "Seamless transitions between tracks",
                enabled = gaplessPlayback,
                onToggle = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onGaplessPlaybackChange(it)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Use ExoPlayer Shuffle
            AudioSettingRow(
                title = "Use ExoPlayer Shuffle",
                description = "Let media player handle shuffle (recommended: OFF)",
                enabled = shuffleUsesExoplayer,
                onToggle = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onShuffleUsesExoplayerChange(it)
                }
            )

            // Spacer(modifier = Modifier.height(20.dp))
            // HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            // Spacer(modifier = Modifier.height(20.dp))

            // // Queue & Playback Section
            // Text(
            //     text = "Queue & Playback",
            //     style = MaterialTheme.typography.bodyLarge,
            //     fontWeight = FontWeight.Medium,
            //     color = MaterialTheme.colorScheme.onSurface
            // )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto Queue
            AudioSettingRow(
                title = "Auto Queue",
                description = "Automatically add related songs to queue",
                enabled = autoAddToQueue,
                onToggle = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onAutoAddToQueueChange(it)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Clear Queue on New Song
            AudioSettingRow(
                title = "Clear Queue on New Song",
                description = "Clear the current queue when playing a new song directly",
                enabled = clearQueueOnNewSong,
                onToggle = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onClearQueueOnNewSongChange(it)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Remember Shuffle Mode
            AudioSettingRow(
                title = "Remember Shuffle Mode",
                description = "Persist shuffle state across sessions",
                enabled = shuffleModePersistence,
                onToggle = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onShuffleModePersistenceChange(it)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Remember Queue
            AudioSettingRow(
                title = "Remember Queue",
                description = "Save and restore queue when restarting app",
                enabled = queuePersistenceEnabled,
                onToggle = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onQueuePersistenceEnabledChange(it)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(20.dp))

            // Lyrics Source Priority
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = context.getString(R.string.playback_lyrics_priority),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = context.getString(R.string.playback_lyrics_priority_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Lyrics Source Options
                LyricsSourcePreference.values().forEach { preference ->
                    Surface(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            onLyricsSourcePreferenceChange(preference)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (lyricsSourcePreference == preference)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = preference.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (lyricsSourcePreference == preference)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (lyricsSourcePreference == preference) {
                                Icon(
                                    imageVector = RhythmIcons.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioSettingRow(
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        AnimatedAudioSwitch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun AnimatedAudioSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumbColor"
    )
    
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "trackColor"
    )
    
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = thumbColor,
            checkedTrackColor = trackColor,
            uncheckedThumbColor = thumbColor,
            uncheckedTrackColor = trackColor
        ),
        thumbContent = {
            AnimatedVisibility(
                visible = checked,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = RhythmIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}


@Composable
private fun getDeviceIcon(location: PlaybackLocation) = when {
    location.id.startsWith("bt_") -> RhythmIcons.BluetoothFilled
    location.id == "wired_headset" -> RhythmIcons.HeadphonesFilled
    location.id == "speaker" -> RhythmIcons.SpeakerFilled
    else -> RhythmIcons.Speaker
}

@Composable
private fun AnimateIn(
    delay: Int = 50,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, delayMillis = 0),
        label = "alpha"
    )

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    val translationY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "translationY"
    )

    Box(
        modifier = modifier.graphicsLayer(
            alpha = alpha,
            scaleX = scale,
            scaleY = scale,
            translationY = translationY
        )
    ) {
        content()
    }
}

