package com.listener.domain.usecase.chunking

import com.listener.domain.model.Segment
import com.listener.domain.model.WhisperResult
import com.listener.domain.model.Word
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ChunkingUseCase 테스트
 *
 * 주요 테스트 케이스:
 * 1. 기본 청킹 동작
 * 2. 동일 세그먼트 내 복수 문장 타임스탬프 분리 (버그 수정 검증)
 * 3. 검색 범위 제한 동작
 */
class ChunkingUseCaseTest {

    private lateinit var chunkingUseCase: ChunkingUseCase
    private lateinit var sentenceSplitter: SentenceSplitter
    private lateinit var timestampMatcher: TimestampMatcher
    private lateinit var chunkMerger: ChunkMerger
    private lateinit var duplicateRemover: DuplicateRemover

    @Before
    fun setup() {
        sentenceSplitter = SentenceSplitter()
        timestampMatcher = TimestampMatcher()
        chunkMerger = ChunkMerger()
        duplicateRemover = DuplicateRemover()
        chunkingUseCase = ChunkingUseCase(
            sentenceSplitter,
            timestampMatcher,
            chunkMerger,
            duplicateRemover
        )
    }

    @Test
    fun `basic chunking produces correct timestamps`() {
        val whisperResult = WhisperResult(
            text = "Hello world. Goodbye world.",
            segments = listOf(
                Segment(
                    start = 0.0,
                    end = 5.0,
                    text = "Hello world. Goodbye world."
                )
            ),
            words = listOf(
                Word(word = "Hello", start = 0.0, end = 0.5),
                Word(word = "world.", start = 0.5, end = 1.0),
                Word(word = "Goodbye", start = 1.5, end = 2.0),
                Word(word = "world.", start = 2.0, end = 2.5)
            )
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        assertEquals(2, chunks.size)
        assertEquals("Hello world.", chunks[0].displayText)
        assertEquals("Goodbye world.", chunks[1].displayText)
    }

    /**
     * 버그 수정 검증: 동일 세그먼트 내 복수 문장이 서로 다른 타임스탬프를 가져야 함
     *
     * 이전 버그:
     * - "It was brilliant. It was such a proud moment." 세그먼트에서
     * - 두 문장 모두 동일한 타임스탬프 (90540, 92280)를 가짐
     *
     * 수정 후:
     * - 각 문장이 개별 단어 타임스탬프를 기반으로 분리됨
     */
    @Test
    fun `multiple sentences in same segment have different timestamps`() {
        // Happy Pod 실제 데이터 재현
        val whisperResult = WhisperResult(
            text = "It was brilliant. It was such a proud moment.",
            segments = listOf(
                Segment(
                    start = 90.54,
                    end = 92.28,
                    text = "It was brilliant. It was such a proud moment."
                )
            ),
            words = listOf(
                Word(word = "It", start = 90.52, end = 90.68),
                Word(word = "was", start = 90.68, end = 90.82),
                Word(word = "brilliant.", start = 90.82, end = 91.30),
                Word(word = "It", start = 91.30, end = 91.32),
                Word(word = "was", start = 91.32, end = 91.42),
                Word(word = "such", start = 91.42, end = 91.54),
                Word(word = "a", start = 91.54, end = 91.64),
                Word(word = "proud", start = 91.64, end = 91.86),
                Word(word = "moment.", start = 91.86, end = 92.42)
            )
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        assertEquals(2, chunks.size)

        val chunk1 = chunks[0]
        val chunk2 = chunks[1]

        assertEquals("It was brilliant.", chunk1.displayText)
        assertEquals("It was such a proud moment.", chunk2.displayText)

        // 핵심 검증: 두 청크가 다른 타임스탬프를 가져야 함
        assertNotEquals(
            "두 청크의 시작 시간이 달라야 함 (버그: 둘 다 세그먼트 시작 시간 사용)",
            chunk1.startMs,
            chunk2.startMs
        )
        assertNotEquals(
            "두 청크의 끝 시간이 달라야 함 (버그: 둘 다 세그먼트 끝 시간 사용)",
            chunk1.endMs,
            chunk2.endMs
        )

        // 타임스탬프 정확성 검증
        // chunk1: "It was brilliant." -> 90.52 ~ 91.30
        assertEquals(90520, chunk1.startMs)
        assertEquals(91300, chunk1.endMs)

        // chunk2: "It was such a proud moment." -> 91.30 ~ 92.42
        assertEquals(91300, chunk2.startMs)
        assertEquals(92420, chunk2.endMs)
    }

    @Test
    fun `chunks do not overlap in timestamps`() {
        val whisperResult = WhisperResult(
            text = "First sentence. Second sentence. Third sentence.",
            segments = listOf(
                Segment(start = 0.0, end = 10.0, text = "First sentence. Second sentence. Third sentence.")
            ),
            words = listOf(
                Word(word = "First", start = 0.0, end = 0.5),
                Word(word = "sentence.", start = 0.5, end = 1.5),
                Word(word = "Second", start = 2.0, end = 2.5),
                Word(word = "sentence.", start = 2.5, end = 3.5),
                Word(word = "Third", start = 4.0, end = 4.5),
                Word(word = "sentence.", start = 4.5, end = 5.5)
            )
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        assertEquals(3, chunks.size)

        // 각 청크가 순차적 타임스탬프를 가져야 함
        for (i in 0 until chunks.size - 1) {
            assertTrue(
                "청크 ${i}의 끝(${chunks[i].endMs})이 청크 ${i + 1}의 시작(${chunks[i + 1].startMs})보다 작거나 같아야 함",
                chunks[i].endMs <= chunks[i + 1].startMs
            )
        }
    }

    @Test
    fun `empty segments returns empty list`() {
        val whisperResult = WhisperResult(
            text = "",
            segments = emptyList(),
            words = emptyList()
        )

        val chunks = chunkingUseCase.process(whisperResult)

        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `words without timestamps falls back gracefully`() {
        val whisperResult = WhisperResult(
            text = "Hello world.",
            segments = listOf(
                Segment(start = 0.0, end = 5.0, text = "Hello world.")
            ),
            words = emptyList()  // 단어 타임스탬프 없음
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        assertEquals(1, chunks.size)
        assertEquals("Hello world.", chunks[0].displayText)
        // 세그먼트 타임스탬프 사용
        assertEquals(0, chunks[0].startMs)
        assertEquals(5000, chunks[0].endMs)
    }

    @Test
    fun `search range is limited to prevent wrong matches`() {
        // 같은 단어가 반복되는 케이스
        val whisperResult = WhisperResult(
            text = "The end. The end is near. The end.",
            segments = listOf(
                Segment(start = 0.0, end = 10.0, text = "The end. The end is near. The end.")
            ),
            words = listOf(
                Word(word = "The", start = 0.0, end = 0.2),
                Word(word = "end.", start = 0.2, end = 0.5),
                Word(word = "The", start = 1.0, end = 1.2),
                Word(word = "end", start = 1.2, end = 1.5),
                Word(word = "is", start = 1.5, end = 1.7),
                Word(word = "near.", start = 1.7, end = 2.0),
                Word(word = "The", start = 3.0, end = 3.2),
                Word(word = "end.", start = 3.2, end = 3.5)
            )
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        assertEquals(3, chunks.size)

        // 각 "The end" 문장이 올바른 위치에 매칭되어야 함
        assertEquals("The end.", chunks[0].displayText)
        assertEquals("The end is near.", chunks[1].displayText)
        assertEquals("The end.", chunks[2].displayText)

        // 첫 번째와 세 번째 청크가 다른 타임스탬프를 가져야 함
        assertNotEquals(chunks[0].startMs, chunks[2].startMs)
    }

    // ===== Chunk-Sound 동기화 버그 테스트 =====

    /**
     * 핵심 원칙: displayText와 (startMs, endMs)는 반드시 동기화되어야 함
     *
     * displayText의 각 단어가 startMs~endMs 범위 내 words와 일치해야 함
     */
    @Test
    fun `displayText must be generated from matched word range`() {
        // 시나리오: words 배열과 segment 텍스트가 약간 다름
        // words에서는 "Hello world" + "there", segment에서는 "Hello world. there."
        val whisperResult = WhisperResult(
            text = "Hello world. there.",
            segments = listOf(
                Segment(start = 0.0, end = 3.0, text = "Hello world. there.")
            ),
            words = listOf(
                Word(word = "Hello", start = 0.0, end = 0.5),
                Word(word = "world.", start = 0.5, end = 1.0),
                Word(word = "there.", start = 1.5, end = 2.0)
            )
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        // 각 chunk의 displayText가 해당 시간 범위의 words와 일치하는지 검증
        for (chunk in chunks) {
            val startSec = chunk.startMs / 1000.0
            val endSec = chunk.endMs / 1000.0

            // chunk의 시간 범위 내에 있는 words 추출
            val wordsInRange = whisperResult.words.filter { word ->
                word.start >= startSec - 0.01 && word.end <= endSec + 0.01
            }

            // displayText가 해당 words로 구성되어 있어야 함
            val expectedText = wordsInRange.joinToString(" ") { it.word }
            assertEquals(
                "Chunk displayText should match words in time range",
                expectedText,
                chunk.displayText
            )
        }
    }

    /**
     * Essential 에피소드 chunk 14 버그 시나리오:
     * - displayText: "Our attentional systems draw us to that." (7단어)
     * - 잘못된 매칭 시: endMs가 15초 뒤의 "that"을 가리킴
     *
     * 이 테스트는 displayText와 timestamps가 항상 동기화되어야 함을 검증
     */
    @Test
    fun `chunk displayText and timestamps must be synchronized - essential episode scenario`() {
        // Essential 에피소드 실제 데이터 간소화 버전
        val whisperResult = WhisperResult(
            text = "brains are drawn to that. Our attentional systems draw us to that. And when you",
            segments = listOf(
                Segment(
                    start = 61.72,
                    end = 66.24,
                    text = "brains are drawn to that. Our attentional systems draw us to that. And when you"
                )
            ),
            words = listOf(
                // 첫 번째 문장의 끝
                Word(word = "brains", start = 61.72, end = 62.0),
                Word(word = "are", start = 62.0, end = 62.18),
                Word(word = "drawn", start = 62.18, end = 62.44),
                Word(word = "to", start = 62.44, end = 62.68),
                Word(word = "that.", start = 62.68, end = 62.98),  // 첫 번째 "that." (index 4)

                // 두 번째 문장 (검증 대상)
                Word(word = "Our", start = 62.98, end = 63.04),    // index 5
                Word(word = "attentional", start = 63.04, end = 63.56),
                Word(word = "systems", start = 63.56, end = 63.9),
                Word(word = "draw", start = 63.9, end = 64.28),
                Word(word = "us", start = 64.28, end = 64.48),
                Word(word = "to", start = 64.48, end = 64.6),
                Word(word = "that.", start = 64.6, end = 64.94),   // 두 번째 "that." (index 11) ← 정답!

                // 세 번째 문장 시작
                Word(word = "And", start = 64.94, end = 65.44),
                Word(word = "when", start = 65.44, end = 65.82),
                Word(word = "you", start = 65.82, end = 66.1)
            )
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        // 두 번째 청크 검증: "Our attentional systems draw us to that."
        val chunk2 = chunks.find { it.displayText.contains("attentional") }
        assertNotNull("Should have chunk with 'attentional'", chunk2)

        // 핵심 검증: displayText의 단어 수와 duration이 합리적인 비율
        val wordCount = chunk2!!.displayText.split(Regex("\\s+")).size
        val durationSec = chunk2.durationMs / 1000.0

        // 초당 약 2-5단어가 합리적 (한 단어당 0.2~0.5초)
        val wordsPerSec = wordCount / durationSec
        assertTrue(
            "Words per second ($wordsPerSec) should be between 1 and 10. " +
                    "displayText='${chunk2.displayText}', duration=${chunk2.durationMs}ms",
            wordsPerSec in 1.0..10.0
        )

        // endMs가 70000ms 이전이어야 함 (79900ms가 아님!)
        assertTrue(
            "endMs (${chunk2.endMs}) should be less than 70000ms (not 79900ms)",
            chunk2.endMs < 70000
        )
    }

    /**
     * displayText는 원본 sentence에서 가져옴 (구두점 보존)
     * - words는 구두점이 없는 경우가 많아 ChunkMerger가 병합 문제 발생
     * - sentence를 사용하면 구두점이 있어 문장별로 올바르게 분리됨
     */
    @Test
    fun `displayText should come from sentence to preserve punctuation`() {
        // segment 텍스트와 words의 텍스트가 약간 다른 경우
        val whisperResult = WhisperResult(
            text = "Hello, world!",
            segments = listOf(
                Segment(
                    start = 0.0,
                    end = 2.0,
                    text = "Hello, world!"  // 쉼표와 느낌표 포함
                )
            ),
            words = listOf(
                Word(word = "Hello", start = 0.0, end = 0.5),   // 쉼표 없음
                Word(word = "world!", start = 0.5, end = 1.0)  // 느낌표 있음
            )
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        assertEquals(1, chunks.size)

        // displayText가 sentence에서 생성됨 (구두점 보존)
        assertEquals("Hello, world!", chunks[0].displayText)
    }
}
