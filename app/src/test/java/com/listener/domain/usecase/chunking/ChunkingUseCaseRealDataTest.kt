package com.listener.domain.usecase.chunking

import com.listener.domain.model.Segment
import com.listener.domain.model.WhisperResult
import com.listener.domain.model.Word
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
            TimestampMatcher(),
            ChunkMerger(),
            DuplicateRemover()
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
