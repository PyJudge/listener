package com.listener.data.remote

import com.listener.data.repository.SettingsRepository
import com.listener.domain.service.TranscriptionService
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 전사 서비스 팩토리
 *
 * 설정에 따라 적절한 전사 서비스 반환
 * - groq: Groq (whisper-large-v3-turbo) - 216x 빠름, 9x 저렴
 * - openai: OpenAI (whisper-1) - 기본
 */
@Singleton
class TranscriptionServiceFactory @Inject constructor(
    private val openAiService: OpenAiService,
    private val groqService: GroqService,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 설정에서 선택된 제공자의 서비스 반환
     */
    suspend fun getService(): TranscriptionService {
        val provider = settingsRepository.settings.first().transcriptionProvider
        return getService(provider)
    }

    /**
     * 특정 제공자의 서비스 반환
     */
    fun getService(provider: String): TranscriptionService {
        return when (provider) {
            "groq" -> groqService
            else -> openAiService
        }
    }

    /**
     * 사용 가능한 모든 제공자 목록
     */
    fun getAvailableProviders(): List<ProviderInfo> = listOf(
        ProviderInfo(
            id = "groq",
            name = "Groq",
            description = "whisper-large-v3-turbo | 216x faster | \$0.04/hour",
            recommended = true
        ),
        ProviderInfo(
            id = "openai",
            name = "OpenAI",
            description = "whisper-1 | Standard | \$0.36/hour",
            recommended = false
        )
    )

    data class ProviderInfo(
        val id: String,
        val name: String,
        val description: String,
        val recommended: Boolean
    )
}
