package com.listener.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.listener.data.local.db.ListenerDatabase
import com.listener.data.local.db.dao.ChunkSettingsDao
import com.listener.data.local.db.dao.FolderDao
import com.listener.data.local.db.dao.LearningProgressDao
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.dao.PlaylistDao
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.dao.RecentLearningDao
import com.listener.data.local.db.dao.RecordingDao
import com.listener.data.local.db.dao.TranscriptionDao
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

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE subscribed_podcasts ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS folders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS folder_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    folderId INTEGER NOT NULL,
                    sourceId TEXT NOT NULL,
                    sourceType TEXT NOT NULL,
                    orderIndex INTEGER NOT NULL,
                    addedAt INTEGER NOT NULL,
                    FOREIGN KEY (folderId) REFERENCES folders(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_folder_items_folderId ON folder_items(folderId)")
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
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

    @Provides
    fun provideFolderDao(database: ListenerDatabase): FolderDao = database.folderDao()
}
