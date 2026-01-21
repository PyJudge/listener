package com.listener.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TranscriptionQueueStatus {
    PENDING,      // 대기
    DOWNLOADING,  // 다운로드 중
    TRANSCRIBING, // 전사 중
    PROCESSING,   // 청크 생성 중
    COMPLETED,    // 완료
    FAILED        // 실패
}

@Entity(tableName = "transcription_queue")
data class TranscriptionQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val sourceType: String,  // PODCAST_EPISODE, LOCAL_FILE
    val title: String,
    val subtitle: String,
    val status: String = TranscriptionQueueStatus.PENDING.name,
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val orderIndex: Int,
    val addedAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null
)
