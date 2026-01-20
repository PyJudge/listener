package com.listener.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscribed_podcasts")
data class SubscribedPodcastEntity(
    @PrimaryKey val feedUrl: String,
    val collectionId: Long?,
    val title: String,
    val description: String?,
    val artworkUrl: String?,
    val lastCheckedAt: Long,
    val addedAt: Long
)
