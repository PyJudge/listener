# CLAUDE.md - Service 계층

## 핵심 불변조건 (INVARIANTS)

```
1. PlaybackService ↔ PlaybackController ↔ PlayerViewModel 상태 동기화
   - playbackState는 항상 단방향으로 전파 (Service → Controller → ViewModel)
   - 상태 업데이트 후 UI 반영 전까지 일관성 보장

2. LearningStateMachine 상태 전이 무결성
   - 현재 PlayMode에 유효한 상태만 존재
   - 모드 변경 시 자동 정규화 (normalizeStateForMode)

3. 청크 전환 중 녹음/재생 작업 안전 종료
   - cancelPendingOperations() 후 다음 작업 시작
```

---

## 알려진 문제점

### CRITICAL - recordingJob 경합

**현상:** 청크 빠르게 전환 시 잘못된 청크의 녹음이 저장됨

**원인:** `recordingJob?.cancel()`은 Job만 취소, 이미 실행 중인 코드는 계속 진행

**발생 확률:** 5-10%

**위치:** `PlaybackService.kt` 라인 330-352

**해결책:**
```kotlin
private suspend fun cancelPendingOperations() {
    gapJob?.cancel()
    recordingJob?.cancel()
    userRecordingJob?.cancel()
    stopPositionMonitoring()

    // 경합 방지: Job 취소 후 완료 대기
    try {
        recordingJob?.join()  // Job이 완료될 때까지 대기
    } catch (e: CancellationException) {
        // 정상 취소
    }

    if (recordingManager.isCurrentlyRecording()) {
        recordingManager.stopRecording()
        recordingManager.deleteRecording(sourceId, currentChunkIndex)
    }
}
```

### HIGH - 상태 동기화 지연

**현상:** `setContent()` 호출 후 `playbackState`가 즉시 반영되지 않음

**원인:** StateFlow 업데이트와 UI 구독 사이의 타이밍 차이

**해결책:**
```kotlin
// setContent() 후 상태 동기화 대기
try {
    withTimeout(1000) {
        playbackState.first { it.sourceId == sourceId && it.totalChunks == chunks.size }
    }
} catch (e: TimeoutCancellationException) {
    Log.w(TAG, "State sync timeout")
}
```

### HIGH - playUserRecording() 중 청크 변경

**현상:** 녹음 재생 완료 직후 `onChunkPlaybackComplete()` 호출 시 이미 청크가 변경됨

**위치:** `PlaybackService.kt` 라인 434-488

**해결책:**
```kotlin
if (currentChunkIndex == targetChunkIndex) {
    player?.setMediaItem(MediaItem.fromUri(originalAudioUri))
    player?.prepare()
    // 한 번 더 검증
    if (currentChunkIndex == targetChunkIndex) {
        onChunkPlaybackComplete()
    }
}
```

### MEDIUM - 녹음 실패 시 불완전 파일 미정리

**현상:** 녹음 실패해도 불완전 파일이 남음

**위치:** `PlaybackService.kt` 라인 404-431

**해결책:**
```kotlin
if (!success) {
    recordingManager.deleteRecording(sourceId, currentChunkIndex)  // 추가
    val result = stateMachine.onRecordingFailed()
    // ...
}
```

### MEDIUM - LearningState.Recording 데드 코드

**현상:** `handleContinue()`에서 `LearningState.Recording` 케이스 존재하지만 실제 사용되지 않음

**위치:** `PlaybackService.kt` 라인 379-390

---

## 학습 모드 상태 다이어그램

### LR 모드 (일반)
```
Idle → Playing → Gap → Playing (반복) → NextChunk
```

### LRLR 모드 (HARD)
```
Idle → PlayingFirst → GapWithRecording → PlayingSecond → PlaybackRecording → PlayingFirst (반복) → NextChunk
```

### NORMAL 모드
```
Idle → Playing → NextChunk (반복 없음)
```

---

## 수정 시 체크리스트

```bash
# 1. LearningStateMachine 테스트
./gradlew test --tests "*.LearningStateMachineTest"

# 2. PlaybackController 테스트
./gradlew test --tests "*.PlaybackControllerTest"

# 3. 실기기 테스트 시나리오
- 청크 빠르게 넘기기 (next, next, next)
- 녹음 중 청크 전환
- LRLR 모드 전체 주기 확인
- pause/resume 위치 복원 확인
```

---

## 핵심 파일

| 파일 | 역할 |
|------|------|
| `PlaybackService.kt` | Media3 기반 재생 서비스 |
| `PlaybackController.kt` | Service 바인딩 및 상태 브리지 |
| `LearningStateMachine.kt` | 학습 모드 상태 전이 로직 |
| `RecordingManager.kt` | 녹음 시작/중지/저장 관리 |
| `AudioCacheManager.kt` | 오디오 파일 캐시 관리 |

---

## 로그 태그

| 태그 | 파일 |
|------|------|
| `PlaybackService` | PlaybackService.kt |
| `PlaybackController` | PlaybackController.kt |
| `LearningStateMachine` | LearningStateMachine.kt |
| `RecordingManager` | RecordingManager.kt |

```bash
# 실시간 로그 모니터링
adb logcat | grep -E "PlaybackService|PlaybackController|LearningStateMachine"
```
