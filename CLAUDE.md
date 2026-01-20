# CLAUDE.md - Listener Android App

## 프로젝트 현황

| 항목 | 값 |
|------|-----|
| Package | com.listener |
| Min SDK | 23 |
| Target SDK | 35 |
| Kotlin | 2.1.10 |
| UI | Jetpack Compose |
| DI | Hilt (KSP) |
| DB | Room |
| Media | Media3 |
| Network | Retrofit + OkHttp |

---

## 라이브러리 버전 (2026-01-20 업데이트)

| 라이브러리 | 버전 |
|-----------|------|
| Compose BOM | 2025.01.01 |
| Room | 2.8.4 |
| Media3 | 1.8.0 |
| Hilt | 2.54 |
| Retrofit | 2.11.0 |
| OkHttp | 4.12.0 |
| Navigation | 2.8.5 |

---

## 구현 현황 (100% 완료)

### 완료된 기능

| 영역 | 기능 | 상태 |
|------|------|------|
| **데이터 계층** | Entity (11개), DAO (8개), Repository | ✅ 완료 |
| **API** | iTunes Search, OpenAI Whisper | ✅ 완료 |
| **분절 알고리즘** | Sentence Splitter, Timestamp Matcher, Chunk Merger | ✅ 완료 |
| **서비스** | PlaybackService, RecordingManager, LearningStateMachine | ✅ 완료 |
| **홈 화면** | Continue Learning, New Episodes, 플레이리스트 생성 다이얼로그 | ✅ 완료 |
| **팟캐스트 화면** | 구독 목록, 검색, 에피소드 목록 | ✅ 완료 |
| **미디어 파일 화면** | 파일 추가, 목록, 삭제 | ✅ 완료 |
| **플레이리스트 화면** | 목록, 생성, 삭제, 상세, **드래그 재정렬** | ✅ 완료 |
| **전사 화면** | 다운로드, 전사, 진행률 | ✅ 완료 |
| **전체화면 플레이어** | Chunk 재생, 가림 모드, 녹음, 플레이리스트 연동 | ✅ 완료 |
| **미니 플레이어** | 컨트롤, 진행률 | ✅ 완료 |
| **설정 화면** | 학습, 전사, 캐시, API 설정 | ✅ 완료 |

---

## 빌드 및 테스트

```bash
# JAVA_HOME 설정 (필수)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# 빌드
./gradlew assembleDebug

# 테스트
./gradlew test                      # Unit Tests
./gradlew connectedAndroidTest      # Instrumented Tests

# 린트
./gradlew lint

# 설치 및 실행
./gradlew installDebug
adb shell am start -n com.listener/.MainActivity

# 스크린샷
adb exec-out screencap -p > screenshot.png
```

---

## 에뮬레이터 배포 규칙

**중요: 코드 변경 후 항상 에뮬레이터에 배포하여 검증할 것**

```bash
# 빌드 + 설치 + 실행 (한 줄)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew installDebug && adb shell am start -n com.listener/.MainActivity

# 에뮬레이터 확인
adb devices

# 에뮬레이터 부팅 (필요시)
emulator -avd Pixel_8_API_35 &
```

### 배포 체크리스트
1. `./gradlew assembleDebug` 빌드 성공 확인
2. `./gradlew installDebug` 에뮬레이터에 설치
3. `adb shell am start -n com.listener/.MainActivity` 앱 실행
4. 변경사항 수동 테스트
5. 필요시 스크린샷 캡처: `adb exec-out screencap -p > screenshot.png`

---

## 테스트 구조

### Unit Tests (`src/test/`)

| 테스트 | 파일 |
|--------|------|
| SentenceSplitter | `domain/usecase/chunking/SentenceSplitterTest.kt` |
| ChunkMerger | `domain/usecase/chunking/ChunkMergerTest.kt` |
| DuplicateRemover | `domain/usecase/chunking/DuplicateRemoverTest.kt` |
| LearningStateMachine | `service/LearningStateMachineTest.kt` |
| RechunkUseCase | `domain/usecase/RechunkUseCaseTest.kt` |
| HomeViewModel | `presentation/home/HomeViewModelTest.kt` |
| PlaylistViewModel | `presentation/playlist/PlaylistViewModelTest.kt` |

### Instrumented Tests (`src/androidTest/`)

| 테스트 | 파일 |
|--------|------|
| PlaylistDao | `data/local/db/PlaylistDaoTest.kt` |
| HomeScreen UI | `presentation/HomeScreenTest.kt` |

---

## 핵심 기능 흐름

### 1. 팟캐스트 → 학습 시작
```
PodcastScreen → 검색 → PodcastSearchScreen → 구독
       ↓
PodcastDetailScreen → 에피소드 선택 → ContentBottomSheet
       ↓
"학습 시작" 클릭 → TranscriptionScreen (다운로드 + 전사)
       ↓
전사 완료 → FullScreenPlayerScreen (chunk 단위 학습)
```

### 2. 미디어 파일 → 학습 시작
```
MediaFileScreen → "+" 버튼 → 파일 선택
       ↓
파일 클릭 → ContentBottomSheet → "학습 시작"
       ↓
TranscriptionScreen → FullScreenPlayerScreen
```

### 3. 플레이리스트 학습
```
PlaylistScreen → 플레이리스트 선택 → PlaylistDetailScreen
       ↓
"이어서 학습" 버튼 → 첫 번째 미완료 항목부터 재생
       ↓
FullScreenPlayerScreen (⏮️ ⏭️로 항목 간 이동)
```

### 4. 학습 모드
```
일반 모드: [원문 재생] → [공백(+녹음)] × 반복횟수
HARD 모드: [1차 원문] → [공백+녹음] → [2차 원문] → [내 녹음 재생] × 반복횟수
```

---

## 수동 테스트 체크리스트

### 홈 화면
- [ ] "이어서 학습하기" 항목 클릭 → 플레이어로 이동
- [ ] "새 에피소드" 항목 클릭 → 바텀시트 표시
- [ ] 바텀시트 "학습 시작" → 전사 화면으로 이동
- [ ] 바텀시트 "플레이리스트에 추가" → 다이얼로그 표시
- [ ] "새 플레이리스트 만들기" → 생성 다이얼로그 표시

### 팟캐스트
- [ ] 검색 → 결과 표시 (300ms debounce)
- [ ] 구독/구독 취소 토글
- [ ] 에피소드 목록 로드 (RSS 파싱)

### 플레이리스트
- [ ] 플레이리스트 생성 다이얼로그
- [ ] 플레이리스트 삭제 확인
- [ ] 상세 화면: 항목 목록, 진행률 표시
- [ ] 상세 화면: 드래그 핸들(☰)로 항목 순서 변경
- [ ] 상세 화면: 항목 삭제

### 전사 화면
- [ ] 다운로드 진행률 표시
- [ ] 전사 진행률 표시
- [ ] 완료 후 "학습 시작" 버튼

### 플레이어
- [ ] Chunk 재생/일시정지
- [ ] 이전/다음 chunk
- [ ] 이전/다음 항목 - 플레이리스트 모드에서만 활성화
- [ ] 가림 모드 토글
- [ ] 녹음 재생
- [ ] 반복 횟수 변경 (1-5)
- [ ] 공백 비율 변경 (0.2-1.0)
- [ ] HARD 모드 토글

### 설정
- [ ] 반복 횟수, 공백 비율 저장
- [ ] 전사 언어 선택
- [ ] OpenAI API 키 입력
- [ ] 캐시 삭제

---

## 주요 파일 위치

### 화면 (Screen)
| 화면 | 파일 |
|------|------|
| 홈 | `presentation/home/HomeScreen.kt` |
| 플레이리스트 목록 | `presentation/playlist/PlaylistScreen.kt` |
| 플레이리스트 상세 | `presentation/playlist/PlaylistDetailScreen.kt` |
| 팟캐스트 목록 | `presentation/podcast/PodcastScreen.kt` |
| 팟캐스트 검색 | `presentation/podcast/PodcastSearchScreen.kt` |
| 팟캐스트 상세 | `presentation/podcast/PodcastDetailScreen.kt` |
| 미디어 파일 | `presentation/media/MediaFileScreen.kt` |
| 전사 진행 | `presentation/transcription/TranscriptionScreen.kt` |
| 전체화면 플레이어 | `presentation/player/FullScreenPlayerScreen.kt` |
| 미니 플레이어 | `presentation/player/MiniPlayer.kt` |
| 설정 | `presentation/settings/SettingsScreen.kt` |

### 공통 컴포넌트
| 컴포넌트 | 파일 |
|----------|------|
| 에피소드 바텀시트 | `presentation/components/ContentBottomSheet.kt` |
| 플레이리스트 선택 | `presentation/components/PlaylistSelectDialog.kt` |
| 카드 | `presentation/components/ListenerCard.kt` |
| 버튼 | `presentation/components/ListenerButton.kt` |
| 빈 상태 | `presentation/components/EmptyState.kt` |

### 서비스
| 서비스 | 파일 |
|--------|------|
| 재생 서비스 | `service/PlaybackService.kt` |
| 녹음 관리 | `service/RecordingManager.kt` |
| 학습 상태 머신 | `service/LearningStateMachine.kt` |

### 데이터
| 영역 | 파일 |
|------|------|
| DB | `data/local/db/ListenerDatabase.kt` |
| DAO | `data/local/db/dao/*.kt` |
| Entity | `data/local/db/entity/*.kt` |
| API | `data/remote/api/*.kt` |
| Repository | `data/repository/*.kt` |

### 분절 알고리즘
| 단계 | 파일 |
|------|------|
| 전체 흐름 | `domain/usecase/chunking/ChunkingUseCase.kt` |
| 문장 분리 | `domain/usecase/chunking/SentenceSplitter.kt` |
| 타임스탬프 매칭 | `domain/usecase/chunking/TimestampMatcher.kt` |
| Chunk 병합 | `domain/usecase/chunking/ChunkMerger.kt` |

---

## 프로젝트 구조

```
app/src/main/java/com/listener/
├── ListenerApp.kt           # @HiltAndroidApp
├── MainActivity.kt          # @AndroidEntryPoint
├── core/di/                 # Hilt Modules
├── data/
│   ├── local/db/            # Room (Entity, DAO, Database)
│   ├── remote/              # API, DTO
│   └── repository/          # Repository 구현
├── domain/
│   ├── model/               # Domain Model
│   ├── repository/          # Repository 인터페이스
│   └── usecase/             # UseCase (분절 알고리즘 등)
├── presentation/
│   ├── components/          # 공통 컴포넌트
│   ├── navigation/          # NavHost, Screen routes
│   ├── theme/               # Theme, Color
│   └── [feature]/           # Screen, ViewModel
└── service/                 # PlaybackService, RecordingManager
```

---

## SPEC 참조

전체 기능 명세는 `SPEC.md` 참조:
- 화면별 UI 명세 및 와이어프레임
- 학습 모드 상태 다이어그램
- 데이터 모델 정의
- API 명세
- 분절 알고리즘 상세
