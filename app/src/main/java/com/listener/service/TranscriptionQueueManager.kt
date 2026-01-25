package com.listener.service

import android.util.Log
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.TranscriptionQueueDao
import com.listener.data.local.db.entity.TranscriptionQueueEntity
import com.listener.data.local.db.entity.TranscriptionQueueStatus
import com.listener.data.repository.SettingsRepository
import com.listener.domain.repository.TranscriptionRepository
import com.listener.domain.repository.TranscriptionState
import com.listener.domain.repository.TranscriptionStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class QueueItem(
    val id: Long,
    val sourceId: String,
    val sourceType: String,
    val title: String,
    val subtitle: String,
    val status: TranscriptionQueueStatus,
    val progress: Float,
    val errorMessage: String?,
    val orderIndex: Int
)

data class TranscriptionQueueState(
    val currentItem: QueueItem? = null,
    val pendingItems: List<QueueItem> = emptyList(),
    val completedItems: List<QueueItem> = emptyList(),
    val failedItems: List<QueueItem> = emptyList()
) {
    val allItems: List<QueueItem>
        get() = listOfNotNull(currentItem) + pendingItems + failedItems + completedItems

    val pendingCount: Int get() = pendingItems.size
    val isProcessing: Boolean get() = currentItem != null
}

@Singleton
class TranscriptionQueueManager @Inject constructor(
    private val transcriptionQueueDao: TranscriptionQueueDao,
    private val transcriptionRepository: TranscriptionRepository,
    private val podcastDao: PodcastDao,
    private val localFileDao: LocalFileDao,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "TranscriptionQueue"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _queueState = MutableStateFlow(TranscriptionQueueState())
    val queueState: StateFlow<TranscriptionQueueState> = _queueState.asStateFlow()

    private var isInitialized = false

    init {
        scope.launch {
            // 큐 DB 변경 관찰
            transcriptionQueueDao.observeAll().collect { entities ->
                updateQueueState(entities)
            }
        }

        scope.launch {
            // 전사 상태 관찰
            transcriptionRepository.transcriptionState.collect { state ->
                handleTranscriptionState(state)
            }
        }
    }

    private fun updateQueueState(entities: List<TranscriptionQueueEntity>) {
        val items = entities.map { it.toQueueItem() }

        val currentItem = items.find {
            it.status in listOf(
                TranscriptionQueueStatus.DOWNLOADING,
                TranscriptionQueueStatus.TRANSCRIBING,
                TranscriptionQueueStatus.PROCESSING
            )
        }

        val pendingItems = items.filter { it.status == TranscriptionQueueStatus.PENDING }
            .sortedBy { it.orderIndex }

        val completedItems = items.filter { it.status == TranscriptionQueueStatus.COMPLETED }

        val failedItems = items.filter { it.status == TranscriptionQueueStatus.FAILED }

        _queueState.value = TranscriptionQueueState(
            currentItem = currentItem,
            pendingItems = pendingItems,
            completedItems = completedItems,
            failedItems = failedItems
        )

        // 자동 시작: 현재 진행 중인 게 없고, 대기 중인 게 있으면 시작
        if (!isInitialized) {
            isInitialized = true
            if (currentItem == null && pendingItems.isNotEmpty()) {
                scope.launch { processNextItem() }
            }
        }
    }

    private suspend fun handleTranscriptionState(state: TranscriptionState) {
        val currentItem = _queueState.value.currentItem ?: return

        when (state) {
            is TranscriptionState.InProgress -> {
                if (state.sourceId == currentItem.sourceId) {
                    val status = when (state.step) {
                        TranscriptionStep.DOWNLOADING -> TranscriptionQueueStatus.DOWNLOADING
                        TranscriptionStep.PREPROCESSING -> TranscriptionQueueStatus.DOWNLOADING // 압축도 다운로드 단계로 표시
                        TranscriptionStep.TRANSCRIBING -> TranscriptionQueueStatus.TRANSCRIBING
                        TranscriptionStep.PROCESSING -> TranscriptionQueueStatus.PROCESSING
                    }
                    val progress = when (state.step) {
                        TranscriptionStep.DOWNLOADING -> state.downloadProgress * 0.15f
                        TranscriptionStep.PREPROCESSING -> 0.15f + state.preprocessProgress * 0.15f
                        TranscriptionStep.TRANSCRIBING -> 0.30f + state.transcriptionProgress * 0.55f
                        TranscriptionStep.PROCESSING -> 0.95f
                    }
                    transcriptionQueueDao.updateProgress(currentItem.id, status.name, progress)
                }
            }

            is TranscriptionState.Complete -> {
                if (state.sourceId == currentItem.sourceId) {
                    Log.d(TAG, "Transcription complete for ${currentItem.title}")
                    transcriptionQueueDao.update(
                        transcriptionQueueDao.getById(currentItem.id)!!.copy(
                            status = TranscriptionQueueStatus.COMPLETED.name,
                            progress = 1f,
                            completedAt = System.currentTimeMillis()
                        )
                    )
                    // 다음 아이템 처리
                    processNextItem()
                }
            }

            is TranscriptionState.Error -> {
                if (state.sourceId == currentItem.sourceId) {
                    Log.e(TAG, "Transcription failed for ${currentItem.title}: ${state.message}")
                    transcriptionQueueDao.updateError(
                        currentItem.id,
                        TranscriptionQueueStatus.FAILED.name,
                        state.message
                    )
                    // 다음 아이템 처리 (실패해도 계속)
                    processNextItem()
                }
            }

            is TranscriptionState.Idle -> {
                // 무시
            }
        }
    }

    private suspend fun processNextItem() {
        val nextItem = transcriptionQueueDao.getNextPending()
        if (nextItem != null) {
            Log.d(TAG, "Starting transcription for ${nextItem.title}")
            transcriptionQueueDao.update(
                nextItem.copy(
                    status = TranscriptionQueueStatus.DOWNLOADING.name,
                    startedAt = System.currentTimeMillis()
                )
            )
            val language = settingsRepository.settings.first().transcriptionLanguage
            transcriptionRepository.startTranscription(nextItem.sourceId, language)
        } else {
            Log.d(TAG, "Queue empty, nothing to process")
        }
    }

    suspend fun addToQueue(sourceId: String, sourceType: String): Long {
        // 이미 큐에 있는지 확인
        val existing = transcriptionQueueDao.getBySourceId(sourceId)
        if (existing != null) {
            Log.d(TAG, "Source $sourceId already in queue")
            return existing.id
        }

        // 이미 전사가 완료된 경우 확인
        val existingChunks = transcriptionRepository.getChunks(sourceId)
        if (existingChunks.isNotEmpty()) {
            Log.d(TAG, "Source $sourceId already transcribed")
            return -1
        }

        // 제목과 부제목 가져오기
        val (title, subtitle) = getSourceInfo(sourceId, sourceType)

        val maxOrderIndex = transcriptionQueueDao.getMaxOrderIndex() ?: -1
        val entity = TranscriptionQueueEntity(
            sourceId = sourceId,
            sourceType = sourceType,
            title = title,
            subtitle = subtitle,
            orderIndex = maxOrderIndex + 1
        )

        val id = transcriptionQueueDao.insert(entity)
        Log.d(TAG, "Added to queue: $title (id=$id)")

        // 현재 처리 중인 게 없으면 시작
        if (_queueState.value.currentItem == null) {
            processNextItem()
        }

        return id
    }

    private suspend fun getSourceInfo(sourceId: String, sourceType: String): Pair<String, String> {
        return when (sourceType) {
            "PODCAST_EPISODE" -> {
                val episode = podcastDao.getEpisode(sourceId)
                if (episode != null) {
                    val podcast = podcastDao.getSubscription(episode.feedUrl)
                    Pair(episode.title, podcast?.title ?: "Podcast")
                } else {
                    Pair("Unknown Episode", "Podcast")
                }
            }
            "LOCAL_FILE" -> {
                val file = localFileDao.getFile(sourceId)
                Pair(file?.displayName ?: "Unknown File", "Local File")
            }
            else -> Pair("Unknown", "Unknown")
        }
    }

    suspend fun removeFromQueue(id: Long) {
        val item = transcriptionQueueDao.getById(id)
        if (item != null) {
            // 현재 진행 중인 아이템이면 취소
            if (item.status in listOf(
                    TranscriptionQueueStatus.DOWNLOADING.name,
                    TranscriptionQueueStatus.TRANSCRIBING.name,
                    TranscriptionQueueStatus.PROCESSING.name
                )
            ) {
                transcriptionRepository.cancelTranscription()
            }
            transcriptionQueueDao.deleteById(id)
            Log.d(TAG, "Removed from queue: ${item.title}")
        }
    }

    suspend fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val pendingItems = transcriptionQueueDao.getByStatus(TranscriptionQueueStatus.PENDING.name)
            .sortedBy { it.orderIndex }

        if (fromIndex < 0 || fromIndex >= pendingItems.size ||
            toIndex < 0 || toIndex >= pendingItems.size
        ) {
            return
        }

        val mutableList = pendingItems.toMutableList()
        val movedItem = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, movedItem)

        mutableList.forEachIndexed { index, item ->
            if (item.orderIndex != index) {
                transcriptionQueueDao.updateOrderIndex(item.id, index)
            }
        }
    }

    suspend fun retry(id: Long) {
        val item = transcriptionQueueDao.getById(id) ?: return
        if (item.status == TranscriptionQueueStatus.FAILED.name) {
            transcriptionQueueDao.update(
                item.copy(
                    status = TranscriptionQueueStatus.PENDING.name,
                    progress = 0f,
                    errorMessage = null
                )
            )
            // 현재 처리 중인 게 없으면 시작
            if (_queueState.value.currentItem == null) {
                processNextItem()
            }
        }
    }

    suspend fun clearCompleted() {
        transcriptionQueueDao.deleteCompleted()
    }

    private fun TranscriptionQueueEntity.toQueueItem() = QueueItem(
        id = id,
        sourceId = sourceId,
        sourceType = sourceType,
        title = title,
        subtitle = subtitle,
        status = try {
            TranscriptionQueueStatus.valueOf(status)
        } catch (e: Exception) {
            TranscriptionQueueStatus.PENDING
        },
        progress = progress,
        errorMessage = errorMessage,
        orderIndex = orderIndex
    )
}
