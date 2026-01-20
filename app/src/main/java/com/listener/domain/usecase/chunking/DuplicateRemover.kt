package com.listener.domain.usecase.chunking

import com.listener.domain.model.Word
import javax.inject.Inject

class DuplicateRemover @Inject constructor() {

    fun removeDuplicates(words: List<Word>): List<Word> {
        if (words.isEmpty()) return emptyList()

        val sorted = words.sortedWith(compareBy({ it.start }, { it.end }))
        val result = mutableListOf(sorted.first())

        for (word in sorted.drop(1)) {
            val prev = result.last()
            // Skip if overlapping timestamp and same word
            val isOverlapping = word.start < prev.end
            val isSameWord = word.word.lowercase().trim() == prev.word.lowercase().trim()

            if (!(isOverlapping && isSameWord)) {
                result.add(word)
            }
        }

        return result
    }
}
