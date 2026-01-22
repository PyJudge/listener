package com.listener.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MAX_CACHE_SIZE_BYTES = 1L * 1024 * 1024 * 1024 // 1GB
        private const val CACHE_DIR_NAME = "audio_cache"
        private const val DOWNLOADS_DIR_NAME = "audio_downloads"
        private const val PREPROCESSED_DIR_NAME = "preprocessed_audio"
    }

    private val cacheDir: File
        get() = File(context.cacheDir, CACHE_DIR_NAME).also {
            if (!it.exists()) it.mkdirs()
        }

    private val downloadsDir: File
        get() = File(context.cacheDir, DOWNLOADS_DIR_NAME)

    private val preprocessedDir: File
        get() = File(context.cacheDir, PREPROCESSED_DIR_NAME)

    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        calculateDirSize(cacheDir) +
        calculateDirSize(downloadsDir) +
        calculateDirSize(preprocessedDir)
    }

    suspend fun getCachedFile(key: String): File? = withContext(Dispatchers.IO) {
        val file = File(cacheDir, key)
        if (file.exists()) file else null
    }

    suspend fun cacheFile(key: String, data: ByteArray): File = withContext(Dispatchers.IO) {
        ensureCacheSpace(data.size.toLong())
        val file = File(cacheDir, key)
        file.writeBytes(data)
        file
    }

    suspend fun deleteCachedFile(key: String): Boolean = withContext(Dispatchers.IO) {
        File(cacheDir, key).delete()
    }

    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        val freedSize = getCacheSize()
        // Clear all audio-related directories
        cacheDir.deleteRecursively()
        downloadsDir.deleteRecursively()
        preprocessedDir.deleteRecursively()
        // Recreate cache dir
        cacheDir.mkdirs()
        freedSize
    }

    suspend fun ensureCacheSpace(requiredBytes: Long) = withContext(Dispatchers.IO) {
        var currentSize = getCacheSize()
        val targetSize = MAX_CACHE_SIZE_BYTES - requiredBytes

        if (currentSize <= targetSize) return@withContext

        // Get all cached files sorted by last modified (oldest first = LRU)
        val files = cacheDir.listFiles()
            ?.sortedBy { it.lastModified() }
            ?: return@withContext

        for (file in files) {
            if (currentSize <= targetSize) break
            val fileSize = file.length()
            if (file.delete()) {
                currentSize -= fileSize
            }
        }
    }

    suspend fun getCacheDetails(): List<CacheFileInfo> = withContext(Dispatchers.IO) {
        cacheDir.listFiles()
            ?.map { file ->
                CacheFileInfo(
                    name = file.name,
                    size = file.length(),
                    lastAccessed = file.lastModified()
                )
            }
            ?.sortedByDescending { it.lastAccessed }
            ?: emptyList()
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }
}

data class CacheFileInfo(
    val name: String,
    val size: Long,
    val lastAccessed: Long
)
