# ERRORFIX - Listener App 개선점 (완료)

## 1. 네비게이션 일관성 ✅

### 1.1 팟캐스트 상세 화면 하단 버튼 비활성화 버그
- **수정**: `BottomNavBar.kt:33`
- **변경**: `it.route == screen.route` → `it.route?.startsWith(screen.route) == true`

---

## 2. UI 중복 제거 ✅

### 2.1 팟캐스트 상세 스피너 중복
- **수정**: `PodcastDetailScreen.kt:242-246`
- **변경**: `if (episodes.isEmpty() && isRefreshing) { LoadingState(...) }` 조건 제거

### 2.2 전사 화면 프로그래스바 중복
- **수정**: `TranscriptionScreen.kt:156-177`
- **변경**: `LinearProgressIndicator` 제거, 원형 프로그래스만 유지

---

## 3. NEW 뱃지 위치 변경 ✅

- **수정**: `PodcastDetailScreen.kt:390-413` (EpisodeItem)
- **변경**:
  - `ListenerCard(badge = null)` - badge 파라미터 제거
  - `Box { ListenerCard(...); if (isNew) Text("new", Alignment.BottomEnd) }` - 카드 우하단 옅은 텍스트

---

## 4. 오디오 저용량 리인코딩 ✅

- **신규**: `domain/usecase/AudioPreprocessUseCase.kt`
- **수정**: `TranscriptionViewModel.kt`
- **추가 의존성**: `media3-transformer`, `media3-effect`, `media3-common`
- **기능**: Media3 Transformer로 전사 전 오디오 압축 (16kHz mono AAC)

---

## 수정된 파일

| 파일 | 변경 내용 |
|------|----------|
| `BottomNavBar.kt` | route startsWith 매칭 |
| `PodcastDetailScreen.kt` | SuccessContent LoadingState 제거, EpisodeItem 뱃지 스타일 |
| `TranscriptionScreen.kt` | LinearProgressIndicator 제거 |
| `TranscriptionViewModel.kt` | AudioPreprocessUseCase 통합 |
| `AudioPreprocessUseCase.kt` | 신규 생성 |
| `libs.versions.toml` | Media3 Transformer 의존성 |
| `build.gradle.kts` | Media3 Transformer 의존성 |
