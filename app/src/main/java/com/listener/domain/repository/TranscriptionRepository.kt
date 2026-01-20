package com.listener.domain.repository

import com.listener.domain.model.Chunk
import com.listener.domain.model.ChunkSettings
import com.listener.domain.model.WhisperResult
import kotlinx.coroutines.flow.Flow

interface TranscriptionRepository {
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
