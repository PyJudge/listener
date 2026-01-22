package com.listener.data.remote

import com.listener.data.remote.api.GroqApi
import com.listener.data.remote.dto.WhisperResponse
import com.listener.data.repository.SettingsRepository
import com.listener.domain.service.TranscriptionService
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Groq Whisper API 서비스
 *
 * - 모델: whisper-large-v3-turbo (216x 실시간 속도)
 * - 가격: $0.04/hour (OpenAI 대비 9x 저렴)
 */
@Singleton
class GroqService @Inject constructor(
    private val groqApi: GroqApi,
    private val settingsRepository: SettingsRepository
) : TranscriptionService {

    override val providerName = "Groq"
    override val modelName = "whisper-large-v3-turbo"

    override suspend fun transcribe(
        audioFile: File,
        language: String?
    ): WhisperResponse {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            throw ApiKeyNotSetException(providerName)
        }

        val filePart = MultipartBody.Part.createFormData(
            "file",
            audioFile.name,
            audioFile.asRequestBody("audio/*".toMediaType())
        )

        val modelPart = modelName.toRequestBody("text/plain".toMediaType())
        val formatPart = "verbose_json".toRequestBody("text/plain".toMediaType())
        val segmentPart = "segment".toRequestBody("text/plain".toMediaType())
        val wordPart = "word".toRequestBody("text/plain".toMediaType())
        val languagePart = language?.toRequestBody("text/plain".toMediaType())

        return groqApi.transcribe(
            authorization = "Bearer $apiKey",
            file = filePart,
            model = modelPart,
            responseFormat = formatPart,
            timestampGranularity1 = segmentPart,
            timestampGranularity2 = wordPart,
            language = languagePart
        )
    }

    override suspend fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    private suspend fun getApiKey(): String {
        return settingsRepository.settings.first().groqApiKey
    }
}
