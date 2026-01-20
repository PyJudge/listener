# Phase 4: 기본 UI & 네비게이션

## 비즈니스 로직

### 해결하는 문제
SPEC.md 2장에 정의된 5탭 네비게이션 구조와 공통 UI 컴포넌트를 구현합니다.
미니 플레이어가 항상 탭바 위에 표시되는 레이아웃을 구축합니다.

### 핵심 규칙
1. **5탭 구조**: 홈, 플레이리스트, 팟캐스트, 미디어파일, 설정
2. **미니 플레이어**: 재생 중일 때 탭바 위 고정
3. **전체화면 플레이어**: 미니 플레이어 확장 시 오버레이

### 제약/예외
- Material 3 디자인 시스템
- Compose Navigation 사용
- 화면 회전 시 상태 유지

---

## 설계

### 네비게이션 구조
```
+-----------------------------------------------------------+
|                    메인 콘텐츠 영역                         |
|  (NavHost: 홈/플레이리스트/팟캐스트/미디어/설정)             |
+-----------------------------------------------------------+
|  EP.289 How to Sound...   <<  ||  >>  =========           |  <- 미니 플레이어
+-----------------------------------------------------------+
|   Home    List    Podcast   Media   Settings              |  <- 탭바
+-----------------------------------------------------------+
```

### 컴포넌트
```
presentation/
├── navigation/
│   ├── ListenerNavHost.kt      # NavHost 정의
│   ├── Screen.kt               # sealed class 라우트
│   └── BottomNavBar.kt         # 하단 탭바
├── components/
│   ├── MiniPlayer.kt           # 미니 플레이어
│   ├── ContentCard.kt          # 공통 카드 컴포넌트
│   ├── ProgressBar.kt          # 진행률 바
│   ├── EmptyState.kt           # 빈 상태 화면
│   └── LoadingState.kt         # 로딩 상태
└── MainScreen.kt               # Scaffold + 탭 + 미니플레이어
```

### Screen 라우트
```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Playlist : Screen("playlist")
    object PlaylistDetail : Screen("playlist/{playlistId}")
    object Podcast : Screen("podcast")
    object PodcastSearch : Screen("podcast/search")
    object PodcastDetail : Screen("podcast/{feedUrl}")
    object MediaFile : Screen("media")
    object Settings : Screen("settings")
    object Transcription : Screen("transcription/{sourceId}")
    object FullScreenPlayer : Screen("player")
}
```

---

## 테스트 케이스

### 네비게이션 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 홈 탭 클릭 | 홈 아이콘 탭 | Home 화면 표시 |
| 정상 | 탭 전환 | 플레이리스트 -> 팟캐스트 | 화면 전환 |
| 정상 | 백스택 | 상세 -> 뒤로가기 | 목록으로 복귀 |
| 정상 | 딥링크 | podcast/{feedUrl} | 상세 화면 직접 이동 |
| 엣지 | 동일 탭 재클릭 | 현재 탭 다시 클릭 | 최상단 스크롤 |

### 미니 플레이어 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 재생 중 표시 | isPlaying = true | 미니 플레이어 visible |
| 정상 | 미재생 숨김 | isPlaying = false | 미니 플레이어 gone |
| 정상 | 탭 -> 확장 | 미니 플레이어 탭 | 전체화면 플레이어 |
| 정상 | 스와이프 확장 | 위로 스와이프 | 전체화면 플레이어 |
| 정상 | 버튼 동작 | Play/Pause 탭 | 재생/일시정지 토글 |

### 공통 컴포넌트 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | ContentCard 표시 | title, subtitle | 올바른 텍스트 표시 |
| 정상 | ProgressBar 표시 | progress = 0.5f | 50% 채워짐 |
| 정상 | EmptyState 표시 | message, actionLabel | 메시지 + 버튼 표시 |

---

## 실행 계획

### TDD 순서
1. **Screen sealed class** 정의
2. **BottomNavBar** 테스트 -> 구현
3. **ListenerNavHost** 테스트 -> 구현
4. **MiniPlayer** 테스트 -> 구현
5. **MainScreen** 통합 (Scaffold)
6. **공통 컴포넌트** 테스트 -> 구현

### 병렬/순차
- **병렬**: 공통 컴포넌트들 (ContentCard, ProgressBar, etc.)
- **순차**: NavHost -> MainScreen -> MiniPlayer

---

## 완료 체크리스트

- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew test` 모든 테스트 통과
- [ ] `./gradlew lint` 경고/오류 없음
- [ ] Android Emulator 실행 확인 (스크린샷)
- [ ] 5탭 네비게이션 동작
- [ ] 탭 전환 시 상태 유지
- [ ] 미니 플레이어 표시/숨김
- [ ] 미니 플레이어 -> 전체화면 전환
- [ ] 공통 컴포넌트 Preview 작성
- [ ] 화면 회전 시 상태 유지

---

## 학습 내용 (Phase 4 완료 후)

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
