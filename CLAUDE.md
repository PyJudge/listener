# CLAUDE.md - Listener Android App

## ⛔ 절대 금지 규칙

```
1. 청크 텍스트와 음성 싱크 깨짐 금지
   - 청크의 displayText는 startMs~endMs 구간의 실제 음성과 일치해야 함
   - 특히 첫 번째 청크의 시작 부분이 중요
   - 수정 후 반드시 실기기에서 재생하며 싱크 확인

2. 에러 디버깅 시 사용자 데이터 삭제 금지
   - Room DB 충돌 → fallbackToDestructiveMigration 또는 버전 업
   - "pm clear" 사용 전 반드시 사용자 확인
   - 에러 재현용 데이터 삭제되면 디버깅 불가능
```

---

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

## 디버깅 (로그 확인)

```bash
# 연결된 디바이스 목록 확인
adb devices -l

# 특정 디바이스 로그 확인 (여러 디바이스 연결 시)
adb -s <device_id> logcat -d | grep -E "PlayerViewModel|PlaybackController|PlaybackService" | tail -100

# 앱 관련 로그만 필터링
adb logcat -d | grep "com.listener" | tail -100

# 앱 태그로 필터링
adb logcat -d | grep -E "PlayerViewModel|PlaybackController|PlaybackService|Chunking" | tail -100

# 에러 로그만 확인
adb logcat -d | grep -E "AndroidRuntime|FATAL|Exception" | grep -i "listener" | tail -50

# 실시간 로그 모니터링
adb logcat | grep -E "PlayerViewModel|PlaybackController|PlaybackService"

# 로그 버퍼 클리어 후 새로 확인
adb logcat -c && adb logcat | grep -E "PlayerViewModel|PlaybackController"
```

### 주요 로그 태그

| 태그 | 파일 |
|------|------|
| `PlayerViewModel` | `presentation/player/PlayerViewModel.kt` |
| `PlaybackController` | `service/PlaybackController.kt` |
| `PlaybackService` | `service/PlaybackService.kt` |
| `FFmpegPreprocess` | `domain/usecase/FFmpegPreprocessUseCase.kt` |
| `RechunkOnStartup` | `service/RechunkOnStartupManager.kt` |

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
| LearningStateMachineGap | `service/LearningStateMachineGapTest.kt` |
| RechunkUseCase | `domain/usecase/RechunkUseCaseTest.kt` |
| HomeViewModel | `presentation/home/HomeViewModelTest.kt` |
| PlaylistViewModel | `presentation/playlist/PlaylistViewModelTest.kt` |
| PlayerViewModelNavigation | `presentation/player/PlayerViewModelNavigationTest.kt` |
| PlayerViewModelPlaylistAutoAdvance | `presentation/player/PlayerViewModelPlaylistAutoAdvanceTest.kt` |
| PlayerViewModelTranscriptionCheck | `presentation/player/PlayerViewModelTranscriptionCheckTest.kt` |
| RecordingManagerStorage | `service/RecordingManagerStorageTest.kt` |
| RecordingManagerPauseResume | `service/RecordingManagerPauseResumeTest.kt` |
| PlaybackServiceProgressSave | `service/PlaybackServiceProgressSaveTest.kt` |
| PlaybackServiceAudioFocus | `service/PlaybackServiceAudioFocusTest.kt` |
| PlaybackServiceMediaSession | `service/PlaybackServiceMediaSessionTest.kt` |
| PlaybackStateError | `domain/model/PlaybackStateErrorTest.kt` |

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

## 핵심 불변조건 (INVARIANTS)

### Chunk 동기화 규칙 (절대 위반 금지)

```
Chunk의 displayText와 (startMs, endMs)는 반드시 동기화되어야 한다.

- displayText의 내용이 startMs~endMs 구간에서 실제로 재생되어야 함
- 이 규칙이 깨지면 사용자가 듣는 소리와 보는 텍스트가 불일치
- 청킹 알고리즘 수정 시 반드시 실기기에서 재생 테스트 필수
```

### 검증 방법

```bash
# 1. 테스트 실행
./gradlew test --tests "*.ChunkingUseCaseRealDataTest"

# 2. 실기기 DB에서 청크 검증
adb exec-out run-as com.listener cat databases/listener_database > /tmp/db.db
sqlite3 /tmp/db.db "SELECT MAX(endMs-startMs)/1000.0 as maxSec FROM chunks"
# 결과: 30초 이하여야 정상

# 3. 실기기에서 직접 재생하며 싱크 확인
```

### 청킹 수정 시 체크리스트

- [ ] `ChunkingUseCaseRealDataTest` 5개 테스트 PASS
- [ ] 실기기 캐시 삭제 후 새로 전사
- [ ] 최소 3개 청크 재생하며 싱크 확인
- [ ] 최대 청크 길이 30초 이하 확인

---

## 영역별 세부 지침 (위임)

**중요: 각 영역 수정 시 해당 폴더의 CLAUDE.md를 반드시 참조**

| 영역 | 세부 지침 | 핵심 위험 |
|------|----------|----------|
| **Chunking 알고리즘** | `domain/usecase/chunking/CLAUDE.md` | 드리프트, 동기화 |
| **재생 서비스** | `service/CLAUDE.md` | 레이스 컨디션, 상태 동기화 |
| **데이터 계층** | `data/CLAUDE.md` | 고아 데이터, 트랜잭션 |
| **UI/Presentation** | `presentation/CLAUDE.md` | 상태 관리, 네비게이션 |

---

## 알려진 주요 문제점 (2026-01 분석)

### CRITICAL (즉시 해결 필요)

| 문제 | 영역 | 영향 |
|------|------|------|
| recordingJob 경합 | PlaybackService | 청크 전환 시 잘못된 녹음 저장 |
| cleartext 트래픽 허용 | AndroidManifest | RSS 피드 MITM 공격 가능 |
| API 키 평문 저장 | SettingsRepository | 루팅 기기에서 키 탈취 |

### HIGH (우선 해결)

| 문제 | 영역 | 영향 |
|------|------|------|
| Orphaned PlaylistItem | 데이터 계층 | 삭제된 에피소드 참조 |
| PlayerViewModel 재주입 | MainScreen | 재생 상태 유실 |
| 반복 단어 드리프트 | Chunking | 잘못된 타임스탬프 매칭 |
| saveChunks 트랜잭션 부재 | TranscriptionRepository | 동시 접근 시 0 chunks |

---

## 보안 체크리스트

**AndroidManifest.xml 수정 필요:**
```xml
<!-- 변경 전 -->
android:usesCleartextTraffic="true"
android:allowBackup="true"

<!-- 변경 후 -->
android:usesCleartextTraffic="false"
android:allowBackup="false"
```

**API 키 보안:**
- BuildConfig에서 API 키 제거 필요
- EncryptedSharedPreferences 도입 검토

---

## ⚠️ 출시 전 필수 변경사항

```
□ Groq API 키 제거 (local.properties)
  - 현재: 개발용 테스트 키가 local.properties에 저장됨
  - 출시 전: local.properties에서 GROQ_API_KEY 라인 삭제
  - 사용자가 설정 화면에서 직접 API 키 입력하도록 유도

□ OpenAI API 키 확인
  - local.properties에서 제거하거나 빈 값으로 설정

□ usesCleartextTraffic="false" 설정
  - AndroidManifest.xml 수정

□ allowBackup="false" 설정
  - AndroidManifest.xml 수정
```

---

## 테스트 커버리지 현황

| 영역 | 커버리지 | 상태 |
|------|---------|------|
| Chunking 알고리즘 | 95% | ✅ 우수 |
| LearningStateMachine | 90% | ✅ 우수 |
| ViewModel | 70% | ⚠️ 보통 |
| PlaybackService | 40% | ❌ 부족 |
| API/Network | 0% | ❌ 없음 |

**누락된 테스트 (추가 필요):**
- PlaybackService 통합 테스트
- API 응답 파싱 테스트
- Recording 기능 테스트
- Database 동시성 테스트

---

## SPEC 참조

전체 기능 명세는 `SPEC.md` 참조:
- 화면별 UI 명세 및 와이어프레임
- 학습 모드 상태 다이어그램
- 데이터 모델 정의
- API 명세
- 분절 알고리즘 상세
