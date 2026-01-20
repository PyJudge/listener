package com.listener.data.local.db.entity

import androidx.room.Entity

@Entity(
    tableName = "user_recordings",
    primaryKeys = ["sourceId", "chunkIndex"]
)
data class UserRecordingEntity(
    val sourceId: String,
    val chunkIndex: Int,
    val filePath: String,
    val durationMs: Long,
    val updatedAt: Long
)
