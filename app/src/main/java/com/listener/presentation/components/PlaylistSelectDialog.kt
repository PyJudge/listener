package com.listener.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.presentation.theme.ListenerTheme

@Composable
fun PlaylistSelectDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (playlistId: Long) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    itemCounts: Map<Long, Int> = emptyMap()
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "플레이리스트 선택")
        },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(
                        text = "플레이리스트가 없습니다.\n새 플레이리스트를 만들어주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(
                            minOf(playlists.size * 56, 280).dp
                        )
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                itemCount = itemCounts[playlist.id] ?: 0,
                                onClick = { onPlaylistSelected(playlist.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Create New Playlist Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCreateNewPlaylist)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "새 플레이리스트 만들기",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun PlaylistItem(
    playlist: PlaylistEntity,
    itemCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${itemCount}개 항목",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistSelectDialogPreview() {
    ListenerTheme {
        val samplePlaylists = listOf(
            PlaylistEntity(
                id = 1,
                name = "영어 공부",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            PlaylistEntity(
                id = 2,
                name = "팟캐스트 모음",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            PlaylistEntity(
                id = 3,
                name = "출퇴근길 학습",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        val itemCounts = mapOf(1L to 5, 2L to 12, 3L to 3)

        PlaylistSelectDialog(
            playlists = samplePlaylists,
            onDismiss = {},
            onPlaylistSelected = {},
            onCreateNewPlaylist = {},
            itemCounts = itemCounts
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistSelectDialogEmptyPreview() {
    ListenerTheme {
        PlaylistSelectDialog(
            playlists = emptyList(),
            onDismiss = {},
            onPlaylistSelected = {},
            onCreateNewPlaylist = {}
        )
    }
}
