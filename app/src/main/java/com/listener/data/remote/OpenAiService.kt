package com.listener.data.remote

import com.listener.data.remote.api.OpenAiApi
import com.listener.data.remote.dto.WhisperResponse
import com.listener.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class ApiKeyNotSetException : Exception("OpenAI API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요.")

@Singleton
class OpenAiService @Inject constructor(
    private val openAiApi: OpenAiApi,
    private val settingsRepository: SettingsRepository
) {
    suspend fun transcribe(
        audioFile: File,
        language: String? = null
    ): WhisperResponse {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            throw ApiKeyNotSetException()
        }

        val filePart = MultipartBody.Part.createFormData(
            "file",
            audioFile.name,
            audioFile.asRequestBody("audio/*".toMediaType())
        )

        val modelPart = "whisper-1".toRequestBody("text/plain".toMediaType())
        val formatPart = "verbose_json".toRequestBody("text/plain".toMediaType())
        val segmentPart = "segment".toRequestBody("text/plain".toMediaType())
        val wordPart = "word".toRequestBody("text/plain".toMediaType())
        val languagePart = language?.toRequestBody("text/plain".toMediaType())

        return openAiApi.transcribe(
            authorization = "Bearer $apiKey",
            file = filePart,
            model = modelPart,
            responseFormat = formatPart,
            timestampGranularity1 = segmentPart,
            timestampGranularity2 = wordPart,
            language = languagePart
        )
    }

    suspend fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    private suspend fun getApiKey(): String {
        return settingsRepository.settings.first().openAiApiKey
    }
}
