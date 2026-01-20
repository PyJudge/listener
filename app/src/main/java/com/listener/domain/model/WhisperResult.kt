package com.listener.domain.model

data class WhisperResult(
    val text: String,
    val segments: List<Segment>,
    val words: List<Word>
)

data class Segment(
    val start: Double,
    val end: Double,
    val text: String
)

data class Word(
    val word: String,
    val start: Double,
    val end: Double
)
