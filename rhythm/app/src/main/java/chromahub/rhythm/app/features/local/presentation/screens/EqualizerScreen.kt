package chromahub.rhythm.app.features.local.presentation.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import android.media.audiofx.AudioEffect
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chromahub.rhythm.app.BuildConfig
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.util.GsonUtils
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.EqualizerUtils
import chromahub.rhythm.app.activities.MainActivity
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.system.exitProcess
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.StandardBottomSheetHeader
import chromahub.rhythm.app.shared.presentation.components.common.StyledProgressBar
import chromahub.rhythm.app.shared.presentation.components.common.CircularStyledProgressBar
import chromahub.rhythm.app.shared.presentation.components.common.ProgressStyle
import chromahub.rhythm.app.shared.presentation.components.common.ThumbStyle
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.LicensesBottomSheet
import chromahub.rhythm.app.ui.utils.LazyListStateSaver
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.presentation.viewmodel.AppUpdaterViewModel
import chromahub.rhythm.app.ui.theme.getFontPreviewStyle
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.features.local.presentation.screens.LibraryTab
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.R
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AddToPlaylistBottomSheet
import chromahub.rhythm.app.features.local.presentation.screens.AddToPlaylistScreen
import chromahub.rhythm.app.features.local.presentation.components.dialogs.CreatePlaylistDialog
import chromahub.rhythm.app.features.local.presentation.components.dialogs.QueueActionDialog
import chromahub.rhythm.app.features.local.presentation.components.player.MiniPlayer
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons.Delete
import chromahub.rhythm.app.features.local.presentation.screens.LibraryScreen
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons.Delete
import chromahub.rhythm.app.features.local.presentation.screens.HomeScreen
import chromahub.rhythm.app.features.local.presentation.screens.ListeningStatsScreen
import chromahub.rhythm.app.features.local.presentation.screens.PlayerScreen
import chromahub.rhythm.app.features.local.presentation.screens.PlaylistDetailScreen
import chromahub.rhythm.app.features.local.presentation.screens.SearchScreen
import chromahub.rhythm.app.features.local.presentation.screens.settings.SettingsScreenWrapper
import chromahub.rhythm.app.features.local.presentation.screens.settings.*
import chromahub.rhythm.app.shared.presentation.components.MediaScanLoader
import chromahub.rhythm.app.shared.presentation.viewmodel.ThemeViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import chromahub.rhythm.app.features.local.presentation.components.dialogs.AutoEQProfileSelector
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.DeviceConfigurationBottomSheet
import chromahub.rhythm.app.shared.data.model.AutoEQProfile

// Equalizer Preset Data Class
data class EqualizerPreset(
    val name: String,
    val icon: ImageVector,
    val bands: List<Float>
)

@Composable
fun TunerAnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbColor by animateColorAsState(
        targetValue = if (checked) 
            MaterialTheme.colorScheme.onPrimary
        else 
            MaterialTheme.colorScheme.outline,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tuner_thumb_color"
    )
    
    val trackColor by animateColorAsState(
        targetValue = if (checked) 
            MaterialTheme.colorScheme.primary
        else 
            MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tuner_track_color"
    )
    
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = thumbColor,
            checkedTrackColor = trackColor,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = thumbColor,
            uncheckedTrackColor = trackColor,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        ),
        thumbContent = {
            AnimatedVisibility(
                visible = checked,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

/**
 * Equalizer Screen - Advanced Audio Equalization
 * Full-featured equalizer with presets, frequency bands, and audio effects
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    navController: NavController,
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Collect states from settings
    val equalizerEnabledState by viewModel.equalizerEnabled.collectAsState()
    val equalizerPresetState by viewModel.equalizerPreset.collectAsState()
    val equalizerBandLevelsState by viewModel.equalizerBandLevels.collectAsState()
    val bassBoostEnabledState by viewModel.bassBoostEnabled.collectAsState()
    val bassBoostStrengthState by viewModel.bassBoostStrength.collectAsState()
    val virtualizerEnabledState by viewModel.virtualizerEnabled.collectAsState()
    val virtualizerStrengthState by viewModel.virtualizerStrength.collectAsState()
    val spatializationStatus by viewModel.spatializationStatus.collectAsState()
    val isSpatializationAvailable by viewModel.isSpatializationAvailable.collectAsState()
    val isBassBoostAvailableState by viewModel.isBassBoostAvailable.collectAsState()
    
    // Update spatialization and bass boost availability when screen is shown
    LaunchedEffect(Unit) {
        viewModel.updateSpatializationStatus()
        viewModel.updateBassBoostAvailability()
    }

    // Local mutable states for UI
    var isEqualizerEnabled by remember(equalizerEnabledState) { mutableStateOf(equalizerEnabledState) }
    var selectedPreset by remember(equalizerPresetState) { mutableStateOf(equalizerPresetState) }
    var bandLevels by remember(equalizerBandLevelsState) {
        mutableStateOf(
            equalizerBandLevelsState.split(",").mapNotNull { it.toFloatOrNull() }.let { levels ->
                when {
                    levels.size == 10 -> levels
                    levels.size == 5 -> List(10) { if (it < 5) levels[it] else 0f }
                    else -> List(10) { 0f }
                }
            }
        )
    }
    var isBassBoostEnabled by remember(bassBoostEnabledState) { mutableStateOf(bassBoostEnabledState) }
    var bassBoostStrength by remember(bassBoostStrengthState) { mutableFloatStateOf(bassBoostStrengthState.toFloat()) }
    var isVirtualizerEnabled by remember(virtualizerEnabledState) { mutableStateOf(virtualizerEnabledState) }
    var virtualizerStrength by remember(virtualizerStrengthState) { mutableFloatStateOf(virtualizerStrengthState.toFloat()) }

    // Preset definitions - Updated to 10 bands with AutoEQ-style precision values
    // Bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val presets = listOf(
        EqualizerPreset("Flat", Icons.Rounded.LinearScale, listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqualizerPreset("Rock", Icons.Rounded.MusicNote, listOf(4.5f, 3.8f, 2.5f, 0.5f, -1.5f, -0.8f, 2.2f, 3.5f, 5.5f, 4.0f)),
        EqualizerPreset("Pop", Icons.Rounded.Star, listOf(-1.5f, 0.5f, 2.8f, 4.2f, 3.5f, 2.0f, 0.5f, 1.5f, 2.5f, 1.0f)),
        EqualizerPreset("Jazz", Icons.Rounded.Piano, listOf(3.5f, 2.8f, 1.5f, 0.5f, -1.5f, -0.5f, 1.8f, 2.5f, 4.0f, 3.0f)),
        EqualizerPreset("Classical", Icons.Rounded.LibraryMusic, listOf(3.0f, 1.5f, -0.5f, -1.5f, -2.0f, -1.5f, 0.5f, 2.5f, 3.5f, 2.5f)),
        EqualizerPreset("Electronic", Icons.Rounded.GraphicEq, listOf(5.5f, 4.8f, 3.5f, 1.5f, 0.5f, 0f, 2.5f, 4.5f, 5.0f, 4.5f)),
        EqualizerPreset("Hip Hop", Icons.Rounded.GraphicEq, listOf(6.5f, 5.5f, 3.5f, 1.5f, -0.5f, -0.8f, 1.8f, 3.5f, 4.5f, 3.5f)),
        EqualizerPreset("Vocal", Icons.Rounded.RecordVoiceOver, listOf(-1.0f, 0.5f, 1.5f, 2.8f, 4.5f, 5.0f, 4.0f, 2.5f, 1.5f, 0.5f)),
        EqualizerPreset("Bass Boost", Icons.Rounded.Speaker, listOf(6.0f, 5.0f, 3.5f, 1.5f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqualizerPreset("Treble Boost", Icons.Rounded.Waves, listOf(0f, 0f, 0f, 0f, 0f, 0.5f, 1.5f, 3.0f, 5.0f, 6.0f)),
        EqualizerPreset("V-Shape", Icons.Rounded.ShowChart, listOf(5.5f, 4.0f, 1.5f, -1.0f, -2.5f, -2.5f, -0.5f, 2.0f, 4.5f, 5.5f)),
        EqualizerPreset("Harman", Icons.Rounded.Headphones, listOf(3.5f, 2.0f, 0.5f, -1.0f, 0f, 0.5f, 1.5f, 2.0f, 2.5f, 1.0f))
    )

    val frequencyLabels = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")

    // Functions
    fun applyPreset(preset: EqualizerPreset) {
        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
        selectedPreset = preset.name
        bandLevels = preset.bands

        // Save to settings
        viewModel.appSettings.setEqualizerPreset(preset.name)
        viewModel.appSettings.setEqualizerBandLevels(preset.bands.joinToString(","))

        // Apply to service
        viewModel.applyEqualizerPreset(preset.name, preset.bands)
    }

    fun updateBandLevel(band: Int, level: Float) {
        val newLevels = bandLevels.toMutableList()
        newLevels[band] = level
        bandLevels = newLevels
        selectedPreset = "Custom"

        // Save to settings
        viewModel.appSettings.setEqualizerBandLevels(newLevels.joinToString(","))
        viewModel.appSettings.setEqualizerPreset("Custom")

        // Apply to service
        val levelShort = (level * 100).toInt().toShort()
        viewModel.setEqualizerBandLevel(band.toShort(), levelShort)
    }

    var showAutoEQSelector by remember { mutableStateOf(false) }
    var showDeviceConfiguration by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Screen entrance animation
    var showContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(50)
        showContent = true
    }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "contentAlpha"
    )
    
    val contentOffset by animateFloatAsState(
        targetValue = if (showContent) 0f else 30f,
        animationSpec = tween(durationMillis = 450),
        label = "contentOffset"
    )

    CollapsibleHeaderScreen(
        title = "Equalizer",
        showBackButton = true,
        onBackClick = { navController.popBackStack() },
        actions = {
            // More Options Button
            FilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                    showMenu = true
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = RhythmIcons.More,
                    contentDescription = "More options",
                    modifier = Modifier.size(20.dp)
                )
            }
            // Dropdown Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(5.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                // AutoEQ Profiles option
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "AutoEQ Profiles",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            showMenu = false
                            showAutoEQSelector = true
                        }
                    )
                }
                
                // Device Configuration option
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Manage Device",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeviceHub,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            showMenu = false
                            showDeviceConfiguration = true
                        }
                    )
                }
                
                // Open System Equalizer option
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "System Equalizer",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Equalizer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            showMenu = false
                            val activity = context as? Activity
                            viewModel.openSystemEqualizer(activity, MainActivity.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL_REQUEST)
                        }
                    )
                }
            }
        }
    ) { modifier ->
        val lazyListState = rememberSaveable(
            key = "equalizer_settings_scroll_state",
            saver = LazyListStateSaver
        ) {
            androidx.compose.foundation.lazy.LazyListState()
        }

        LazyColumn(
            state = lazyListState,
            modifier = modifier.fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha
                    translationY = contentOffset
                },
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Equalizer Enable/Disable Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEqualizerEnabled)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(40.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = null,
                            tint = if (isEqualizerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = when {
                                        isEqualizerEnabled -> "Active"
                                        else -> "Disabled"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        TunerAnimatedSwitch(
                            checked = isEqualizerEnabled,
                            onCheckedChange = { enabled ->
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                isEqualizerEnabled = enabled
                                viewModel.setEqualizerEnabled(enabled)
                            }
                        )
                    }
                }
            }

            // Presets Section with animation
            item {
                AnimatedVisibility(
                    visible = isEqualizerEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Presets",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = selectedPreset,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (selectedPreset == "Custom") {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledIconButton(
                                        onClick = { applyPreset(presets[0]) },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Reset to Flat",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(presets, key = { "preset_${it.name}" }) { preset ->
                                    val isSelected = selectedPreset == preset.name

                                    Card(
                                        onClick = { applyPreset(preset) },
                                        modifier = Modifier.size(width = 85.dp, height = 100.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = preset.icon,
                                                contentDescription = null,
                                                tint = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(if (isSelected) 28.dp else 24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = preset.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Frequency Bands Section with animation
            item {
                AnimatedVisibility(
                    visible = isEqualizerEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Equalizer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = context.getString(R.string.frequency_bands),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Frequency Response Chart
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = context.getString(R.string.frequency_response),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Define colors at the Column scope for reuse
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    val secondaryColor = MaterialTheme.colorScheme.secondary
                                    val tertiaryColor = MaterialTheme.colorScheme.tertiary
                                    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    val outlineColor = MaterialTheme.colorScheme.outline

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                    ) {

                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val width = size.width
                                            val height = size.height
                                            val bandWidth = width / bandLevels.size

                                            // Draw subtle grid lines
                                            for (i in 1..4) {
                                                val y = height * i / 5f
                                                drawLine(
                                                    color = outlineColor.copy(alpha = 0.1f),
                                                    start = Offset(0f, y),
                                                    end = Offset(width, y),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }

                                            // Draw center line (0dB) with emphasis
                                            drawLine(
                                                color = outlineColor.copy(alpha = 0.4f),
                                                start = Offset(0f, height / 2),
                                                end = Offset(width, height / 2),
                                                strokeWidth = 1.5.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                                            )

                                            // Draw frequency response curve
                                            val points = bandLevels.mapIndexed { index, level ->
                                                val x = (index + 0.5f) * bandWidth
                                                val normalizedLevel = (level + 15f) / 30f
                                                val y = height * (1f - normalizedLevel)
                                                Offset(x, y)
                                            }

                                            // Draw filled area under curve with gradient
                                            if (points.size > 1) {
                                                val filledPath = Path().apply {
                                                    moveTo(0f, height / 2)
                                                    lineTo(points[0].x, points[0].y)
                                                    for (i in 1 until points.size) {
                                                        val p0 = points[i - 1]
                                                        val p1 = points[i]
                                                        val controlX = (p0.x + p1.x) / 2
                                                        quadraticTo(controlX, p0.y, p1.x, p1.y)
                                                    }
                                                    lineTo(width, height / 2)
                                                    close()
                                                }

                                                drawPath(
                                                    path = filledPath,
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            primaryColor.copy(alpha = 0.25f),
                                                            primaryColor.copy(alpha = 0.05f)
                                                        )
                                                    )
                                                )

                                                // Draw smooth curve with gradient stroke
                                                val curvePath = Path().apply {
                                                    moveTo(points[0].x, points[0].y)
                                                    for (i in 1 until points.size) {
                                                        val p0 = points[i - 1]
                                                        val p1 = points[i]
                                                        val controlX = (p0.x + p1.x) / 2
                                                        quadraticTo(controlX, p0.y, p1.x, p1.y)
                                                    }
                                                }

                                                drawPath(
                                                    path = curvePath,
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            secondaryColor,
                                                            primaryColor,
                                                            tertiaryColor
                                                        )
                                                    ),
                                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                                )
                                            }

                                            // Draw points with glow effect
                                            points.forEachIndexed { index, point ->
                                                val pointColor = when (index) {
                                                    0, 1 -> secondaryColor // Bass
                                                    2, 3, 4, 5, 6, 7 -> primaryColor // Mid
                                                    else -> tertiaryColor // Treble
                                                }

                                                // Outer glow
                                                drawCircle(
                                                    color = pointColor.copy(alpha = 0.3f),
                                                    radius = 8.dp.toPx(),
                                                    center = point
                                                )
                                                // Main point
                                                drawCircle(
                                                    color = pointColor,
                                                    radius = 5.dp.toPx(),
                                                    center = point
                                                )
                                                // Inner highlight
                                                drawCircle(
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    radius = 2.dp.toPx(),
                                                    center = Offset(point.x - 1.dp.toPx(), point.y - 1.dp.toPx())
                                                )
                                            }
                                        }
                                    }

                                    // Frequency labels row
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        frequencyLabels.forEachIndexed { index, label ->
                                            val labelColor = when (index) {
                                                0, 1 -> secondaryColor // Bass
                                                2, 3, 4, 5, 6, 7 -> primaryColor // Mid
                                                else -> tertiaryColor // Treble
                                            }
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = labelColor.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Frequency Bands Grid with gradient colors
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val secondaryColor = MaterialTheme.colorScheme.secondary
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val tertiaryColor = MaterialTheme.colorScheme.tertiary

                                bandLevels.forEachIndexed { index, level ->
                                    // Color based on frequency range (bass = secondary, mid = primary, treble = tertiary)
                                    val bandColor = when (index) {
                                        0, 1 -> secondaryColor // Bass frequencies (31Hz, 62Hz)
                                        2, 3, 4, 5, 6, 7 -> primaryColor // Mid frequencies (125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz)
                                        else -> tertiaryColor // Treble frequencies (8kHz, 16kHz)
                                    }
                                    val bandLabel = when (index) {
                                        0 -> "Sub Bass"
                                        1 -> "Bass"
                                        2 -> "Low Mid"
                                        3 -> "Mid"
                                        4 -> "Mid"
                                        5 -> "Upper Mid"
                                        6 -> "Presence"
                                        7 -> "Presence"
                                        8 -> "Brilliance"
                                        9 -> "Air"
                                        else -> ""
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = bandColor.copy(alpha = if (level != 0f) 0.12f else 0.06f)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Frequency Icon/Indicator
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = bandColor.copy(alpha = 0.2f),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = frequencyLabels[index],
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = bandColor,
                                                            fontSize = 9.sp
                                                        )
                                                        Text(
                                                            text = if (level >= 0) "+${String.format("%.1f", level)}" else String.format("%.1f", level),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = bandColor
                                                        )
                                                    }
                                                }
                                            }

                                            // Band info
                                            Column(
                                                modifier = Modifier.width(56.dp)
                                            ) {
                                                Text(
                                                    text = bandLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "dB",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Slider Control with custom colors - supports decimal values
                                            Slider(
                                                value = level,
                                                onValueChange = { newLevel ->
                                                    // Round to 1 decimal place for precision
                                                    val roundedLevel = (kotlin.math.round(newLevel * 10) / 10f)
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                    updateBandLevel(index, roundedLevel)
                                                },
                                                valueRange = -15f..15f,
                                                steps = 299, // -15 to +15 with 0.1 steps = 300 positions (299 steps)
                                                modifier = Modifier.weight(1f),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = bandColor,
                                                    activeTrackColor = bandColor,
                                                    inactiveTrackColor = bandColor.copy(alpha = 0.2f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Audio Effects Section with animation
            item {
                AnimatedVisibility(
                    visible = isEqualizerEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = context.getString(R.string.audio_effects),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bass Boost with secondary color
                            val secondaryColor = MaterialTheme.colorScheme.secondary
                            // Check if device supports bass boost (based on service detection of AudioFlinger error -38)
                            val bassBoostStatusText = if (isBassBoostAvailableState) {
                                if (isBassBoostEnabled) "${(bassBoostStrength/10).toInt()}% intensity" else "Enhance low frequencies"
                            } else {
                                "Not available on this device"
                            }
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBassBoostEnabled && isBassBoostAvailableState)
                                        secondaryColor.copy(alpha = 0.15f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isBassBoostEnabled && isBassBoostAvailableState)
                                                secondaryColor.copy(alpha = 0.2f)
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Speaker,
                                                    contentDescription = null,
                                                    tint = if (isBassBoostEnabled && isBassBoostAvailableState)
                                                        secondaryColor
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Bass Boost",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = bassBoostStatusText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isBassBoostEnabled && isBassBoostAvailableState) secondaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        TunerAnimatedSwitch(
                                            checked = isBassBoostEnabled && isBassBoostAvailableState,
                                            onCheckedChange = { enabled ->
                                                if (isBassBoostAvailableState) {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                                    isBassBoostEnabled = enabled
                                                    viewModel.setBassBoost(enabled, bassBoostStrength.toInt().toShort())
                                                }
                                            }
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isBassBoostEnabled && isBassBoostAvailableState,
                                        enter = fadeIn() + androidx.compose.animation.expandVertically(),
                                        exit = fadeOut() + androidx.compose.animation.shrinkVertically()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(12.dp))

                                            Slider(
                                                value = bassBoostStrength,
                                                onValueChange = { strength ->
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                    bassBoostStrength = strength
                                                    viewModel.setBassBoost(true, strength.toInt().toShort())
                                                },
                                                valueRange = 0f..1000f,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = secondaryColor,
                                                    activeTrackColor = secondaryColor,
                                                    inactiveTrackColor = secondaryColor.copy(alpha = 0.2f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Spatial Audio (Virtualizer) with tertiary color
                            val tertiaryColor = MaterialTheme.colorScheme.tertiary
                            val isAndroid12Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                            val isAndroid13Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            val statusText = when {
                                !isAndroid13Plus -> "Requires Android 13+"
                                !isSpatializationAvailable -> "Not available on this device"
                                isVirtualizerEnabled -> spatializationStatus
                                else -> "Spatial audio enhancement"
                            }
                            val showIntensitySlider = isAndroid13Plus && isSpatializationAvailable && isVirtualizerEnabled
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isVirtualizerEnabled && isSpatializationAvailable)
                                        tertiaryColor.copy(alpha = 0.15f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isVirtualizerEnabled && isSpatializationAvailable)
                                                tertiaryColor.copy(alpha = 0.2f)
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Headphones,
                                                    contentDescription = null,
                                                    tint = if (isVirtualizerEnabled && isSpatializationAvailable)
                                                        tertiaryColor
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAndroid13Plus) "Spatial Audio" else context.getString(R.string.virtualizer),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = statusText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isVirtualizerEnabled && isSpatializationAvailable) 
                                                    tertiaryColor 
                                                else 
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        TunerAnimatedSwitch(
                                            checked = isVirtualizerEnabled && isSpatializationAvailable,
                                            onCheckedChange = { enabled ->
                                                if (isAndroid12Plus && isSpatializationAvailable) {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                                    isVirtualizerEnabled = enabled
                                                    viewModel.setVirtualizer(enabled, virtualizerStrength.toInt().toShort())
                                                }
                                            }
                                        )
                                    }

                                    // Info text for Android < 13
                                    if (!isAndroid13Plus && isVirtualizerEnabled) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "🔔 This feature requires Android 13 or higher. Please update your device OS.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                    
                                    // Info text for unavailable spatializer
                                    if (isAndroid13Plus && !isSpatializationAvailable && isVirtualizerEnabled) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "ℹ️ Spatial audio is not supported on this device.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                    
                                    // Info about system settings
                                    if (isAndroid13Plus && isSpatializationAvailable && spatializationStatus.contains("system settings") && isVirtualizerEnabled) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "⚙️ To use spatial audio, enable it in your device's Sound settings.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = tertiaryColor,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }

                                    // Slider for intensity (informational only, as Spatializer is system-controlled)
                                    AnimatedVisibility(
                                        visible = showIntensitySlider,
                                        enter = fadeIn() + androidx.compose.animation.expandVertically(),
                                        exit = fadeOut() + androidx.compose.animation.shrinkVertically()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Text(
                                                text = "Intensity preference: ${(virtualizerStrength/10).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                            )

                                            Slider(
                                                value = virtualizerStrength,
                                                onValueChange = { strength ->
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                    virtualizerStrength = strength
                                                    viewModel.setVirtualizer(true, strength.toInt().toShort())
                                                },
                                                valueRange = 0f..1000f,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = tertiaryColor,
                                                    activeTrackColor = tertiaryColor,
                                                    inactiveTrackColor = tertiaryColor.copy(alpha = 0.2f)
                                                )
                                            )
                                            
                                            Text(
                                                text = "Note: Actual spatial audio effect is system-controlled and depends on your audio output device.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AutoEQ and System Equalizer Cards
            item {
                AnimatedVisibility(
                    visible = isEqualizerEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // AutoEQ Profiles Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "AutoEQ Profiles",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "Apply headphone-specific equalization profiles",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                        showAutoEQSelector = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
//                                    Icon(
//                                        imageVector = Icons.Rounded.AutoMode,
//                                        contentDescription = null,
//                                        modifier = Modifier.size(20.dp)
//                                    )
//                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Browse Presets",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Manage AutoEQ Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeviceHub,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Manage AutoEQ",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "Import, export, and organize your AutoEQ profiles",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedButton(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                        showDeviceConfiguration = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
//                                    Icon(
//                                        imageVector = Icons.Rounded.ManageAccounts,
//                                        contentDescription = null,
//                                        modifier = Modifier.size(20.dp)
//                                    )
//                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Manage Profiles",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Open System Equalizer Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowOutward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Open System Equalizer",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "Access Android's built-in equalizer settings",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedButton(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                        val activity = context as? Activity
                                        viewModel.openSystemEqualizer(activity, MainActivity.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL_REQUEST)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
//                                    Icon(
//                                        imageVector = Icons.Rounded.Settings,
//                                        contentDescription = null,
//                                        modifier = Modifier.size(20.dp)
//                                    )
//                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Open Equalizer",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // AutoEQ Profile Selector
        if (showAutoEQSelector) {
            AutoEQProfileSelector(
                musicViewModel = viewModel,
                onDismiss = { showAutoEQSelector = false },
                onProfileSelected = { profile: AutoEQProfile ->
                    viewModel.applyAutoEQProfile(profile)
                    showAutoEQSelector = false
                }
            )
        }
        
        // Device Configuration Bottom Sheet
        if (showDeviceConfiguration) {
            DeviceConfigurationBottomSheet(
                musicViewModel = viewModel,
                onDismiss = { showDeviceConfiguration = false }
            )
        }
    }
}