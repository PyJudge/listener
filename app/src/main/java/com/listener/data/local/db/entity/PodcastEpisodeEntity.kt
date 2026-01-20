package com.listener.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "podcast_episodes",
    foreignKeys = [
        ForeignKey(
            entity = SubscribedPodcastEntity::class,
            parentColumns = ["feedUrl"],
            childColumns = ["feedUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("feedUrl")]
)
data class PodcastEpisodeEntity(
    @PrimaryKey val id: String,
    val feedUrl: String,
    val title: String,
    val audioUrl: String,
    val description: String?,
    val durationMs: Long?,
    val pubDate: Long,
    val isNew: Boolean
)
