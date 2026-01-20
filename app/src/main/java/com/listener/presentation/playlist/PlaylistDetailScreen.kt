package com.listener.presentation.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PlaylistItemEntity
import com.listener.presentation.components.EmptyState
import com.listener.presentation.components.ListenerButton
import com.listener.presentation.components.LoadingState
import com.listener.presentation.theme.ListenerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onNavigateBack: () -> Unit = {},
    onNavigateToPlayer: (sourceId: String) -> Unit = {},
    onNavigateToTranscription: (sourceId: String) -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val state = uiState) {
                            is PlaylistDetailUiState.Success -> state.playlist.name
                            else -> "Playlist"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Name") },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Playlist") },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is PlaylistDetailUiState.Loading -> {
                LoadingState(
                    modifier = Modifier.padding(paddingValues),
                    message = "Loading playlist..."
                )
            }
            is PlaylistDetailUiState.Error -> {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    title = "Error",
                    description = state.message,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is PlaylistDetailUiState.Success -> {
                if (state.items.isEmpty()) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                        title = "Playlist is Empty",
                        description = "Add episodes or audio files to this playlist",
                        actionLabel = "Add Content",
                        onAction = { /* Add content */ },
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    PlaylistDetailContent(
                        state = state,
                        onResumeClick = {
                            val firstIncomplete = state.items.firstOrNull { !it.isCompleted }
                            if (firstIncomplete != null) {
                                onNavigateToPlayer(firstIncomplete.playlistItem.sourceId)
                            }
                        },
                        onItemClick = { item ->
                            // Navigate based on whether transcription exists
                            onNavigateToPlayer(item.playlistItem.sourceId)
                        },
                        onMoveUp = { index ->
                            if (index > 0) {
                                viewModel.reorderItems(index, index - 1)
                            }
                        },
                        onMoveDown = { index ->
                            if (index < state.items.size - 1) {
                                viewModel.reorderItems(index, index + 1)
                            }
                        },
                        onRemoveItem = { item ->
                            viewModel.removeItem(item)
                        },
                        onReorder = { from, to ->
                            viewModel.reorderItems(from, to)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                // Edit Name Dialog
                if (showEditDialog) {
                    EditPlaylistNameDialog(
                        currentName = state.playlist.name,
                        onDismiss = { showEditDialog = false },
                        onConfirm = { newName ->
                            viewModel.updatePlaylistName(newName)
                            showEditDialog = false
                        }
                    )
                }

                // Delete Confirmation Dialog
                if (showDeleteDialog) {
                    DeletePlaylistDialog(
                        playlistName = state.playlist.name,
                        onDismiss = { showDeleteDialog = false },
                        onConfirm = {
                            viewModel.deletePlaylist {
                                onNavigateBack()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailContent(
    state: PlaylistDetailUiState.Success,
    onResumeClick: () -> Unit,
    onItemClick: (PlaylistDetailItem) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemoveItem: (PlaylistDetailItem) -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeight = 80.dp

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        // Header Section
        item {
            PlaylistHeaderSection(
                itemCount = state.items.size,
                totalDurationMs = state.totalDurationMs,
                progress = state.progress,
                completedCount = state.completedCount,
                onResumeClick = onResumeClick
            )
        }

        // Items List
        itemsIndexed(
            items = state.items,
            key = { _, item -> item.playlistItem.id }
        ) { index, item ->
            val isDragging = draggingItemIndex == index
            val elevation by animateDpAsState(
                targetValue = if (isDragging) 8.dp else 1.dp,
                label = "elevation"
            )

            PlaylistItemRow(
                item = item,
                index = index,
                isDragging = isDragging,
                elevation = elevation,
                dragOffset = if (isDragging) dragOffset else 0f,
                onItemClick = { onItemClick(item) },
                onRemove = { onRemoveItem(item) },
                onDragStart = {
                    draggingItemIndex = index
                },
                onDrag = { change ->
                    dragOffset += change
                    // Calculate target index based on drag offset
                    val targetIndex = (index + (dragOffset / itemHeight.value).roundToInt())
                        .coerceIn(0, state.items.size - 1)
                    if (targetIndex != index && draggingItemIndex == index) {
                        onReorder(index, targetIndex)
                        draggingItemIndex = targetIndex
                        dragOffset = 0f
                    }
                },
                onDragEnd = {
                    draggingItemIndex = -1
                    dragOffset = 0f
                }
            )
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun PlaylistHeaderSection(
    itemCount: Int,
    totalDurationMs: Long,
    progress: Float,
    completedCount: Int,
    onResumeClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Item count and duration
            Text(
                text = "$itemCount items  |  ${formatDuration(totalDurationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${(progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resume button
            ListenerButton(
                text = if (completedCount == 0) "Start Learning" else "Resume Learning",
                onClick = onResumeClick,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.PlayArrow
            )
        }
    }
}

@Composable
private fun PlaylistItemRow(
    item: PlaylistDetailItem,
    index: Int,
    isDragging: Boolean = false,
    elevation: androidx.compose.ui.unit.Dp = 1.dp,
    dragOffset: Float = 0f,
    onItemClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    var showItemMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            .zIndex(if (isDragging) 1f else 0f)
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else if (isDragging) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    },
                tint = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Status icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            item.isCompleted -> MaterialTheme.colorScheme.primary
                            item.isCurrent -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    item.isCompleted -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    item.isCurrent -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "In progress",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                    else -> {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (item.isCurrent) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.durationMs != null) {
                        Text(
                            text = " | ${formatDuration(item.durationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Progress bar for current item
                if (item.isCurrent && item.progress > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(item.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // More options
            Box {
                IconButton(onClick = { showItemMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Item options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showItemMenu,
                    onDismissRequest = { showItemMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove from Playlist") },
                        onClick = {
                            showItemMenu = false
                            onRemove()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditPlaylistNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Playlist Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Playlist") },
        text = {
            Text("Are you sure you want to delete \"$playlistName\"? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes} min"
        else -> "< 1 min"
    }
}

// Preview Composables

@Preview(showBackground = true)
@Composable
private fun PlaylistDetailScreenPreview() {
    ListenerTheme {
        PlaylistDetailContentPreview()
    }
}

@Composable
private fun PlaylistDetailContentPreview() {
    val mockItems = listOf(
        PlaylistDetailItem(
            playlistItem = PlaylistItemEntity(
                id = 1,
                playlistId = 1,
                sourceId = "ep1",
                sourceType = "PODCAST_EPISODE",
                orderIndex = 0,
                addedAt = System.currentTimeMillis()
            ),
            title = "EP.285 Basic Greetings",
            subtitle = "All Ears English",
            durationMs = 18 * 60 * 1000L,
            progress = 1f,
            isCompleted = true,
            isCurrent = false
        ),
        PlaylistDetailItem(
            playlistItem = PlaylistItemEntity(
                id = 2,
                playlistId = 1,
                sourceId = "ep2",
                sourceType = "PODCAST_EPISODE",
                orderIndex = 1,
                addedAt = System.currentTimeMillis()
            ),
            title = "EP.286 Small Talk",
            subtitle = "All Ears English",
            durationMs = 22 * 60 * 1000L,
            progress = 1f,
            isCompleted = true,
            isCurrent = false
        ),
        PlaylistDetailItem(
            playlistItem = PlaylistItemEntity(
                id = 3,
                playlistId = 1,
                sourceId = "ep3",
                sourceType = "PODCAST_EPISODE",
                orderIndex = 2,
                addedAt = System.currentTimeMillis()
            ),
            title = "EP.287 Asking Questions",
            subtitle = "All Ears English",
            durationMs = 20 * 60 * 1000L,
            progress = 0.4f,
            isCompleted = false,
            isCurrent = true
        ),
        PlaylistDetailItem(
            playlistItem = PlaylistItemEntity(
                id = 4,
                playlistId = 1,
                sourceId = "ep4",
                sourceType = "PODCAST_EPISODE",
                orderIndex = 3,
                addedAt = System.currentTimeMillis()
            ),
            title = "EP.288 Business English",
            subtitle = "All Ears English",
            durationMs = 25 * 60 * 1000L,
            progress = 0f,
            isCompleted = false,
            isCurrent = false
        )
    )

    val mockState = PlaylistDetailUiState.Success(
        playlist = PlaylistEntity(
            id = 1,
            name = "English Conversation Practice",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ),
        items = mockItems,
        totalDurationMs = 4 * 60 * 60 * 1000L + 32 * 60 * 1000L, // 4h 32m
        progress = 0.67f,
        completedCount = 2
    )

    PlaylistDetailContent(
        state = mockState,
        onResumeClick = {},
        onItemClick = {},
        onMoveUp = {},
        onMoveDown = {},
        onRemoveItem = {},
        onReorder = { _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaylistHeaderSectionPreview() {
    ListenerTheme {
        PlaylistHeaderSection(
            itemCount = 12,
            totalDurationMs = 4 * 60 * 60 * 1000L + 32 * 60 * 1000L,
            progress = 0.67f,
            completedCount = 8,
            onResumeClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistItemRowPreview() {
    ListenerTheme {
        Column {
            PlaylistItemRow(
                item = PlaylistDetailItem(
                    playlistItem = PlaylistItemEntity(
                        id = 1,
                        playlistId = 1,
                        sourceId = "ep1",
                        sourceType = "PODCAST_EPISODE",
                        orderIndex = 0,
                        addedAt = System.currentTimeMillis()
                    ),
                    title = "EP.287 Asking Questions",
                    subtitle = "All Ears English",
                    durationMs = 20 * 60 * 1000L,
                    progress = 0.4f,
                    isCompleted = false,
                    isCurrent = true
                ),
                index = 2,
                onItemClick = {},
                onRemove = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPlaylistPreview() {
    ListenerTheme {
        EmptyState(
            icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
            title = "Playlist is Empty",
            description = "Add episodes or audio files to this playlist",
            actionLabel = "Add Content",
            onAction = {}
        )
    }
}
