# Phase 3: 재생 & 녹음 서비스

## 비즈니스 로직

### 해결하는 문제
Media3 ExoPlayer를 활용한 chunk 단위 반복 재생과 사용자 녹음 기능을 구현합니다.
SPEC.md 4장의 일반 모드/HARD 모드 학습 상태 머신을 구현합니다.

### 핵심 규칙
1. **일반 모드 패턴**: `[원문 재생] -> [공백(+선택적 녹음)] x 반복횟수`
2. **HARD 모드 패턴**: `[1차 원문] -> [공백+녹음] -> [2차 원문] -> [내 녹음 재생] x 반복횟수`
3. **녹음 설정**: AAC 64kbps, 22050Hz, Mono
4. **녹음 저장**: chunk당 1개, 덮어쓰기
5. **백그라운드 재생**: MediaSessionService 필수

### 제약/예외
- HARD 모드에서 녹음 강제 ON
- 녹음 재생 시 현재 상태 일시정지 후 재개
- 오디오 캐시 최대 1GB

---

## 설계

### 상태 머신 (일반 모드)
```
        play()
IDLE ----------> PLAY
  ^               |
  |               | 재생 완료
  |               v
  |              GAP (+녹음 if enabled)
  |               |
  |               | 공백 완료
  |               v
  |         [반복 남음?]
  |          YES | NO
  |           v   v
  +-- PLAY <-+   다음 chunk
```

### 상태 머신 (HARD 모드)
```
        play()
IDLE ----------> PLAY_1
  ^                |
  |                | 재생 완료
  |                v
  |            GAP_REC (녹음 강제)
  |                |
  |                | 공백+녹음 완료
  |                v
  |            PLAY_2 (2차 원문)
  |                |
  |                | 재생 완료
  |                v
  |          PLAYBACK_REC (내 녹음)
  |                |
  |                | 재생 완료
  |                v
  |         [반복 남음?]
  |          YES | NO
  |           v   v
  +-- PLAY_1<-+  다음 chunk
```

### 컴포넌트
```
service/
├── PlaybackService.kt          # MediaSessionService
├── LearningStateMachine.kt     # 상태 머신 구현
├── RecordingManager.kt         # 녹음 관리
└── AudioCacheManager.kt        # 캐시 관리

domain/model/
├── LearningState.kt            # IDLE, PLAY, GAP, etc.
├── LearningMode.kt             # NORMAL, HARD
└── LearningSettings.kt         # repeatCount, gapRatio, etc.
```

---

## 테스트 케이스

### LearningStateMachine 테스트 (일반 모드)

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | play 호출 | IDLE + play() | PLAY 상태 |
| 정상 | 재생 완료 | PLAY + onComplete | GAP 상태 |
| 정상 | GAP 완료 (반복 있음) | GAP + 반복 1회 남음 | PLAY 상태 |
| 정상 | GAP 완료 (반복 없음) | GAP + 반복 0회 | 다음 chunk |
| 정상 | pause 호출 | PLAY + pause() | PAUSED 상태 |
| 정상 | resume 호출 | PAUSED + resume() | 이전 상태 복원 |
| 정상 | 이전 chunk | currentIndex > 0 | currentIndex - 1 |
| 정상 | 다음 chunk | currentIndex < max | currentIndex + 1 |
| 엣지 | 첫 chunk 이전 | currentIndex = 0, prev() | 변화 없음 |
| 엣지 | 마지막 chunk 다음 | currentIndex = max, next() | 재생 종료 |

### LearningStateMachine 테스트 (HARD 모드)

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 1차 재생 완료 | PLAY_1 + onComplete | GAP_REC 상태 |
| 정상 | GAP+녹음 완료 | GAP_REC + onComplete | PLAY_2 상태 |
| 정상 | 2차 재생 완료 | PLAY_2 + onComplete | PLAYBACK_REC 상태 |
| 정상 | 녹음 재생 완료 | PLAYBACK_REC + onComplete | 다음 반복 또는 chunk |
| 정상 | 녹음 버튼 비활성화 | HARD 모드 | isRecordingEnabled = true (고정) |

### RecordingManager 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 녹음 시작 | startRecording() | MediaRecorder 시작 |
| 정상 | 녹음 중지 | stopRecording() | 파일 저장 |
| 정상 | 녹음 덮어쓰기 | 동일 chunk 재녹음 | 기존 파일 삭제 후 저장 |
| 정상 | 녹음 파일 경로 | sourceId, chunkIndex | `recordings/{sourceId}/chunk_{index}.m4a` |
| 정상 | 녹음 재생 | playRecording() | 저장된 파일 재생 |
| 예외 | 녹음 없음 | 미녹음 chunk 재생 | null 반환 |

### AudioCacheManager 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 캐시 저장 | audioUrl | 파일 다운로드 |
| 정상 | 캐시 조회 | audioUrl | 로컬 파일 경로 |
| 정상 | 캐시 용량 초과 | 1GB 초과 | LRU 삭제 |
| 정상 | 캐시 전체 삭제 | clearAll() | 모든 캐시 삭제 |

### MediaSessionService 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 서비스 시작 | startForeground | Notification 표시 |
| 정상 | 미디어 버튼 | PLAY/PAUSE | 상태 변경 |
| 정상 | 알림 컨트롤 | 이전/다음 버튼 | chunk 이동 |

---

## 실행 계획

### TDD 순서
1. **LearningState enum** 정의
2. **LearningStateMachine** 일반 모드 테스트 -> 구현
3. **LearningStateMachine** HARD 모드 테스트 -> 구현
4. **RecordingManager** 테스트 -> 구현
5. **AudioCacheManager** 테스트 -> 구현
6. **PlaybackService** 통합 테스트 -> 구현

### 병렬/순차
- **병렬**: RecordingManager, AudioCacheManager
- **순차**: LearningStateMachine -> PlaybackService

---

## 완료 체크리스트

- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew test` 모든 테스트 통과
- [ ] `./gradlew connectedAndroidTest` 통과
- [ ] `./gradlew lint` 경고/오류 없음
- [ ] Android Emulator 실행 확인 (스크린샷)
- [ ] 일반 모드 상태 전이 모든 테스트 통과
- [ ] HARD 모드 상태 전이 모든 테스트 통과
- [ ] 녹음 시작/중지/저장 테스트 통과
- [ ] 캐시 LRU 삭제 테스트 통과
- [ ] MediaSessionService 백그라운드 재생 동작
- [ ] 알림 컨트롤 동작

---

## 학습 내용 (Phase 3 완료 후)

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
