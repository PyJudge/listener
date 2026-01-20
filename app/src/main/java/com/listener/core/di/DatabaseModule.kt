package com.listener.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.listener.data.local.db.ListenerDatabase
import com.listener.data.local.db.dao.LearningProgressDao
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.dao.RecordingDao
import com.listener.data.local.db.dao.TranscriptionDao
import com.listener.data.local.db.dao.ChunkSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE subscribed_podcasts ADD COLUMN description TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ListenerDatabase {
        return Room.databaseBuilder(
            context,
            ListenerDatabase::class.java,
            "listener_database"
        )
            .addMigrations(MIGRATION_2_3)
            .build()
    }

    @Provides
    fun providePodcastDao(database: ListenerDatabase): PodcastDao = database.podcastDao()

    @Provides
    fun provideLocalFileDao(database: ListenerDatabase): LocalFileDao = database.localFileDao()

    @Provides
    fun provideTranscriptionDao(database: ListenerDatabase): TranscriptionDao = database.transcriptionDao()

    @Provides
    fun provideChunkSettingsDao(database: ListenerDatabase): ChunkSettingsDao = database.chunkSettingsDao()

    @Provides
    fun provideLearningProgressDao(database: ListenerDatabase): LearningProgressDao = database.learningProgressDao()

    @Provides
    fun provideRecordingDao(database: ListenerDatabase): RecordingDao = database.recordingDao()

    @Provides
    fun providePlaylistDao(database: ListenerDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideRecentLearningDao(database: ListenerDatabase): RecentLearningDao = database.recentLearningDao()
}
