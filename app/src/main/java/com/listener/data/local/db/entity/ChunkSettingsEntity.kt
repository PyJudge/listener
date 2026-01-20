package com.listener.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chunk_settings")
data class ChunkSettingsEntity(
    @PrimaryKey val sourceId: String,
    val sentenceOnly: Boolean,
    val minChunkMs: Long,
    val updatedAt: Long
)
