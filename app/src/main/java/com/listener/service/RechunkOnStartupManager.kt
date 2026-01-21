package com.listener.service

import android.util.Log
import com.listener.data.local.db.dao.TranscriptionDao
import com.listener.data.repository.SettingsRepository
import com.listener.domain.model.ChunkSettings
import com.listener.domain.usecase.RechunkUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RechunkOnStartupManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transcriptionDao: TranscriptionDao,
    private val rechunkUseCase: RechunkUseCase
) {
    companion object {
        private const val TAG = "RechunkOnStartup"
    }

    private val _isRechunking = MutableStateFlow(false)
    val isRechunking: StateFlow<Boolean> = _isRechunking.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            checkAndRechunkIfNeeded()
        }
    }

    private suspend fun checkAndRechunkIfNeeded() {
        val pending = settingsRepository.pendingRechunk.first()
        if (!pending) {
            Log.d(TAG, "No pending rechunk, skipping")
            return
        }

        val transcriptions = transcriptionDao.getAllTranscriptionsList()
        if (transcriptions.isEmpty()) {
            Log.d(TAG, "No transcriptions to rechunk")
            settingsRepository.setPendingRechunk(false)
            return
        }

        Log.d(TAG, "Starting rechunk for ${transcriptions.size} transcriptions")
        _isRechunking.value = true

        try {
            val appSettings = settingsRepository.settings.first()
            val chunkSettings = ChunkSettings(
                sentenceOnly = appSettings.sentenceOnly,
                minChunkMs = appSettings.minChunkMs
            )

            transcriptions.forEach { transcription ->
                Log.d(TAG, "Rechunking: ${transcription.sourceId}")
                rechunkUseCase.execute(
                    sourceId = transcription.sourceId,
                    newSettings = chunkSettings,
                    deleteRecordings = true
                )
            }

            settingsRepository.setPendingRechunk(false)
            Log.d(TAG, "Rechunk complete")
        } catch (e: Exception) {
            Log.e(TAG, "Rechunk failed: ${e.message}", e)
        } finally {
            _isRechunking.value = false
        }
    }
}
