package com.listener.data.remote

import com.listener.data.remote.api.OpenAiApi
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

class ApiKeyNotSetException(provider: String) :
    Exception("$provider API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요.")

@Singleton
class OpenAiService @Inject constructor(
    private val openAiApi: OpenAiApi,
    private val settingsRepository: SettingsRepository
) : TranscriptionService {

    override val providerName = "OpenAI"
    override val modelName = "whisper-1"

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
        val promptPart = promptForLanguage(language).toRequestBody("text/plain".toMediaType())

        return openAiApi.transcribe(
            authorization = "Bearer $apiKey",
            file = filePart,
            model = modelPart,
            responseFormat = formatPart,
            timestampGranularity1 = segmentPart,
            timestampGranularity2 = wordPart,
            language = languagePart,
            prompt = promptPart
        )
    }

    override suspend fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    private suspend fun getApiKey(): String {
        return settingsRepository.settings.first().openAiApiKey
    }

    private fun promptForLanguage(language: String?): String {
        return when (language) {
            "en" -> "Hello, welcome to my lecture."
            "ko" -> "안녕하세요. 강의를 시작하겠습니다."
            "ja" -> "こんにちは。講義を始めます。"
            "zh" -> "你好，欢迎来到我的讲座。"
            "es" -> "Hola, bienvenido a mi conferencia."
            "fr" -> "Bonjour, bienvenue à ma conférence."
            "de" -> "Hallo, willkommen zu meinem Vortrag."
            else -> "Hello."
        }
    }
}
