package com.listener.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_audio_files")
data class LocalAudioFileEntity(
    @PrimaryKey val contentHash: String,
    val uri: String,
    val displayName: String,
    val durationMs: Long?,
    val addedAt: Long
)
