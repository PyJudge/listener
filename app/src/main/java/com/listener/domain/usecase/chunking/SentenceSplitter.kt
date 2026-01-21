package com.listener.domain.usecase.chunking

import javax.inject.Inject

class SentenceSplitter @Inject constructor() {

    private val sentenceDelimiters = setOf('.', '!', '?')
    private val phraseDelimiters = setOf(',', '.', '!', '?')

    // 약어 목록 (소문자, 마침표 제거 형태)
    private val abbreviations = setOf(
        // 경칭
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "rev", "hon", "gen", "col", "lt", "sgt", "capt",
        // 학위 (부분 약어 포함: Ph.D. → "ph", "phd")
        "ph", "phd", "md", "ba", "ma", "bs", "ms", "jd", "esq", "dds", "rn", "mba", "llb", "dmin",
        // 일반 약어
        "etc", "vs", "viz", "al", "eg", "ie", "cf", "approx", "apt", "dept", "est",
        "vol", "no", "fig", "inc", "corp", "ltd", "co", "govt", "assn", "bros", "misc",
        // 주소
        "mt", "st", "ave", "blvd", "rd", "ln", "ct", "pl", "hwy",
        // 월
        "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "sept", "oct", "nov", "dec",
        // 측정
        "hr", "hrs", "min", "sec", "lb", "lbs", "oz", "ft", "in", "cm", "km", "kg", "mg", "ml", "pt", "qt", "gal",
        // 국가
        "us", "usa", "uk", "eu"
    )

    fun split(text: String, sentenceOnly: Boolean = true): List<String> {
        val delimiters = if (sentenceOnly) sentenceDelimiters else phraseDelimiters
        val sentences = mutableListOf<String>()
        val current = StringBuilder()

        var i = 0
        while (i < text.length) {
            val char = text[i]
            current.append(char)

            if (char in delimiters) {
                // 연속 구두점 처리 (e.g., "?!", "...")
                while (i + 1 < text.length && text[i + 1] in delimiters) {
                    i++
                    current.append(text[i])
                }

                // 마침표인 경우 약어인지 확인
                val shouldSplit = if (char == '.') {
                    !isAbbreviation(current.toString())
                } else {
                    true // ! 또는 ?는 항상 문장 끝
                }

                if (shouldSplit) {
                    val sentence = current.toString().trim()
                    if (sentence.isNotEmpty()) {
                        sentences.add(sentence)
                    }
                    current.clear()
                }
            }
            i++
        }

        // Add remaining text as new sentence (ankigpt 방식)
        val remaining = current.toString().trim()
        if (remaining.isNotEmpty()) {
            sentences.add(remaining)
        }

        return sentences
    }

    /**
     * 현재 텍스트가 약어로 끝나는지 확인
     */
    private fun isAbbreviation(text: String): Boolean {
        val lastWord = extractLastWord(text)
        if (lastWord.isEmpty()) return false

        // 마침표 모두 제거하고 소문자로
        val normalized = lastWord.replace(".", "").lowercase()

        // 1. 약어 목록에 있으면 약어
        if (normalized in abbreviations) return true

        // 2. 한 글자면 이니셜 (e.g., "John F.")
        if (normalized.length == 1 && normalized[0].isLetter()) return true

        // 3. U.S.A. 패턴 (글자.글자.글자.)
        if (lastWord.matches(Regex("([A-Za-z]\\.){2,}"))) return true

        return false
    }

    private fun extractLastWord(text: String): String {
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty()) return ""

        var end = trimmed.length
        var start = end - 1

        // 뒤에서부터 단어 경계 찾기 (글자, 숫자, 마침표 포함)
        while (start > 0 && (trimmed[start - 1].isLetterOrDigit() || trimmed[start - 1] == '.')) {
            start--
        }

        return trimmed.substring(start, end)
    }

    fun findSentenceBoundaries(text: String, sentenceOnly: Boolean = true): List<Int> {
        val delimiters = if (sentenceOnly) sentenceDelimiters else phraseDelimiters
        val boundaries = mutableListOf<Int>()
        val words = text.split(Regex("\\s+"))

        words.forEachIndexed { index, word ->
            if (word.isNotEmpty() && word.last() in delimiters) {
                boundaries.add(index)
            }
        }

        return boundaries
    }
}
