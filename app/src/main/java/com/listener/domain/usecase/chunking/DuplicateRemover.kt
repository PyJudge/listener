package com.listener.domain.usecase.chunking

import com.listener.domain.model.Word
import javax.inject.Inject

class DuplicateRemover @Inject constructor() {

    /**
     * 중복 단어 제거 (원본 순서 유지!)
     *
     * 중요: 시간순 정렬하면 안 됨!
     * Whisper가 가끔 비순차 타임스탬프를 출력하는데 (예: words[177] @ 62.44s, words[178] @ 62.26s),
     * 이를 정렬하면 단어 순서가 뒤바뀌어 TimestampMatcher 매칭이 실패함.
     * 원본 순서를 유지해야 문장 경계 매칭이 정확히 동작함.
     */
    fun removeDuplicates(words: List<Word>): List<Word> {
        if (words.isEmpty()) return emptyList()

        // 정렬 제거 - 원본 순서 유지!
        val result = mutableListOf(words.first())

        for (word in words.drop(1)) {
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
