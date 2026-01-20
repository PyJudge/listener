package com.listener.domain.usecase.chunking

import javax.inject.Inject

class SentenceSplitter @Inject constructor() {

    private val sentenceDelimiters = setOf('.', '!', '?')
    private val phraseDelimiters = setOf(',', '.', '!', '?')

    fun split(text: String, sentenceOnly: Boolean = true): List<String> {
        val delimiters = if (sentenceOnly) sentenceDelimiters else phraseDelimiters
        val sentences = mutableListOf<String>()
        val current = StringBuilder()

        var i = 0
        while (i < text.length) {
            val char = text[i]
            current.append(char)

            if (char in delimiters) {
                // Check for consecutive punctuation (e.g., "?!")
                while (i + 1 < text.length && text[i + 1] in delimiters) {
                    i++
                    current.append(text[i])
                }

                val sentence = current.toString().trim()
                if (sentence.isNotEmpty()) {
                    sentences.add(sentence)
                }
                current.clear()
            }
            i++
        }

        // Add remaining text if any
        val remaining = current.toString().trim()
        if (remaining.isNotEmpty()) {
            if (sentences.isNotEmpty()) {
                // Append to last sentence if no ending punctuation
                sentences[sentences.lastIndex] = sentences.last() + " " + remaining
            } else {
                sentences.add(remaining)
            }
        }

        return sentences
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
