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
 * 실제 Huberman Lab Essential 에피소드 데이터로 Chunking 검증
 *
 * 테스트 데이터: 약 40분 팟캐스트
 * - words: 6,500+ 단어
 * - segments: 100+ 세그먼트
 *
 * 테스트 통과 조건:
 * 1. 모든 청크 duration < 60초
 * 2. 청크 개수 >= 30개
 * 3. 동일 (startMs, endMs) 청크 없음
 * 4. 연속 동일 텍스트 청크 없음
 */
class ChunkingUseCaseRealDataTest {

    private lateinit var chunkingUseCase: ChunkingUseCase
    private lateinit var realWhisperResult: WhisperResult

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

        // 실제 데이터 로드 (테스트 리소스에서)
        realWhisperResult = loadRealEpisodeData()
    }

    /**
     * 조건 1: 모든 청크가 1분(60초) 이하
     */
    @Test
    fun `all chunks must be under 60 seconds`() {
        val chunks = chunkingUseCase.process(realWhisperResult)

        val longChunks = chunks.filter { it.durationMs > 60_000 }

        assertTrue(
            "Found ${longChunks.size} chunks over 60 seconds: " +
                longChunks.take(5).joinToString { "idx=${it.orderIndex}, dur=${it.durationMs}ms" },
            longChunks.isEmpty()
        )
    }

    /**
     * 조건 2: 청크가 최소 30개 이상 생성
     */
    @Test
    fun `must produce at least 30 chunks`() {
        val chunks = chunkingUseCase.process(realWhisperResult)

        assertTrue(
            "Expected at least 30 chunks, but got ${chunks.size}",
            chunks.size >= 30
        )
    }

    /**
     * 조건 3: 같은 시간대 겹치는 청크 없음
     */
    @Test
    fun `no overlapping timestamps`() {
        val chunks = chunkingUseCase.process(realWhisperResult)

        // 동일한 (startMs, endMs) 조합이 없어야 함
        val timestampPairs = chunks.map { Pair(it.startMs, it.endMs) }
        val uniquePairs = timestampPairs.toSet()

        assertEquals(
            "Found ${timestampPairs.size - uniquePairs.size} duplicate timestamp pairs",
            timestampPairs.size,
            uniquePairs.size
        )
    }

    /**
     * 조건 4: 연속 동일 텍스트 없음 (중복 청크 방지)
     */
    @Test
    fun `no consecutive duplicate text chunks`() {
        val chunks = chunkingUseCase.process(realWhisperResult)

        val duplicates = mutableListOf<Pair<Int, String>>()
        for (i in 0 until chunks.size - 1) {
            if (chunks[i].displayText == chunks[i + 1].displayText) {
                duplicates.add(Pair(i, chunks[i].displayText))
            }
        }

        assertTrue(
            "Found ${duplicates.size} consecutive duplicates: " +
                duplicates.take(3).joinToString { "idx=${it.first}, text='${it.second.take(30)}...'" },
            duplicates.isEmpty()
        )
    }

    /**
     * 추가 검증: 청크 순서가 시간순
     */
    @Test
    fun `chunks are in chronological order`() {
        val chunks = chunkingUseCase.process(realWhisperResult)

        for (i in 0 until chunks.size - 1) {
            assertTrue(
                "Chunk $i (end=${chunks[i].endMs}) should be <= chunk ${i + 1} (start=${chunks[i + 1].startMs})",
                chunks[i].endMs <= chunks[i + 1].startMs + 100  // 100ms 오차 허용
            )
        }
    }

    /**
     * 핵심 불변조건: displayText와 타임스탬프 동기화
     *
     * 이 테스트는 실제 발생한 버그를 재현합니다:
     * - Whisper가 비순차 타임스탬프 출력 (words[177] @ 62.44s, words[178] @ 62.26s)
     * - DuplicateRemover가 정렬하면 단어 순서가 뒤바뀜
     * - 결과: "Our attentional systems draw us to that." 청크가 62980ms가 아닌 64940ms에서 시작
     *
     * 수정: DuplicateRemover에서 정렬 제거
     */
    @Test
    fun `chunk displayText must sync with audio timestamps - regression test`() {
        val chunks = chunkingUseCase.process(realWhisperResult)
        val words = realWhisperResult.words

        // "Our attentional systems draw us to that." 문장 찾기
        val targetChunk = chunks.find { it.displayText.contains("Our attentional systems draw us to that") }
        assertNotNull("Target sentence not found in chunks", targetChunk)

        // words에서 "Our attentional" 시작 위치 찾기
        var ourIndex = -1
        for (i in words.indices) {
            if (words[i].word.equals("Our", ignoreCase = true) &&
                i + 1 < words.size &&
                words[i + 1].word.equals("attentional", ignoreCase = true)) {
                ourIndex = i
                break
            }
        }
        assertTrue("'Our attentional' not found in words", ourIndex >= 0)

        val expectedStartMs = (words[ourIndex].start * 1000).toLong()

        // 핵심 검증: chunk의 startMs가 실제 "Our" 단어 시작 시간과 일치해야 함
        // 허용 오차: 500ms (이전 청크 병합으로 인한 prevEndMs 영향)
        val tolerance = 500L
        assertTrue(
            "SYNC BROKEN! Chunk startMs=${targetChunk!!.startMs}ms but 'Our' starts at ${expectedStartMs}ms. " +
                "Difference: ${targetChunk.startMs - expectedStartMs}ms (tolerance: ${tolerance}ms). " +
                "This indicates DuplicateRemover sorting is breaking word order.",
            kotlin.math.abs(targetChunk.startMs - expectedStartMs) <= tolerance
        )
    }

    /**
     * 비순차 타임스탬프 데이터에서 청크 동기화 검증
     *
     * 실제 데이터에 72개의 비순차 타임스탬프 쌍이 존재.
     * 예: words[177] "to" @ 62.44s, words[178] "that." @ 62.26s (역순!)
     *
     * 이 테스트는 비순차 데이터가 있어도 청크가 올바르게 생성되는지 확인.
     */
    @Test
    fun `handles out-of-order timestamps without breaking sync`() {
        val words = realWhisperResult.words

        // 비순차 타임스탬프 쌍 찾기
        val outOfOrderPairs = mutableListOf<Triple<Int, String, String>>()
        for (i in 1 until words.size) {
            if (words[i].start < words[i - 1].start) {
                outOfOrderPairs.add(Triple(i, words[i - 1].word, words[i].word))
            }
        }

        // 비순차 데이터가 존재함을 확인
        assertTrue(
            "Expected out-of-order timestamps in test data, but found none. " +
                "This test data should have ~72 out-of-order pairs.",
            outOfOrderPairs.isNotEmpty()
        )

        // 청크 생성
        val chunks = chunkingUseCase.process(realWhisperResult)

        // 모든 청크의 startMs가 이전 청크의 endMs 이후여야 함 (시간순)
        for (i in 1 until chunks.size) {
            assertTrue(
                "Chunk $i (start=${chunks[i].startMs}) overlaps with chunk ${i - 1} (end=${chunks[i - 1].endMs})",
                chunks[i].startMs >= chunks[i - 1].endMs - 100 // 100ms 오차 허용
            )
        }

        // 청크 duration이 비정상적으로 길지 않아야 함 (동기화 오류 징후)
        val maxReasonableDuration = 30_000L // 30초
        val suspiciousChunks = chunks.filter { it.durationMs > maxReasonableDuration }
        assertTrue(
            "Found ${suspiciousChunks.size} chunks with duration > 30s, " +
                "which may indicate sync issues: ${suspiciousChunks.take(3).map { "idx=${it.orderIndex}, dur=${it.durationMs}ms" }}",
            suspiciousChunks.size <= 5 // 일부 긴 문장은 허용
        )
    }

    /**
     * 드리프트 방지 테스트: 첫 단어 강제 동기화
     *
     * 핵심 불변조건: 모든 청크의 displayText 첫 단어는 startMs 위치의 오디오와 일치해야 함
     *
     * 드리프트란?
     * - 이전 청크의 끝 매칭 오류가 다음 청크의 시작 위치에 영향
     * - 오차가 누적되어 청크 300번대에서는 완전히 다른 위치가 됨
     *
     * 이 테스트는 청크의 첫 단어가 words 배열에서 해당 시간에 실제로 존재하는지 확인
     */
    @Test
    fun `no drift - chunk first word must match audio at startMs`() {
        val chunks = chunkingUseCase.process(realWhisperResult)
        val words = realWhisperResult.words

        // 샘플 청크들 검증 (처음, 중간, 끝)
        val sampleIndices = listOf(0, 10, 50, 100, 150, 200, 250, 300, 350, 400)
            .filter { it < chunks.size }

        val mismatches = mutableListOf<String>()

        for (idx in sampleIndices) {
            val chunk = chunks[idx]
            val chunkFirstWord = chunk.displayText.split(Regex("\\s+")).firstOrNull()
                ?.lowercase()?.replace(Regex("[^a-z0-9]"), "") ?: continue

            // startMs 근처(±500ms)에서 해당 단어 찾기
            val startSec = chunk.startMs / 1000.0
            val tolerance = 0.5 // 500ms

            val matchingWord = words.find { word ->
                val wordNorm = word.word.lowercase().replace(Regex("[^a-z0-9]"), "")
                word.start >= startSec - tolerance &&
                    word.start <= startSec + tolerance &&
                    wordNorm == chunkFirstWord
            }

            if (matchingWord == null) {
                // 실제로 그 시간에 어떤 단어가 있는지 찾기
                val actualWord = words.find { it.start >= startSec - tolerance && it.start <= startSec + tolerance }
                mismatches.add(
                    "Chunk $idx: displayText starts with '$chunkFirstWord' at ${chunk.startMs}ms, " +
                        "but actual word at that time is '${actualWord?.word ?: "none"}'"
                )
            }
        }

        assertTrue(
            "DRIFT DETECTED! ${mismatches.size} chunks have first word mismatch:\n" +
                mismatches.take(5).joinToString("\n"),
            mismatches.isEmpty()
        )
    }

    /**
     * 반복 단어 드리프트 테스트
     *
     * "that", "the", "and" 같은 흔한 단어가 반복될 때 잘못된 위치 매칭 방지
     */
    @Test
    fun `handles repeated common words without drift`() {
        val chunks = chunkingUseCase.process(realWhisperResult)
        val words = realWhisperResult.words

        // 연속된 청크들의 시간이 단조 증가해야 함 (역행 없음)
        for (i in 1 until chunks.size) {
            val prevEnd = chunks[i - 1].endMs
            val currStart = chunks[i].startMs

            // 시간 역행 검사 (100ms 오차 허용)
            assertTrue(
                "Time regression at chunk $i: prev.endMs=$prevEnd > curr.startMs=$currStart",
                currStart >= prevEnd - 100
            )

            // 큰 점프 검사 (30초 이상 갭은 의심)
            val gap = currStart - prevEnd
            assertTrue(
                "Suspicious gap at chunk $i: ${gap}ms (${gap/1000}s) gap between chunks",
                gap < 30_000 // 30초 미만
            )
        }
    }

    /**
     * 청크 300번대 드리프트 엄격 검증
     *
     * 300번 청크 주변에서 displayText 첫 단어가 startMs 시간의 실제 오디오와 일치하는지 검증.
     * 허용 오차: 200ms (더 엄격)
     */
    @Test
    fun `chunk 300 region - strict first word sync verification`() {
        val chunks = chunkingUseCase.process(realWhisperResult)
        val words = realWhisperResult.words

        // 청크 개수 확인
        println("Total chunks generated: ${chunks.size}")
        println("Total words in data: ${words.size}")

        // 300번대 청크가 없으면 가능한 마지막 50개 청크로 테스트
        val testStart = if (chunks.size > 300) 280 else maxOf(0, chunks.size - 50)
        val testEnd = minOf(testStart + 40, chunks.size)

        println("Testing chunks from $testStart to ${testEnd - 1}")

        val mismatches = mutableListOf<String>()
        val strictTolerance = 0.2 // 200ms - 더 엄격

        for (idx in testStart until testEnd) {
            val chunk = chunks[idx]
            val chunkFirstWord = chunk.displayText.split(Regex("\\s+")).firstOrNull()
                ?.lowercase()?.replace(Regex("[^a-z0-9가-힣]"), "") ?: continue

            val startSec = chunk.startMs / 1000.0

            // startMs 시간에 가장 가까운 단어 찾기
            val nearestWord = words.minByOrNull { kotlin.math.abs(it.start - startSec) }
            val nearestWordNorm = nearestWord?.word?.lowercase()?.replace(Regex("[^a-z0-9가-힣]"), "") ?: ""

            // 첫 단어가 일치하는지, 그리고 시간이 200ms 이내인지 확인
            val timeMatch = nearestWord != null && kotlin.math.abs(nearestWord.start - startSec) <= strictTolerance
            val wordMatch = nearestWordNorm == chunkFirstWord

            if (!timeMatch || !wordMatch) {
                mismatches.add(
                    "Chunk $idx: text='${chunkFirstWord}' @${chunk.startMs}ms, " +
                        "actual='${nearestWord?.word}' @${((nearestWord?.start ?: 0.0) * 1000).toLong()}ms, " +
                        "diff=${((nearestWord?.start ?: 0.0) * 1000 - chunk.startMs).toLong()}ms"
                )
            }
        }

        // 디버깅용 출력
        if (mismatches.isNotEmpty()) {
            println("=== DRIFT DETECTED ===")
            mismatches.forEach { println(it) }
        }

        assertTrue(
            "DRIFT at chunks $testStart-${testEnd-1}! ${mismatches.size} mismatches:\n" +
                mismatches.take(10).joinToString("\n"),
            mismatches.size <= 2 // 최대 2개 오차 허용 (5%)
        )
    }

    /**
     * 실제 에피소드 데이터 로드
     */
    private fun loadRealEpisodeData(): WhisperResult {
        val classLoader = javaClass.classLoader

        // test_segments.json 로드
        val segmentsStream = classLoader.getResourceAsStream("test_segments.json")
            ?: throw IllegalStateException("test_segments.json not found in test resources")
        val segmentsJson = InputStreamReader(segmentsStream).use { it.readText() }

        // test_words.json 로드
        val wordsStream = classLoader.getResourceAsStream("test_words.json")
            ?: throw IllegalStateException("test_words.json not found in test resources")
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
