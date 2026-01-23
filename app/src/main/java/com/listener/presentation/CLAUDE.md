# CLAUDE.md - Presentation 계층

## ⚠️ 수정 시 주의사항

### 플레이어 네비게이션 이벤트
- 플레이어 화면에서 전사 화면으로 자동 이동 기능 있음
- 청크가 0개일 때 전사 화면으로 이동 이벤트 발행
- **FullScreenPlayerScreen에서 이벤트를 반드시 관찰해야 함** (안 하면 "no content" 화면 멈춤)
- 이벤트 처리 후 이벤트 소비 함수 호출 필수

### 청크 탐색 Debounce
- seekBar나 청크 리스트 연타 시 마지막 요청만 처리 (300ms debounce)
- **다음/이전 청크 버튼은 debounce 없음** (모든 요청 순차 처리)
- 5번 연속 "다음" 누르면 5개 청크 이동해야 함

### 플레이리스트 자동 전환
- 현재 아이템의 마지막 청크 완료 시 다음 아이템으로 자동 전환
- 플레이리스트 모드일 때만 동작
- **백그라운드에서도 동작해야 함** (화면 꺼도 다음 아이템 재생)

### 전사 미완료 에피소드 선택 시
- 청크가 0개인 에피소드 선택하면 자동으로 전사 화면으로 이동
- FullScreenPlayerScreen → ListenerNavHost 콜백 연결 필수
- 콜백 누락 시 "no content" 화면 멈춤

---

## 핵심 불변조건 (INVARIANTS)

```
1. PlayerViewModel은 앱 전체에서 단일 인스턴스 유지 (재생 상태 보존)
2. 화면 회전/백그라운드 전환 시 ViewModel 상태 유지
3. 플레이리스트 재생 시 현재 항목 인덱스는 실제 재생 상태와 일치
```

---

## 알려진 문제점

### HIGH - 플레이어 ViewModel 재생성
**현상:** 화면 전환 시 플레이어 상태(재생 위치, 현재 청크)가 유실됨
**원인:** MainScreen에서 ViewModel이 매번 새로 생성됨
**해결책:** DI에서 싱글톤으로 바인딩하거나 Activity 범위 ViewModel 사용

### HIGH - 플레이어 화면 진입 시 재로드
**현상:** 같은 에피소드인데 화면 전환할 때마다 처음부터 다시 로드
**해결책:** 이미 로드된 콘텐츠면 재로드 스킵

### HIGH - 플레이리스트 현재 재생 항목 표시 오류
**현상:** 플레이리스트에서 "현재 재생 중" 표시가 실제 재생 상태와 다름
**원인:** 첫 번째 미완료 항목을 현재 항목으로 가정
**해결책:** 실제 재생 중인 sourceId와 비교

### MEDIUM - 홈 화면 UI 깜빡임
**현상:** 홈 화면 로드 시 데이터가 순차적으로 표시되며 깜빡임
**원인:** 4개의 독립적인 데이터 스트림을 개별 구독
**해결책:** combine으로 한 번에 상태 업데이트

### MEDIUM - 미니 플레이어 "No content" 표시
**현상:** 플레이어에서 뒤로 나가면 미니 플레이어에 "No content" 표시
**원인:** 메타데이터가 상태 스트림이 아닌 일반 변수로 관리됨

### MEDIUM - 전사 에러 메시지 너무 빨리 사라짐
**현상:** 전사 실패 시 에러 메시지가 2초 후 자동으로 화면 닫힘
**문제:** 사용자가 에러 내용을 읽기 어려움

---

## 네비게이션 구조

```
BottomNavBar
├── HomeScreen
├── PlaylistScreen → PlaylistDetailScreen
├── PodcastScreen → PodcastSearchScreen → PodcastDetailScreen
├── MediaFileScreen
└── SettingsScreen

공통
├── MiniPlayer (하단 고정)
├── FullScreenPlayerScreen (오버레이)
├── TranscriptionScreen (전사 진행)
└── ContentBottomSheet (에피소드 상세)
```

---

## 사용자 흐름 검증 체크리스트

### 팟캐스트 → 학습
- [ ] PodcastScreen → 검색 → 구독
- [ ] PodcastDetailScreen → 에피소드 선택 → ContentBottomSheet
- [ ] "학습 시작" → TranscriptionScreen → FullScreenPlayerScreen
- [ ] 전사 중 다른 탭 이동 → 돌아오기 → 진행률 유지

### 플레이리스트 학습
- [ ] PlaylistDetailScreen → "이어서 학습" → FullScreenPlayerScreen
- [ ] ⏮️ ⏭️로 항목 간 이동
- [ ] 드래그 재정렬 후 재생 순서 확인
- [ ] 현재 재생 중인 항목 드래그 → 동작 확인

### 미니 플레이어
- [ ] 플레이어에서 뒤로가기 → 미니 플레이어 제목 표시
- [ ] 미니 플레이어 탭 → FullScreenPlayerScreen 확장
- [ ] 위로 스와이프 → 확장

---

## 수정 시 체크리스트

```bash
# 1. ViewModel 테스트
./gradlew test --tests "*.HomeViewModelTest"
./gradlew test --tests "*.PlayerViewModelTest"
./gradlew test --tests "*.PlaylistViewModelTest"

# 2. UI 테스트 (Instrumented)
./gradlew connectedAndroidTest --tests "*.HomeScreenTest"

# 3. 실기기 테스트 시나리오
- 화면 회전 → 상태 유지 확인
- 백그라운드 전환 → 복귀 후 상태 확인
- 메모리 부족 상황 시뮬레이션 (개발자 옵션)
```

---

## 핵심 파일

### 화면 (Screen)

| 화면 | 파일 |
|------|------|
| 홈 | `home/HomeScreen.kt` |
| 플레이리스트 목록 | `playlist/PlaylistScreen.kt` |
| 플레이리스트 상세 | `playlist/PlaylistDetailScreen.kt` |
| 팟캐스트 목록 | `podcast/PodcastScreen.kt` |
| 팟캐스트 검색 | `podcast/PodcastSearchScreen.kt` |
| 팟캐스트 상세 | `podcast/PodcastDetailScreen.kt` |
| 미디어 파일 | `media/MediaFileScreen.kt` |
| 전사 진행 | `transcription/TranscriptionScreen.kt` |
| 전체화면 플레이어 | `player/FullScreenPlayerScreen.kt` |
| 미니 플레이어 | `player/MiniPlayer.kt` |
| 설정 | `settings/SettingsScreen.kt` |

### 공통 컴포넌트

| 컴포넌트 | 파일 |
|----------|------|
| 에피소드 바텀시트 | `components/ContentBottomSheet.kt` |
| 플레이리스트 선택 | `components/PlaylistSelectDialog.kt` |
| 카드 | `components/ListenerCard.kt` |
| 버튼 | `components/ListenerButton.kt` |
| 빈 상태 | `components/EmptyState.kt` |
| 로딩 상태 | `components/LoadingState.kt` |

### 네비게이션

| 파일 | 역할 |
|------|------|
| `navigation/Screen.kt` | Screen 라우트 정의 |
| `navigation/ListenerNavHost.kt` | NavHost 구성 |
| `navigation/BottomNavBar.kt` | 하단 탭 네비게이션 |
| `MainScreen.kt` | 메인 화면 (NavHost + BottomNav + MiniPlayer) |
