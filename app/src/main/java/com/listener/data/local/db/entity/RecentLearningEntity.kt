package com.listener.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_learnings",
    indices = [
        Index(value = ["lastAccessedAt"], orders = [Index.Order.DESC])
    ]
)
data class RecentLearningEntity(
    @PrimaryKey val sourceId: String,
    val sourceType: String, // "PODCAST_EPISODE" or "LOCAL_FILE"
    val title: String,
    val subtitle: String,
    val currentChunkIndex: Int,
    val totalChunks: Int,
    val thumbnailUrl: String?,
    val lastAccessedAt: Long
)
