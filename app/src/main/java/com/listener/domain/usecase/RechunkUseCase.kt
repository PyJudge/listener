package com.listener.domain.usecase

import com.listener.domain.model.Chunk
import com.listener.domain.model.ChunkSettings
import com.listener.domain.repository.TranscriptionRepository
import com.listener.domain.usecase.chunking.ChunkingUseCase
import com.listener.service.RecordingManager
import javax.inject.Inject

/**
 * 설정 변경 시 Chunk를 재계산하는 UseCase
 *
 * - 전사 결과는 보존 (재전사 안 함)
 * - Chunk만 새로운 설정으로 재계산
 * - 기존 녹음은 삭제 (chunk 인덱스가 달라지므로)
 */
class RechunkUseCase @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository,
    private val chunkingUseCase: ChunkingUseCase,
    private val recordingManager: RecordingManager
) {
    /**
     * 설정 변경으로 인한 rechunk 수행
     *
     * @param sourceId 콘텐츠 ID
     * @param newSettings 새로운 chunk 설정
     * @param deleteRecordings 녹음 삭제 여부 (사용자가 확인한 경우 true)
     * @return 새로운 chunk 목록, 또는 녹음 삭제 확인이 필요한 경우
     */
    suspend fun execute(
        sourceId: String,
        newSettings: ChunkSettings,
        deleteRecordings: Boolean = false
    ): RechunkResult {
        // 1. 기존 설정 확인
        val oldSettings = transcriptionRepository.getChunkSettings(sourceId)

        // 2. 설정이 같으면 재계산 불필요
        if (oldSettings == newSettings) {
            val existingChunks = transcriptionRepository.getChunks(sourceId)
            return RechunkResult.Success(existingChunks)
        }

        // 3. 녹음이 있는지 확인
        val existingChunks = transcriptionRepository.getChunks(sourceId)
        val hasRecordings = existingChunks.any { chunk ->
            recordingManager.hasRecording(sourceId, chunk.orderIndex)
        }

        // 4. 녹음이 있고, 삭제 확인 안 받았으면 확인 필요
        if (hasRecordings && !deleteRecordings) {
            return RechunkResult.RecordingsExist(
                recordingCount = existingChunks.count {
                    recordingManager.hasRecording(sourceId, it.orderIndex)
                }
            )
        }

        // 5. 녹음 삭제 (필요시)
        if (hasRecordings) {
            recordingManager.deleteAllRecordings(sourceId)
        }

        // 6. 전사 결과 가져오기 (재전사 안 함!)
        val whisperResult = transcriptionRepository.getTranscription(sourceId)
            ?: return RechunkResult.Error("전사 결과를 찾을 수 없습니다")

        // 7. 새로운 설정으로 chunk 재계산
        val newChunks = chunkingUseCase.process(
            whisperResult = whisperResult,
            sentenceOnly = newSettings.sentenceOnly,
            minChunkMs = newSettings.minChunkMs
        )

        // 8. 새 chunk 저장
        transcriptionRepository.saveChunks(sourceId, newChunks)
        transcriptionRepository.saveChunkSettings(sourceId, newSettings)

        return RechunkResult.Success(newChunks)
    }

    /**
     * 녹음이 존재하는지 확인
     */
    suspend fun hasAnyRecordings(sourceId: String): Boolean {
        val chunks = transcriptionRepository.getChunks(sourceId)
        return chunks.any { recordingManager.hasRecording(sourceId, it.orderIndex) }
    }

    /**
     * 녹음 개수 확인
     */
    suspend fun getRecordingCount(sourceId: String): Int {
        val chunks = transcriptionRepository.getChunks(sourceId)
        return chunks.count { recordingManager.hasRecording(sourceId, it.orderIndex) }
    }
}

sealed class RechunkResult {
    data class Success(val chunks: List<Chunk>) : RechunkResult()
    data class RecordingsExist(val recordingCount: Int) : RechunkResult()
    data class Error(val message: String) : RechunkResult()
}
