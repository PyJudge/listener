package com.listener.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var isRecording = false

    fun getRecordingFile(sourceId: String, chunkIndex: Int): File {
        val dir = File(context.filesDir, "recordings/$sourceId")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "chunk_$chunkIndex.m4a")
    }

    suspend fun startRecording(sourceId: String, chunkIndex: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = getRecordingFile(sourceId, chunkIndex)
                if (file.exists()) file.delete()

                currentFilePath = file.absolutePath

                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(22050)
                    setAudioEncodingBitRate(64000)
                    setAudioChannels(1)
                    setOutputFile(currentFilePath)
                    prepare()
                    start()
                }

                isRecording = true
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun stopRecording(): String? {
        return withContext(Dispatchers.IO) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                currentFilePath
            } catch (e: Exception) {
                e.printStackTrace()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false
                null
            }
        }
    }

    fun hasRecording(sourceId: String, chunkIndex: Int): Boolean {
        return getRecordingFile(sourceId, chunkIndex).exists()
    }

    fun getRecordingPath(sourceId: String, chunkIndex: Int): String? {
        val file = getRecordingFile(sourceId, chunkIndex)
        return if (file.exists()) file.absolutePath else null
    }

    suspend fun deleteRecording(sourceId: String, chunkIndex: Int): Boolean {
        return withContext(Dispatchers.IO) {
            getRecordingFile(sourceId, chunkIndex).delete()
        }
    }

    suspend fun deleteAllRecordings(sourceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "recordings/$sourceId")
            dir.deleteRecursively()
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
