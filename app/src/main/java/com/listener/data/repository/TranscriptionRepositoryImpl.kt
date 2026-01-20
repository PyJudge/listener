package com.listener.data.repository

import com.google.gson.Gson
import com.listener.data.local.db.dao.ChunkSettingsDao
import com.listener.data.local.db.dao.TranscriptionDao
import com.listener.data.local.db.entity.ChunkEntity
import com.listener.data.local.db.entity.ChunkSettingsEntity
import com.listener.data.local.db.entity.TranscriptionResultEntity
import com.listener.domain.model.Chunk
import com.listener.domain.model.ChunkSettings
import com.listener.domain.model.Segment
import com.listener.domain.model.WhisperResult
import com.listener.domain.model.Word
import com.listener.domain.repository.TranscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepositoryImpl @Inject constructor(
    private val transcriptionDao: TranscriptionDao,
    private val chunkSettingsDao: ChunkSettingsDao,
    private val gson: Gson
) : TranscriptionRepository {

    override suspend fun hasTranscription(sourceId: String, language: String): Boolean {
        return transcriptionDao.hasTranscription(sourceId, language)
    }

    override suspend fun getTranscription(sourceId: String): WhisperResult? {
        val entity = transcriptionDao.getTranscription(sourceId) ?: return null
        return WhisperResult(
            text = entity.fullText,
            segments = gson.fromJson(entity.segmentsJson, Array<Segment>::class.java).toList(),
            words = gson.fromJson(entity.wordsJson, Array<Word>::class.java).toList()
        )
    }

    override suspend fun saveTranscription(sourceId: String, language: String, result: WhisperResult) {
        val entity = TranscriptionResultEntity(
            sourceId = sourceId,
            language = language,
            fullText = result.text,
            segmentsJson = gson.toJson(result.segments),
            wordsJson = gson.toJson(result.words),
            createdAt = System.currentTimeMillis()
        )
        transcriptionDao.insertTranscription(entity)
    }

    override suspend fun getChunks(sourceId: String): List<Chunk> {
        return transcriptionDao.getChunksList(sourceId).map { it.toDomain() }
    }

    override fun observeChunks(sourceId: String): Flow<List<Chunk>> {
        return transcriptionDao.getChunks(sourceId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveChunks(sourceId: String, chunks: List<Chunk>) {
        val entities = chunks.map { chunk ->
            ChunkEntity(
                sourceId = sourceId,
                orderIndex = chunk.orderIndex,
                startMs = chunk.startMs,
                endMs = chunk.endMs,
                displayText = chunk.displayText
            )
        }
        transcriptionDao.deleteChunks(sourceId)
        transcriptionDao.insertChunks(entities)
    }

    override suspend fun deleteChunks(sourceId: String) {
        transcriptionDao.deleteChunks(sourceId)
    }

    override suspend fun getChunkSettings(sourceId: String): ChunkSettings? {
        val entity = chunkSettingsDao.getSettings(sourceId) ?: return null
        return ChunkSettings(
            sentenceOnly = entity.sentenceOnly,
            minChunkMs = entity.minChunkMs
        )
    }

    override suspend fun saveChunkSettings(sourceId: String, settings: ChunkSettings) {
        val entity = ChunkSettingsEntity(
            sourceId = sourceId,
            sentenceOnly = settings.sentenceOnly,
            minChunkMs = settings.minChunkMs,
            updatedAt = System.currentTimeMillis()
        )
        chunkSettingsDao.insertSettings(entity)
    }

    private fun ChunkEntity.toDomain() = Chunk(
        orderIndex = orderIndex,
        startMs = startMs,
        endMs = endMs,
        displayText = displayText
    )
}
