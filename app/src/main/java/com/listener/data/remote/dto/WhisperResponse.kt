package com.listener.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WhisperResponse(
    @SerializedName("text") val text: String,
    @SerializedName("segments") val segments: List<WhisperSegment>?,
    @SerializedName("words") val words: List<WhisperWord>?
)

data class WhisperSegment(
    @SerializedName("id") val id: Int,
    @SerializedName("start") val start: Double,
    @SerializedName("end") val end: Double,
    @SerializedName("text") val text: String
)

data class WhisperWord(
    @SerializedName("word") val word: String,
    @SerializedName("start") val start: Double,
    @SerializedName("end") val end: Double
)
