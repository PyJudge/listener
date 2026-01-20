package com.listener.presentation.podcast

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.listener.data.local.db.entity.SubscribedPodcastEntity
import com.listener.presentation.components.EmptyState
import com.listener.presentation.components.LoadingState
import com.listener.presentation.theme.ListenerTheme
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: PodcastViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showUnsubscribeDialog by remember { mutableStateOf<SubscribedPodcastEntity?>(null) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Podcasts",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search podcasts"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.subscriptions.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Outlined.Podcasts,
                        title = "No Subscriptions",
                        description = "Search and subscribe to your favorite podcasts to start learning",
                        actionLabel = "Search Podcasts",
                        onAction = onNavigateToSearch
                    )
                }
                else -> {
                    val gridState = rememberLazyGridState()
                    val itemSize = 100.dp // approximate item size for calculating target index

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.subscriptions,
                            key = { _, podcast -> podcast.feedUrl }
                        ) { index, podcast ->
                            val isDragging = draggingIndex == index
                            var hasDragged by remember { mutableStateOf(false) }
                            var wasLongPressed by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .then(
                                        if (isDragging) {
                                            Modifier
                                                .offset {
                                                    IntOffset(
                                                        dragOffsetX.roundToInt(),
                                                        dragOffsetY.roundToInt()
                                                    )
                                                }
                                                .scale(1.1f)
                                        } else Modifier
                                    )
                                    .pointerInput(index) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                wasLongPressed = true
                                                draggingIndex = index
                                                dragOffsetX = 0f
                                                dragOffsetY = 0f
                                                hasDragged = false
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetX += dragAmount.x
                                                dragOffsetY += dragAmount.y
                                                if (abs(dragOffsetX) > 10 || abs(dragOffsetY) > 10) {
                                                    hasDragged = true
                                                }
                                            },
                                            onDragEnd = {
                                                if (hasDragged) {
                                                    // Calculate target index based on drag offset
                                                    val columns = 3
                                                    val itemWidth = 120.dp.toPx()
                                                    val itemHeight = 120.dp.toPx()

                                                    val colOffset = (dragOffsetX / itemWidth).roundToInt()
                                                    val rowOffset = (dragOffsetY / itemHeight).roundToInt()

                                                    val currentRow = draggingIndex / columns
                                                    val currentCol = draggingIndex % columns

                                                    val targetCol = (currentCol + colOffset).coerceIn(0, columns - 1)
                                                    val targetRow = (currentRow + rowOffset).coerceAtLeast(0)
                                                    val targetIndex = (targetRow * columns + targetCol)
                                                        .coerceIn(0, uiState.subscriptions.size - 1)

                                                    if (targetIndex != draggingIndex) {
                                                        viewModel.reorderPodcasts(draggingIndex, targetIndex)
                                                    }
                                                } else {
                                                    // Long press without drag - show unsubscribe
                                                    showUnsubscribeDialog = podcast
                                                }

                                                draggingIndex = -1
                                                dragOffsetX = 0f
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggingIndex = -1
                                                dragOffsetX = 0f
                                                dragOffsetY = 0f
                                                wasLongPressed = false
                                            }
                                        )
                                    }
                                    .pointerInput(index, wasLongPressed) {
                                        detectTapGestures(
                                            onTap = {
                                                if (!wasLongPressed) {
                                                    onNavigateToDetail(podcast.feedUrl)
                                                }
                                                wasLongPressed = false
                                            }
                                        )
                                    }
                            ) {
                                PodcastGridItem(
                                    podcast = podcast,
                                    isDragging = isDragging
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Unsubscribe confirmation dialog
    showUnsubscribeDialog?.let { podcast ->
        AlertDialog(
            onDismissRequest = { showUnsubscribeDialog = null },
            title = { Text("구독 취소") },
            text = { Text("'${podcast.title}' 구독을 취소하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unsubscribe(podcast.feedUrl)
                    showUnsubscribeDialog = null
                }) {
                    Text("취소하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsubscribeDialog = null }) {
                    Text("아니오")
                }
            }
        )
    }
}

@Composable
private fun PodcastGridItem(
    podcast: SubscribedPodcastEntity,
    isDragging: Boolean
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDragging) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        AsyncImage(
            model = podcast.artworkUrl,
            contentDescription = podcast.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PodcastScreenPreview() {
    ListenerTheme {
        // Preview without ViewModel
    }
}
