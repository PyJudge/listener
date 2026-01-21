package com.listener.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.listener.presentation.theme.ListenerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentBottomSheet(
    title: String,
    subtitle: String,
    description: String?,
    durationMs: Long?,
    pubDate: Long?,
    onDismiss: () -> Unit,
    onStartLearning: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        ContentBottomSheetContent(
            title = title,
            subtitle = subtitle,
            description = description,
            durationMs = durationMs,
            pubDate = pubDate,
            onStartLearning = onStartLearning,
            onAddToPlaylist = onAddToPlaylist,
            onAddToQueue = onAddToQueue
        )
    }
}

@Composable
private fun ContentBottomSheetContent(
    title: String,
    subtitle: String,
    description: String?,
    durationMs: Long?,
    pubDate: Long?,
    onStartLearning: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle (podcast name)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Duration and Date
        val metaInfo = buildMetaInfo(durationMs, pubDate)
        if (metaInfo.isNotEmpty()) {
            Text(
                text = metaInfo,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Description - scrollable with larger height
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(20.dp))

            // Clean HTML tags from description
            val cleanDescription = description.replace(Regex("<[^>]*>"), "").trim()
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .heightIn(min = 150.dp, max = 300.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = cleanDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Buttons
        ListenerButton(
            text = "학습 시작",
            onClick = onStartLearning,
            icon = Icons.Outlined.PlayArrow,
            style = ButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        ListenerButton(
            text = "플레이리스트에 추가",
            onClick = onAddToPlaylist,
            icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
            style = ButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth()
        )

        if (onAddToQueue != null) {
            Spacer(modifier = Modifier.height(12.dp))

            ListenerButton(
                text = "전사 대기열에 추가",
                onClick = onAddToQueue,
                icon = Icons.Outlined.Queue,
                style = ButtonStyle.Outline,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun buildMetaInfo(durationMs: Long?, pubDate: Long?): String {
    val parts = mutableListOf<String>()

    durationMs?.let {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(it)
        parts.add("${minutes}분")
    }

    pubDate?.let {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        parts.add(dateFormat.format(Date(it)))
    }

    return parts.joinToString(" · ")
}

@Preview(showBackground = true)
@Composable
private fun ContentBottomSheetContentPreview() {
    ListenerTheme {
        ContentBottomSheetContent(
            title = "EP.289 How to Sound Natural",
            subtitle = "All Ears English",
            description = "So in today's episode, we're going to talk about how native speakers actually sound when they're having a casual conversation...",
            durationMs = 25 * 60 * 1000L,
            pubDate = System.currentTimeMillis(),
            onStartLearning = {},
            onAddToPlaylist = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentBottomSheetContentNoDescriptionPreview() {
    ListenerTheme {
        ContentBottomSheetContent(
            title = "Audio Recording.m4a",
            subtitle = "미디어 파일",
            description = null,
            durationMs = 5 * 60 * 1000L,
            pubDate = null,
            onStartLearning = {},
            onAddToPlaylist = {}
        )
    }
}
