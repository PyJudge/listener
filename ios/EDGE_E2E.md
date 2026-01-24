# EDGE_E2E.md - Edge Case End-to-End Test Scenarios

> Listener Android App 실사용 시나리오 기반 Edge Case 정의 및 기대 동작

---

## 목차

1. [Edge Case 분류](#1-edge-case-분류)
2. [모드별 시나리오](#2-모드별-시나리오)
3. [전환 시나리오](#3-전환-시나리오)
4. [중단 시나리오](#4-중단-시나리오)
5. [동시성 시나리오](#5-동시성-시나리오)
6. [시스템 이벤트 시나리오](#6-시스템-이벤트-시나리오)
7. [비활성화 상태 모드 연속성](#7-비활성화-상태-모드-연속성)
8. [테스트 검증 체크리스트](#8-테스트-검증-체크리스트)
9. [구현 규칙](#9-구현-규칙)

---

## 1. Edge Case 분류

### 우선순위 정의

| Level | 정의 | 예시 |
|-------|------|------|
| **CRITICAL** | 데이터 손실/손상, 앱 크래시 | 녹음 파일 손상, 재생 위치 영구 손실 |
| **HIGH** | 잘못된 동작, 심각한 UX 저하 | 오디오-텍스트 불일치, 무한 대기 |
| **MEDIUM** | 불편함, 비정상적 UI | 순간 깜빡임, 잘못된 메타데이터 표시 |
| **LOW** | 미미한 영향 | 버퍼링 시 UI 딜레이 |

---

## 2. 모드별 시나리오

### 2.1 NORMAL 모드

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| N-1 | 재생 중 같은 청크 터치 | 해당 청크 처음부터 재시작 | MEDIUM |
| N-2 | 재생 중 다른 청크 터치 | 현재 재생 중단, 선택 청크로 즉시 이동 | HIGH |
| N-3 | 재생 완료 시 자동 다음 청크 | repeatCount 만큼 반복 후 다음 청크로 이동 | HIGH |
| N-4 | 마지막 청크 재생 완료 | 플레이리스트 모드: 다음 아이템으로 이동 / 단독: 정지 | HIGH |

### 2.2 LR (Listen & Repeat) 모드

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| LR-1 | Gap 중 일시정지 | Gap 타이머 일시정지, 재개 시 남은 Gap 시간 계속 | HIGH |
| LR-2 | Gap 중 다른 청크 터치 | Gap 취소, 새 청크 Playing 상태로 전환 | HIGH |
| LR-3 | Gap 중 이전/다음 버튼 | Gap 취소, 해당 청크로 즉시 이동 | HIGH |
| LR-4 | Gap 중 같은 청크 터치 | Gap 취소, 현재 청크 Playing 상태로 재시작 | MEDIUM |

### 2.3 LRLR (HARD 모드)

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| LRLR-1 | PlayingFirst 중 다른 청크 터치 | 재생 중단, 새 청크 PlayingFirst로 전환 | HIGH |
| LRLR-2 | GapWithRecording 중 다른 청크 터치 | **녹음 중단 및 삭제**, 새 청크 PlayingFirst로 전환 | **CRITICAL** |
| LRLR-3 | PlayingSecond 중 다른 청크 터치 | 재생 중단, 새 청크 PlayingFirst로 전환 | HIGH |
| LRLR-4 | PlaybackRecording 중 다른 청크 터치 | 녹음 재생 중단, 새 청크 PlayingFirst로 전환 | HIGH |
| LRLR-5 | 녹음 권한 없이 LRLR 모드 진입 시도 | 권한 요청 팝업 표시, **거부 시 LR 모드로 자동 전환** | **CRITICAL** |
| LRLR-6 | 녹음 중 마이크 사용 불가 (다른 앱 점유) | 녹음 실패 감지, LR 모드로 자동 전환, 에러 토스트 | HIGH |
| LRLR-7 | 동시 녹음 요청 발생 | 두 번째 요청 무시 또는 첫 번째 정지 후 시작, **파일 손상 방지** | HIGH |

#### LRLR 구현 시 주의사항

```
□ 모드 토글 시 권한 체크 결과에 따라 실제 상태 변경 (권한 거부 시 LRLR 진입 차단)
  - 파일: FullScreenPlayerScreen.kt
□ Recording file access에 동시성 보호 적용 (Mutex 또는 synchronized)
  - 파일: RecordingManager.kt
```

---

## 3. 전환 시나리오

### 3.1 모드 전환

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| MT-1 | Playing 중 NORMAL → LR | 현재 청크 Playing 상태 유지, 완료 시 Gap 진입 | HIGH |
| MT-2 | Playing 중 LR → LRLR | Playing → PlayingFirst로 상태 변경, 완료 시 GapWithRecording 진입 | HIGH |
| MT-3 | Gap 중 LR → NORMAL | Gap 취소, Playing 상태로 전환 | HIGH |
| MT-4 | GapWithRecording 중 LRLR → LR | **녹음 중단 및 삭제**, Gap 상태로 전환 | **CRITICAL** |
| MT-5 | PlaybackRecording 중 LRLR → NORMAL | 녹음 재생 중단, Playing 상태로 전환 | HIGH |
| MT-6 | 빠른 모드 토글 연타 (5회 이상) | 최종 모드만 적용, 중간 상태 정리 완료 | HIGH |

### 3.2 에피소드/콘텐츠 전환

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| ET-1 | Playing 중 다른 에피소드 선택 | 현재 재생 정지, 새 에피소드 로드 후 자동 재생 | HIGH |
| ET-2 | Gap 중 다른 에피소드 선택 | Gap 취소, 새 에피소드 로드 | HIGH |
| ET-3 | GapWithRecording 중 다른 에피소드 선택 | **녹음 중단 및 삭제**, 새 에피소드 로드 | **CRITICAL** |
| ET-4 | 플레이리스트 아이템 전환 (⏮️/⏭️) 중 녹음 | **cancelPendingOperations 호출 후** 다음 아이템으로 이동 | **CRITICAL** |
| ET-5 | 전사 미완료 에피소드 선택 | TranscriptionScreen으로 자동 이동, 완료 후 플레이어 진입 | HIGH |

#### 콘텐츠 전환 시 주의사항

```
□ loadPlaylistItem() 호출 전 cancelPendingOperations() 반드시 호출
  - 파일: PlayerViewModel.kt
□ 새 에피소드 로드 시 이전 상태 완전 정리 확인
  - recordingJob, gapJob, 모든 pending operation cancel + join
```

### 3.3 청크 네비게이션

| ID | 시나리오 | 기대 동작 | 우선순위 | 상태 |
|----|----------|----------|---------|------|
| CN-1 | 빠른 청크 스킵 연타 (1초에 5회) | navigationMutex 직렬화, 최종 청크만 재생, **Channel 용량 제한** | **CRITICAL** | |
| CN-2 | 첫 청크에서 이전 버튼 | 무반응 또는 현재 청크 재시작 | LOW | |
| CN-3 | 마지막 청크에서 다음 버튼 | 플레이리스트: 다음 아이템 / 단독: 무반응 | MEDIUM | |
| CN-4 | seekBar 드래그 중 다른 청크 터치 | seekBar 드래그 취소, 터치한 청크로 이동 | MEDIUM | |
| CN-5 | seekBar 드래그 완료와 동시에 청크 터치 | 마지막 입력만 처리 (청크 터치 우선) | MEDIUM | |
| CN-6 | Service connection timeout | **명시적 에러 반환**, 호출자에게 실패 알림 | MEDIUM | |
| CN-7 | **정지 상태에서 청크 터치** | 해당 청크로 이동 + 재생 시작 + UI 업데이트 | HIGH | ✅ 수정됨 |

#### 청크 네비게이션 시 주의사항

```
□ NavigationChannel 용량 제한 적용 (Channel.CONFLATED 또는 적절한 버퍼 크기)
□ Service connection timeout 시 명시적 에러 처리
```

---

## 4. 중단 시나리오

### 4.1 일시정지/재개

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| PR-1 | Playing 중 일시정지 → 재개 | 정확한 위치에서 재개 (±30ms) | HIGH |
| PR-2 | Gap 중 일시정지 → 재개 | Gap 타이머 재개, 남은 시간 계속 | HIGH |
| PR-3 | GapWithRecording 중 일시정지 | 녹음 일시정지 (MediaRecorder.pause), Gap 타이머 정지 | HIGH |
| PR-4 | GapWithRecording 중 일시정지 → 재개 | 녹음 재개 (MediaRecorder.resume), Gap 타이머 재개 | HIGH |
| PR-5 | PlaybackRecording 중 일시정지 → 재개 | 녹음 재생 정확한 위치에서 재개 | MEDIUM |
| PR-6 | 일시정지 후 1시간 경과 → 재개 | 저장된 위치에서 정상 재개 | MEDIUM |
| PR-7 | 일시정지 후 청크 변경 → 재개 | **새 청크 처음부터 재생** (이전 position 무시, position 검증 필수) | **CRITICAL** |

#### 일시정지/재개 시 주의사항

```
□ 일시정지 후 청크 변경 시 position 검증 필수
  - 파일: PlaybackService.kt
```

### 4.2 앱 중단

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| AS-1 | 홈 버튼 (앱 배경) | 재생 계속 (Foreground Service) | **CRITICAL** |
| AS-2 | 화면 끄기 중 재생 | 재생 계속 (Screen Off doesn't stop service) | **CRITICAL** |
| AS-3 | 화면 끄기 중 녹음 (LRLR) | 녹음 계속, **WakeLock으로 CPU 유지** | **CRITICAL** |
| AS-4 | 최근 앱에서 스와이프 종료 | 서비스 정지, 진행 상황 저장 | HIGH |
| AS-5 | 설정 > 강제 종료 | 서비스 즉시 종료, **녹음 중이면 MediaRecorder 정리** | HIGH |
| AS-6 | 배경 10분 이상 유지 | 서비스 유지 (**startForeground + Notification 필수**) | **CRITICAL** |
| AS-7 | 메모리 부족으로 프로세스 종료 | 서비스 재시작 (START_STICKY), 진행 상황 복구 | HIGH |

#### 앱 중단 시 주의사항

```
□ PlaybackService에 startForeground() 필수
  - 파일: PlaybackService.kt:onCreate
□ 녹음 중 WakeLock 획득 필수
  - 파일: RecordingManager.kt:startRecording
□ Force Stop 시 MediaRecorder 정리 로직 필수
  - 파일: PlaybackService.kt:onDestroy
```

---

## 5. 동시성 시나리오

### 5.1 Race Condition

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| RC-1 | 녹음 중 빠른 청크 스킵 | **녹음 job cancel + join 완료 후** 스킵 | **CRITICAL** |
| RC-2 | 녹음 재생 중 청크 변경 | 녹음 재생 중단, **targetChunkIndex 검증** | HIGH |
| RC-3 | setContent 중 다른 setContent 호출 | navigationMutex로 직렬화 | HIGH |
| RC-4 | updateChunks 중 playCurrentChunk 호출 | **bounds check + 동시 접근 보호**로 IndexOutOfBounds 방지 | HIGH |

#### Race Condition 방지 주의사항

```
□ recordingJob cancel 후 반드시 join() 호출
  - 파일: PlaybackService.kt
□ 청크 변경 시 targetChunkIndex 검증
  - 파일: PlaybackService.kt
□ updateChunks 중 chunk list 동시 접근 보호
```

### 5.2 UI 동기화

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| US-1 | 화면 회전 중 청크 변경 | Configuration change 후 정확한 상태 복원 | HIGH |
| US-2 | LaunchedEffect 실행 전 청크 터치 | 최신 입력 우선 처리 | MEDIUM |
| US-3 | seekBar 드래그 중 외부 청크 변경 | seekBar UI 업데이트, 드래그 취소 | MEDIUM |
| US-4 | 빠른 blind mode 토글 연타 | 최종 상태만 적용, peek 상태 초기화 | LOW |
| US-5 | 다른 화면에서 MiniPlayer 메타데이터 표시 | **StateFlow로 실시간 동기화**, 정확한 제목 표시 | HIGH |

#### UI 동기화 주의사항

```
□ MiniPlayer metadata는 StateFlow로 관리
  - 파일: PlayerViewModel.kt
```

---

## 6. 시스템 이벤트 시나리오

### 6.1 오디오 포커스

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| AF-1 | 전화 수신 중 재생 | 자동 일시정지, 통화 종료 후 재개 | HIGH |
| AF-2 | 다른 미디어 앱 재생 시작 | 자동 일시정지 | HIGH |
| AF-3 | 알림 소리 재생 시 | 볼륨 덕킹 또는 일시 정지 | MEDIUM |
| AF-4 | 블루투스 연결 끊김 | 자동 일시정지 | HIGH |
| AF-5 | 이어폰 뽑힘 | 자동 일시정지 | HIGH |

### 6.2 권한 및 리소스

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| PM-1 | 녹음 권한 거부 상태에서 LRLR 진입 | 권한 요청, 거부 시 LR 모드로 전환 | **CRITICAL** |
| PM-2 | 재생 중 저장공간 부족 | 녹음 불가 알림, LR 모드로 전환 | HIGH |
| PM-3 | 배터리 절약 모드 진입 | 재생 계속 (Foreground Service 우선순위 유지) | MEDIUM |
| PM-4 | 알 수 없는 오디오 파일 형식 | 에러 토스트, 해당 에피소드 건너뛰기 | MEDIUM |

### 6.3 네트워크

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| NW-1 | 스트리밍 중 네트워크 끊김 | 버퍼 소진 시 일시정지, 재연결 후 재개 | MEDIUM |
| NW-2 | 다운로드 중 네트워크 끊김 | 재시도 로직, 실패 시 에러 표시 | MEDIUM |

---

## 7. 비활성화 상태 모드 연속성

> **핵심 원칙**: 화면 꺼짐/앱 백그라운드 상태에서도 **현재 모드의 전체 사이클이 끊김 없이 완료**되어야 함

### 7.1 모드별 비활성화 연속성 사이클

#### NORMAL 모드 비활성화 시 사이클

```
[화면 활성] Playing → [화면 끄기] → Playing 계속 → repeatCount 완료 → NextChunk → Playing → ...
```

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| BG-N1 | Playing 중 화면 끄기 | 재생 계속 → 청크 완료 → 자동 다음 청크 이동 | **CRITICAL** |
| BG-N2 | Playing 중 화면 끄기 (repeatCount=3) | 3회 반복 완료 → 다음 청크 → 3회 반복 → ... | **CRITICAL** |
| BG-N3 | 마지막 청크 Playing 중 화면 끄기 | 청크 완료 → 플레이리스트 아이템 자동 전환 또는 정지 | HIGH |

#### LR 모드 비활성화 시 사이클

```
[화면 활성] Playing → [화면 끄기] → Playing 완료 → Gap → Gap 완료 → Playing → (repeat) → NextChunk → ...
```

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| BG-LR1 | Playing 중 화면 끄기 | Playing 완료 → Gap 진입 → Gap 완료 → 다음 사이클 | **CRITICAL** |
| BG-LR2 | Gap 중 화면 끄기 | Gap 타이머 계속 → Gap 완료 → Playing 재개 | **CRITICAL** |
| BG-LR3 | Gap 중 화면 끄기 (repeatCount=2) | Gap → Playing → Gap → NextChunk (총 2회 반복) | **CRITICAL** |
| BG-LR4 | 화면 끄고 5분 후 | LR 사이클 계속 진행 중 | **CRITICAL** |

#### LRLR 모드 비활성화 시 사이클 (가장 복잡)

```
[화면 활성] PlayingFirst → [화면 끄기]
→ PlayingFirst 완료
→ GapWithRecording (녹음 시작 및 계속)
→ GapWithRecording 완료 (녹음 종료)
→ PlayingSecond
→ PlayingSecond 완료
→ PlaybackRecording (내 녹음 재생)
→ PlaybackRecording 완료
→ (repeat 또는 NextChunk)
→ ...
```

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| BG-LRLR1 | PlayingFirst 중 화면 끄기 | PlayingFirst 완료 → GapWithRecording (녹음 시작) → 전체 사이클 완료 | **CRITICAL** |
| BG-LRLR2 | GapWithRecording 중 화면 끄기 | **녹음 계속** (WakeLock 필요) → Gap 완료 → PlayingSecond → 사이클 완료 | **CRITICAL** |
| BG-LRLR3 | PlayingSecond 중 화면 끄기 | PlayingSecond 완료 → PlaybackRecording (녹음 재생) → 사이클 완료 | **CRITICAL** |
| BG-LRLR4 | PlaybackRecording 중 화면 끄기 | 녹음 재생 완료 → 다음 repeat 또는 NextChunk | **CRITICAL** |
| BG-LRLR5 | 화면 끄고 LRLR 2사이클 연속 | 사이클 1 완료 → 사이클 2 시작 및 완료 → NextChunk | **CRITICAL** |
| BG-LRLR6 | 화면 끄고 10분 (LRLR 모드) | 서비스 유지 + 녹음 정상 동작 | **CRITICAL** |

### 7.2 비활성화 상태 알림/잠금화면 컨트롤

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| BG-C1 | 화면 끄기 후 알림에서 일시정지 | 현재 모드 상태 그대로 일시정지 (Gap이면 Gap, Recording이면 Recording) | HIGH |
| BG-C2 | 화면 끄기 후 알림에서 재개 | 일시정지된 상태에서 그대로 재개 | HIGH |
| BG-C3 | 잠금화면에서 재생/일시정지 토글 | 위와 동일 | HIGH |
| BG-C4 | 알림에서 다음/이전 청크 | 청크 전환 (녹음 중이면 녹음 정리 후 전환) | HIGH |

### 7.3 비활성화 상태 플레이리스트 연속 재생

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| BG-P1 | 화면 끄기 상태에서 플레이리스트 아이템 자동 전환 | 다음 아이템 로드 → 현재 모드로 자동 재생 시작 | **CRITICAL** |
| BG-P2 | 화면 끄기 상태에서 마지막 아이템 완료 | 플레이리스트 완료 상태로 정지, 알림 업데이트 | HIGH |
| BG-P3 | LRLR 모드로 플레이리스트 3개 아이템 연속 재생 (화면 끄기) | 각 아이템마다 LRLR 사이클 완료 → 다음 아이템 전환 | **CRITICAL** |

### 7.4 비활성화 상태 불변조건 (Invariants)

```
화면 꺼짐/백그라운드 상태에서 반드시 보장되어야 하는 동작:

1. [NORMAL] Playing → (repeat × N) → NextChunk → Playing → ...
   - 화면 상태와 무관하게 repeatCount 만큼 반복 후 다음 청크 이동

2. [LR] Playing → Gap → Playing → (repeat × N) → NextChunk → ...
   - Gap 타이머가 화면 상태와 무관하게 정확히 동작
   - Gap 완료 후 자동으로 Playing 상태 진입

3. [LRLR] PlayingFirst → GapWithRecording → PlayingSecond → PlaybackRecording → (repeat × N) → NextChunk → ...
   - 녹음이 화면 꺼진 상태에서도 정상 진행 (WakeLock 필수)
   - 녹음 파일이 정상적으로 저장됨
   - 녹음 재생이 정상적으로 완료됨
   - 전체 사이클이 중단 없이 완료됨
```

### 7.5 비활성화 상태 필수 구현 사항

| 구현 사항 | 파일 |
|----------|------|
| startForeground() + Notification | `PlaybackService.kt:onCreate` |
| 녹음 중 WakeLock 획득 | `RecordingManager.kt:startRecording` |
| MediaSession notification 표시 | `PlaybackService.kt` |
| Gap 타이머 AlarmManager 또는 WorkManager 검토 | `PlaybackService.kt` |

---

## 8. 테스트 검증 체크리스트

### 수동 테스트 필수 항목

#### 비활성화 모드 연속성 테스트

```
□ [NORMAL] 화면 끄고 5분 재생 → 여러 청크 자동 전환 확인
□ [LR] 화면 끄고 5분 → Playing-Gap-Playing 사이클 계속 확인
□ [LRLR] 화면 끄고 GapWithRecording 진입 → 녹음 파일 생성 확인
□ [LRLR] 화면 끄고 5분 → 전체 사이클 2회 이상 완료 확인
□ [LRLR] 화면 끄고 PlaybackRecording → 녹음 재생 완료 확인
□ 백그라운드 15분 유지 → 서비스 생존 확인
□ 잠금화면 알림에서 일시정지/재개 → 상태 유지 확인
□ 플레이리스트 LRLR 모드로 3개 아이템 화면 끄고 연속 재생
```

#### 기본 Edge Case 테스트

```
□ LRLR 모드에서 녹음 중 청크 스킵 3회 연타 → 녹음 파일 정리 확인
□ 권한 거부 후 LRLR 토글 → LR 모드로 fallback 확인
□ 플레이리스트 아이템 전환 중 녹음 → 녹음 정리 확인
□ 앱 강제 종료 후 재시작 → 진행 상황 복구 확인
□ 메모리 부족 상황 재현 → 서비스 재시작 확인
```

### 자동화 테스트 커버리지

| 영역 | 테스트 파일 | 커버리지 |
|------|------------|---------|
| 빠른 청크 네비게이션 | `PlaybackServiceNavigationTest.kt` | ✅ |
| 녹음 중 청크 변경 | `PlaybackServiceNavigationTest.kt` | ✅ |
| ViewModel 로드 | `PlayerViewModelLoadTest.kt` | ✅ |
| 권한 + 모드 토글 | - | ❌ 누락 |
| 플레이리스트 전환 중 녹음 | - | ❌ 누락 |
| Foreground Service 유지 | - | ❌ 누락 |
| 비활성화 상태 모드 사이클 | - | ❌ 누락 |

---

## 9. 구현 규칙

### 새 기능 추가 시 체크리스트

```
□ 모든 Learning State에서 해당 기능이 동작하는지 검토
□ 녹음 중 호출될 경우 cancelPendingOperations 필요 여부 확인
□ navigationMutex 보호가 필요한지 확인
□ 비활성화(화면 끄기/배경) 상태에서 동작 검토
□ 화면 회전 시 상태 복원 검토
□ 이 문서에 관련 시나리오 추가
```

### 청크 관련 코드 수정 시

```
□ bounds check 추가 여부 확인
□ chunks list 동시 접근 가능성 검토
□ currentChunkIndex 변경 시 state machine 알림 확인
□ ChunkingUseCaseRealDataTest 실행
□ 실기기에서 3개 이상 청크 재생 싱크 확인
```

### 녹음 관련 코드 수정 시

```
□ recordingJob cancel 시 반드시 join() 호출
□ targetChunkIndex 검증으로 경합 방지
□ Recording file access 동시성 보호
□ Force Stop 시 MediaRecorder 정리
□ WakeLock 획득/해제 확인
```

### 비활성화 상태 관련 코드 수정 시

```
□ Foreground Service로 보호되는지 확인
□ WakeLock 필요 여부 검토 (녹음, 장시간 타이머)
□ Handler/delay가 화면 꺼짐 시 정상 동작하는지 확인
□ 실기기에서 화면 끄고 해당 기능 5분 테스트
```

### 일시정지/재개 관련 코드 수정 시

```
□ 청크 변경 후 position 초기화 확인
□ 이전 청크의 position이 새 청크에 영향 없는지 검증
```

---

*Last Updated: 2026-01-23*
*Generated by code review with Claude*
