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
        "us", "usa", "uk", "eu",
        // 웹
        "www"
    )

    // 도메인 확장자 (소문자)
    private val domainExtensions = setOf(
        "com", "net", "org", "edu", "gov", "io", "co", "ai", "app",
        "dev", "me", "info", "biz", "tv", "fm", "ly", "to"
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

                // 마침표인 경우 약어, 도메인, 소수점인지 확인
                val shouldSplit = if (char == '.') {
                    // 소수점 확인: 앞이 숫자이고 뒤가 숫자면 split 안 함
                    val prevIsDigit = current.length > 1 && current[current.length - 2].isDigit()
                    val nextIsDigit = i + 1 < text.length && text[i + 1].isDigit()
                    if (prevIsDigit && nextIsDigit) {
                        false  // 소수점이면 split 안 함
                    } else {
                        // Look-ahead: 다음 단어가 도메인 확장자인지 확인
                        val nextWord = extractNextWord(text, i + 1)
                        if (nextWord.lowercase() in domainExtensions) {
                            false  // 도메인 확장자면 split 안 함
                        } else {
                            !isAbbreviation(current.toString())
                        }
                    }
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
     * 현재 텍스트가 약어 또는 도메인으로 끝나는지 확인
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

        // 4. 도메인 주소 패턴 (example.com, www.site.net)
        if (isDomainAddress(lastWord)) return true

        return false
    }

    /**
     * 도메인 주소인지 확인 (example.com, www.site.org 등)
     */
    private fun isDomainAddress(word: String): Boolean {
        val lower = word.lowercase()

        // 마지막 마침표 이후의 부분이 도메인 확장자인지 확인
        val lastDotIndex = lower.lastIndexOf('.')
        if (lastDotIndex <= 0) return false

        val extension = lower.substring(lastDotIndex + 1)
        val beforeDot = lower.substring(0, lastDotIndex)

        // 확장자가 도메인 목록에 있고, 앞부분이 비어있지 않으면 도메인
        return extension in domainExtensions && beforeDot.isNotEmpty()
    }

    /**
     * 주어진 위치부터 다음 단어(공백/구두점 전까지) 추출
     */
    private fun extractNextWord(text: String, startIndex: Int): String {
        if (startIndex >= text.length) return ""

        var end = startIndex
        while (end < text.length && text[end].isLetterOrDigit()) {
            end++
        }
        return text.substring(startIndex, end)
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
