# CLAUDE.md - Presentation 계층

## 핵심 불변조건 (INVARIANTS)

```
1. PlayerViewModel은 앱 전체에서 단일 인스턴스 유지 (재생 상태 보존)
2. 화면 회전/백그라운드 전환 시 ViewModel 상태 유지
3. 플레이리스트 재생 시 현재 항목 인덱스는 실제 재생 상태와 일치
```

---

## 알려진 문제점

### HIGH - PlayerViewModel 재주입

**현상:** MainScreen에서 PlayerViewModel이 매번 새로 주입되어 재생 상태 유실

**위치:** `MainScreen.kt` 라인 34

**현재 코드:**
```kotlin
fun MainScreen(
    playerViewModel: PlayerViewModel = hiltViewModel(),  // 문제
    ...
)
```

**해결책:** DI에서 `@Singleton` 바인딩 또는 Activity 범위 ViewModel

### HIGH - 플레이어 상태 유실 (매번 재로드)

**현상:** 화면 전환 시 `loadBySourceId()` 재호출로 진행률 초기화

**위치:** `FullScreenPlayerScreen.kt` 라인 120-124

**해결책:** 이미 로드된 sourceId면 재로드 스킵

```kotlin
LaunchedEffect(sourceId) {
    if (sourceId.isNotEmpty() && sourceId != viewModel.currentSourceId) {
        viewModel.loadBySourceId(sourceId)
    }
}
```

### HIGH - 플레이리스트 현재 항목 결정 오류

**현상:** `isCurrent`가 첫 번째 미완료 항목으로 결정되지만 실제 재생 상태와 불일치 가능

**위치:** `PlaylistDetailViewModel.kt` 라인 113-115

**해결책:** RecentLearningDao에서 현재 활성 sourceId 조회

### MEDIUM - HomeViewModel 데이터 동기화 지연

**현상:** 4개의 독립적인 Flow 구독으로 UI 깜빡임

**위치:** `HomeViewModel.kt` 라인 54-59

**해결책:** `combine()` 사용
```kotlin
combine(
    recentLearningDao.getRecentLearnings(5),
    podcastDao.getAllSubscriptions(),
    podcastDao.getNewEpisodeCount(),
    ...
) { learnings, subscriptions, count, ... ->
    // 한 번에 상태 업데이트
}
```

### MEDIUM - MiniPlayer 메타데이터 표시 오류

**현상:** 플레이어에서 뒤로 나가면 미니 플레이어에 "No content" 표시

**원인:** PlayerViewModel 메타데이터가 메모리 변수로 관리 (StateFlow 아님)

**위치:** `PlayerViewModel.kt` 라인 79-82

**해결책:** 메타데이터를 StateFlow로 관리

### MEDIUM - 에러 메시지 2초 자동 dismiss

**현상:** TranscriptionScreen 에러 발생 시 2초 후 자동 뒤로가기

**위치:** `TranscriptionScreen.kt` 라인 59-62

**문제:** 사용자가 에러 메시지 읽을 시간 부족

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
