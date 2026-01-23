package com.listener.domain.usecase.chunking

import com.listener.domain.model.Segment
import com.listener.domain.model.WhisperResult
import com.listener.domain.model.Word
import com.listener.domain.usecase.chunking.aligner.TwoPointerAligner
import com.listener.domain.usecase.chunking.aligner.TimestampAssigner
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

class ChunkingDebugTest {

    private lateinit var realWhisperResult: WhisperResult
    private val gson = Gson()

    @Before
    fun setup() {
        realWhisperResult = loadRealEpisodeData()
    }

    @Test
    fun `debug chunking process`() {
        val sentenceSplitter = SentenceSplitter()
        val duplicateRemover = DuplicateRemover()
        val aligner = TwoPointerAligner()
        val timestampAssigner = TimestampAssigner()

        val words = duplicateRemover.removeDuplicates(realWhisperResult.words)
        val segments = realWhisperResult.segments

        println("Total words: ${words.size}")
        println("Total segments: ${segments.size}")

        val fullText = segments.joinToString(" ") { it.text.trim() }
        val sentences = sentenceSplitter.split(fullText, true)

        println("Total sentences: ${sentences.size}")

        var prevEndTimeMs = 0L
        var skippedEmpty = 0
        var skippedValidation = 0
        var windowNotFound = 0
        var successCount = 0

        for ((sentenceIdx, sentence) in sentences.withIndex()) {
            val phrases = sentence.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (phrases.isEmpty()) {
                skippedEmpty++
                continue
            }

            // 시간 기반 윈도우 찾기
            val timeSec = prevEndTimeMs / 1000.0
            var windowStartIdx = -1
            for (i in words.indices) {
                if (words[i].start >= timeSec) {
                    windowStartIdx = i
                    break
                }
            }
            
            if (windowStartIdx < 0 || windowStartIdx >= words.size) {
                windowNotFound++
                if (sentenceIdx < 5 || sentenceIdx > sentences.size - 5) {
                    println("Sentence $sentenceIdx: window not found, prevEndTimeMs=$prevEndTimeMs")
                }
                continue
            }

            val windowEnd = minOf(windowStartIdx + phrases.size * 3 + 50, words.size)
            val windowWords = words.subList(windowStartIdx, windowEnd)

            val alignResults = aligner.align(phrases, windowWords)
            val alignedTokens = timestampAssigner.assign(phrases, windowWords, alignResults)

            if (alignedTokens.isEmpty()) {
                skippedEmpty++
                continue
            }

            var startMs = alignedTokens.first().startMs
            var endMs = alignedTokens.last().endMs

            if (startMs < prevEndTimeMs || endMs <= startMs) {
                if (windowWords.isNotEmpty()) {
                    val fallbackEndIdx = minOf(phrases.size, windowWords.size) - 1
                    startMs = (windowWords.first().start * 1000).toLong()
                    endMs = (windowWords[fallbackEndIdx].end * 1000).toLong()
                }
            }

            if (startMs < prevEndTimeMs) {
                skippedValidation++
                if (sentenceIdx < 10 || sentenceIdx > sentences.size - 10) {
                    println("Sentence $sentenceIdx SKIPPED: startMs=$startMs < prevEndTimeMs=$prevEndTimeMs")
                    println("  Text: ${sentence.take(50)}...")
                }
                continue
            }

            if (endMs <= startMs) {
                endMs = startMs + 500
            }

            successCount++
            prevEndTimeMs = endMs

            if (sentenceIdx < 3) {
                println("Sentence $sentenceIdx OK: startMs=$startMs, endMs=$endMs")
                println("  Text: ${sentence.take(50)}...")
            }
        }

        println("\n=== Summary ===")
        println("Success: $successCount")
        println("Skipped (empty): $skippedEmpty")
        println("Skipped (validation): $skippedValidation")
        println("Window not found: $windowNotFound")
        println("Total sentences: ${sentences.size}")
    }

    private fun loadRealEpisodeData(): WhisperResult {
        val classLoader = javaClass.classLoader
        val segmentsStream = classLoader?.getResourceAsStream("test_segments.json")
            ?: throw IllegalStateException("test_segments.json not found")
        val segmentsJson = InputStreamReader(segmentsStream).use { it.readText() }

        val wordsStream = classLoader.getResourceAsStream("test_words.json")
            ?: throw IllegalStateException("test_words.json not found")
        val wordsJson = InputStreamReader(wordsStream).use { it.readText() }

        val segmentType = object : TypeToken<List<Segment>>() {}.type
        val wordType = object : TypeToken<List<Word>>() {}.type

        val segments: List<Segment> = gson.fromJson(segmentsJson, segmentType)
        val words: List<Word> = gson.fromJson(wordsJson, wordType)

        val fullText = segments.joinToString(" ") { it.text.trim() }

        return WhisperResult(
            text = fullText,
            segments = segments,
            words = words
        )
    }
}
