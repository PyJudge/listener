package com.listener.domain.service

import com.listener.data.remote.dto.WhisperResponse
import java.io.File

/**
 * 전사 서비스 인터페이스
 * OpenAI, Groq 등 다양한 제공자 지원
 */
interface TranscriptionService {
    /**
     * 오디오 파일을 텍스트로 전사
     */
    suspend fun transcribe(
        audioFile: File,
        language: String? = null
    ): WhisperResponse

    /**
     * API 키가 설정되어 있는지 확인
     */
    suspend fun hasApiKey(): Boolean

    /**
     * 제공자 이름 (예: "OpenAI", "Groq")
     */
    val providerName: String

    /**
     * 사용 모델 이름 (예: "whisper-1", "whisper-large-v3-turbo")
     */
    val modelName: String
}
