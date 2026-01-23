package com.listener.domain.usecase.chunking

import com.listener.domain.model.Segment
import com.listener.domain.model.WhisperResult
import com.listener.domain.model.Word
import com.listener.domain.usecase.chunking.aligner.TwoPointerAligner
import com.listener.domain.usecase.chunking.aligner.TimestampAssigner
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
    private lateinit var chunkMerger: ChunkMerger
    private lateinit var duplicateRemover: DuplicateRemover
    private lateinit var aligner: TwoPointerAligner
    private lateinit var timestampAssigner: TimestampAssigner

    @Before
    fun setup() {
        sentenceSplitter = SentenceSplitter()
        chunkMerger = ChunkMerger()
        duplicateRemover = DuplicateRemover()
        aligner = TwoPointerAligner()
        timestampAssigner = TimestampAssigner()
        chunkingUseCase = ChunkingUseCase(
            sentenceSplitter,
            chunkMerger,
            duplicateRemover,
            aligner,
            timestampAssigner
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

        println("DEBUG: chunk count = ${chunks.size}")
        chunks.forEach { println("  ${it.displayText} @ ${it.startMs}-${it.endMs}ms") }

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

    // ===== 드리프트 시뮬레이션 테스트 =====

    /**
     * 드리프트 문제 시뮬레이션
     *
     * 문제 상황:
     * - 300개 이상의 청크를 처리할 때 wordIdx 누적 오차 발생
     * - findStartIndex가 실패하면 누적된 wordIdx를 사용 → 드리프트 고착
     *
     * 이 테스트는 다음을 검증:
     * 1. 모든 청크의 displayText 첫 단어가 startMs 시간의 실제 단어와 일치
     * 2. 누적 오차 없이 시간 기반으로 정확한 위치 찾기
     */
    @Test
    fun `drift simulation - first word must match at startMs after many chunks`() {
        // 300개 문장 생성 (드리프트가 발생하는 상황 시뮬레이션)
        val sentenceCount = 300
        val wordsPerSentence = 5
        val wordDuration = 0.3 // 각 단어 0.3초
        val gapBetweenSentences = 0.2 // 문장 간 0.2초 gap

        val segments = mutableListOf<Segment>()
        val words = mutableListOf<Word>()

        var currentTime = 0.0

        for (i in 0 until sentenceCount) {
            val sentenceWords = mutableListOf<String>()
            val sentenceStart = currentTime

            // 각 문장에 고유한 첫 단어 + 일반 단어들
            for (j in 0 until wordsPerSentence) {
                val wordText = if (j == 0) {
                    "Word${i}" // 첫 단어는 고유하게 (Word0, Word1, Word2...)
                } else if (j == wordsPerSentence - 1) {
                    "end." // 마지막 단어
                } else {
                    "common" // 중간 단어는 반복 (드리프트 유발 가능)
                }

                words.add(Word(
                    word = wordText,
                    start = currentTime,
                    end = currentTime + wordDuration
                ))
                sentenceWords.add(wordText)
                currentTime += wordDuration
            }

            val sentenceText = sentenceWords.joinToString(" ")
            segments.add(Segment(
                start = sentenceStart,
                end = currentTime,
                text = sentenceText
            ))

            currentTime += gapBetweenSentences
        }

        val whisperResult = WhisperResult(
            text = segments.joinToString(" ") { it.text },
            segments = segments,
            words = words
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        // 검증: 모든 청크의 첫 단어가 startMs 시간과 일치해야 함
        val mismatches = mutableListOf<String>()

        for (chunk in chunks) {
            val chunkFirstWord = chunk.displayText.split(Regex("\\s+")).firstOrNull()
                ?.lowercase()?.replace(Regex("[^a-z0-9]"), "") ?: continue

            val startSec = chunk.startMs / 1000.0
            val tolerance = 0.05 // 50ms

            // startMs 시간에 가장 가까운 단어 찾기
            val nearestWord = words.minByOrNull { kotlin.math.abs(it.start - startSec) }
            val nearestWordNorm = nearestWord?.word?.lowercase()?.replace(Regex("[^a-z0-9]"), "") ?: ""

            val timeDiff = kotlin.math.abs((nearestWord?.start ?: 0.0) - startSec)

            if (timeDiff > tolerance || nearestWordNorm != chunkFirstWord) {
                mismatches.add(
                    "Chunk ${chunk.orderIndex}: text='$chunkFirstWord' @${chunk.startMs}ms, " +
                        "actual='${nearestWord?.word}' @${((nearestWord?.start ?: 0.0) * 1000).toLong()}ms"
                )
            }
        }

        assertTrue(
            "DRIFT DETECTED! ${mismatches.size} mismatches in $sentenceCount chunks:\n" +
                mismatches.take(10).joinToString("\n"),
            mismatches.isEmpty()
        )
    }

    /**
     * 드리프트 시뮬레이션 - wordIdx 누적 오차로 인한 드리프트
     *
     * 시나리오:
     * - segment 텍스트: "AAA. BBB. CCC."
     * - words 배열: 각 단어마다 word가 1개씩 더 있음 (Whisper가 추가 단어 출력)
     * - 결과: wordIdx가 점점 뒤로 밀림 → 마지막 문장의 startMs가 틀어짐
     *
     * 핵심: wordIdx 누적 방식은 Whisper의 추가/누락 단어에 취약
     */
    @Test
    fun `drift due to wordIdx accumulation - extra words cause offset`() {
        // 시나리오: Whisper가 각 문장마다 추가 단어를 출력
        val segments = listOf(
            Segment(start = 0.0, end = 2.0, text = "First sentence."),
            Segment(start = 2.5, end = 4.5, text = "Second sentence."),
            Segment(start = 5.0, end = 7.0, text = "Third sentence."),
            Segment(start = 7.5, end = 9.5, text = "Fourth sentence."),
            Segment(start = 10.0, end = 12.0, text = "Final sentence.")
        )

        val words = listOf(
            // 첫 문장 + 추가 단어
            Word(word = "First", start = 0.0, end = 0.4),
            Word(word = "sentence.", start = 0.4, end = 0.8),
            Word(word = "um", start = 1.0, end = 1.2),  // 추가 단어 (filler)

            // 두 번째 문장 + 추가 단어
            Word(word = "Second", start = 2.5, end = 2.9),
            Word(word = "sentence.", start = 2.9, end = 3.3),
            Word(word = "uh", start = 3.5, end = 3.7),  // 추가 단어

            // 세 번째 문장 + 추가 단어
            Word(word = "Third", start = 5.0, end = 5.4),
            Word(word = "sentence.", start = 5.4, end = 5.8),
            Word(word = "yeah", start = 6.0, end = 6.2),  // 추가 단어

            // 네 번째 문장 + 추가 단어
            Word(word = "Fourth", start = 7.5, end = 7.9),
            Word(word = "sentence.", start = 7.9, end = 8.3),
            Word(word = "so", start = 8.5, end = 8.7),  // 추가 단어

            // 다섯 번째 문장 (Final) - startMs는 10000ms여야 함
            Word(word = "Final", start = 10.0, end = 10.4),
            Word(word = "sentence.", start = 10.4, end = 10.8)
        )

        val whisperResult = WhisperResult(
            text = segments.joinToString(" ") { it.text },
            segments = segments,
            words = words
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        // Final sentence 청크 찾기
        val finalChunk = chunks.find { it.displayText.contains("Final") }
        assertNotNull("Should have 'Final sentence.' chunk", finalChunk)

        // 핵심 검증: Final의 startMs는 10000ms여야 함
        // 드리프트가 있으면 wordIdx가 밀려서 다른 위치를 가리킴
        val expectedStartMs = 10000L
        val tolerance = 100L

        assertTrue(
            "DRIFT! Final chunk startMs (${finalChunk!!.startMs}) should be ${expectedStartMs}ms ± ${tolerance}ms. " +
                "If using accumulated wordIdx with extra words, it will be wrong.",
            kotlin.math.abs(finalChunk.startMs - expectedStartMs) <= tolerance
        )
    }

    /**
     * 드리프트 시뮬레이션 - findStartIndex 검색 범위 밖 문제
     *
     * 시나리오:
     * - 청크 처리 중 wordIdx가 실제 위치보다 뒤처짐
     * - maxSearch = wordIdx + wordCount*3 + 100 범위에 실제 단어가 없음
     * - findStartIndex 실패 → wordIdx fallback → 드리프트 고착
     */
    @Test
    fun `drift when actual position is outside search range`() {
        // 시나리오: words에 큰 gap이 있어서 wordIdx가 실제 위치에서 많이 벗어남
        val segments = listOf(
            Segment(start = 0.0, end = 1.0, text = "Start here."),
            Segment(start = 50.0, end = 51.0, text = "Far away.")  // 50초 뒤!
        )

        // words: 첫 문장은 정상, 두 번째 문장은 50초 뒤
        val words = mutableListOf<Word>()

        // 첫 문장
        words.add(Word(word = "Start", start = 0.0, end = 0.3))
        words.add(Word(word = "here.", start = 0.3, end = 0.6))

        // 중간에 많은 filler words (예: 팟캐스트 광고 구간)
        for (i in 1..200) {
            words.add(Word(word = "ad$i", start = i * 0.2, end = i * 0.2 + 0.1))
        }

        // 두 번째 문장 (50초 위치)
        words.add(Word(word = "Far", start = 50.0, end = 50.3))
        words.add(Word(word = "away.", start = 50.3, end = 50.6))

        val whisperResult = WhisperResult(
            text = segments.joinToString(" ") { it.text },
            segments = segments,
            words = words
        )

        val chunks = chunkingUseCase.process(whisperResult, minChunkMs = 0)

        // "Far away." 청크 찾기
        val farChunk = chunks.find { it.displayText.contains("Far") }
        assertNotNull("Should have 'Far away.' chunk", farChunk)

        // 핵심 검증: startMs는 50000ms여야 함
        val expectedStartMs = 50000L
        val tolerance = 100L

        assertTrue(
            "DRIFT! 'Far away.' startMs (${farChunk!!.startMs}) should be ${expectedStartMs}ms. " +
                "If wordIdx-based search failed, it will be at wrong position.",
            kotlin.math.abs(farChunk.startMs - expectedStartMs) <= tolerance
        )
    }
}
