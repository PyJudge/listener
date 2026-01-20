package com.listener.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_progress")
data class LearningProgressEntity(
    @PrimaryKey val sourceId: String,
    val currentChunkIndex: Int,
    val repeatCount: Int,
    val gapRatio: Float,
    val isRecordingEnabled: Boolean,
    val isHardMode: Boolean,
    val updatedAt: Long
)
