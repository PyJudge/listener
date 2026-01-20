package com.listener.presentation.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.RecentLearningEntity
import com.listener.presentation.components.ContentBottomSheet
import com.listener.presentation.components.EmptyState
import com.listener.presentation.components.ListenerCard
import com.listener.presentation.components.LoadingState
import com.listener.presentation.components.PlaylistSelectDialog
import com.listener.presentation.theme.ListenerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlayer: (sourceId: String) -> Unit = {},
    onNavigateToPodcast: () -> Unit = {},
    onNavigateToContinueLearning: () -> Unit = {},
    onNavigateToTranscription: (sourceId: String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Bottom sheet state
    var selectedEpisode by remember { mutableStateOf<PodcastEpisodeEntity?>(null) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSourceIdForPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedSourceTypeForPlaylist by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listener") }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.padding(padding))
            }
            !uiState.hasSubscriptions && uiState.recentLearnings.isEmpty() -> {
                EmptyState(
                    icon = Icons.Outlined.School,
                    title = "Start Learning",
                    description = "Subscribe to podcasts or add media files to begin your language learning journey",
                    actionLabel = "Browse Podcasts",
                    onAction = onNavigateToPodcast,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Continue Learning Section
                    if (uiState.recentLearnings.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Continue Learning",
                                onSeeAll = onNavigateToContinueLearning
                            )
                        }

                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.recentLearnings,
                                    key = { it.sourceId }
                                ) { learning ->
                                    RecentLearningCard(
                                        learning = learning,
                                        onClick = { onNavigateToPlayer(learning.sourceId) }
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }

                    // New Episodes Section
                    if (uiState.newEpisodes.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "New Episodes",
                                onSeeAll = onNavigateToPodcast
                            )
                        }

                        items(
                            items = uiState.newEpisodes,
                            key = { it.id }
                        ) { episode ->
                            NewEpisodeItem(
                                episode = episode,
                                onClick = { selectedEpisode = episode },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    } else if (uiState.hasSubscriptions) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.Podcasts,
                                title = "No New Episodes",
                                description = "Your subscribed podcasts are up to date",
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Content Bottom Sheet
    selectedEpisode?.let { episode ->
        ContentBottomSheet(
            title = episode.title,
            subtitle = "Podcast Episode",
            description = episode.description,
            durationMs = episode.durationMs,
            pubDate = episode.pubDate,
            onDismiss = { selectedEpisode = null },
            onStartLearning = {
                viewModel.markEpisodeAsPlayed(episode.id)
                selectedEpisode = null
                onNavigateToTranscription(episode.id)
            },
            onAddToPlaylist = {
                selectedSourceIdForPlaylist = episode.id
                selectedSourceTypeForPlaylist = "PODCAST_EPISODE"
                showPlaylistDialog = true
                selectedEpisode = null
            }
        )
    }

    // Playlist Select Dialog
    if (showPlaylistDialog && selectedSourceIdForPlaylist != null) {
        PlaylistSelectDialog(
            playlists = uiState.playlists,
            onDismiss = {
                showPlaylistDialog = false
                selectedSourceIdForPlaylist = null
                selectedSourceTypeForPlaylist = null
            },
            onPlaylistSelected = { playlistId ->
                viewModel.addToPlaylist(
                    playlistId = playlistId,
                    sourceId = selectedSourceIdForPlaylist!!,
                    sourceType = selectedSourceTypeForPlaylist ?: "PODCAST_EPISODE"
                )
                showPlaylistDialog = false
                selectedSourceIdForPlaylist = null
                selectedSourceTypeForPlaylist = null
            },
            onCreateNewPlaylist = {
                showPlaylistDialog = false
                viewModel.showCreatePlaylistDialog()
            },
            itemCounts = uiState.playlistItemCounts
        )
    }

    // Create Playlist Dialog
    if (uiState.showCreatePlaylistDialog && selectedSourceIdForPlaylist != null) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                viewModel.createPlaylistAndAddItem(
                    name = name,
                    sourceId = selectedSourceIdForPlaylist!!,
                    sourceType = selectedSourceTypeForPlaylist ?: "PODCAST_EPISODE"
                )
                selectedSourceIdForPlaylist = null
                selectedSourceTypeForPlaylist = null
            },
            onDismiss = {
                viewModel.dismissCreatePlaylistDialog()
                selectedSourceIdForPlaylist = null
                selectedSourceTypeForPlaylist = null
            }
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
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
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See All")
            }
        }
    }
}

@Composable
private fun RecentLearningCard(
    learning: RecentLearningEntity,
    onClick: () -> Unit
) {
    val progress = if (learning.totalChunks > 0) {
        learning.currentChunkIndex.toFloat() / learning.totalChunks
    } else 0f

    ListenerCard(
        title = learning.title,
        subtitle = learning.subtitle,
        imageUrl = learning.thumbnailUrl,
        progress = progress,
        onClick = onClick,
        modifier = Modifier.width(280.dp)
    )
}

@Composable
private fun NewEpisodeItem(
    episode: PodcastEpisodeEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListenerCard(
        title = episode.title,
        subtitle = formatDuration(episode.durationMs),
        badge = if (episode.isNew) "NEW" else null,
        onClick = onClick,
        modifier = modifier
    )
}

private fun formatDuration(ms: Long?): String {
    if (ms == null) return ""
    val minutes = ms / 60000
    return "${minutes}min"
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ListenerTheme {
        // Preview without ViewModel
    }
}
