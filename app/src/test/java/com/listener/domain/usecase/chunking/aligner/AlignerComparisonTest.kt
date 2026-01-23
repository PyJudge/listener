package com.listener.domain.usecase.chunking.aligner

import com.listener.domain.model.Word
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 3가지 Aligner 알고리즘 비교 테스트
 *
 * 동일한 입력에 대해 각 알고리즘의 결과를 비교.
 */
class AlignerComparisonTest {

    private lateinit var editDistanceAligner: EditDistanceAligner
    private lateinit var greedyAligner: GreedyAligner
    private lateinit var twoPointerAligner: TwoPointerAligner
    private lateinit var timestampAssigner: TimestampAssigner

    @Before
    fun setup() {
        editDistanceAligner = EditDistanceAligner()
        greedyAligner = GreedyAligner()
        twoPointerAligner = TwoPointerAligner()
        timestampAssigner = TimestampAssigner()
    }

    // ===== 기본 테스트 =====

    @Test
    fun `basic matching - all aligners should match correctly`() {
        val phrases = listOf("Hello", "world")
        val words = listOf(
            Word("Hello", 0.0, 0.5),
            Word("world", 0.5, 1.0)
        )

        val aligners = mapOf(
            "EditDistance" to editDistanceAligner,
            "Greedy" to greedyAligner,
            "TwoPointer" to twoPointerAligner
        )

        for ((name, aligner) in aligners) {
            val results = aligner.align(phrases, words)
            val matches = results.filter { it.op == AlignOp.MATCH }

            assertEquals("$name: should have 2 matches", 2, matches.size)
            assertEquals("$name: first match phraseIdx", 0, matches[0].phraseIdx)
            assertEquals("$name: first match wordIdx", 0, matches[0].wordIdx)
            assertEquals("$name: second match phraseIdx", 1, matches[1].phraseIdx)
            assertEquals("$name: second match wordIdx", 1, matches[1].wordIdx)
        }
    }

    @Test
    fun `punctuation handling - phrases with punctuation should match`() {
        val phrases = listOf("Hello,", "world.")
        val words = listOf(
            Word("Hello", 0.0, 0.5),
            Word("world", 0.5, 1.0)
        )

        for (aligner in listOf(editDistanceAligner, greedyAligner, twoPointerAligner)) {
            val results = aligner.align(phrases, words)
            val matches = results.filter { it.op == AlignOp.MATCH }

            assertEquals("should have 2 matches", 2, matches.size)
        }
    }

    // ===== Filler 단어 테스트 =====

    @Test
    fun `filler word handling - should skip um uh etc`() {
        val phrases = listOf("Hello", "world")
        val words = listOf(
            Word("Hello", 0.0, 0.5),
            Word("um", 0.5, 0.7),
            Word("world", 0.7, 1.0)
        )

        for (aligner in listOf(editDistanceAligner, greedyAligner, twoPointerAligner)) {
            val results = aligner.align(phrases, words)
            val matches = results.filter { it.op == AlignOp.MATCH }

            assertEquals("should have 2 matches (skip 'um')", 2, matches.size)
            assertEquals("first match wordIdx", 0, matches[0].wordIdx)
            assertEquals("second match wordIdx", 2, matches[1].wordIdx)
        }
    }

    @Test
    fun `multiple fillers - should skip all`() {
        val phrases = listOf("Hello", "world")
        val words = listOf(
            Word("Hello", 0.0, 0.5),
            Word("um", 0.5, 0.6),
            Word("uh", 0.6, 0.7),
            Word("like", 0.7, 0.8),
            Word("world", 0.8, 1.0)
        )

        for (aligner in listOf(editDistanceAligner, greedyAligner, twoPointerAligner)) {
            val results = aligner.align(phrases, words)
            val matches = results.filter { it.op == AlignOp.MATCH }

            assertEquals("should have 2 matches", 2, matches.size)
            assertEquals("first match wordIdx", 0, matches[0].wordIdx)
            assertEquals("second match wordIdx", 4, matches[1].wordIdx)
        }
    }

    // ===== 누락 단어 테스트 =====

    @Test
    fun `missing word in words array - phrase should be PHRASE_ONLY`() {
        val phrases = listOf("Hello", "there", "world")
        val words = listOf(
            Word("Hello", 0.0, 0.5),
            // "there" 누락
            Word("world", 0.5, 1.0)
        )

        for (aligner in listOf(editDistanceAligner, greedyAligner, twoPointerAligner)) {
            val results = aligner.align(phrases, words)

            val thereResult = results.find { it.phraseIdx == 1 }
            assertNotNull("should have result for 'there'", thereResult)
            assertEquals("'there' should be PHRASE_ONLY", AlignOp.PHRASE_ONLY, thereResult?.op)
        }
    }

    // ===== 타임스탬프 할당 테스트 =====

    @Test
    fun `timestamp assignment - basic case`() {
        val phrases = listOf("Hello", "world")
        val words = listOf(
            Word("Hello", 0.0, 0.5),
            Word("world", 0.5, 1.0)
        )

        for (aligner in listOf(editDistanceAligner, greedyAligner, twoPointerAligner)) {
            val alignResults = aligner.align(phrases, words)
            val aligned = timestampAssigner.assign(phrases, words, alignResults)

            assertEquals(2, aligned.size)
            assertEquals("Hello", aligned[0].text)
            assertEquals(0L, aligned[0].startMs)
            assertEquals(500L, aligned[0].endMs)
            assertEquals("world", aligned[1].text)
            assertEquals(500L, aligned[1].startMs)
            assertEquals(1000L, aligned[1].endMs)
        }
    }

    @Test
    fun `timestamp interpolation - missing word should be interpolated`() {
        val phrases = listOf("Hello", "there", "world")
        val words = listOf(
            Word("Hello", 0.0, 0.5),
            Word("world", 1.0, 1.5)
        )

        for (aligner in listOf(editDistanceAligner, greedyAligner, twoPointerAligner)) {
            val alignResults = aligner.align(phrases, words)
            val aligned = timestampAssigner.assign(phrases, words, alignResults)

            assertEquals(3, aligned.size)

            // "there"는 보간되어야 함 (Hello.end와 world.start 사이)
            val thereToken = aligned[1]
            assertEquals("there", thereToken.text)
            assertTrue("'there' startMs should be between 500 and 1000",
                thereToken.startMs >= 500 && thereToken.startMs <= 1000)
        }
    }

    // ===== 실제 시나리오 테스트 =====

    @Test
    fun `real scenario - podcast with fillers`() {
        val phrases = listOf("Our", "attentional", "systems", "draw", "us", "to", "that.")
        val words = listOf(
            Word("Our", 62.98, 63.04),
            Word("attentional", 63.04, 63.56),
            Word("systems", 63.56, 63.9),
            Word("um", 63.9, 64.0),  // filler
            Word("draw", 64.0, 64.28),
            Word("us", 64.28, 64.48),
            Word("to", 64.48, 64.6),
            Word("that.", 64.6, 64.94)
        )

        println("\n=== Real Scenario Test ===")

        for ((name, aligner) in mapOf(
            "EditDistance" to editDistanceAligner,
            "Greedy" to greedyAligner,
            "TwoPointer" to twoPointerAligner
        )) {
            val alignResults = aligner.align(phrases, words)
            val aligned = timestampAssigner.assign(phrases, words, alignResults)

            println("\n$name:")
            aligned.forEach { println("  ${it.text}: ${it.startMs}-${it.endMs}ms") }

            // 검증
            assertEquals("$name: should have 7 aligned tokens", 7, aligned.size)

            // 첫 단어는 정확한 타임스탬프
            assertEquals("$name: 'Our' startMs", 62980L, aligned[0].startMs)

            // 마지막 단어도 정확
            assertEquals("$name: 'that.' endMs", 64940L, aligned[6].endMs)
        }
    }

    @Test
    fun `complex scenario - mismatched words`() {
        // Whisper가 "you?" 대신 "ya"로 인식한 경우
        val phrases = listOf("How", "are", "you?")
        val words = listOf(
            Word("How", 0.0, 0.3),
            Word("are", 0.3, 0.5),
            Word("ya", 0.5, 0.8)  // "you" 대신 "ya"
        )

        println("\n=== Mismatched Words Test ===")

        for ((name, aligner) in mapOf(
            "EditDistance" to editDistanceAligner,
            "Greedy" to greedyAligner,
            "TwoPointer" to twoPointerAligner
        )) {
            val alignResults = aligner.align(phrases, words)
            val aligned = timestampAssigner.assign(phrases, words, alignResults)

            println("$name:")
            alignResults.filter { it.phraseIdx != null }.forEach {
                println("  phrase[${it.phraseIdx}] '${phrases[it.phraseIdx!!]}' -> word[${it.wordIdx}] ${it.op}")
            }

            assertEquals("$name: should have 3 aligned tokens", 3, aligned.size)
        }
    }

    // ===== 성능 비교 (대량 데이터) =====

    @Test
    fun `performance comparison - 100 phrases`() {
        val phrases = (0 until 100).map { "word$it" }
        val words = (0 until 100).flatMap { i ->
            listOf(
                Word("word$i", i * 0.5, i * 0.5 + 0.4),
                Word("um", i * 0.5 + 0.4, i * 0.5 + 0.5)  // filler between each
            )
        }

        println("\n=== Performance Test (100 phrases, 200 words) ===")

        for ((name, aligner) in mapOf(
            "EditDistance" to editDistanceAligner,
            "Greedy" to greedyAligner,
            "TwoPointer" to twoPointerAligner
        )) {
            val startTime = System.currentTimeMillis()
            val alignResults = aligner.align(phrases, words)
            val aligned = timestampAssigner.assign(phrases, words, alignResults)
            val elapsed = System.currentTimeMillis() - startTime

            val matchCount = alignResults.count { it.op == AlignOp.MATCH }

            println("$name: $elapsed ms, $matchCount matches")

            assertEquals("$name: should have 100 matches", 100, matchCount)
        }
    }

    // ===== 빈 입력 테스트 =====

    @Test
    fun `empty phrases - should return empty`() {
        val phrases = emptyList<String>()
        val words = listOf(Word("Hello", 0.0, 0.5))

        for (aligner in listOf(editDistanceAligner, greedyAligner, twoPointerAligner)) {
            val results = aligner.align(phrases, words)
            assertTrue("empty phrases should return empty results", results.isEmpty() ||
                results.all { it.phraseIdx == null })
        }
    }

    @Test
    fun `empty words - all phrases should be PHRASE_ONLY`() {
        val phrases = listOf("Hello", "world")
        val words = emptyList<Word>()

        for (aligner in listOf(editDistanceAligner, greedyAligner, twoPointerAligner)) {
            val results = aligner.align(phrases, words)
            val phraseOnlyCount = results.count { it.op == AlignOp.PHRASE_ONLY }

            assertEquals("all phrases should be PHRASE_ONLY", 2, phraseOnlyCount)
        }
    }
}
