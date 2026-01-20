package com.listener.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcription_results")
data class TranscriptionResultEntity(
    @PrimaryKey val sourceId: String,
    val language: String,
    val fullText: String,
    val segmentsJson: String,
    val wordsJson: String,
    val createdAt: Long
)
