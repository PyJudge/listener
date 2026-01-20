# Phase 6: 플레이어 UI

## 비즈니스 로직

### 해결하는 문제
SPEC.md 3.7~3.8장의 미니 플레이어와 전체화면 플레이어를 구현합니다.
가림 모드, 제스처, 학습 설정 UI를 포함합니다.

### 핵심 규칙
1. **미니 플레이어**: 제목, 서브타이틀, 이전/재생/다음 버튼, 진행바
2. **전체화면 플레이어**: 전사문 표시, 가림 모드, 5버튼, 설정 버튼
3. **가림 모드**: 텍스트 숨김, 개별 눈 아이콘 탭으로 열기
4. **제스처**: 스와이프 -> chunk 이동, 스크롤 -> 전사문 탐색

### 제약/예외
- HARD 모드 시 녹음 버튼 비활성화 (회색)
- 플레이리스트 없으면 이전/다음 항목 버튼 비활성화
- 현재 chunk 하이라이트

---

## 설계

### 컴포넌트
```
presentation/player/
├── MiniPlayer.kt               # 이미 Phase 4에서 기본 구현
├── FullScreenPlayer.kt         # 전체화면 플레이어
├── TranscriptView.kt           # 전사문 스크롤 뷰
├── ChunkItem.kt                # 개별 chunk 표시
├── PlayerControls.kt           # 5버튼 컨트롤
├── LearningSettings.kt         # 반복/공백/녹음/모드 설정
└── PlayerViewModel.kt          # 통합 ViewModel
```

### 서브타이틀 규칙
```kotlin
fun getSubtitle(playlist: Playlist?, source: Source): String {
    return when {
        playlist != null && source is PodcastEpisode ->
            "${playlist.name} | ${source.podcastName}"
        playlist != null && source is LocalFile ->
            "${playlist.name} | 미디어 파일"
        source is PodcastEpisode ->
            source.podcastName
        else ->
            "미디어 파일"
    }
}
```

---

## 테스트 케이스

### PlayerViewModel 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | chunk 목록 로드 | loadChunks(sourceId) | List<Chunk> |
| 정상 | 현재 chunk 변경 | setCurrentChunk(5) | currentIndex = 5 |
| 정상 | 반복 횟수 변경 | setRepeatCount(3) | repeatCount = 3 |
| 정상 | 공백 비율 변경 | setGapRatio(0.6f) | gapRatio = 0.6f |
| 정상 | 녹음 토글 | toggleRecording() | ON <-> OFF |
| 정상 | HARD 모드 토글 | toggleHardMode() | NORMAL <-> HARD |
| 정상 | HARD 녹음 고정 | HARD 모드 시 | isRecordingEnabled = true |
| 정상 | 가림 모드 토글 | toggleBlindMode() | ON <-> OFF |
| 정상 | 개별 chunk 열기 | revealChunk(3) | revealedChunks.add(3) |

### 제스처 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 좌 스와이프 | 전사문 영역 | 다음 chunk |
| 정상 | 우 스와이프 | 전사문 영역 | 이전 chunk |
| 정상 | chunk 탭 | 특정 chunk | 해당 chunk로 이동 |
| 정상 | 스피커 아이콘 탭 | 녹음 있는 chunk | 녹음 재생 |
| 정상 | 헤더 아래 스와이프 | 헤더 영역 | 미니 플레이어로 축소 |
| 정상 | 미니 위 스와이프 | 미니 플레이어 | 전체화면 확장 |

### UI 상태 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 현재 chunk 하이라이트 | currentIndex = 3 | chunk 3 강조 표시 |
| 정상 | 녹음 아이콘 표시 | hasRecording = true | 스피커 아이콘 표시 |
| 정상 | 녹음 아이콘 숨김 | hasRecording = false | 스피커 아이콘 숨김 |
| 정상 | 가림 모드 텍스트 | blindMode = true | 타임스탬프만 표시 |
| 정상 | 가림 모드 열린 chunk | revealedChunks.contains(3) | 텍스트 표시 |
| 엣지 | 플레이리스트 없음 | playlist = null | 이전/다음 항목 비활성화 |

---

## 실행 계획

### TDD 순서
1. **PlayerViewModel** 테스트 -> 구현
2. **TranscriptView** 테스트 -> 구현
3. **ChunkItem** 테스트 -> 구현
4. **PlayerControls** 테스트 -> 구현
5. **LearningSettings** 테스트 -> 구현
6. **FullScreenPlayer** 통합
7. **MiniPlayer** 업데이트 (Phase 4에서 확장)

### 병렬/순차
- **병렬**: ChunkItem, PlayerControls, LearningSettings
- **순차**: ViewModel -> TranscriptView -> FullScreenPlayer

---

## 완료 체크리스트

- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew test` 모든 테스트 통과
- [ ] `./gradlew lint` 경고/오류 없음
- [ ] Android Emulator 실행 확인 (스크린샷)
- [ ] 전사문 스크롤 및 하이라이트
- [ ] chunk 탭 -> 이동
- [ ] 스와이프 제스처 동작
- [ ] 가림 모드 토글 + 개별 열기
- [ ] 5버튼 컨트롤 동작
- [ ] 학습 설정 버튼 동작
- [ ] 미니 <-> 전체화면 전환

---

## 학습 내용 (Phase 6 완료 후)

> Phase 완료 후 아래 내용을 기록하세요.

- 예상과 달랐던 점:
- 발견한 기술적 제약:
- 설계와 다르게 구현한 부분:
- 더 나은 접근법:

---

## 개정 이력

| 날짜 | 개정 내용 |
|------|----------|
| (초기) | 초기 계획 작성 |
