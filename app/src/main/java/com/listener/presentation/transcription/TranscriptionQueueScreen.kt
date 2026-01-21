package com.listener.presentation.transcription

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.listener.data.local.db.entity.TranscriptionQueueStatus
import com.listener.service.QueueItem
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionQueueScreen(
    onNavigateBack: () -> Unit,
    viewModel: TranscriptionQueueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val queueState = uiState.queueState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("전사 대기열") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 현재 진행 중인 아이템
            queueState.currentItem?.let { currentItem ->
                CurrentItemSection(item = currentItem)
            }

            // 대기열이 비어있을 때
            if (queueState.allItems.isEmpty()) {
                EmptyQueueContent(
                    modifier = Modifier.weight(1f)
                )
            } else {
                // 대기열 목록
                QueueList(
                    pendingItems = queueState.pendingItems,
                    failedItems = queueState.failedItems,
                    completedItems = queueState.completedItems,
                    showDeleteConfirm = uiState.showDeleteConfirm,
                    onShowDeleteConfirm = viewModel::showDeleteConfirm,
                    onHideDeleteConfirm = viewModel::hideDeleteConfirm,
                    onRemove = viewModel::removeItem,
                    onRetry = viewModel::retry,
                    onReorder = viewModel::reorderQueue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CurrentItemSection(
    item: QueueItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상태 표시
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (item.status) {
                        TranscriptionQueueStatus.DOWNLOADING -> "다운로드 중..."
                        TranscriptionQueueStatus.TRANSCRIBING -> "전사 중..."
                        TranscriptionQueueStatus.PROCESSING -> "청크 생성 중..."
                        else -> "처리 중..."
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 제목
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 부제목
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 진행률 바
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 진행률 퍼센트
            Text(
                text = "${(item.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueList(
    pendingItems: List<QueueItem>,
    failedItems: List<QueueItem>,
    completedItems: List<QueueItem>,
    showDeleteConfirm: Long?,
    onShowDeleteConfirm: (Long) -> Unit,
    onHideDeleteConfirm: () -> Unit,
    onRemove: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeight = with(density) { 80.dp.toPx() }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth()
    ) {
        // 대기 중인 아이템
        if (pendingItems.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "대기열",
                    count = pendingItems.size
                )
            }

            itemsIndexed(
                items = pendingItems,
                key = { _, item -> item.id }
            ) { index, item ->
                val isDragging = draggingItemIndex == index
                val offsetY = if (isDragging) dragOffset else 0f

                QueueItemRow(
                    item = item,
                    showDeleteButton = showDeleteConfirm == item.id,
                    isDragging = isDragging,
                    offsetY = offsetY,
                    onLongPress = {
                        if (showDeleteConfirm == item.id) {
                            onHideDeleteConfirm()
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShowDeleteConfirm(item.id)
                        }
                    },
                    onDelete = { onRemove(item.id) },
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        draggingItemIndex = index
                    },
                    onDrag = { change ->
                        dragOffset += change
                        val targetIndex = (index + (dragOffset / itemHeight).roundToInt())
                            .coerceIn(0, pendingItems.size - 1)
                        if (targetIndex != index && targetIndex != draggingItemIndex) {
                            onReorder(draggingItemIndex, targetIndex)
                            draggingItemIndex = targetIndex
                            dragOffset = 0f
                        }
                    },
                    onDragEnd = {
                        draggingItemIndex = -1
                        dragOffset = 0f
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }

        // 실패한 아이템
        if (failedItems.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "실패",
                    count = failedItems.size
                )
            }

            items(
                count = failedItems.size,
                key = { failedItems[it].id }
            ) { index ->
                val item = failedItems[index]
                FailedItemRow(
                    item = item,
                    showDeleteButton = showDeleteConfirm == item.id,
                    onLongPress = {
                        if (showDeleteConfirm == item.id) {
                            onHideDeleteConfirm()
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShowDeleteConfirm(item.id)
                        }
                    },
                    onDelete = { onRemove(item.id) },
                    onRetry = { onRetry(item.id) }
                )
            }
        }

        // 완료된 아이템
        if (completedItems.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "완료",
                    count = completedItems.size
                )
            }

            items(
                count = completedItems.size,
                key = { completedItems[it].id }
            ) { index ->
                val item = completedItems[index]
                CompletedItemRow(
                    item = item,
                    showDeleteButton = showDeleteConfirm == item.id,
                    onLongPress = {
                        if (showDeleteConfirm == item.id) {
                            onHideDeleteConfirm()
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShowDeleteConfirm(item.id)
                        }
                    },
                    onDelete = { onRemove(item.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueItemRow(
    item: QueueItem,
    showDeleteButton: Boolean,
    isDragging: Boolean,
    offsetY: Float,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isDragging) 0.9f else 1f,
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .scale(scale)
            .alpha(alpha)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.surfaceContainerHigh
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 드래그 핸들
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Drag to reorder",
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = { _, dragAmount -> onDrag(dragAmount.y) },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 콘텐츠
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 삭제 버튼 (롱프레스 시 표시)
            AnimatedVisibility(
                visible = showDeleteButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 대기 중 표시
            if (!showDeleteButton) {
                Text(
                    text = "대기",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FailedItemRow(
    item: QueueItem,
    showDeleteButton: Boolean,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 에러 아이콘
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Failed",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 콘텐츠
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.errorMessage ?: "알 수 없는 오류",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 삭제 버튼 (롱프레스 시 표시)
            AnimatedVisibility(
                visible = showDeleteButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 재시도 버튼
            if (!showDeleteButton) {
                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompletedItemRow(
    item: QueueItem,
    showDeleteButton: Boolean,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 완료 아이콘
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 콘텐츠
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 삭제 버튼 (롱프레스 시 표시)
            AnimatedVisibility(
                visible = showDeleteButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 완료 표시
            if (!showDeleteButton) {
                Text(
                    text = "완료",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyQueueContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "전사할 콘텐츠가 없습니다",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "팟캐스트나 오디오 파일을 추가해주세요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

