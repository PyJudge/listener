package chromahub.rhythm.app.features.local.presentation.components.bottomsheets

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.shared.presentation.components.common.SimpleCircularLoader
import chromahub.rhythm.app.features.local.presentation.components.player.formatDuration
import chromahub.rhythm.app.shared.presentation.components.common.MarqueeText
import chromahub.rhythm.app.util.ImageUtils
import chromahub.rhythm.app.util.MediaUtils
import chromahub.rhythm.app.util.HapticUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Data class to hold additional song metadata
// 
// AUDIO QUALITY NOTES:
// - Lossless formats (ALAC, FLAC, WAV) preserve all original audio data bit-perfectly
// - Lossy formats (MP3, AAC, OGG) discard data to reduce file size - NOT lossless!
// - Bit depth alone does NOT determine lossless vs lossy:
//   * Lossy MP3/AAC decode to 16-bit but are still lossy (data was discarded during encoding)
//   * Lossless can be 16-bit (CD quality) or 24-bit (Hi-Res)
// - Standard Lossless (CD Quality): 16-bit/44.1kHz, ~96 dB dynamic range
// - High-Resolution Lossless: 24-bit/96kHz+, ~144 dB dynamic range
data class ExtendedSongInfo(
    val fileSize: Long = 0,
    val bitrate: String = "Unknown",
    val sampleRate: String = "Unknown",
    val format: String = "Unknown",
    val composer: String = "",
    val discNumber: Int = 0,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val filePath: String = "",
    val albumArtist: String = "",
    val year: Int = 0,
    val mimeType: String = "",
    val channels: String = "Unknown",
    val hasLyrics: Boolean = false,
    val genre: String = "", // Add genre field
    // Audio quality indicators
    val isLossless: Boolean = false,
    val isDolby: Boolean = false,
    val isDTS: Boolean = false,
    val isHiRes: Boolean = false,
    val audioCodec: String = "Unknown",
    val formatName: String = "Unknown",
    // Enhanced quality information
    val qualityType: String = "Unknown",       // e.g., "Hi-Res Lossless", "CD Quality"
    val qualityLabel: String = "Unknown",       // e.g., "Hi-Res Lossless"
    val qualityDescription: String = "",        // e.g., "24-bit / 96 kHz Lossless"
    val bitDepth: Int = 0,                      // Actual or estimated bit depth (16, 24, etc.)
    val qualityCategory: String = "Unknown"     // "Lossless", "Lossy", "Surround"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoBottomSheet(
    song: Song?,
    onDismiss: () -> Unit,
    appSettings: AppSettings,
    onEditSong: ((title: String, artist: String, album: String, genre: String, year: Int, trackNumber: Int) -> Unit)? = null,
    onShowLyricsEditor: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    var extendedInfo by remember { mutableStateOf<ExtendedSongInfo?>(null) }
    var isLoadingMetadata by remember { mutableStateOf(true) }
    var showEditSheet by remember { mutableStateOf(false) }
    
    // Detect tablet mode
    val isTablet = configuration.screenWidthDp >= 600
    
    // Time format setting
    val useHoursFormat by appSettings.useHoursInTimeFormat.collectAsState()
    
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    
    // Track the current song state to allow updates
    var currentSong by remember(song?.id) { mutableStateOf(song) }
    
    // Update currentSong when the original song changes
    LaunchedEffect(song) {
        if (song != null) {
            currentSong = song
        }
    }
    
    // Blacklist states
    val blacklistedSongs by appSettings.blacklistedSongs.collectAsState()
    val blacklistedFolders by appSettings.blacklistedFolders.collectAsState()
    var isLoadingBlacklist by remember { mutableStateOf(false) }
    
    // Whitelist states
    val whitelistedSongs by appSettings.whitelistedSongs.collectAsState()
    val whitelistedFolders by appSettings.whitelistedFolders.collectAsState()
    var isLoadingWhitelist by remember { mutableStateOf(false) }
    
    // Check if song is blacklisted
    val isBlacklisted = song?.let { blacklistedSongs.contains(it.id) } ?: false
    
    // Check if song is whitelisted
    val isWhitelisted = song?.let { whitelistedSongs.contains(it.id) } ?: false
    
    // Check if song is in a blacklisted folder
    val folderPath = remember(song?.uri) {
        song?.let { 
            try {
                when (it.uri.scheme) {
                    "content" -> {
                        val projection = arrayOf(MediaStore.Audio.Media.DATA)
                        context.contentResolver.query(it.uri, projection, null, null, null)
                            ?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                                    val filePath = cursor.getString(dataIndex)
                                    File(filePath).parent
                                } else null
                            }
                    }
                    "file" -> File(it.uri.path ?: "").parent
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    val isInBlacklistedFolder = folderPath != null && blacklistedFolders.any { blacklistedPath ->
        folderPath.startsWith(blacklistedPath, ignoreCase = true)
    }
    
    val isInWhitelistedFolder = folderPath != null && whitelistedFolders.any { whitelistedPath ->
        folderPath.startsWith(whitelistedPath, ignoreCase = true)
    }

    if (song == null) {
        onDismiss()
        return
    }

    // Load extended metadata
    LaunchedEffect(song.id) {
        isLoadingMetadata = true
        extendedInfo = withContext(Dispatchers.IO) {
            MediaUtils.getExtendedSongInfo(context, song)
        }
        isLoadingMetadata = false
    }

    // Animation trigger
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        showContent = true
    }

    if (isTablet) {
        // Tablet layout: Dialog with side-by-side layout
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .navigationBarsPadding()
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left side: Song artwork and info
                        Surface(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight(),
                            color = Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 24.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Song artwork
                                Surface(
                                    modifier = Modifier
                                        .size(180.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    shadowElevation = 16.dp,
                                    tonalElevation = 8.dp
                                ) {
                                    Box {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .apply(
                                                    ImageUtils.buildImageRequest(
                                                        song.artworkUri,
                                                        song.title,
                                                        context.cacheDir,
                                                        M3PlaceholderType.TRACK
                                                    )
                                                )
                                                .build(),
                                            contentDescription = "Song artwork",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Song info
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = song.album,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Right side: Metadata grid
                        Surface(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight(),
                            color = Color.Transparent
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Header with close and edit buttons
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Details",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Action buttons
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Edit button
                                            onEditSong?.let {
                                                FilledTonalIconButton(
                                                    onClick = {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptics,
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        showEditSheet = true
                                                    },
                                                    modifier = Modifier.size(44.dp),
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Edit,
                                                        contentDescription = "Edit",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // Close button on tablet
                                            IconButton(
                                                onClick = onDismiss,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Close",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Metadata grid
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    shape = RoundedCornerShape(
                                        topStart = 28.dp,
                                        topEnd = 28.dp
                                    ),
//                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    tonalElevation = 1.dp
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            top = 8.dp,
                                            bottom = 24.dp,
                                            start = 16.dp,
                                            end = 16.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        userScrollEnabled = true
                                    ) {
                                        item {
                                            MetadataGridSection(
                                                song = currentSong ?: song,
                                                extendedInfo = extendedInfo,
                                                isLoading = isLoadingMetadata,
                                                useHoursFormat = useHoursFormat
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edit sheet for tablet
        if (showEditSheet) {
            EditSongSheet(
                song = currentSong ?: song,
                onDismiss = { showEditSheet = false },
                onSave = { title: String, artist: String, album: String, genre: String, year: Int, trackNumber: Int ->
                    currentSong = currentSong?.copy(
                        title = title,
                        artist = artist,
                        album = album,
                        genre = genre,
                        year = year,
                        trackNumber = trackNumber
                    )
                    onEditSong?.invoke(title, artist, album, genre, year, trackNumber)
                    showEditSheet = false
                },
                onShowLyricsEditor = onShowLyricsEditor
            )
        }
    } else {
        // Phone layout: Bottom sheet
        ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { 
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header with album art and track info
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art with modern styling
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .apply(
                                        ImageUtils.buildImageRequest(
                                            song.artworkUri,
                                            song.title,
                                            context.cacheDir,
                                            M3PlaceholderType.TRACK
                                        )
                                    )
                                    .build(),
                                contentDescription = "Album artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Song info with improved layout
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MarqueeText(
                                text = song.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                gradientEdgeColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            MarqueeText(
                                text = song.artist,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                gradientEdgeColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            MarqueeText(
                                text = song.album,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                gradientEdgeColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Edit button
                        onEditSong?.let { editCallback ->
                            FilledTonalIconButton(
                                onClick = { 
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showEditSheet = true
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit metadata"
                                )
                            }
                        }
                    }
                }
            }
            item {
                // Actions section
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.bottomsheet_actions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Block/Unblock Song
                            FilledTonalButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isLoadingBlacklist = true
                                    
                                    song?.let { songToBlock ->
                                        if (isBlacklisted) {
                                            appSettings.removeFromBlacklist(songToBlock.id)
                                        } else {
                                            appSettings.addToBlacklist(songToBlock.id)
                                        }
                                        
                                        isLoadingBlacklist = false
                                        val message = if (isBlacklisted) "Song removed from blacklist" else "Song added to blacklist"
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isLoadingBlacklist,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isBlacklisted) 
                                        MaterialTheme.colorScheme.errorContainer 
                                    else 
                                        MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                if (isLoadingBlacklist) {
                                    SimpleCircularLoader(
                                        size = 16.dp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isBlacklisted) Icons.Rounded.Block else Icons.Rounded.DoNotDisturb,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isBlacklisted) "Un-Blacklist Track" else "Blacklist Track")
                            }
                            
                            // Block/Unblock Folder
                            if (folderPath != null) {
                                FilledTonalButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isLoadingBlacklist = true
                                        
                                        if (isInBlacklistedFolder) {
                                            appSettings.removeFolderFromBlacklist(folderPath)
                                        } else {
                                            appSettings.addFolderToBlacklist(folderPath)
                                        }
                                        
                                        isLoadingBlacklist = false
                                        val message = if (isInBlacklistedFolder) "Folder removed from blacklist" else "Folder added to blacklist"
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = !isLoadingBlacklist,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isInBlacklistedFolder) 
                                            MaterialTheme.colorScheme.errorContainer 
                                        else 
                                            MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    if (isLoadingBlacklist) {
                                        SimpleCircularLoader(
                                            size = 16.dp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isInBlacklistedFolder) Icons.Rounded.FolderOff else Icons.Rounded.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isInBlacklistedFolder) "Un-Blacklist Folder" else "Blacklist Folder")
                                }
                            }
                        }
                        
                        // Row(
                        //     modifier = Modifier.fillMaxWidth(),
                        //     horizontalArrangement = Arrangement.spacedBy(8.dp)
                        // ) {
                        //     // Whitelist/Remove from whitelist Song
                        //     FilledTonalButton(
                        //         onClick = {
                        //             haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        //             isLoadingWhitelist = true
                                    
                        //             song?.let { songToWhitelist ->
                        //                 if (isWhitelisted) {
                        //                     appSettings.removeFromWhitelist(songToWhitelist.id)
                        //                 } else {
                        //                     appSettings.addToWhitelist(songToWhitelist.id)
                        //                 }
                                        
                        //                 isLoadingWhitelist = false
                        //                 val message = if (isWhitelisted) "Song removed from whitelist" else "Song added to whitelist" 
                        //                 Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        //             }
                        //         },
                        //         enabled = !isLoadingWhitelist,
                        //         modifier = Modifier.weight(1f),
                        //         colors = ButtonDefaults.filledTonalButtonColors(
                        //             containerColor = if (isWhitelisted) 
                        //                 MaterialTheme.colorScheme.primaryContainer 
                        //             else 
                        //                 MaterialTheme.colorScheme.secondaryContainer
                        //         )
                        //     ) {
                        //         if (isLoadingWhitelist) {
                        //             SimpleCircularLoader(
                        //                 size = 16.dp,
                        //                 color = MaterialTheme.colorScheme.onSecondaryContainer
                        //             )
                        //         } else {
                        //             Icon(
                        //                 imageVector = if (isWhitelisted) Icons.Rounded.CheckCircle else Icons.Rounded.Add,
                        //                 contentDescription = null,
                        //                 modifier = Modifier.size(16.dp)
                        //             )
                        //         }
                        //         Spacer(modifier = Modifier.width(8.dp))
                        //         Text(if (isWhitelisted) "Un-Whitelist Track" else "Whitelist Track")
                        //     }
                            
                        //     // Whitelist/Remove from whitelist Folder
                        //     if (folderPath != null) {
                        //         FilledTonalButton(
                        //             onClick = {
                        //                 haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        //                 isLoadingWhitelist = true
                                        
                        //                 if (isInWhitelistedFolder) {
                        //                     appSettings.removeFolderFromWhitelist(folderPath)
                        //                 } else {
                        //                     appSettings.addFolderToWhitelist(folderPath)
                        //                 }
                                        
                        //                 isLoadingWhitelist = false
                        //                 val message = if (isInWhitelistedFolder) "Folder removed from whitelist" else "Folder added to whitelist"
                        //                 Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        //             },
                        //             enabled = !isLoadingWhitelist,
                        //             modifier = Modifier.weight(1f),
                        //             colors = ButtonDefaults.filledTonalButtonColors(
                        //                 containerColor = if (isInWhitelistedFolder) 
                        //                     MaterialTheme.colorScheme.primaryContainer 
                        //                 else 
                        //                     MaterialTheme.colorScheme.tertiaryContainer
                        //             )
                        //         ) {
                        //             if (isLoadingWhitelist) {
                        //                 SimpleCircularLoader(
                        //                     size = 16.dp,
                        //                     color = MaterialTheme.colorScheme.onTertiaryContainer
                        //                 )
                        //             } else {
                        //                 Icon(
                        //                     imageVector = if (isInWhitelistedFolder) Icons.Rounded.FolderOff else Icons.Rounded.Folder,
                        //                     contentDescription = null,
                        //                     modifier = Modifier.size(16.dp)
                        //                 )
                        //             }
                        //             Spacer(modifier = Modifier.width(8.dp))
                        //             Text(if (isInWhitelistedFolder) "Un-Whitelist Folder" else "Whitelist Folder")
                        //         }
                        //     }
                        // }
                    }
                }
            }
            
            // item {
            //     // Action buttons (Bottom Row)
            //     Row(
            //         modifier = Modifier.fillMaxWidth(),
            //         horizontalArrangement = Arrangement.spacedBy(8.dp)
            //     ) {
            //         // Share Song Info
            //         FilledTonalButton(
            //             onClick = {
            //                 haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            //                 val shareIntent = Intent().apply {
            //                     action = Intent.ACTION_SEND
            //                     putExtra(Intent.EXTRA_TEXT, "Now playing: ${song.title} by ${song.artist}")
            //                     type = "text/plain"
            //                 }
            //                 context.startActivity(Intent.createChooser(shareIntent, "Share song"))
            //             },
            //             modifier = Modifier.weight(1f),
            //             colors = ButtonDefaults.filledTonalButtonColors(
            //                 containerColor = MaterialTheme.colorScheme.primaryContainer
            //             )
            //         ) {
            //             Icon(
            //                 imageVector = Icons.Rounded.Share,
            //                 contentDescription = null,
            //                 modifier = Modifier.size(16.dp)
            //             )
            //             Spacer(modifier = Modifier.width(8.dp))
            //             Text("Share Info")
            //         }
                    
            //         // Share Original File
            //         FilledTonalButton(
            //             onClick = {
            //                 haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            //                 try {
            //                     val shareIntent = Intent().apply {
            //                         action = Intent.ACTION_SEND
            //                         type = "audio/*"
            //                         putExtra(Intent.EXTRA_STREAM, song.uri)
            //                         addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            //                     }
            //                     context.startActivity(Intent.createChooser(shareIntent, "Share original file"))
            //                 } catch (e: Exception) {
            //                     Toast.makeText(context, "Unable to share file", Toast.LENGTH_SHORT).show()
            //                 }
            //             },
            //             modifier = Modifier.weight(1f),
            //             colors = ButtonDefaults.filledTonalButtonColors(
            //                 containerColor = MaterialTheme.colorScheme.secondaryContainer
            //             )
            //         ) {
            //             Icon(
            //                 imageVector = Icons.Rounded.AudioFile,
            //                 contentDescription = null,
            //                 modifier = Modifier.size(16.dp)
            //             )
            //             Spacer(modifier = Modifier.width(8.dp))
            //             Text("Share File")
            //         }
                    
            //         // Open in external player
            //         FilledTonalButton(
            //             onClick = {
            //                 haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            //                 val intent = Intent().apply {
            //                     action = Intent.ACTION_VIEW
            //                     setDataAndType(song.uri, "audio/*")
            //                     addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            //                 }
            //                 try {
            //                     context.startActivity(intent)
            //                 } catch (_: Exception) {
            //                     Toast.makeText(context, "No app found to open file", Toast.LENGTH_SHORT).show()
            //                 }
            //             },
            //             modifier = Modifier.weight(1f),
            //             colors = ButtonDefaults.filledTonalButtonColors(
            //                 containerColor = MaterialTheme.colorScheme.tertiaryContainer
            //             )
            //         ) {
            //             Icon(
            //                 imageVector = Icons.Rounded.PlayArrow,
            //                 contentDescription = null,
            //                 modifier = Modifier.size(16.dp)
            //             )
            //             Spacer(modifier = Modifier.width(8.dp))
            //             Text("Open")
            //         }
            //     }
            // }

            item {
                // Metadata grid section
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    MetadataGridSection(
                        song = song,
                        extendedInfo = extendedInfo,
                        isLoading = isLoadingMetadata,
                        useHoursFormat = useHoursFormat
                    )
                }
            }
        }
        
        // Show Edit Sheet
        if (showEditSheet) {
            EditSongSheet(
                song = song,
                onDismiss = { showEditSheet = false },
                onSave = { title, artist, album, genre, year, trackNumber ->
                    onEditSong?.invoke(title, artist, album, genre, year, trackNumber)
                    showEditSheet = false
                },
                onShowLyricsEditor = onShowLyricsEditor
            )
        }
    }
    }
}

@Composable
private fun MetadataGridSection(
    song: Song,
    extendedInfo: ExtendedSongInfo?,
    isLoading: Boolean,
    useHoursFormat: Boolean = false
) {
    val context = LocalContext.current
    // Prepare metadata items (avoiding duplicates and showing more information)
    val metadataItems = buildList {
        // Basic song info
        add(MetadataItem("Duration", formatDuration(song.duration, useHoursFormat), Icons.Rounded.Schedule))
        
        // Track info (prefer extended info if available)
        val trackNum = if (song.trackNumber > 0) song.trackNumber else 0
        val discNum = extendedInfo?.discNumber ?: 0
        when {
            trackNum > 0 && discNum > 0 -> add(MetadataItem("Track", "$discNum.$trackNum", Icons.Rounded.FormatListNumbered))
            trackNum > 0 -> add(MetadataItem("Track", trackNum.toString(), Icons.Rounded.FormatListNumbered))
        }
        
        // Year (prefer song data, fallback to extended info)
        val yearValue = if (song.year > 0) song.year else extendedInfo?.year ?: 0
        if (yearValue > 0) {
            add(MetadataItem("Year", yearValue.toString(), Icons.Rounded.DateRange))
        }
        
        // Genre (prefer song data, fallback to extended info)
        val genreValue = if (!song.genre.isNullOrEmpty()) song.genre else extendedInfo?.genre
        if (!genreValue.isNullOrEmpty()) {
            add(MetadataItem("Genre", genreValue.trim(), Icons.Rounded.Category))
        }
        
        extendedInfo?.let { info ->
            // Enhanced Audio Quality Badge - show detailed quality type
            if (info.qualityLabel != "Unknown" && info.qualityLabel.isNotEmpty()) {
                val qualityIcon = when {
                    info.isDolby -> Icons.Rounded.SurroundSound
                    info.isDTS -> Icons.Rounded.SurroundSound
                    info.isLossless -> Icons.Rounded.HighQuality
                    info.isHiRes -> Icons.Rounded.HighQuality
                    else -> Icons.Rounded.GraphicEq
                }
                add(MetadataItem("Quality", info.qualityLabel, qualityIcon))
            }
            
            // Legacy quality badges for backward compatibility (only if not covered by qualityLabel)
            if (info.qualityLabel == "Unknown") {
                if (info.isLossless) {
                    add(MetadataItem("Quality", "Lossless", Icons.Rounded.HighQuality))
                }
                if (info.isDolby) {
                    add(MetadataItem("Audio Tech", "Dolby", Icons.Rounded.SurroundSound))
                }
                if (info.isDTS) {
                    add(MetadataItem("Audio Tech", "DTS", Icons.Rounded.SurroundSound))
                }
                if (info.isHiRes && !info.isLossless) {
                    add(MetadataItem("Quality", "Hi-Res", Icons.Rounded.HighQuality))
                }
            }
            
            // Audio quality info
            if (info.bitDepth > 0) {
                add(MetadataItem("Bit Depth", "${info.bitDepth}-bit", Icons.Rounded.HighQuality))
            }
            if (info.bitrate != "Unknown") {
                add(MetadataItem("Bitrate", info.bitrate, Icons.Rounded.GraphicEq))
            }
            if (info.sampleRate != "Unknown") {
                add(MetadataItem("Sample Rate", info.sampleRate, Icons.Rounded.Tune))
            }
            if (info.channels != "Unknown") {
                add(MetadataItem("Channels", info.channels, Icons.Rounded.SettingsInputComponent))
            }
            if (info.formatName != "Unknown") {
                add(MetadataItem("Format", info.formatName, Icons.Rounded.MusicNote))
            } else if (info.format != "Unknown") {
                add(MetadataItem("Format", info.format, Icons.Rounded.MusicNote))
            }
            
            // File info
            if (info.fileSize > 0) {
                add(MetadataItem("File Size", formatFileSize(info.fileSize), Icons.Rounded.FolderOpen))
            }
            
            // Additional metadata (non-duplicating)
            if (info.composer.isNotEmpty() && info.composer != song.artist) {
                add(MetadataItem("Composer", info.composer, Icons.Rounded.EditNote))
            }
            if (info.albumArtist.isNotEmpty() && info.albumArtist != song.artist) {
                add(MetadataItem("Album Artist", info.albumArtist, Icons.Rounded.Person))
            }
            if (info.hasLyrics) {
                add(MetadataItem("Lyrics", "Available", Icons.Rounded.Lyrics))
            }
            if (info.mimeType.isNotEmpty()) {
                add(MetadataItem("MIME Type", info.mimeType.substringAfter("/").uppercase(), Icons.Rounded.Code))
            }
            
            // Date info
            if (info.dateAdded > 0) {
                add(MetadataItem("Date Added", formatDate(info.dateAdded), Icons.Rounded.Add))
            }
            if (info.dateModified > 0 && info.dateModified != info.dateAdded) {
                add(MetadataItem("Modified", formatDate(info.dateModified), Icons.Rounded.Update))
            }
        }
    }
    
    // Always show the card, but show loader when loading or no metadata
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = context.getString(R.string.bottomsheet_metadata),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                // Show loader while loading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SimpleCircularLoader(
                        size = 32.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (metadataItems.isNotEmpty()) {
                // Show metadata grid with staggered item animations
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(((metadataItems.size / 2 + metadataItems.size % 2) * 80).dp)
                ) {
                    itemsIndexed(
                        items = metadataItems,
                        span = { index, item -> if (index == metadataItems.lastIndex && metadataItems.size % 2 == 1) GridItemSpan(2) else GridItemSpan(1) }
                    ) { index, item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 500,
                                    delayMillis = 400 + (index * 100)
                                )
                            ) + slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 500,
                                    delayMillis = 400 + (index * 100)
                                ),
                                initialOffsetY = { it / 5 }
                            )
                        ) {
                            MetadataGridItem(item = item)
                        }
                    }
                }
            } else {
                // Show empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.song_info_no_metadata),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataGridItem(
    item: MetadataItem
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSongSheet(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String, genre: String, year: Int, trackNumber: Int) -> Unit,
    onShowLyricsEditor: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Detect tablet mode
    val isTablet = configuration.screenWidthDp >= 600
    
    // Store original values for undo functionality
    val originalTitle by remember { mutableStateOf(song.title) }
    val originalArtist by remember { mutableStateOf(song.artist) }
    val originalAlbum by remember { mutableStateOf(song.album) }
    val originalGenre by remember { mutableStateOf(song.genre ?: "") }
    val originalYear by remember { mutableStateOf(if (song.year > 0) song.year.toString() else "") }
    val originalTrackNumber by remember { mutableStateOf(if (song.trackNumber > 0) song.trackNumber.toString() else "") }
    
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var genre by remember { mutableStateOf(song.genre ?: "") }
    var year by remember { mutableStateOf(if (song.year > 0) song.year.toString() else "") }
    var trackNumber by remember { mutableStateOf(if (song.trackNumber > 0) song.trackNumber.toString() else "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val haptics = LocalHapticFeedback.current
    var showWarningDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    
    // Animation effect
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        showContent = true
    }
    
    // Function to reset all fields to original values
    val resetToOriginal = {
        title = originalTitle
        artist = originalArtist
        album = originalAlbum
        genre = originalGenre
        year = originalYear
        trackNumber = originalTrackNumber
        selectedImageUri = null
        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
    }
    
    // Helper function to proceed with save after permissions are granted
    val proceedWithSave = { 
        val yearInt = year.toIntOrNull() ?: 0
        val trackInt = trackNumber.toIntOrNull() ?: 0
        
        // Pass the basic metadata to the save callback
        onSave(title.trim(), artist.trim(), album.trim(), genre.trim(), yearInt, trackInt)
        
        // Reset saving state after initiating save
        isSaving = false
        
        // TODO: Handle artwork saving separately if selectedImageUri is not null
        if (selectedImageUri != null) {
            Toast.makeText(context, "Artwork editing coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launchers for different scenarios
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            proceedWithSave()
        } else {
            isSaving = false
            Toast.makeText(
                context, 
                "Storage permission is required to edit audio file metadata", 
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Multiple permissions launcher for Android 13+
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            proceedWithSave()
        } else {
            isSaving = false
            Toast.makeText(
                context,
                "Media permissions are required to edit audio file metadata",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }
    
    // Function to handle save with permission checks
    fun handleSave() {
        if (isSaving) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        showWarningDialog = true
    }
    
    fun proceedAfterWarning() {
        isSaving = true
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ - Check if audio permission is already granted
                val hasAudioPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                
                if (hasAudioPermission) {
                    proceedWithSave()
                } else {
                    // Request only audio permission (images are optional)
                    multiplePermissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_MEDIA_AUDIO
                        )
                    )
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11-12 - Use scoped storage (no special permissions needed for MediaStore)
                proceedWithSave()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10 - Scoped storage but may need some permissions
                proceedWithSave()
            }
            else -> {
                // Android 9 and below - Request write permission
                val hasWritePermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                
                if (hasWritePermission) {
                    proceedWithSave()
                } else {
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    if (isTablet) {
        // Tablet layout: Dialog with side-by-side layout
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left side: Artwork editing
                        Surface(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight(),
                            color = Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 24.dp, start = 32.dp, end = 16.dp, bottom = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Artwork display
                                Box(
                                    modifier = Modifier
                                        .size(220.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(selectedImageUri ?: song.artworkUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Album artwork",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Show fallback icon if no image
                                    if (selectedImageUri == null && (song.artworkUri == null || song.artworkUri.toString().isEmpty())) {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(100.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Artwork action buttons
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Change artwork button
                                    Button(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Change Artwork")
                                    }

                                    // Reset button
                                    FilledTonalButton(
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            resetToOriginal()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.RestartAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Reset All")
                                    }
                                }
                            }
                        }

                        // Right side: Form fields
                        Surface(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight(),
                            color = Color.Transparent
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Header with close button
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = context.getString(R.string.edit_metadata),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Close button
                                        IconButton(
                                            onClick = onDismiss,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Close",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Form fields
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    tonalElevation = 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Title field
                                        OutlinedTextField(
                                            value = title,
                                            onValueChange = { title = it },
                                            label = { Text("Title") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.MusicNote,
                                                    contentDescription = null
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            singleLine = true
                                        )

                                        // Artist field
                                        OutlinedTextField(
                                            value = artist,
                                            onValueChange = { artist = it },
                                            label = { Text("Artist") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Person,
                                                    contentDescription = null
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            singleLine = true
                                        )

                                        // Album field
                                        OutlinedTextField(
                                            value = album,
                                            onValueChange = { album = it },
                                            label = { Text("Album") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Album,
                                                    contentDescription = null
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            singleLine = true
                                        )

                                        // Genre field
                                        OutlinedTextField(
                                            value = genre,
                                            onValueChange = { genre = it },
                                            label = { Text("Genre") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Category,
                                                    contentDescription = null
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            singleLine = true
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Year field
                                            OutlinedTextField(
                                                value = year,
                                                onValueChange = { year = it },
                                                label = { Text("Year") },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DateRange,
                                                        contentDescription = null
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(16.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )

                                            // Track number field
                                            OutlinedTextField(
                                                value = trackNumber,
                                                onValueChange = { trackNumber = it },
                                                label = { Text("Track") },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Rounded.FormatListNumbered,
                                                        contentDescription = null
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(16.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Action buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = onDismiss,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text("Cancel")
                                            }

                                            Button(
                                                onClick = { handleSave() },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(16.dp),
                                                enabled = title.isNotBlank() && artist.isNotBlank() && !isSaving
                                            ) {
                                                if (isSaving) {
                                                    SimpleCircularLoader(
                                                        size = 16.dp,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Saving...")
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Save,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Save")
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Warning Dialog for tablet
        if (showWarningDialog) {
            AlertDialog(
                onDismissRequest = { showWarningDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Irreversible Changes")
                    }
                },
                text = {
                    Column {
                        Text(
                            "The changes you're about to make will permanently modify the audio file's metadata.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This action cannot be undone. Make sure you have a backup if needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showWarningDialog = false
                            proceedAfterWarning()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Proceed")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { 
                            showWarningDialog = false
                            isSaving = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    } else {
        // Phone layout: Bottom sheet
        ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = context.getString(R.string.edit_metadata),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            
            // Artwork section - clean and simple
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Artwork display with rounded corners
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(selectedImageUri ?: song.artworkUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Show fallback icon if no image
                    if (selectedImageUri == null && (song.artworkUri == null || song.artworkUri.toString().isEmpty())) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }
                
                // Artwork action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Change artwork button
                    FilledTonalButton(
                        onClick = { 
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change")
                    }
                    
                    // Reset button
                    OutlinedButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            resetToOriginal()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset")
                    }
                    
                    // Edit Lyrics button
//                    FilledTonalButton(
//                        onClick = {
//                            onShowLyricsEditor?.invoke()
//                        },
//                        modifier = Modifier.weight(1f),
//                        shape = RoundedCornerShape(16.dp),
//                        colors = ButtonDefaults.filledTonalButtonColors(
//                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
//                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
//                        )
//                    ) {
//                        Icon(
//                            imageVector = Icons.Rounded.Edit,
//                            contentDescription = null,
//                            modifier = Modifier.size(18.dp)
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Lyrics")
//                    }
                }
                
                // Remove artwork button (only show if artwork is selected)
                if (selectedImageUri != null) {
                    OutlinedButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Selected Image")
                    }
                }
            }
            
            // Metadata Fields Section
            Text(
                text = context.getString(R.string.song_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            // Artist field
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artist") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            // Album field
            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text("Album") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            // Genre field
            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("Genre") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Category,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            // Year and Track Number in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Year field
                OutlinedTextField(
                    value = year,
                    onValueChange = { input ->
                        // Only allow digits and limit to 4 characters
                        if (input.all { it.isDigit() } && input.length <= 4) {
                            year = input
                        }
                    },
                    label = { Text("Year") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DateRange,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                // Track Number field
                OutlinedTextField(
                    value = trackNumber,
                    onValueChange = { input ->
                        // Only allow digits and limit to 3 characters
                        if (input.all { it.isDigit() } && input.length <= 3) {
                            trackNumber = input
                        }
                    },
                    label = { Text("Track") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.FormatListNumbered,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { handleSave() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = title.isNotBlank() && artist.isNotBlank() && !isSaving
                    ) {
                        if (isSaving) {
                            SimpleCircularLoader(
                                size = 16.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Changes")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    
    // Warning Dialog for phone layout
    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Irreversible Changes")
                }
            },
            text = {
                Column {
                    Text(
                        "The changes you're about to make will permanently modify the audio file's metadata.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone. Make sure you have a backup if needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWarningDialog = false
                        proceedAfterWarning()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Proceed")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showWarningDialog = false
                        isSaving = false  // Reset saving state when user cancels
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
    }
}

// Data classes
data class MetadataItem(
    val label: String,
    val value: String,
    val icon: ImageVector
)

// Helper functions
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}

private fun formatDate(timestamp: Long): String {
    return if (timestamp > 0) {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        formatter.format(date)
    } else {
        "Unknown"
    }
}
