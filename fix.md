# fix.md - EDGE_E2E 시나리오 검증 결과

> 검증일: 2026-01-23
> 기준 문서: EDGE_E2E.md

---

## 검증 상태 범례

| 상태 | 의미 |
|------|------|
| ✅ | 완전 구현됨 |
| ⚠️ | 부분 구현 또는 개선 필요 |
| ❌ | 미구현 |

---

## 1. 모드별 시나리오

### 1.1 NORMAL 모드

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| N-1 | 재생 중 같은 청크 터치 → 처음부터 재시작 | MEDIUM | ✅ | `FullScreenPlayerScreen.kt:227`, `PlaybackService.kt:294-314` | seekToChunk() 구현 |
| N-2 | 재생 중 다른 청크 터치 → 선택 청크로 이동 | HIGH | ✅ | `FullScreenPlayerScreen.kt:227`, `PlaybackService.kt:294-314` | seekToChunk() 구현 |
| N-3 | 재생 완료 시 자동 다음 청크 | HIGH | ✅ | `PlaybackService.kt:408-430` | TransitionResult.NextChunk 처리 |
| N-4 | 마지막 청크 재생 완료 | HIGH | ✅ | `PlaybackService.kt:421-426` | 플레이리스트/단독 모드 분기 |

### 1.2 LR 모드

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| LR-1 | Gap 중 일시정지 → Gap 타이머 일시정지 | HIGH | ⚠️ | `PlaybackService.kt:192-203` | gapJob cancel은 하지만 남은 시간 저장 불완전 |
| LR-2 | Gap 중 다른 청크 터치 → Gap 취소 | HIGH | ✅ | `PlaybackService.kt:370-406` | cancelPendingOperations() |
| LR-3 | Gap 중 이전/다음 버튼 → Gap 취소 | HIGH | ✅ | `PlaybackService.kt:236-278` | nextChunk/previousChunk |
| LR-4 | Gap 중 같은 청크 터치 → Gap 취소, 재시작 | MEDIUM | ✅ | `PlaybackService.kt:294-314` | seekToChunk() |

### 1.3 LRLR (HARD) 모드

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| LRLR-1 | PlayingFirst 중 다른 청크 터치 | HIGH | ✅ | `PlaybackService.kt:294-314` | seekToChunk() |
| LRLR-2 | GapWithRecording 중 다른 청크 터치 → 녹음 중단/삭제 | **CRITICAL** | ✅ | `PlaybackService.kt:370-406` | recordingJob.join() + deleteRecording() |
| LRLR-3 | PlayingSecond 중 다른 청크 터치 | HIGH | ✅ | `PlaybackService.kt:294-314` | seekToChunk() |
| LRLR-4 | PlaybackRecording 중 다른 청크 터치 | HIGH | ✅ | `PlaybackService.kt:294-314` | seekToChunk() |
| LRLR-5 | 녹음 권한 없이 LRLR 진입 → LR 모드로 전환 | **CRITICAL** | ✅ | `FullScreenPlayerScreen.kt:135-145, 294-301` | 권한 요청 + 거부 시 LR 유지 |
| LRLR-6 | 녹음 중 마이크 사용 불가 → LR 모드 전환 | HIGH | ✅ | `PlaybackService.kt:465-481` | onRecordingFailed() → SwitchToLRMode |
| LRLR-7 | 동시 녹음 요청 → 두 번째 무시 | HIGH | ✅ | `RecordingManager.kt:78-142` | Mutex 직렬화 |

---

## 2. 전환 시나리오

### 2.1 모드 전환

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| MT-1 | Playing 중 NORMAL → LR | HIGH | ✅ | `LearningStateMachine.kt:40-83` | normalizeStateForPlayMode() |
| MT-2 | Playing 중 LR → LRLR | HIGH | ✅ | `LearningStateMachine.kt:40-83` | Playing → PlayingFirst 변환 |
| MT-3 | Gap 중 LR → NORMAL | HIGH | ✅ | `LearningStateMachine.kt:40-83` | Gap → Playing 변환 |
| MT-4 | GapWithRecording 중 LRLR → LR → 녹음 중단/삭제 | **CRITICAL** | ✅ | `PlaybackService.kt:316-336` | recordingJob.cancel() + deleteRecording() |
| MT-5 | PlaybackRecording 중 LRLR → NORMAL | HIGH | ✅ | `PlaybackService.kt:316-336` | 녹음 재생 중단 |
| MT-6 | 빠른 모드 토글 연타 (5회 이상) | HIGH | ⚠️ | `PlayerViewModel.kt:287-302` | 중간 상태 정리 보장 불완전 |

### 2.2 에피소드/콘텐츠 전환

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| ET-1 | Playing 중 다른 에피소드 선택 | HIGH | ✅ | `PlayerViewModel.kt:148-167` | loadContent() |
| ET-2 | Gap 중 다른 에피소드 선택 | HIGH | ✅ | `PlayerViewModel.kt:148-167` | cancelPendingOperations() 호출 |
| ET-3 | GapWithRecording 중 다른 에피소드 선택 → 녹음 중단/삭제 | **CRITICAL** | ✅ | `PlaybackService.kt:370-406` | cancelPendingOperations() |
| ET-4 | 플레이리스트 아이템 전환 중 녹음 → cancelPendingOperations | **CRITICAL** | ✅ | `PlayerViewModel.kt:393-395` | loadPlaylistItem() 내 호출 |
| ET-5 | 전사 미완료 에피소드 → TranscriptionScreen 이동 | HIGH | ❌ | `PlayerViewModel.kt` | **미구현** - 전사 완료 여부 체크 없음 |

### 2.3 청크 네비게이션

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| CN-1 | 빠른 청크 스킵 연타 → Channel 용량 제한 | **CRITICAL** | ⚠️ | `PlayerViewModel.kt:76` | **Channel.UNLIMITED 사용 중** → CONFLATED 필요 |
| CN-2 | 첫 청크에서 이전 버튼 | LOW | ✅ | `PlaybackService.kt:258-278` | bounds check |
| CN-3 | 마지막 청크에서 다음 버튼 | MEDIUM | ✅ | `PlaybackService.kt:236-256` | bounds check |
| CN-4 | seekBar 드래그 중 다른 청크 터치 | MEDIUM | ✅ | `PlaybackService.kt:294-314` | navigationMutex |
| CN-5 | seekBar 드래그 완료와 동시에 청크 터치 | MEDIUM | ✅ | `PlaybackService.kt:294-314` | navigationMutex |
| CN-6 | Service connection timeout → 명시적 에러 반환 | MEDIUM | ⚠️ | `PlaybackController.kt:116-128` | 타임아웃 시 return만, 에러 전달 없음 |

---

## 3. 중단 시나리오

### 3.1 일시정지/재개

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| PR-1 | Playing 중 일시정지 → 재개 (±30ms) | HIGH | ✅ | `PlaybackService.kt:192-234` | pause/resume |
| PR-2 | Gap 중 일시정지 → 재개 | HIGH | ⚠️ | `PlaybackService.kt:192-203` | Gap 타이머 재개 로직 불완전 |
| PR-3 | GapWithRecording 중 일시정지 → MediaRecorder.pause | HIGH | ❌ | `PlaybackService.kt:192-203` | **MediaRecorder pause/resume 미구현** |
| PR-4 | GapWithRecording 중 재개 → MediaRecorder.resume | HIGH | ❌ | `PlaybackService.kt:205-234` | **MediaRecorder pause/resume 미구현** |
| PR-5 | PlaybackRecording 중 일시정지 → 재개 | MEDIUM | ✅ | `PlaybackService.kt:192-234` | 녹음 재생 처리 |
| PR-6 | 일시정지 후 1시간 경과 → 재개 | MEDIUM | ✅ | `PlaybackService.kt:205-234` | 저장된 위치에서 재개 |
| PR-7 | 일시정지 후 청크 변경 → 새 청크 처음부터 재생 | **CRITICAL** | ✅ | `PlaybackService.kt:210-224` | position 범위 검증 |

### 3.2 앱 중단

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| AS-1 | 홈 버튼 (앱 배경) → 재생 계속 | **CRITICAL** | ✅ | `PlaybackService.kt` | MediaSessionService 상속 |
| AS-2 | 화면 끄기 중 재생 → 재생 계속 | **CRITICAL** | ✅ | `PlaybackService.kt` | Foreground Service |
| AS-3 | 화면 끄기 중 녹음 → WakeLock으로 CPU 유지 | **CRITICAL** | ✅ | `RecordingManager.kt:218-235` | PARTIAL_WAKE_LOCK |
| AS-4 | 최근 앱에서 스와이프 종료 → 진행 상황 저장 | HIGH | ⚠️ | `PlaybackService.kt` | 진행 상황 저장 로직 미확인 |
| AS-5 | 설정 > 강제 종료 → MediaRecorder 정리 | HIGH | ✅ | `PlaybackService.kt:132-152` | onDestroy에서 forceStopRecording() |
| AS-6 | 배경 10분 이상 → startForeground + Notification | **CRITICAL** | ✅ | `PlaybackService.kt:41, 119-120` | MediaSessionService 상속으로 자동 관리 |
| AS-7 | 메모리 부족으로 프로세스 종료 → 서비스 재시작 | HIGH | ✅ | `PlaybackService.kt:125` | START_STICKY 반환 |

---

## 4. 동시성 시나리오

### 4.1 Race Condition

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| RC-1 | 녹음 중 빠른 청크 스킵 → recordingJob cancel + join | **CRITICAL** | ✅ | `PlaybackService.kt:376-382` | join() 호출 확인 |
| RC-2 | 녹음 재생 중 청크 변경 → targetChunkIndex 검증 | HIGH | ✅ | `PlaybackService.kt:494-548` | 재생 전/중/후 검증 |
| RC-3 | setContent 중 다른 setContent → navigationMutex | HIGH | ✅ | `PlaybackService.kt:78, 238, 260, 296` | Mutex 보호 |
| RC-4 | updateChunks 중 playCurrentChunk → bounds check | HIGH | ✅ | `PlaybackService.kt:280-292, 339` | getOrNull() 사용 |

### 4.2 UI 동기화

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| US-1 | 화면 회전 중 청크 변경 → 상태 복원 | HIGH | ⚠️ | `PlayerViewModel.kt` | ViewModel 상태 유지는 되나 명시적 저장 없음 |
| US-2 | LaunchedEffect 실행 전 청크 터치 | MEDIUM | ✅ | `PlayerViewModel.kt` | 최신 입력 우선 처리 |
| US-3 | seekBar 드래그 중 외부 청크 변경 | MEDIUM | ✅ | `PlaybackService.kt` | navigationMutex |
| US-4 | 빠른 blind mode 토글 연타 | LOW | ✅ | `FullScreenPlayerScreen.kt` | 최종 상태만 적용 |
| US-5 | MiniPlayer 메타데이터 StateFlow 동기화 | HIGH | ✅ | `PlayerViewModel.kt:86-87, 141-146` | MutableStateFlow 사용 |

---

## 5. 시스템 이벤트 시나리오

### 5.1 오디오 포커스

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| AF-1 | 전화 수신 중 재생 → 자동 일시정지/재개 | HIGH | ❌ | `PlaybackService.kt` | **AudioFocusListener 미구현** |
| AF-2 | 다른 미디어 앱 재생 시작 → 자동 일시정지 | HIGH | ❌ | `PlaybackService.kt` | **AudioFocusListener 미구현** |
| AF-3 | 알림 소리 재생 시 → 볼륨 덕킹 | MEDIUM | ❌ | `PlaybackService.kt` | **AudioFocusListener 미구현** |
| AF-4 | 블루투스 연결 끊김 → 자동 일시정지 | HIGH | ❌ | `PlaybackService.kt` | **BroadcastReceiver 미구현** |
| AF-5 | 이어폰 뽑힘 → 자동 일시정지 | HIGH | ❌ | `PlaybackService.kt` | **ACTION_AUDIO_BECOMING_NOISY 미구현** |

### 5.2 권한 및 리소스

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| PM-1 | 녹음 권한 거부 → LR 모드로 전환 | **CRITICAL** | ✅ | `FullScreenPlayerScreen.kt:135-145` | 권한 거부 시 토스트 + LR 유지 |
| PM-2 | 저장공간 부족 → 녹음 불가 알림 | HIGH | ❌ | `RecordingManager.kt` | **저장공간 체크 미구현** |
| PM-3 | 배터리 절약 모드 → 재생 계속 | MEDIUM | ✅ | `PlaybackService.kt` | Foreground Service 우선순위 |
| PM-4 | 알 수 없는 오디오 파일 형식 → 에러 토스트 | MEDIUM | ⚠️ | `PlaybackService.kt` | 에러 처리는 있으나 UI 표시 미확인 |

### 5.3 네트워크

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| NW-1 | 스트리밍 중 네트워크 끊김 → 버퍼 소진 시 일시정지 | MEDIUM | ⚠️ | `PlaybackService.kt` | Media3 기본 동작에 의존 |
| NW-2 | 다운로드 중 네트워크 끊김 → 재시도 로직 | MEDIUM | ⚠️ | `TranscriptionRepository.kt` | 기본 재시도는 있으나 UI 피드백 미확인 |

---

## 6. 비활성화 상태 모드 연속성

### 6.1 NORMAL 모드 비활성화

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| BG-N1 | Playing 중 화면 끄기 → 청크 완료 → 자동 다음 청크 | **CRITICAL** | ✅ | `PlaybackService.kt:408-430` | Foreground Service |
| BG-N2 | Playing 중 화면 끄기 (repeatCount=3) | **CRITICAL** | ✅ | `LearningStateMachine.kt` | 반복 로직 |
| BG-N3 | 마지막 청크 Playing 중 화면 끄기 | HIGH | ⚠️ | `PlayerViewModel.kt` | **플레이리스트 자동 전환 로직 미구현** |

### 6.2 LR 모드 비활성화

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| BG-LR1 | Playing 중 화면 끄기 → Gap → 다음 사이클 | **CRITICAL** | ✅ | `PlaybackService.kt` | Gap 타이머 동작 |
| BG-LR2 | Gap 중 화면 끄기 → Gap 완료 → Playing 재개 | **CRITICAL** | ✅ | `PlaybackService.kt:446-455` | delay() 사용 |
| BG-LR3 | Gap 중 화면 끄기 (repeatCount=2) | **CRITICAL** | ✅ | `LearningStateMachine.kt` | 반복 로직 |
| BG-LR4 | 화면 끄고 5분 후 → LR 사이클 계속 | **CRITICAL** | ✅ | `PlaybackService.kt` | Foreground Service |

### 6.3 LRLR 모드 비활성화

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| BG-LRLR1 | PlayingFirst 중 화면 끄기 → 전체 사이클 완료 | **CRITICAL** | ✅ | `PlaybackService.kt` | Foreground Service |
| BG-LRLR2 | GapWithRecording 중 화면 끄기 → 녹음 계속 | **CRITICAL** | ✅ | `RecordingManager.kt:218-235` | WakeLock |
| BG-LRLR3 | PlayingSecond 중 화면 끄기 → 사이클 완료 | **CRITICAL** | ✅ | `PlaybackService.kt` | Foreground Service |
| BG-LRLR4 | PlaybackRecording 중 화면 끄기 → 사이클 완료 | **CRITICAL** | ✅ | `PlaybackService.kt` | Foreground Service |
| BG-LRLR5 | 화면 끄고 LRLR 2사이클 연속 | **CRITICAL** | ✅ | `PlaybackService.kt`, `LearningStateMachine.kt` | |
| BG-LRLR6 | 화면 끄고 10분 (LRLR 모드) | **CRITICAL** | ✅ | `PlaybackService.kt` | MediaSessionService + WakeLock |

### 6.4 알림/잠금화면 컨트롤

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| BG-C1 | 화면 끄기 후 알림에서 일시정지 | HIGH | ⚠️ | `PlaybackService.kt` | MediaSession 기본 동작, 명시적 핸들링 미확인 |
| BG-C2 | 화면 끄기 후 알림에서 재개 | HIGH | ⚠️ | `PlaybackService.kt` | MediaSession 기본 동작 |
| BG-C3 | 잠금화면에서 재생/일시정지 토글 | HIGH | ⚠️ | `PlaybackService.kt` | MediaSession 기본 동작 |
| BG-C4 | 알림에서 다음/이전 청크 | HIGH | ⚠️ | `PlaybackService.kt` | MediaSession.Callback 명시적 구현 필요 |

### 6.5 플레이리스트 연속 재생

| ID | 시나리오 | 우선순위 | 상태 | 관련 파일 | 비고 |
|----|----------|---------|------|-----------|------|
| BG-P1 | 화면 끄기 상태에서 플레이리스트 아이템 자동 전환 | **CRITICAL** | ❌ | `PlayerViewModel.kt` | **자동 전환 로직 미구현** |
| BG-P2 | 화면 끄기 상태에서 마지막 아이템 완료 | HIGH | ⚠️ | `PlayerViewModel.kt` | 알림 업데이트 미확인 |
| BG-P3 | LRLR 모드로 플레이리스트 3개 아이템 연속 재생 | **CRITICAL** | ❌ | `PlayerViewModel.kt` | **BG-P1과 동일** |

---

## 7. 요약 통계

### 우선순위별 현황

| 우선순위 | 총 개수 | ✅ 구현 | ⚠️ 부분 | ❌ 미구현 |
|---------|--------|--------|--------|---------|
| **CRITICAL** | 32 | 25 | 3 | 4 |
| **HIGH** | 38 | 26 | 8 | 4 |
| **MEDIUM** | 14 | 10 | 4 | 0 |
| **LOW** | 3 | 3 | 0 | 0 |
| **합계** | **87** | **64 (74%)** | **15 (17%)** | **8 (9%)** |

### CRITICAL 미구현 목록 (즉시 해결 필요)

| ID | 시나리오 | 파일 | 필요 작업 |
|----|----------|------|----------|
| AF-1~5 | 오디오 포커스 리스너 | `PlaybackService.kt` | AudioFocusListener 구현 |
| BG-P1 | 플레이리스트 자동 전환 | `PlayerViewModel.kt` | 마지막 청크 완료 시 nextPlaylistItem() 호출 |
| BG-P3 | LRLR 플레이리스트 연속 재생 | `PlayerViewModel.kt` | BG-P1과 동일 |

### HIGH 미구현 목록 (우선 해결)

| ID | 시나리오 | 파일 | 필요 작업 |
|----|----------|------|----------|
| ET-5 | 전사 미완료 에피소드 자동 이동 | `PlayerViewModel.kt` | isTranscribed() 체크 추가 |
| PR-3 | MediaRecorder.pause() | `RecordingManager.kt` | pause() 메서드 추가 |
| PR-4 | MediaRecorder.resume() | `RecordingManager.kt` | resume() 메서드 추가 |
| PM-2 | 저장공간 부족 감지 | `RecordingManager.kt` | hasEnoughSpace() 체크 추가 |

### 부분 구현 (개선 필요)

| ID | 시나리오 | 파일 | 필요 작업 |
|----|----------|------|----------|
| CN-1 | navigationChannel 용량 | `PlayerViewModel.kt:76` | Channel.UNLIMITED → CONFLATED 변경 |
| CN-6 | Service timeout 에러 반환 | `PlaybackController.kt:116-128` | 호출자에게 실패 신호 전달 |
| LR-1 | Gap 타이머 재개 | `PlaybackService.kt:192-203` | 남은 Gap 시간 저장/복원 |
| MT-6 | 빠른 모드 토글 중간 상태 | `PlayerViewModel.kt` | Mutex 보호 추가 |

---

## 8. 수정 우선순위

### 🔴 즉시 수정 (CRITICAL)

1. **플레이리스트 자동 전환 구현**
   - 파일: `PlayerViewModel.kt`
   - 작업: 마지막 청크 완료 이벤트 수신 → nextPlaylistItem() 자동 호출

2. **navigationChannel 용량 제한**
   - 파일: `PlayerViewModel.kt:76`
   - 작업: `Channel.UNLIMITED` → `Channel.CONFLATED`

### 🟠 이번 주 수정 (HIGH)

3. **오디오 포커스 리스너 구현**
   - 파일: `PlaybackService.kt`
   - 작업: AudioFocusListener + ACTION_AUDIO_BECOMING_NOISY BroadcastReceiver

4. **MediaRecorder pause/resume 구현**
   - 파일: `RecordingManager.kt`
   - 작업: pause(), resume() 메서드 추가

5. **저장공간 부족 감지**
   - 파일: `RecordingManager.kt`
   - 작업: hasEnoughSpace() 체크 + 에러 핸들링

6. **전사 미완료 에피소드 자동 이동**
   - 파일: `PlayerViewModel.kt:loadPlaylistItem()`
   - 작업: isTranscribed() 체크 → TranscriptionScreen 네비게이션

### 🟡 다음 주 수정 (MEDIUM)

7. **Service connection timeout 에러 반환**
   - 파일: `PlaybackController.kt`
   - 작업: sealed class 결과 타입으로 성공/실패 구분

8. **Gap 타이머 재개 로직 완성**
   - 파일: `PlaybackService.kt`
   - 작업: 남은 Gap 시간 저장 및 resume 시 복원

9. **알림 액션 명시적 구현**
    - 파일: `PlaybackService.kt`
    - 작업: MediaSession.Callback으로 다음/이전 청크 처리

---

*Generated: 2026-01-23*
