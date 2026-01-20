package chromahub.rhythm.app.features.local.presentation.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.features.local.presentation.components.player.MiniPlayer
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons.Search
import chromahub.rhythm.app.ui.LocalMiniPlayerPadding
import chromahub.rhythm.app.ui.UiConstants
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.features.local.presentation.components.dialogs.PlaylistExportDialog
import chromahub.rhythm.app.features.local.presentation.components.dialogs.PlaylistImportDialog
import chromahub.rhythm.app.features.local.presentation.components.dialogs.PlaylistOperationProgressDialog
import chromahub.rhythm.app.features.local.presentation.components.dialogs.PlaylistOperationResultDialog
import chromahub.rhythm.app.util.PlaylistImportExportUtils
import android.net.Uri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.util.ImageUtils
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.features.local.presentation.components.player.formatDuration
import kotlinx.coroutines.delay // Import delay
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.runtime.collectAsState
import chromahub.rhythm.app.features.local.presentation.components.player.PlayingEqIcon
import androidx.compose.ui.graphics.Color
import androidx.room.util.copy

// Playlist sort order enum
enum class PlaylistSortOrder {
    TITLE_ASC, TITLE_DESC,
    ARTIST_ASC, ARTIST_DESC,
    ALBUM_ASC, ALBUM_DESC,
    DURATION_ASC, DURATION_DESC,
    DATE_ADDED_ASC, DATE_ADDED_DESC
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onPlayerClick: () -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit = {},
    onSongClick: (Song) -> Unit,
    onPlaySongFromPlaylist: ((Song, List<Song>) -> Unit)? = null,
    onBack: () -> Unit,
    onRemoveSong: (Song, String) -> Unit = { _, _ -> },
    onRenamePlaylist: (String) -> Unit = {},
    onDeletePlaylist: () -> Unit = {},
    onAddSongsToPlaylist: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onExportPlaylist: ((PlaylistImportExportUtils.PlaylistExportFormat) -> Unit)? = null,
    onExportPlaylistToCustomLocation: ((PlaylistImportExportUtils.PlaylistExportFormat, Uri) -> Unit)? = null,
    onImportPlaylist: ((Uri, (Result<String>) -> Unit, (() -> Unit)?) -> Unit)? = null,
    onReorderSongs: ((Int, Int) -> Unit)? = null,
    onUpdatePlaylistSongs: ((List<Song>) -> Unit)? = null
) {
    // Screen size detection for responsive UI
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isExtraSmallWidth = screenWidthDp < 360
    val isCompactWidth = screenWidthDp < 400
    val isMidWidth = screenWidthDp in 400..499
    val isTablet = screenWidthDp >= 600
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isLandscapeTablet = isTablet && isLandscape
    val isCompactHeight = screenHeightDp < 600
    val isLargeHeight = screenHeightDp > 800

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showOperationProgress by remember { mutableStateOf(false) }
    var operationInProgress by remember { mutableStateOf("") }
    var operationResult by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var newPlaylistName by remember { mutableStateOf(playlist.name) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var showQueueOptionsDialog by remember { mutableStateOf(false) }
    var selectedSongForQueue by remember { mutableStateOf<Song?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Multi-select mode state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedSongs by remember { mutableStateOf(setOf<String>()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    
    // Track current sort order for playlist
    var currentPlaylistSort by remember { mutableStateOf(PlaylistSortOrder.TITLE_ASC) }

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val appSettings = remember { chromahub.rhythm.app.shared.data.model.AppSettings.getInstance(context) }
    val playlistClickBehavior by appSettings.playlistClickBehavior.collectAsState(initial = "ask")
    val useHoursFormat by appSettings.useHoursInTimeFormat.collectAsState()

    // Queue Options Dialog - matches app-wide dialog design
    if (showQueueOptionsDialog && selectedSongForQueue != null) {
        AlertDialog(
            onDismissRequest = { 
                showQueueOptionsDialog = false
                selectedSongForQueue = null
            },
            icon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { 
                Text(
                    "Play from Playlist",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Song info
                    Text(
                        selectedSongForQueue!!.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${selectedSongForQueue!!.artist} • ${playlist.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Option 1: Load Playlist & Play
                    Surface(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            onPlaySongFromPlaylist?.invoke(selectedSongForQueue!!, playlist.songs)
                            showQueueOptionsDialog = false
                            selectedSongForQueue = null
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Load Playlist & Play",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Replace queue with playlist",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    // Option 2: Play This Song Only
                    Surface(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            onSongClick(selectedSongForQueue!!)
                            showQueueOptionsDialog = false
                            selectedSongForQueue = null
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = RhythmIcons.Play,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Play This Song Only",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Don't change the queue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                        showQueueOptionsDialog = false
                        selectedSongForQueue = null
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
    
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(context.getString(R.string.playlist_rename_title)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(context.getString(R.string.playlist_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                        onRenamePlaylist(newPlaylistName)
                        showRenameDialog = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                    showRenameDialog = false
                }) {
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(context.getString(R.string.playlist_delete_title)) },
            text = { Text(context.getString(R.string.dialog_delete_playlist_message, playlist.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                        onDeletePlaylist()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                    showDeleteDialog = false
                }) {
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

    // Bulk delete dialog
    if (showBulkDeleteDialog && selectedSongs.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remove ${selectedSongs.size} Songs") },
            text = { Text("Are you sure you want to remove ${selectedSongs.size} song${if (selectedSongs.size > 1) "s" else ""} from this playlist?") },
            confirmButton = {
                Button(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                        // Remove selected songs
                        selectedSongs.forEach { songId ->
                            playlist.songs.find { it.id == songId }?.let { song ->
                                onRemoveSong(song, "Song removed from playlist")
                            }
                        }
                        selectedSongs = emptySet()
                        isMultiSelectMode = false
                        showBulkDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                    showBulkDeleteDialog = false
                }) {
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

    // Export dialog
    if (showExportDialog && (onExportPlaylist != null || onExportPlaylistToCustomLocation != null)) {
        PlaylistExportDialog(
            playlistName = playlist.name,
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                operationInProgress = "Exporting"
                showOperationProgress = true
                onExportPlaylist?.invoke(format)
                showExportDialog = false
            },
            onExportToCustomLocation = { format, directoryUri ->
                operationInProgress = "Exporting"
                showOperationProgress = true
                onExportPlaylistToCustomLocation?.invoke(format, directoryUri)
                showExportDialog = false
            }
        )
    }
    
    // Import dialog
    if (showImportDialog && onImportPlaylist != null) {
        PlaylistImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { uri, onResult, onRestartRequired ->
                operationInProgress = "Importing"
                showOperationProgress = true
                onImportPlaylist(uri, onResult, onRestartRequired)
                showImportDialog = false
            }
        )
    }
    
    // Operation progress dialog
    if (showOperationProgress) {
        PlaylistOperationProgressDialog(
            operation = operationInProgress,
            onDismiss = { /* Cannot dismiss during operation */ }
        )
    }
    
    // Operation result dialog
    operationResult?.let { (message, isError) ->
        PlaylistOperationResultDialog(
            title = if (isError) context.getString(R.string.playlist_operation_failed) else context.getString(R.string.playlist_operation_complete),
            message = message,
            isError = isError,
            onDismiss = { operationResult = null }
        )
    }

    CollapsibleHeaderScreen(
        title = playlist.name,
        showBackButton = true,
        onBackClick = {
            if (showSearchBar) {
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                showSearchBar = false
                searchQuery = ""
            } else {
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                onBack()
            }
        },
        actions = {
            // Sort button (only show if sorting is available)
            val isDefault = playlist.id == "1" || playlist.id == "2" || playlist.id == "3"
            if (isDefault || (onUpdatePlaylistSongs != null && playlist.songs.size > 1)) {
                val sortButtonScale by animateFloatAsState(
                    targetValue = if (showSortMenu) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "sortButtonScale"
                )
                
                FilledTonalButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        showSortMenu = true
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.graphicsLayer {
                        scaleX = sortButtonScale
                        scaleY = sortButtonScale
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Sort order text
                    val sortText = when (currentPlaylistSort) {
                        PlaylistSortOrder.TITLE_ASC, PlaylistSortOrder.TITLE_DESC -> "Title"
                        PlaylistSortOrder.ARTIST_ASC, PlaylistSortOrder.ARTIST_DESC -> "Artist"
                        PlaylistSortOrder.ALBUM_ASC, PlaylistSortOrder.ALBUM_DESC -> "Album"
                        PlaylistSortOrder.DURATION_ASC, PlaylistSortOrder.DURATION_DESC -> "Duration"
                        PlaylistSortOrder.DATE_ADDED_ASC, PlaylistSortOrder.DATE_ADDED_DESC -> "Date Added"
                    }

                    Text(
                        text = sortText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    val sortArrowIcon = when (currentPlaylistSort) {
                        PlaylistSortOrder.TITLE_ASC, PlaylistSortOrder.ARTIST_ASC, PlaylistSortOrder.ALBUM_ASC, 
                        PlaylistSortOrder.DURATION_ASC, PlaylistSortOrder.DATE_ADDED_ASC -> Icons.Default.ArrowUpward
                        else -> Icons.Default.ArrowDownward
                    }
                    
                    Icon(
                        imageVector = sortArrowIcon,
                        contentDescription = if (currentPlaylistSort.name.endsWith("_ASC")) "Ascending" else "Descending",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            if (true) {
                FilledIconButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                        showMenu = true
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = RhythmIcons.More,
                        contentDescription = context.getString(R.string.playlist_more_options),
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .widthIn(min = 220.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(5.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    // Reorder songs option
                    if (isDefault || (onReorderSongs != null && playlist.songs.isNotEmpty())) {
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
                                        if (isReorderMode) context.getString(R.string.playlist_done_reordering) else context.getString(R.string.playlist_reorder_songs),
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
                                            imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.Reorder,
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
                                    isReorderMode = !isReorderMode
                                    // Exit multi-select mode when entering reorder mode
                                    if (isReorderMode) {
                                        isMultiSelectMode = false
                                        selectedSongs = emptySet()
                                    }
                                }
                            )
                        }
                    }
                    
                    // Select songs option (multi-select mode)
                    if (!isDefault && playlist.songs.isNotEmpty()) {
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
                                        if (isMultiSelectMode) "Cancel selection" else "Select songs",
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
                                            imageVector = if (isMultiSelectMode) Icons.Default.Close else Icons.Default.CheckBox,
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
                                    isMultiSelectMode = !isMultiSelectMode
                                    // Exit reorder mode when entering multi-select mode
                                    if (isMultiSelectMode) {
                                        isReorderMode = false
                                    } else {
                                        selectedSongs = emptySet()
                                    }
                                }
                            )
                        }
                    }
                    
                    // Export playlist option
                    if (!isDefault && (onExportPlaylist != null || onExportPlaylistToCustomLocation != null)) {
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
                                        "Export playlist",
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
                                            imageVector = Icons.Default.FileUpload,
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
                                    showExportDialog = true
                                }
                            )
                        }
                    }
                    
                    // Import playlist option
                    if (!isDefault && onImportPlaylist != null) {
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
                                        "Import playlist",
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
                                            imageVector = RhythmIcons.Actions.Download,
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
                                    showImportDialog = true
                                }
                            )
                        }
                    }
                    
                    // Rename playlist option
                    if (!isDefault) {
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
                                        "Rename playlist",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    showMenu = false
                                    newPlaylistName = playlist.name
                                    showRenameDialog = true
                                }
                            )
                        }
                    }
                    
                    // Delete playlist option
                    if (!isDefault) {
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
                                        "Delete playlist",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
                
                // Sort menu dropdown
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(4.dp)
                ) {
                    PlaylistSortOrder.values().forEach { sortOrder ->
                        val isSelected = currentPlaylistSort == sortOrder
                        Surface(
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            else 
                                Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = when (sortOrder) {
                                            PlaylistSortOrder.TITLE_ASC, PlaylistSortOrder.TITLE_DESC -> "Title"
                                            PlaylistSortOrder.ARTIST_ASC, PlaylistSortOrder.ARTIST_DESC -> "Artist"
                                            PlaylistSortOrder.ALBUM_ASC, PlaylistSortOrder.ALBUM_DESC -> "Album"
                                            PlaylistSortOrder.DURATION_ASC, PlaylistSortOrder.DURATION_DESC -> "Duration"
                                            PlaylistSortOrder.DATE_ADDED_ASC, PlaylistSortOrder.DATE_ADDED_DESC -> "Date Added"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (sortOrder) {
                                            PlaylistSortOrder.TITLE_ASC, PlaylistSortOrder.TITLE_DESC -> Icons.Filled.SortByAlpha
                                            PlaylistSortOrder.ARTIST_ASC, PlaylistSortOrder.ARTIST_DESC -> Icons.Filled.Person
                                            PlaylistSortOrder.ALBUM_ASC, PlaylistSortOrder.ALBUM_DESC -> RhythmIcons.Music.Album
                                            PlaylistSortOrder.DURATION_ASC, PlaylistSortOrder.DURATION_DESC -> Icons.Filled.Timer
                                            PlaylistSortOrder.DATE_ADDED_ASC, PlaylistSortOrder.DATE_ADDED_DESC -> Icons.Filled.DateRange
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    when (sortOrder) {
                                        PlaylistSortOrder.TITLE_ASC, PlaylistSortOrder.ARTIST_ASC, PlaylistSortOrder.ALBUM_ASC, 
                                        PlaylistSortOrder.DURATION_ASC, PlaylistSortOrder.DATE_ADDED_ASC -> {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "Ascending",
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "Descending",
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    currentPlaylistSort = sortOrder
                                    showSortMenu = false
                                    val sortedSongs = when (sortOrder) {
                                        PlaylistSortOrder.TITLE_ASC -> playlist.songs.sortedBy { it.title.lowercase() }
                                        PlaylistSortOrder.TITLE_DESC -> playlist.songs.sortedByDescending { it.title.lowercase() }
                                        PlaylistSortOrder.ARTIST_ASC -> playlist.songs.sortedBy { it.artist.lowercase() }
                                        PlaylistSortOrder.ARTIST_DESC -> playlist.songs.sortedByDescending { it.artist.lowercase() }
                                        PlaylistSortOrder.ALBUM_ASC -> playlist.songs.sortedBy { it.album.lowercase() }
                                        PlaylistSortOrder.ALBUM_DESC -> playlist.songs.sortedByDescending { it.album.lowercase() }
                                        PlaylistSortOrder.DURATION_ASC -> playlist.songs.sortedBy { it.duration }
                                        PlaylistSortOrder.DURATION_DESC -> playlist.songs.sortedByDescending { it.duration }
                                        PlaylistSortOrder.DATE_ADDED_ASC -> playlist.songs.sortedBy { it.dateAdded }
                                        PlaylistSortOrder.DATE_ADDED_DESC -> playlist.songs.sortedByDescending { it.dateAdded }
                                    }
                                    onUpdatePlaylistSongs?.invoke(sortedSongs)
                                },
                                colors = androidx.compose.material3.MenuDefaults.itemColors(
                                    textColor = if (isSelected) 
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { modifier ->
        if (isTablet && !isCompactHeight) {
            // Tablet split-view layout: Left side (art + controls), Right side (song list)
            Row(modifier = modifier.fillMaxSize()) {
                // Left Column: Playlist Art and Controls
                Column(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 48.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val context = LocalContext.current

                    // Playlist artwork
                    val playlistArtSize = 180.dp
                    Surface(
                        modifier = Modifier.size(playlistArtSize),
                        shape = RoundedCornerShape(32.dp),
                        tonalElevation = 8.dp,
                        shadowElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (playlist.artworkUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .apply(ImageUtils.buildImageRequest(
                                            playlist.artworkUri,
                                            playlist.name,
                                            context.cacheDir,
                                            M3PlaceholderType.PLAYLIST
                                        ))
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(32.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.PlaylistFilled,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(90.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Playlist info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Action buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (playlist.songs.isNotEmpty()) {
                                // Play All button
                                Button(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                        onPlayAll()
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Play,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Play All",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                // Shuffle button
                                FilledTonalButton(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                        onShufflePlay()
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Shuffle,
                                        contentDescription = "Shuffle play",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Shuffle",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            // Add Songs button
                            FilledTonalButton(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    onAddSongsToPlaylist()
                                },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Add Songs",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            // Search button
                            FilledTonalButton(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    showSearchBar = !showSearchBar
                                    if (!showSearchBar) {
                                        searchQuery = ""
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (showSearchBar)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = if (showSearchBar)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (showSearchBar) "Searching" else "Search",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                // Right Column: Song List with Search
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    val filteredSongs = remember(playlist.songs, searchQuery) {
                        if (searchQuery.isBlank()) {
                            playlist.songs
                        } else {
                            playlist.songs.filter { song ->
                                song.title.contains(searchQuery, ignoreCase = true) ||
                                        song.artist.contains(searchQuery, ignoreCase = true) ||
                                        song.album.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }

                    val listState = rememberLazyListState()

                    LaunchedEffect(showSearchBar) {
                        if (showSearchBar) {
                            listState.animateScrollToItem(0)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 20.dp
                        )
                    ) {
                        // Search field for tablet
                        if (showSearchBar) {
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Search songs") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                searchQuery = ""
                                            }) {
                                                Icon(
                                                    imageVector = RhythmIcons.Close,
                                                    contentDescription = "Clear search"
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Section header
                        if (filteredSongs.isNotEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = context.getString(R.string.common_songs),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${filteredSongs.size}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // Empty state
                        if (filteredSongs.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        modifier = Modifier.size(80.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        tonalElevation = 4.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = RhythmIcons.MusicNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = if (searchQuery.isNotEmpty()) context.getString(R.string.nav_no_matching_songs) else context.getString(R.string.playlist_no_songs_yet),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Song items
                            itemsIndexed(filteredSongs, key = { index, song -> "${song.id}-$index" }) { index, song ->
                                AnimateIn {
                                    PlaylistSongItem(
                                        song = song,
                                        onClick = {
                                            if (isMultiSelectMode) {
                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                selectedSongs = if (selectedSongs.contains(song.id)) {
                                                    selectedSongs - song.id
                                                } else {
                                                    selectedSongs + song.id
                                                }
                                                return@PlaylistSongItem
                                            }
                                            if (isReorderMode) {
                                                return@PlaylistSongItem
                                            }
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                            when (playlistClickBehavior) {
                                                "play_all" -> {
                                                    onPlaySongFromPlaylist?.invoke(song, playlist.songs) ?: onSongClick(song)
                                                }
                                                "play_one" -> {
                                                    onSongClick(song)
                                                }
                                                else -> {
                                                    selectedSongForQueue = song
                                                    showQueueOptionsDialog = true
                                                }
                                            }
                                        },
                                        onRemove = if (isReorderMode || isMultiSelectMode) null else { message -> onRemoveSong(song, message) },
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        useHoursFormat = useHoursFormat,
                                        isReorderMode = isReorderMode,
                                        index = index,
                                        totalCount = filteredSongs.size,
                                        onMoveUp = if (isReorderMode && index > 0) {
                                            {
                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                onReorderSongs?.invoke(index, index - 1)
                                            }
                                        } else null,
                                        onMoveDown = if (isReorderMode && index < filteredSongs.size - 1) {
                                            {
                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                onReorderSongs?.invoke(index, index + 1)
                                            }
                                        } else null,
                                        isMultiSelectMode = isMultiSelectMode,
                                        isSelected = selectedSongs.contains(song.id)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Phone/Compact layout: Original vertical layout
            Box(modifier = modifier.fillMaxSize()) {
            val filteredSongs = remember(playlist.songs, searchQuery) {
                if (searchQuery.isBlank()) {
                    playlist.songs
                } else {
                    playlist.songs.filter { song ->
                        song.title.contains(searchQuery, ignoreCase = true) ||
                                song.artist.contains(searchQuery, ignoreCase = true) ||
                                song.album.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

            val listState = rememberLazyListState()

            LaunchedEffect(showSearchBar) {
                if (showSearchBar) {
                    listState.animateScrollToItem(1) // Scroll to the top to show the search bar
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    bottom = (LocalMiniPlayerPadding.current.calculateBottomPadding() + 20.dp).coerceAtLeast(120.dp)
                )
            ) {
            item { // Wrap playlist header in an item
                // Enhanced Playlist header with better visual hierarchy
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val context = LocalContext.current

                        // Enhanced Playlist artwork without shadows as requested - responsive sizing
                        val playlistArtSize = when {
                            isExtraSmallWidth -> 120.dp
                            isCompactWidth -> 140.dp
                            isMidWidth -> 150.dp
                            else -> 160.dp
                        }
                        val artworkCornerRadius = when {
                            isExtraSmallWidth -> 28.dp
                            isCompactWidth -> 30.dp
                            else -> 34.dp
                        }
                        
                        Surface(
                            modifier = Modifier.size(playlistArtSize),
                            shape = RoundedCornerShape(artworkCornerRadius),
                            tonalElevation = 8.dp, 
                            shadowElevation = 0.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                if (playlist.artworkUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .apply(ImageUtils.buildImageRequest(
                                                playlist.artworkUri,
                                                playlist.name,
                                                context.cacheDir,
                                                M3PlaceholderType.PLAYLIST
                                            ))
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val iconSize = when {
                                        isExtraSmallWidth -> 60.dp
                                        isCompactWidth -> 70.dp
                                        isMidWidth -> 75.dp
                                        else -> 84.dp
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(artworkCornerRadius)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.PlaylistFilled,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(iconSize)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Playlist info section
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Song count with improved typography
                            // Text(
                            //     text = if (playlist.songs.size == 1) "1 song" else "${playlist.songs.size} songs",
                            //     style = MaterialTheme.typography.titleMedium,
                            //     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            // )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Redesigned action buttons with modern UI
                            if (playlist.songs.isNotEmpty()) {
                                // Main action row with improved spacing
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    // Play All button - primary action, more prominent
                                    Button(
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            onPlayAll()
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        contentPadding = PaddingValues(
                                            horizontal = when {
                                                isExtraSmallWidth -> 14.dp
                                                isCompactWidth -> 18.dp
                                                else -> 22.dp
                                            },
                                            vertical = 14.dp
                                        ),
                                        modifier = Modifier
                                            .weight(0.7f)
                                            .height(when {
                                                isExtraSmallWidth -> 44.dp
                                                isCompactWidth -> 48.dp
                                                else -> 52.dp
                                            })
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Play,
                                            contentDescription = null,
                                            modifier = Modifier.size(when {
                                                isExtraSmallWidth -> 18.dp
                                                isCompactWidth -> 20.dp
                                                else -> 22.dp
                                            })
                                        )
                                        Spacer(modifier = Modifier.width(when {
                                            isExtraSmallWidth -> 4.dp
                                            else -> 6.dp
                                        }))
                                        Text(
                                            text = when {
                                                isExtraSmallWidth -> "Play"
                                                else -> "Play All"
                                            },
                                            style = when {
                                                isExtraSmallWidth -> MaterialTheme.typography.labelLarge
                                                else -> MaterialTheme.typography.titleSmall
                                            },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Shuffle button - secondary action with improved styling
                                    FilledTonalButton(
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            onShufflePlay()
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        contentPadding = PaddingValues(12.dp),
                                        modifier = Modifier.size(when {
                                            isExtraSmallWidth -> 44.dp
                                            isCompactWidth -> 48.dp
                                            else -> 52.dp
                                        })
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Shuffle,
                                            contentDescription = "Shuffle play",
                                            modifier = Modifier.size(when {
                                                isExtraSmallWidth -> 20.dp
                                                isCompactWidth -> 22.dp
                                                else -> 24.dp
                                            })
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showSearchBar) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search songs") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp), // Added rounded corners
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                        searchQuery = ""
                                    }) {
                                        Icon(
                                            imageVector = RhythmIcons.Close,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Section header for songs list
                if (filteredSongs.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = context.getString(R.string.common_songs),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Text(
                                    text = "${filteredSongs.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Songs list
                if (filteredSongs.isEmpty()) {
                    item { // Enhanced empty state with better visual design
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Empty state icon
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 4.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Empty state text
                            Text(
                                text = if (searchQuery.isNotEmpty()) context.getString(R.string.nav_no_matching_songs) else context.getString(R.string.playlist_no_songs_yet),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (searchQuery.isNotEmpty()) "Try a different search query" else "Start building your playlist by adding some songs",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Call-to-action button
                            Button(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    onAddSongsToPlaylist()
                                },
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Add Songs",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                } else {
                    // Multi-select mode banner
                    if (isMultiSelectMode) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                color = Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Select All button
                                        TextButton(
                                            onClick = {
                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                if (selectedSongs.size == filteredSongs.size) {
                                                    selectedSongs = emptySet()
                                                } else {
                                                    selectedSongs = filteredSongs.map { it.id }.toSet()
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selectedSongs.size == filteredSongs.size) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (selectedSongs.size == filteredSongs.size) "${selectedSongs.size} selected" else "${selectedSongs.size} selected"
                                            )
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {

                                        // Delete Selected button
                                        if (selectedSongs.isNotEmpty()) {
                                            Button(
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                                    showBulkDeleteDialog = true
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteSweep,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Remove")
                                            }
                                        }
                                        // Done button
                                        Button(
                                            onClick = {
                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                                isMultiSelectMode = false
                                                selectedSongs = emptySet()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Done")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Reorder mode banner
                    if (isReorderMode) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                color = Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Reorder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Reorder Songs",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // Done button
                                    Button(
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            isReorderMode = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Done")
                                    }
                                }
                            }
                        }
                    }
                    
                    itemsIndexed(filteredSongs, key = { index, song -> "${song.id}-$index" }) { index, song ->
                        AnimateIn {
                            PlaylistSongItem(
                                song = song,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        // Toggle selection
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                        selectedSongs = if (selectedSongs.contains(song.id)) {
                                            selectedSongs - song.id
                                        } else {
                                            selectedSongs + song.id
                                        }
                                        return@PlaylistSongItem
                                    }
                                    if (isReorderMode) {
                                        // Don't play in reorder mode
                                        return@PlaylistSongItem
                                    }
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    when (playlistClickBehavior) {
                                        "play_all" -> {
                                            // Load entire playlist and play from selected song
                                            onPlaySongFromPlaylist?.invoke(song, playlist.songs) ?: onSongClick(song)
                                        }
                                        "play_one" -> {
                                            // Play only this song
                                            onSongClick(song)
                                        }
                                        else -> {
                                            // "ask" - Show dialog
                                            selectedSongForQueue = song
                                            showQueueOptionsDialog = true
                                        }
                                    }
                                },
                                onRemove = if (isReorderMode || isMultiSelectMode) null else { message -> onRemoveSong(song, message) },
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                useHoursFormat = useHoursFormat,
                                isReorderMode = isReorderMode,
                                index = index,
                                totalCount = filteredSongs.size,
                                onMoveUp = if (isReorderMode && index > 0) {
                                    {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                        onReorderSongs?.invoke(index, index - 1)
                                    }
                                } else null,
                                onMoveDown = if (isReorderMode && index < filteredSongs.size - 1) {
                                    {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                        onReorderSongs?.invoke(index, index + 1)
                                    }
                                } else null,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelected = selectedSongs.contains(song.id)
                            )
                        }
                    }
                }
                item { // Extra bottom space for mini player
                    Spacer(modifier = Modifier.height(16.dp)) // Simple spacing
                }
            }
            
            // Bottom Floating Action Bar with Material 3 expressive design - only show when playlist has songs
            if (playlist.songs.isNotEmpty()) {
            val context = LocalContext.current
            val haptics = LocalHapticFeedback.current
            
            // Animate bar entrance
            val barAlpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, delayMillis = 200),
                label = "barAlpha"
            )
            
            val barOffset by animateDpAsState(
                targetValue = 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "barOffset"
            )
            
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = when {
                        isExtraSmallWidth -> 12.dp
                        isCompactWidth -> 16.dp
                        else -> 24.dp
                    })
                    .padding(bottom = if (LocalMiniPlayerPadding.current.calculateBottomPadding() > 0.dp) 40.dp else 44.dp)
                    .offset(y = barOffset)
                    .graphicsLayer { alpha = barAlpha },
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 8.dp,
                shadowElevation = 0.dp,
//                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search button
                    var searchPressed by remember { mutableStateOf(false) }
                    val searchScale by animateFloatAsState(
                        targetValue = if (searchPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
                        label = "searchScale"
                    )
                    
                    LaunchedEffect(searchPressed) {
                        if (searchPressed) {
                            delay(120)
                            searchPressed = false
                        }
                    }
                    
                    val buttonHeight = when {
                        isExtraSmallWidth -> 48.dp
                        isCompactWidth -> 52.dp
                        else -> 56.dp
                    }
                    val buttonIconSize = when {
                        isExtraSmallWidth -> 20.dp
                        isCompactWidth -> 22.dp
                        else -> 24.dp
                    }
                    val buttonTextStyle = when {
                        isExtraSmallWidth -> MaterialTheme.typography.labelSmall
                        isCompactWidth -> MaterialTheme.typography.labelMedium
                        else -> MaterialTheme.typography.labelLarge
                    }
                    
                    FilledTonalButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) {
                                searchQuery = ""
                            }
                            searchPressed = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight)
                            .graphicsLayer {
                                scaleX = searchScale
                                scaleY = searchScale
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (showSearchBar) 
                                MaterialTheme.colorScheme.primaryContainer
                            else 
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = if (showSearchBar) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Search,
                            contentDescription = "Toggle search",
                            modifier = Modifier.size(buttonIconSize)
                        )
                        if (!isExtraSmallWidth) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showSearchBar) "Searching" else "Search",
                                style = buttonTextStyle,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Add songs button
                    var addPressed by remember { mutableStateOf(false) }
                    val addScale by animateFloatAsState(
                        targetValue = if (addPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
                        label = "addScale"
                    )
                    
                    LaunchedEffect(addPressed) {
                        if (addPressed) {
                            delay(120)
                            addPressed = false
                        }
                    }
                    
                    Button(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            onAddSongsToPlaylist()
                            addPressed = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight)
                            .graphicsLayer {
                                scaleX = addScale
                                scaleY = addScale
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Add,
                            contentDescription = "Add songs to playlist",
                            modifier = Modifier.size(buttonIconSize)
                        )
                        if (!isExtraSmallWidth) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Songs",
                                style = buttonTextStyle,
                                fontWeight = FontWeight.Medium
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

@Composable
private fun AnimateIn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, delayMillis = 50),
        label = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = modifier.graphicsLayer(
            alpha = alpha,
            scaleX = scale,
            scaleY = scale
        )
    ) {
        content()
    }
}

@Composable
fun PlaylistSongItem(
    song: Song,
    onClick: () -> Unit,
    onRemove: ((String) -> Unit)? = null,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    useHoursFormat: Boolean = false,
    isReorderMode: Boolean = false,
    index: Int = 0,
    totalCount: Int = 0,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false
) {
    val context = LocalContext.current
    var showRemoveDialog by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current // Capture haptics here
    val isCurrentSong = currentSong?.id == song.id
    
    // Animated colors for current song
    val titleColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(300),
        label = "titleColor"
    )
    val artistColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        animationSpec = tween(300),
        label = "artistColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "containerColor"
    )
    
    // Remove confirmation dialog (only show if onRemove is provided)
    if (showRemoveDialog && onRemove != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.RemoveCircleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remove Song") },
            text = { Text("Remove '${song.title}' from this playlist?") },
            confirmButton = {
                Button(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove) // Use captured haptics
                        onRemove?.invoke("Removed ${song.title} from playlist")
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.RemoveCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove) // Use captured haptics
                    showRemoveDialog = false
                }) {
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
    
    // Update container color for selection
    val selectionContainerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) 
                      else if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) 
                      else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "selectionContainerColor"
    )
    
    Surface(
        onClick = onClick,
        color = selectionContainerColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp, // Remove shadow as requested
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for multi-select mode
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null, // Handled by Surface onClick
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            // Enhanced album art with better styling
            Box {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp,
                    border = if (isCurrentSong) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary) else null
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .apply(ImageUtils.buildImageRequest(
                                song.artworkUri,
                                song.title,
                                context.cacheDir,
                                M3PlaceholderType.TRACK
                            ))
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isCurrentSong && isPlaying) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .offset(x = 4.dp, y = 4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayingEqIcon(
                                modifier = Modifier.size(width = 12.dp, height = 10.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                isPlaying = isPlaying,
                                bars = 3
                            )
                        }
                    }
                }
            }
            
            // Enhanced song info with better typography
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = titleColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = artistColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (song.album.isNotEmpty() && song.album != song.artist) {
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Duration display (hide in reorder mode to make room for buttons)
            if (song.duration > 0 && !isReorderMode) {
                Text(
                    text = formatDuration(song.duration, useHoursFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            
            // Show reorder buttons or remove button depending on mode
            if (isReorderMode) {
                // Reorder buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Move up button
                    FilledIconButton(
                        onClick = { onMoveUp?.invoke() },
                        enabled = onMoveUp != null,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move up",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Move down button
                    FilledIconButton(
                        onClick = { onMoveDown?.invoke() },
                        enabled = onMoveDown != null,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move down",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else if (onRemove != null) {
                // Enhanced remove button with confirmation
                FilledIconButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove) // Use captured haptics
                        showRemoveDialog = true
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Remove,
                        contentDescription = "Remove from playlist",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
