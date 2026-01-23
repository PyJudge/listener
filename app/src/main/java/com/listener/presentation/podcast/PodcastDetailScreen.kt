package com.listener.presentation.podcast

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.SubscribedPodcastEntity
import com.listener.presentation.components.ContentBottomSheet
import com.listener.presentation.components.EmptyState
import com.listener.presentation.components.ListenerButton
import com.listener.presentation.components.ListenerCard
import com.listener.presentation.components.LoadingState
import com.listener.presentation.theme.ListenerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailScreen(
    feedUrl: String,
    onNavigateBack: () -> Unit = {},
    onNavigateToTranscription: (String) -> Unit = {},
    viewModel: PodcastDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val state = uiState) {
                        is PodcastDetailUiState.Success -> state.podcast.title
                        else -> "Podcast"
                    }
                    Text(
                        text = title,
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
                    if (uiState is PodcastDetailUiState.Success) {
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
                                    text = { Text("새로고침") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.refresh()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Refresh,
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("구독 취소") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.unsubscribe { onNavigateBack() }
                                    }
                                )
                            }
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
            is PodcastDetailUiState.Loading -> {
                LoadingState(
                    message = "Loading episodes...",
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is PodcastDetailUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is PodcastDetailUiState.Success -> {
                var selectedEpisode by remember { mutableStateOf<PodcastEpisodeEntity?>(null) }

                SuccessContent(
                    podcast = state.podcast,
                    episodes = state.episodes,
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    onEpisodeClick = { episode ->
                        selectedEpisode = episode
                    },
                    modifier = Modifier.padding(paddingValues)
                )

                // Episode Bottom Sheet
                selectedEpisode?.let { episode ->
                    ContentBottomSheet(
                        title = episode.title,
                        subtitle = state.podcast.title,
                        description = episode.description,
                        durationMs = episode.durationMs,
                        pubDate = episode.pubDate,
                        onDismiss = { selectedEpisode = null },
                        onStartLearning = {
                            viewModel.markAsRead(episode.id)
                            selectedEpisode = null
                            onNavigateToTranscription(episode.id)
                        },
                        onAddToPlaylist = {
                            // TODO: Show playlist select dialog
                            selectedEpisode = null
                        },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    podcast: SubscribedPodcastEntity,
    episodes: List<PodcastEpisodeEntity>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onEpisodeClick: (PodcastEpisodeEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Calculate header collapse progress (0 = fully expanded, 1 = fully collapsed)
    val collapseProgress by remember {
        derivedStateOf {
            val firstVisibleItem = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            when {
                firstVisibleItem > 0 -> 1f
                else -> (firstVisibleOffset / 400f).coerceIn(0f, 1f)
            }
        }
    }

    val headerAlpha by animateFloatAsState(
        targetValue = 1f - collapseProgress,
        label = "headerAlpha"
    )

    val headerScale by animateFloatAsState(
        targetValue = 1f - (collapseProgress * 0.1f),
        label = "headerScale"
    )

    // 첫 로딩 중에는 PullToRefreshBox 대신 LoadingState만 표시 (스피너 중복 방지)
    if (isRefreshing && episodes.isEmpty()) {
        LoadingState(
            message = "Loading episodes...",
            modifier = modifier.fillMaxSize()
        )
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (episodes.isEmpty()) {
            // 로딩 완료 후 에피소드 없음
            EmptyState(
                icon = Icons.Outlined.Podcasts,
                title = "No Episodes",
                description = "Pull down to refresh and check for new episodes.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Collapsible Podcast Header
                item(key = "header") {
                    CollapsiblePodcastHeader(
                        podcast = podcast,
                        alpha = headerAlpha,
                        scale = headerScale,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }

                // Episode count section
                item(key = "episode_count") {
                    Text(
                        text = "${episodes.size}개의 에피소드",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Episode list
                items(episodes, key = { it.id }) { episode ->
                    EpisodeItem(
                        episode = episode,
                        podcastArtworkUrl = podcast.artworkUrl,
                        onClick = { onEpisodeClick(episode) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsiblePodcastHeader(
    podcast: SubscribedPodcastEntity,
    alpha: Float,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large centered artwork
        AsyncImage(
            model = podcast.artworkUrl,
            contentDescription = podcast.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Podcast title
        Text(
            text = podcast.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subscription info
        Text(
            text = "구독일: ${formatDate(podcast.addedAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Podcast description
        podcast.description?.let { description ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PodcastHeader(
    podcast: SubscribedPodcastEntity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = podcast.artworkUrl,
            contentDescription = podcast.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Added ${formatDate(podcast.addedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EpisodeItem(
    episode: PodcastEpisodeEntity,
    podcastArtworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ListenerCard(
            title = episode.title,
            subtitle = buildSubtitle(episode),
            imageUrl = podcastArtworkUrl,
            onClick = onClick
        )
        if (episode.isNew) {
            Text(
                text = "new",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Failed to load",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            ListenerButton(
                text = "Retry",
                onClick = onRetry
            )
        }
    }
}

private fun buildSubtitle(episode: PodcastEpisodeEntity): String {
    val date = formatDate(episode.pubDate)
    val duration = episode.durationMs?.let { formatDuration(it) }
    return if (duration != null) "$date - $duration" else date
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

@Preview(showBackground = true)
@Composable
private fun PodcastDetailScreenPreview() {
    ListenerTheme {
        SuccessContent(
            podcast = SubscribedPodcastEntity(
                feedUrl = "https://example.com/feed",
                collectionId = 123,
                title = "Sample Podcast",
                description = "This is a sample podcast description that explains what the podcast is about.",
                artworkUrl = null,
                lastCheckedAt = System.currentTimeMillis(),
                addedAt = System.currentTimeMillis()
            ),
            episodes = listOf(
                PodcastEpisodeEntity(
                    id = "1",
                    feedUrl = "https://example.com/feed",
                    title = "Episode 1: Introduction",
                    audioUrl = "https://example.com/ep1.mp3",
                    description = "This is the first episode",
                    durationMs = 3600000,
                    pubDate = System.currentTimeMillis(),
                    isNew = true
                ),
                PodcastEpisodeEntity(
                    id = "2",
                    feedUrl = "https://example.com/feed",
                    title = "Episode 2: Deep Dive",
                    audioUrl = "https://example.com/ep2.mp3",
                    description = "Going deeper",
                    durationMs = 2700000,
                    pubDate = System.currentTimeMillis() - 86400000,
                    isNew = false
                )
            ),
            isRefreshing = false,
            onRefresh = {},
            onEpisodeClick = {}
        )
    }
}
