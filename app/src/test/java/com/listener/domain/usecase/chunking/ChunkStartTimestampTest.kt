package com.listener.domain.usecase.chunking

import com.listener.domain.model.Segment
import com.listener.domain.model.WhisperResult
import com.listener.domain.model.Word
import com.listener.domain.usecase.chunking.aligner.TwoPointerAligner
import com.listener.domain.usecase.chunking.aligner.TimestampAssigner
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

/**
 * 청킹 시작 타임스탬프 검증 테스트
 *
 * 목표: 모든 청크의 startMs가 displayText 첫 단어의 타임스탬프와 완전히 일치(0ms)하는지 검증
 * 데이터: 실기기에서 추출한 전사 데이터
 */
class ChunkStartTimestampTest {

    private lateinit var chunkingUseCase: ChunkingUseCase
    private lateinit var realWhisperResult: WhisperResult
    private lateinit var words: List<Word>

    private val gson = Gson()

    @Before
    fun setup() {
        chunkingUseCase = ChunkingUseCase(
            SentenceSplitter(),
            ChunkMerger(),
            DuplicateRemover(),
            TwoPointerAligner(),
            TimestampAssigner()
        )

        realWhisperResult = loadRealDeviceData()
        words = DuplicateRemover().removeDuplicates(realWhisperResult.words)
    }

    /**
     * 핵심 테스트: 모든 청크의 startMs는 displayText 첫 단어의 start와 완전히 일치해야 한다
     */
    @Test
    fun `모든 청크의 startMs는 displayText 첫 단어의 start와 완전히 일치해야 한다`() {
        val chunks = chunkingUseCase.process(realWhisperResult)

        val errors = mutableListOf<String>()

        for (chunk in chunks) {
            val firstWord = extractFirstWord(chunk.displayText)
            if (firstWord.isEmpty()) continue

            val expectedStartMs = findWordTimestamp(firstWord, chunk.startMs)

            if (expectedStartMs == null) {
                errors.add(
                    "Chunk ${chunk.orderIndex}: 첫 단어 '$firstWord'를 찾을 수 없음 (startMs=${chunk.startMs})"
                )
            } else if (chunk.startMs != expectedStartMs) {
                errors.add(
                    "Chunk ${chunk.orderIndex}: startMs=${chunk.startMs}, expected=$expectedStartMs, " +
                        "firstWord='$firstWord', diff=${chunk.startMs - expectedStartMs}ms"
                )
            }
        }

        assertTrue(
            "시작 타임스탬프 불일치 (${errors.size}개):\n${errors.joinToString("\n")}",
            errors.isEmpty()
        )
    }

    /**
     * 문장에서 첫 단어 추출 (정규화)
     */
    private fun extractFirstWord(text: String): String {
        return text.split(Regex("\\s+"))
            .firstOrNull()
            ?.lowercase()
            ?.replace(Regex("[.!?,]"), "")
            ?: ""
    }

    /**
     * words 배열에서 해당 단어의 타임스탬프 찾기
     * aroundMs에 가장 가까운 매칭 단어 검색 (±5초 범위 내)
     */
    private fun findWordTimestamp(targetWord: String, aroundMs: Long): Long? {
        val aroundSec = aroundMs / 1000.0
        val tolerance = 5.0

        // aroundMs에 가장 가까운 매칭 단어 찾기
        return words.filter { word ->
            word.start >= aroundSec - tolerance &&
                word.start <= aroundSec + tolerance &&
                normalize(word.word) == targetWord
        }.minByOrNull { kotlin.math.abs(it.start - aroundSec) }
            ?.let { (it.start * 1000).toLong() }
    }

    private fun normalize(word: String): String {
        return word.lowercase().replace(Regex("[.!?,]"), "")
    }

    private fun loadRealDeviceData(): WhisperResult {
        val classLoader = javaClass.classLoader

        val segmentsStream = classLoader.getResourceAsStream("real_device_segments.json")
            ?: throw IllegalStateException("real_device_segments.json not found in test resources")
        val segmentsJson = InputStreamReader(segmentsStream).use { it.readText() }

        val wordsStream = classLoader.getResourceAsStream("real_device_words.json")
            ?: throw IllegalStateException("real_device_words.json not found in test resources")
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
