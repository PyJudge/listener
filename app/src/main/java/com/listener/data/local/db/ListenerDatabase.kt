package com.listener.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.listener.data.local.db.dao.ChunkSettingsDao
import com.listener.data.local.db.dao.FolderDao
import com.listener.data.local.db.dao.LearningProgressDao
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.dao.RecordingDao
import com.listener.data.local.db.dao.TranscriptionDao
import com.listener.data.local.db.dao.TranscriptionQueueDao
import com.listener.data.local.db.entity.ChunkEntity
import com.listener.data.local.db.entity.ChunkSettingsEntity
import com.listener.data.local.db.entity.FolderEntity
import com.listener.data.local.db.entity.FolderItemEntity
import com.listener.data.local.db.entity.LearningProgressEntity
import com.listener.data.local.db.entity.LocalAudioFileEntity
import com.listener.data.local.db.entity.PlaylistEntity
import com.listener.data.local.db.entity.PlaylistItemEntity
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.RecentLearningEntity
import com.listener.data.local.db.entity.SubscribedPodcastEntity
import com.listener.data.local.db.entity.TranscriptionQueueEntity
import com.listener.data.local.db.entity.TranscriptionResultEntity
import com.listener.data.local.db.entity.UserRecordingEntity

@Database(
    entities = [
        SubscribedPodcastEntity::class,
        PodcastEpisodeEntity::class,
        LocalAudioFileEntity::class,
        TranscriptionResultEntity::class,
        ChunkEntity::class,
        ChunkSettingsEntity::class,
        LearningProgressEntity::class,
        UserRecordingEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        RecentLearningEntity::class,
        FolderEntity::class,
        FolderItemEntity::class,
        TranscriptionQueueEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class ListenerDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun localFileDao(): LocalFileDao
    abstract fun transcriptionDao(): TranscriptionDao
    abstract fun chunkSettingsDao(): ChunkSettingsDao
    abstract fun learningProgressDao(): LearningProgressDao
    abstract fun recordingDao(): RecordingDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentLearningDao(): RecentLearningDao
    abstract fun folderDao(): FolderDao
    abstract fun transcriptionQueueDao(): TranscriptionQueueDao
}
