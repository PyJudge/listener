package com.listener.domain.repository

import com.listener.domain.model.Chunk
import com.listener.domain.model.ChunkSettings
import com.listener.domain.model.WhisperResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// 전사 진행 상태 - Application 레벨에서 유지
sealed class TranscriptionState {
    data object Idle : TranscriptionState()
    data class InProgress(
        val sourceId: String,
        val step: TranscriptionStep,
        val downloadProgress: Float = 0f,
        val preprocessProgress: Float = 0f,
        val transcriptionProgress: Float = 0f
    ) : TranscriptionState()
    data class Complete(val sourceId: String, val chunkCount: Int) : TranscriptionState()
    data class Error(val sourceId: String, val message: String) : TranscriptionState()
}

enum class TranscriptionStep {
    DOWNLOADING, PREPROCESSING, TRANSCRIBING, PROCESSING
}

interface TranscriptionRepository {
    // 전사 상태 관찰 (Application 레벨 스코프에서 유지)
    val transcriptionState: StateFlow<TranscriptionState>

    // 전사 시작 (이미 진행 중이면 무시)
    fun startTranscription(sourceId: String, language: String = "en")

    // 전사 취소
    fun cancelTranscription()

    suspend fun hasTranscription(sourceId: String, language: String): Boolean
    suspend fun getTranscription(sourceId: String): WhisperResult?
    suspend fun saveTranscription(sourceId: String, language: String, result: WhisperResult)

    suspend fun getChunks(sourceId: String): List<Chunk>
    fun observeChunks(sourceId: String): Flow<List<Chunk>>
    suspend fun saveChunks(sourceId: String, chunks: List<Chunk>)
    suspend fun deleteChunks(sourceId: String)

    suspend fun getChunkSettings(sourceId: String): ChunkSettings?
    suspend fun saveChunkSettings(sourceId: String, settings: ChunkSettings)
}
