package com.listener.data.remote.api

import com.listener.data.remote.dto.WhisperResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface OpenAiApi {
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribe(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("response_format") responseFormat: RequestBody,
        @Part("timestamp_granularities[]") timestampGranularity1: RequestBody,
        @Part("timestamp_granularities[]") timestampGranularity2: RequestBody,
        @Part("language") language: RequestBody?,
        @Part("prompt") prompt: RequestBody?
    ): WhisperResponse
}
