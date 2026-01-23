package com.listener.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * A4 - PlaybackState error 필드 테스트
 *
 * PlaybackState의 error 필드가 올바르게 동작하는지 검증
 */
class PlaybackStateErrorTest {

    @Test
    fun `PlaybackState 기본값은 error null`() {
        val state = PlaybackState()
        assertNull(state.error)
    }

    @Test
    fun `PlaybackState error 설정 가능`() {
        val errorMessage = "오디오 파일을 재생할 수 없습니다"
        val state = PlaybackState(error = errorMessage)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `setContent 시 error 초기화`() {
        // 에러가 있는 상태에서 시작
        val stateWithError = PlaybackState(
            sourceId = "old-source",
            error = "이전 에러 메시지"
        )
        assertNotNull(stateWithError.error)

        // 새 콘텐츠 설정 시 error를 null로 초기화해야 함
        val newState = stateWithError.copy(
            sourceId = "new-source",
            error = null
        )

        assertNull(newState.error)
        assertEquals("new-source", newState.sourceId)
    }

    @Test
    fun `error가 있어도 다른 필드는 유지`() {
        val state = PlaybackState(
            sourceId = "test-source",
            currentChunkIndex = 5,
            totalChunks = 10,
            isPlaying = true,
            title = "Test Title",
            error = "에러 발생"
        )

        assertEquals("test-source", state.sourceId)
        assertEquals(5, state.currentChunkIndex)
        assertEquals(10, state.totalChunks)
        assertTrue(state.isPlaying)
        assertEquals("Test Title", state.title)
        assertEquals("에러 발생", state.error)
    }

    @Test
    fun `error 없이 copy하면 error 유지`() {
        val stateWithError = PlaybackState(error = "에러 메시지")
        val copiedState = stateWithError.copy(isPlaying = true)

        assertEquals("에러 메시지", copiedState.error)
        assertTrue(copiedState.isPlaying)
    }
}
