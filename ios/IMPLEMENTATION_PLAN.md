# Listener iOS - Implementation Plan

> Android 언어 학습 앱을 iOS로 마이그레이션하는 종합 구현 계획서

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [아키텍처 설계](#2-아키텍처-설계)
3. [디바이스 지원](#3-디바이스-지원)
4. [다국어 지원](#4-다국어-지원)
5. [데이터 모델](#5-데이터-모델)
6. [화면별 상세 명세](#6-화면별-상세-명세)
7. [학습 모드](#7-학습-모드)
8. [청킹 알고리즘](#8-청킹-알고리즘)
9. [서비스 계층](#9-서비스-계층)
10. [API 명세](#10-api-명세)
11. [구현 순서](#11-구현-순서)
12. [E2E 검증 체크리스트](#12-e2e-검증-체크리스트)
13. [Edge Case 테스트](#13-edge-case-테스트)
14. [개발 방법론](#14-개발-방법론)

---

## 1. 프로젝트 개요

### 1.1 목표 UX

1. 팟캐스트 에피소드 또는 기기 오디오 파일 선택
2. 재생 전 전사 완료 (OpenAI Whisper API / Apple Speech Recognition)
3. 전사 결과를 문장부호 기준으로 chunk 분절
4. 학습 재생: `[원문 재생] → [무음 공백] → 다음 chunk` 반복
5. 전사/분절/진행 상태 저장 (재전사 방지)
6. 공백 구간 사용자 발화 녹음 저장

### 1.2 기술 스택

| 항목 | iOS 기술 |
|------|----------|
| 언어 | Swift 6.0 |
| UI | SwiftUI |
| 미디어 | AVFoundation / AVPlayer |
| 백그라운드 재생 | AVAudioSession + Background Mode |
| 로컬 DB | SwiftData |
| 전사 API | OpenAI Whisper + Apple Speech Recognition |
| 팟캐스트 검색 | iTunes Search API |
| DI | Environment Injection |

### 1.3 확정된 설정값

| 항목 | 값 |
|------|-----|
| 오디오 캐시 최대 용량 | 1GB (초과 시 오래된 것부터 삭제) |
| 녹음 형식 | AAC 64kbps / 22kHz / Mono |
| 최소 chunk 길이 | 1.2초 (minChunkMs = 1200) |
| 기본 반복 횟수 | 2회 |
| 기본 공백 비율 | 0.4x |

---

## 2. 아키텍처 설계

### 2.1 프로젝트 구조

```
ListenerIOS/
├── App/
│   ├── ListenerApp.swift           # @main
│   └── ContentView.swift
├── Data/
│   ├── Models/                     # SwiftData @Model (14개)
│   ├── Repositories/               # Repository 구현체
│   └── Remote/                     # API (iTunes, Whisper)
├── Domain/
│   ├── Models/                     # Domain struct
│   ├── Repositories/               # Repository 프로토콜
│   └── UseCases/
│       └── Chunking/               # 청킹 알고리즘
├── Presentation/
│   ├── Navigation/
│   │   ├── AppNavigation.swift     # Size class 기반 분기
│   │   ├── CompactNavigation.swift # iPhone: TabView + NavigationStack
│   │   └── RegularNavigation.swift # iPad: NavigationSplitView
│   ├── Components/
│   │   ├── Adaptive/               # 적응형 컴포넌트
│   │   └── Shared/                 # 공통 컴포넌트
│   └── Screens/                    # 12개 화면
├── Services/
│   ├── Audio/
│   │   ├── PlaybackService.swift
│   │   ├── RecordingManager.swift
│   │   └── LearningStateMachine.swift
│   └── Transcription/
│       ├── WhisperService.swift
│       └── AppleSpeechService.swift
└── Core/
    ├── Extensions/
    ├── Utilities/
    └── DesignSystem/
```

### 2.2 계층별 매핑 (Android → iOS)

#### 데이터 계층
| Android (Room) | iOS (SwiftData) |
|---------------|-----------------|
| `@Entity` | `@Model` |
| `@Dao` | ModelContext + `#Predicate` |
| `Flow<T>` | `@Query` + `@Observable` |

#### 프레젠테이션 계층
| Android (Compose) | iOS (SwiftUI) |
|-------------------|---------------|
| `@HiltViewModel` | `@Observable` |
| `StateFlow<T>` | `@State` / `@Published` |
| `NavHost` | `NavigationStack` |
| `BottomNavigation` | `TabView` |

#### 서비스 계층
| Android | iOS |
|---------|-----|
| Media3 ExoPlayer | AVPlayer |
| MediaRecorder | AVAudioRecorder |
| ForegroundService | Background Audio Mode |
| WakeLock | AVAudioSession |

---

## 3. 디바이스 지원

### 3.1 iPhone 라인업 (2026년 1월 기준)

| 디바이스 | 화면 크기 | 해상도 (pt) | 레이아웃 전략 |
|---------|----------|------------|--------------|
| iPhone SE (3rd) | 4.7" | 375 x 667 | Compact - 세로 스택, 미니 플레이어 |
| iPhone 14 | 6.1" | 390 x 844 | Compact - 기준 레이아웃 |
| iPhone 14 Plus | 6.7" | 428 x 926 | Compact - 확장 미니 플레이어 |
| iPhone 14 Pro | 6.1" | 393 x 852 | Compact - Dynamic Island 대응 |
| iPhone 14 Pro Max | 6.7" | 430 x 932 | Compact - 확장 + Dynamic Island |
| iPhone 15 | 6.1" | 393 x 852 | Compact - Dynamic Island 대응 |
| iPhone 15 Plus | 6.7" | 430 x 932 | Compact - 확장 + Dynamic Island |
| iPhone 15 Pro | 6.1" | 393 x 852 | Compact - Dynamic Island 대응 |
| iPhone 15 Pro Max | 6.7" | 430 x 932 | Compact - 확장 + Dynamic Island |
| iPhone 16 | 6.1" | 393 x 852 | Compact - Dynamic Island + 카메라 컨트롤 |
| iPhone 16 Plus | 6.7" | 430 x 932 | Compact - 확장 레이아웃 |
| iPhone 16 Pro | 6.3" | 402 x 874 | Compact - 확장 기준 |
| iPhone 16 Pro Max | 6.9" | 440 x 956 | Compact - 최대 확장 |

### 3.2 iPad 라인업

| 디바이스 | 화면 크기 | 해상도 (pt) | 레이아웃 전략 |
|---------|----------|------------|--------------|
| iPad Mini (6th) | 8.3" | 744 x 1133 | Regular - 사이드바, Split View |
| iPad (10th) | 10.9" | 820 x 1180 | Regular - 기준 레이아웃 |
| iPad Air (M2) | 11" | 820 x 1180 | Regular - 기준 + Stage Manager |
| iPad Air 13" (M2) | 13" | 1024 x 1366 | Regular - 3-column |
| iPad Pro 11" (M4) | 11" | 834 x 1194 | Regular - Stage Manager |
| iPad Pro 13" (M4) | 13" | 1024 x 1366 | Regular - 3-column + Stage Manager |

### 3.3 Size Class 기반 적응형 레이아웃

```swift
@Environment(\.horizontalSizeClass) var horizontalSizeClass
@Environment(\.verticalSizeClass) var verticalSizeClass

// Compact: iPhone (세로), iPad Split View
// Regular: iPad (전체), iPhone (가로 - Pro Max)
```

### 3.4 iPad 전용 기능

1. **사이드바 네비게이션**: NavigationSplitView 사용
2. **멀티태스킹**: Split View, Slide Over 지원
3. **Stage Manager**: 다중 윈도우 지원
4. **키보드 단축키**: 재생/일시정지(스페이스), 다음/이전 청크
5. **Pointer 지원**: 마우스/트랙패드 hover 효과

### 3.5 Dynamic Island / Live Activity 미니플레이어

> iPhone 14 Pro 이상에서 Dynamic Island, 그 외 기기에서 Live Activity 배너로 재생 상태 표시

#### Dynamic Island 표시 항목

| 상태 | Compact (축소) | Expanded (확장) |
|------|---------------|-----------------|
| **재생 중** | 파형 애니메이션 + 제목 (잘림) | 제목 + 진행률 바 + 재생/일시정지/다음 버튼 |
| **일시정지** | 일시정지 아이콘 + 제목 | 제목 + 진행률 바 + 재생 버튼 |
| **Gap 상태** | 타이머 카운트다운 | 남은 시간 + 모드 표시 (LR/LRLR) |
| **녹음 중** | 녹음 아이콘 (빨간 점 깜빡임) | 녹음 시간 + 파형 |

#### Live Activity 구현

```swift
// ActivityKit 사용
import ActivityKit

struct ListenerPlaybackAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        var title: String
        var subtitle: String
        var progress: Double
        var isPlaying: Bool
        var learningState: String  // "playing", "gap", "recording"
        var remainingGapSeconds: Int?
    }

    var sourceId: String
    var artworkUrl: String?
}

// Live Activity 시작
func startLiveActivity(for content: ContentMetadata) async {
    let attributes = ListenerPlaybackAttributes(
        sourceId: content.sourceId,
        artworkUrl: content.artworkUrl
    )

    let state = ListenerPlaybackAttributes.ContentState(
        title: content.title,
        subtitle: content.subtitle,
        progress: 0,
        isPlaying: true,
        learningState: "playing"
    )

    do {
        let activity = try Activity.request(
            attributes: attributes,
            content: .init(state: state, staleDate: nil),
            pushType: nil
        )
    } catch {
        print("Live Activity 시작 실패: \(error)")
    }
}
```

#### Dynamic Island UI 레이아웃

**Compact Leading (좌측 최소화)**
```
┌───────┐
│  🎵   │  파형 또는 일시정지 아이콘
└───────┘
```

**Compact Trailing (우측 최소화)**
```
┌──────────────┐
│ How to So... │  제목 (12자 제한)
└──────────────┘
```

**Expanded (확장)**
```
┌─────────────────────────────────────┐
│  🎙️  EP.289 How to Sound Natural   │
│      All Ears English               │
│  ▓▓▓▓▓▓▓▓░░░░░░░░ 45%              │
│      ⏪     ⏯️     ⏩               │
└─────────────────────────────────────┘
```

**Gap/Recording 확장 상태**
```
┌─────────────────────────────────────┐
│  🔴 녹음 중... 0:03                  │
│  ▓▓▓▓░░░░░░░░░░░░ Gap 2.1초        │
│         [ 스킵 ]                    │
└─────────────────────────────────────┘
```

#### 잠금화면 Live Activity

```
┌─────────────────────────────────────┐
│  🎙️  Listener                       │
│                                     │
│  EP.289 How to Sound Natural        │
│  All Ears English                   │
│                                     │
│  ▓▓▓▓▓▓▓▓░░░░░░░░ 45%              │
│                                     │
│     ⏪        ⏯️        ⏩          │
└─────────────────────────────────────┘
```

#### Live Activity 상태 업데이트

```swift
// PlaybackService에서 상태 변경 시 호출
func updateLiveActivity(state: PlaybackState) async {
    guard let activity = Activity<ListenerPlaybackAttributes>.activities.first else { return }

    let contentState = ListenerPlaybackAttributes.ContentState(
        title: state.title,
        subtitle: state.subtitle,
        progress: Double(state.currentPositionMs) / Double(state.chunkDurationMs),
        isPlaying: state.isPlaying,
        learningState: state.learningState.rawValue,
        remainingGapSeconds: state.learningState == .gap ? state.remainingGapSeconds : nil
    )

    await activity.update(
        ActivityContent(state: contentState, staleDate: nil)
    )
}

// 재생 종료 시 Live Activity 종료
func endLiveActivity() async {
    for activity in Activity<ListenerPlaybackAttributes>.activities {
        await activity.end(nil, dismissalPolicy: .immediate)
    }
}
```

#### 테스트 체크리스트

- [ ] iPhone 14 Pro+: Dynamic Island Compact 표시
- [ ] iPhone 14 Pro+: Dynamic Island Expanded 탭 시 확장
- [ ] iPhone 14 Pro+: Dynamic Island에서 재생/일시정지 동작
- [ ] iPhone 14 Pro+: Dynamic Island에서 다음/이전 청크 동작
- [ ] iPhone 14 이하: Live Activity 배너 표시
- [ ] 잠금화면: Live Activity 컨트롤 동작
- [ ] Gap 상태: 타이머 카운트다운 표시
- [ ] 녹음 상태: 빨간 점 깜빡임 + 시간 표시
- [ ] 앱 종료 시: Live Activity 자동 종료

---

## 4. 다국어 지원

### 4.1 지원 언어

| 언어 | 코드 | 우선순위 |
|------|------|---------|
| 한국어 | ko | 기본 (개발 언어) |
| 영어 | en | 필수 |
| 일본어 | ja | 높음 |
| 중국어 (간체) | zh-Hans | 높음 |
| 중국어 (번체) | zh-Hant | 중간 |
| 스페인어 | es | 중간 |

### 4.2 String Catalog 패턴 (iOS 17+)

```swift
// 기본 사용
Text("home_continue_learning")  // Localizable.xcstrings에서 자동 참조

// 변수 포함
Text("chunk_count \(count)")    // "청크 %lld개" / "%lld chunks"

// 복수형 (Plural)
Text("episode_count \(count)")  // 1개: "에피소드 1개" / 2개+: "에피소드 2개"
```

### 4.3 전사 언어와 UI 언어 분리

```swift
// UI 언어: 시스템 설정 따름
// 전사 언어: 사용자 선택 (설정에서)
enum TranscriptionLanguage: String, CaseIterable {
    case english = "en"
    case korean = "ko"
    case japanese = "ja"
    case chinese = "zh"
    case spanish = "es"
    case french = "fr"
    case german = "de"
    case auto = "auto"  // Whisper 자동 감지
}
```

---

## 5. 데이터 모델

### 5.1 SwiftData Models (14개)

#### SubscribedPodcast
```swift
@Model
final class SubscribedPodcast {
    @Attribute(.unique) var feedUrl: String
    var collectionId: Int64?
    var title: String
    var artworkUrl: String?
    var podcastDescription: String?
    var lastCheckedAt: Date
    var addedAt: Date

    @Relationship(deleteRule: .cascade, inverse: \PodcastEpisode.podcast)
    var episodes: [PodcastEpisode] = []
}
```

#### PodcastEpisode
```swift
@Model
final class PodcastEpisode {
    @Attribute(.unique) var id: String
    var feedUrl: String
    var title: String
    var audioUrl: String
    var episodeDescription: String?
    var durationMs: Int64?
    var pubDate: Date
    var isNew: Bool

    var podcast: SubscribedPodcast?
}
```

#### Chunk
```swift
@Model
final class Chunk {
    var id: UUID = UUID()
    var sourceId: String
    var orderIndex: Int
    var startMs: Int64
    var endMs: Int64
    var displayText: String

    var durationMs: Int64 { endMs - startMs }
}
```

### 5.2 Domain Models

```swift
// Learning State
enum LearningState: Sendable {
    case idle
    case playing
    case paused
    case gap
    case recording
    // HARD mode states
    case playingFirst
    case gapWithRecording
    case playingSecond
    case playbackRecording
}

// Play Mode
enum PlayMode: String, Codable, Sendable {
    case normal
    case lr      // Listen & Repeat
    case lrlr    // Listen & Repeat + Recording (HARD)
}

// Playback State
struct PlaybackState: Sendable {
    var sourceId: String = ""
    var isPlaying: Bool = false
    var currentChunkIndex: Int = 0
    var totalChunks: Int = 0
    var currentPositionMs: Int64 = 0
    var chunkDurationMs: Int64 = 0
    var learningState: LearningState = .idle
    var playMode: PlayMode = .normal
    var repeatCount: Int = 2
    var gapRatio: Float = 0.4
    var error: String? = nil
}
```

---

## 6. 화면별 상세 명세

### 6.1 네비게이션 구조

```
┌─────────────────────────────────────────────────────────┐
│                    메인 콘텐츠 영역                      │
├─────────────────────────────────────────────────────────┤
│  🎙️ EP.289 How to Sound...   ⏪  ⏯️  ⏩  ▓▓▓░░░       │  ← 미니 플레이어
│     영어 회화 연습 | All Ears English                   │
├─────────────────────────────────────────────────────────┤
│   🏠      📋      🎙️      📁      ⚙️                   │
│   홈    플레이    팟캐     미디어   설정                  │
│         리스트    스트     파일                          │
└─────────────────────────────────────────────────────────┘
```

### 6.2 화면 목록

| 화면 | iPhone 구현 | iPad 구현 |
|------|------------|----------|
| 홈 | TabView + NavigationStack | NavigationSplitView 사이드바 |
| 플레이리스트 | List + Sheet | Master-Detail |
| 팟캐스트 | 2열 Grid | 3-4열 Grid |
| 미디어 파일 | List | 파일 브라우저 Grid |
| 설정 | Form | Form + Sidebar |
| 전사 진행 | FullScreen | Split View 내 표시 |
| 전체 화면 플레이어 | Sheet (fullScreenCover) | Trailing Column |
| 미니 플레이어 | 하단 고정 | 하단 고정 |

### 6.3 홈 탭

```
┌─────────────────────────────────────┐
│  Listener                           │
├─────────────────────────────────────┤
│                                     │
│  ▶️ 이어서 학습하기                   │
│  ───────────────────────────────── │
│  ┌───────────────────────────────┐  │
│  │ 🎙️ EP.289 How to Sound Natural│  │
│  │   All Ears English · 80%      │  │
│  └───────────────────────────────┘  │
│                                     │
│  📬 새 에피소드                      │
│  ───────────────────────────────── │
│  ┌───────────────────────────────┐  │
│  │ 🔴 EP.290 Advanced Tips        │  │
│  │    All Ears English · 18분    │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

**기능 명세:**
- 이어서 학습하기: `lastAccessedAt DESC`, 최대 5개
- 새 에피소드: `pubDate DESC`, 최대 10개
- 빈 상태: 팟캐스트 탭으로 유도 버튼

### 6.4 전체 화면 플레이어

```
┌─────────────────────────────────────┐
│  ▼  EP.289 How to Sound Natural     │
│     영어 회화 연습 | All Ears English│
├─────────────────────────────────────┤
│     12 / 87              🙈  ⋮      │
├─────────────────────────────────────┤
│  │ So the first thing you need     │
│  │ to understand is that native    │
│  ┃ and that's actually a good      │  ← 현재 chunk
│  ┃ thing to know.               🔊  │
│  │ Because when you realize this,  │
├─────────────────────────────────────┤
│         상태:  PLAY                  │
│    ▓▓▓▓▓▓▓▓▓▓▓▓░░░░ 2.1s           │
├─────────────────────────────────────┤
│  ⏮️      ⏪      ⏯️      ⏩      ⏭️   │
│  이전    이전    재생    다음    다음  │
│  항목   chunk          chunk   항목  │
├─────────────────────────────────────┤
│   🔁      📊      🎤      ⚡        │
│  반복x2  공백0.4  녹음ON  일반       │
└─────────────────────────────────────┘
```

---

## 7. 학습 모드

### 7.1 일반 모드 (NORMAL)

```
┌─────────────────────────────────────────────────────────┐
│   ┌──────┐   play()   ┌──────┐                         │
│   │ IDLE │ ─────────▶ │ PLAY │  원문 재생              │
│   └──────┘            └──────┘                         │
│       ▲                   │                            │
│       │                   │ 재생 완료                   │
│       │                   ▼                            │
│       │              ┌──────┐                          │
│       │              │ GAP  │  공백 (녹음 ON이면 녹음)  │
│       │              └──────┘                          │
│       │                   │                            │
│       │         ┌─────────────────┐                    │
│       │         │ 반복 남았나?     │                    │
│       │         └─────────────────┘                    │
│       │            │YES      │NO                       │
│       │            ▼         ▼                         │
│       │         PLAY로    다음 chunk                   │
│       └─────────────────────────────────────────────── │
└─────────────────────────────────────────────────────────┘
```

**패턴:** `[원문] → [공백(+녹음)] × 반복횟수`

### 7.2 HARD 모드 (LRLR)

```
┌─────────────────────────────────────────────────────────┐
│   ┌──────┐   play()   ┌────────┐                       │
│   │ IDLE │ ─────────▶ │ PLAY_1 │  1차 원문 재생        │
│   └──────┘            └────────┘                       │
│       ▲                   │                            │
│       │                   ▼                            │
│       │              ┌────────┐                        │
│       │              │  GAP   │  공백 + 녹음           │
│       │              │ + REC  │  (여기서 녹음!)        │
│       │              └────────┘                        │
│       │                   │                            │
│       │                   ▼                            │
│       │              ┌────────┐                        │
│       │              │ PLAY_2 │  2차 원문 재생         │
│       │              └────────┘                        │
│       │                   │                            │
│       │                   ▼                            │
│       │              ┌────────┐                        │
│       │              │PLAYBACK│  방금 녹음 재생        │
│       │              │  REC   │  (자기 발음 비교)      │
│       │              └────────┘                        │
│       │                   │                            │
│       │         ┌─────────────────┐                    │
│       │         │ 반복 남았나?     │                    │
│       │         └─────────────────┘                    │
│       │            │YES      │NO                       │
│       │            ▼         ▼                         │
│       │        PLAY_1로   다음 chunk                   │
│       └─────────────────────────────────────────────── │
└─────────────────────────────────────────────────────────┘
```

**패턴:** `[1차 원문] → [공백+녹음] → [2차 원문] → [내 녹음 재생] × 반복횟수`

---

## 8. 청킹 알고리즘

### 8.1 핵심 불변조건 (INVARIANT)

```
⛔ 가장 중요한 규칙:

Chunk의 displayText와 (startMs, endMs)는 반드시 동기화되어야 한다.

- displayText의 각 단어는 [startMs, endMs] 범위 내에서 실제로 재생되어야 함
- 청크의 첫 단어가 재생될 때 실제로 그 단어가 들려야 함
- startMs가 틀리면 사용자가 "재생" 눌렀을 때 엉뚱한 부분부터 들림
- 모든 수정 후 반드시 실기기에서 재생 테스트 필수
```

### 8.2 알고리즘 파이프라인

```
WhisperResult (segments + words)
        ↓
[1단계] SentenceSplitter: 문장 경계 탐지
        ↓
[2단계] TimestampMatcher: 문장 시작 위치 탐지
        ↓
[3단계] TwoPointerAligner: Phrase-Word 정렬
        ↓
[4단계] TimestampAssigner: 타임스탬프 할당
        ↓
[5단계] ChunkMerger: 최소 길이 병합 (1.2초)
        ↓
Result: List<Chunk>
```

### 8.3 Swift 구현 예시

```swift
actor ChunkingUseCase {
    private let sentenceSplitter: SentenceSplitter
    private let chunkMerger: ChunkMerger
    private let aligner: TwoPointerAligner

    func process(
        whisperResult: WhisperResult,
        sentenceOnly: Bool = true,
        minChunkMs: Int64 = 1200
    ) -> [Chunk] {
        // 1. 문장 분리
        let sentences = sentenceSplitter.split(
            whisperResult.text,
            sentenceOnly: sentenceOnly
        )

        // 2. 타임스탬프 매칭
        var chunks: [Chunk] = []
        var wordIndex = 0

        for sentence in sentences {
            // TwoPointerAligner로 정렬
            // TimestampAssigner로 타임스탬프 할당
        }

        // 3. 최소 길이 병합
        return chunkMerger.merge(chunks, minChunkMs: minChunkMs)
    }
}
```

### 8.4 알려진 문제점 및 해결책

| 문제 | 우선순위 | 해결책 |
|------|---------|--------|
| 반복 단어 드리프트 | HIGH | searchStartIndex로 검색 시작점 제한 + 시간 기반 바운드 추가 |
| 비순차 타임스탬프 | HIGH | DuplicateRemover에서 정렬 제거, 원본 순서 유지 |
| 1글자 단어 매칭 | MEDIUM | Fuzzy 매칭에서 1글자 단어 특별 처리 |
| 콜론/세미콜론 분할 | MEDIUM | 구분자 목록에 추가 검토 |

### 8.5 테스트 요구사항

```bash
# 포팅해야 할 테스트 수
- SentenceSplitter: 47개
- TimestampMatcher: 12개
- ChunkMerger: 15개
- DuplicateRemover: 4개
- ChunkingUseCase: 8개
- 실제 데이터: 8개

# 총 94개 테스트
```

---

## 9. 서비스 계층

### 9.1 핵심 불변조건

```
1. PlaybackService ↔ ViewModel 상태 동기화
   - playbackState는 항상 단방향으로 전파 (Service → ViewModel)
   - 상태 업데이트 후 UI 반영 전까지 일관성 보장

2. LearningStateMachine 상태 전이 무결성
   - 현재 PlayMode에 유효한 상태만 존재
   - 모드 변경 시 자동 정규화 (normalizeStateForMode)

3. 청크 전환 중 녹음/재생 작업 안전 종료
   - cancelPendingOperations() 후 다음 작업 시작
```

### 9.2 PlaybackService (iOS)

```swift
@Observable
@MainActor
final class PlaybackService {
    private let player = AVPlayer()
    private let stateMachine: LearningStateMachine
    private var chunks: [Chunk] = []
    private var currentChunkIndex = 0

    private(set) var playbackState = PlaybackState()

    func setContent(sourceId: String, audioURL: URL, chunks: [Chunk], settings: LearningSettings) async
    func play() async
    func pause()
    func resume()
    func nextChunk() async
    func previousChunk() async
    func seekToChunk(index: Int) async
}
```

### 9.3 RecordingManager (iOS)

```swift
@Observable
@MainActor
final class RecordingManager {
    private var recorder: AVAudioRecorder?
    private(set) var isRecording = false
    private(set) var isPaused = false

    func hasRecordPermission() -> Bool
    func requestPermission() async -> Bool
    func startRecording(sourceId: String, chunkIndex: Int) async -> Bool
    func stopRecording() async -> URL?
    func pauseRecording() -> Bool
    func resumeRecording() -> Bool
    func deleteRecording(sourceId: String, chunkIndex: Int) async
}
```

### 9.4 알려진 문제점 및 해결책

| 문제 | 우선순위 | iOS 해결책 |
|------|---------|------------|
| recordingJob 경합 | CRITICAL | Task cancel 후 await 완료 대기 |
| 상태 동기화 지연 | HIGH | AsyncStream으로 상태 전파 |
| 청크 변경 중 녹음 | CRITICAL | 청크 인덱스 검증 후 작업 수행 |

---

## 10. API 명세

### 10.1 iTunes Search API

```
GET https://itunes.apple.com/search
  ?term={query}
  &media=podcast
  &limit=20
  &offset={offset}
```

### 10.2 OpenAI Whisper API

```
POST https://api.openai.com/v1/audio/transcriptions
Content-Type: multipart/form-data

file: <audio_file>
model: whisper-1
response_format: verbose_json
timestamp_granularities[]: segment
timestamp_granularities[]: word
language: {설정된 언어}
```

### 10.3 Apple Speech Recognition (NEW)

```swift
import Speech

actor AppleSpeechService: TranscriptionService {
    let providerName = "Apple"
    let modelName = "SFSpeechRecognizer"

    func transcribe(audioFile: URL, language: String?) async throws -> WhisperResult {
        let recognizer = SFSpeechRecognizer(locale: Locale(identifier: language ?? "en-US"))
        let request = SFSpeechURLRecognitionRequest(url: audioFile)
        request.shouldReportPartialResults = false

        // 전사 수행 및 WhisperResult 형식으로 변환
    }
}
```

---

## 11. 구현 순서

### Phase 1: 프로젝트 설정 + 핵심 모델
- [1.1] Xcode 프로젝트 생성 (iOS 17.0+, iPad 지원)
- [1.2] Localization 설정 (String Catalog)
- [1.3] SwiftData 스키마 정의 (14개 Model)
- [1.4] Domain 모델 정의
- [1.5] Repository 프로토콜 정의
- [1.6] DesignSystem 기초

### Phase 2: 청킹 알고리즘 (TDD)
- [2.1] SentenceSplitter 테스트 → 구현
- [2.2] TwoPointerAligner 테스트 → 구현
- [2.3] TimestampAssigner 테스트 → 구현
- [2.4] ChunkMerger 테스트 → 구현
- [2.5] ChunkingUseCase 통합 테스트 → 구현

### Phase 3: 데이터 계층
- [3.1] Repository 구현
- [3.2] iTunes Search API 클라이언트
- [3.3] RSS Parser
- [3.4] Whisper API 클라이언트

### Phase 4: 전사 서비스 (TDD)
- [4.1] TranscriptionService 프로토콜 정의
- [4.2] OpenAIWhisperService 테스트 → 구현
- [4.3] AppleSpeechService 테스트 → 구현
- [4.4] AudioPreprocessor 테스트 → 구현

### Phase 5: 학습 상태 머신 (TDD)
- [5.1] LearningStateMachine 테스트 → 구현
- [5.2] NORMAL 모드 전이 테스트
- [5.3] LR 모드 전이 테스트
- [5.4] LRLR 모드 전이 테스트

### Phase 6: 재생 서비스 (TDD)
- [6.1] PlaybackService 테스트 → 구현
- [6.2] AVPlayer 청크 기반 재생
- [6.3] 백그라운드 오디오 설정
- [6.4] Now Playing Info 연동
- [6.5] Live Activity / Dynamic Island 연동
  - ActivityKit 설정
  - ListenerPlaybackAttributes 정의
  - 재생/Gap/녹음 상태별 UI
  - 잠금화면 컨트롤

### Phase 7: 녹음 서비스 (TDD)
- [7.1] RecordingManager 테스트 → 구현
- [7.2] AVAudioRecorder 설정

### Phase 8: 적응형 UI 구현
- [8.1] DesignSystem 구축
- [8.2] 적응형 네비게이션
- [8.3] 모든 화면 구현 (iPhone + iPad)

### Phase 9: 디바이스 테스트 + 검증
- [9.1] iPhone 기종별 테스트
- [9.2] iPad 기종별 테스트
- [9.3] 다국어 테스트
- [9.4] 멀티태스킹 테스트
- [9.5] 청크 싱크 검증

---

## 12. E2E 검증 체크리스트

### 12.1 첫 사용 시나리오

- [ ] 앱 실행 시 홈 화면 표시
- [ ] 5개 탭 (홈/플레이리스트/팟캐스트/미디어/설정) 표시
- [ ] 빈 상태 시 팟캐스트 탭으로 유도 버튼 표시
- [ ] 설정 탭 → API 섹션 → OpenAI API 키 입력
- [ ] 키 저장 후 "설정됨" 표시 확인

### 12.2 팟캐스트 학습 플로우

- [ ] 팟캐스트 탭 → 검색 버튼
- [ ] "english learning" 검색 (300ms debounce)
- [ ] iTunes 검색 결과 표시
- [ ] 팟캐스트 선택 → 구독 버튼
- [ ] 구독 후 목록에 표시
- [ ] 에피소드 탭 → 바텀시트 표시
- [ ] "학습 시작" 버튼 → 전사 진행 화면
- [ ] 다운로드 진행률 표시
- [ ] 전사 진행률 표시
- [ ] 완료 시 "학습 시작" 버튼

### 12.3 학습 재생 (일반 모드)

- [ ] 전체화면 플레이어 표시
- [ ] 전사문 스크롤 뷰 표시
- [ ] 현재 chunk 하이라이트
- [ ] 5버튼 컨트롤 동작
- [ ] 단독 재생 시 이전/다음 항목 비활성화
- [ ] 재생 → 공백 → 반복 사이클 동작

### 12.4 학습 재생 (HARD 모드)

- [ ] 모드 버튼 탭 → HARD 모드 전환
- [ ] 녹음 버튼 비활성화 (회색)
- [ ] 원문 → 공백+녹음 → 원문 → 녹음재생 사이클
- [ ] 녹음 아이콘 표시 (녹음 있는 chunk)

### 12.5 제스처 동작

- [ ] 전사문 좌 스와이프 → 다음 chunk
- [ ] 전사문 우 스와이프 → 이전 chunk
- [ ] chunk 탭 → 해당 chunk로 이동
- [ ] 녹음 아이콘 탭 → 녹음 재생
- [ ] 헤더 아래 스와이프 → 미니 플레이어

### 12.6 가림 모드

- [ ] 가림 모드 버튼 탭 → 텍스트 숨김
- [ ] 타임스탬프만 표시
- [ ] 개별 눈 아이콘 탭 → 해당 chunk 텍스트 표시
- [ ] 다음 chunk 이동 시 이전 열린 것 닫힘

### 12.7 플레이리스트 플로우

- [ ] 플레이리스트 생성
- [ ] 항목 추가
- [ ] 드래그로 순서 변경
- [ ] 이어서 학습 → 첫 미완료 항목 재생
- [ ] 항목 완료 시 다음 항목 자동 이동

### 12.8 설정 검증

- [ ] 기본 반복 횟수: 1~5 (기본값 2)
- [ ] 기본 공백 비율: 0.2/0.4/0.6/0.8/1.0 (기본값 0.4)
- [ ] 자동 녹음: ON/OFF (기본값 ON)
- [ ] 전사 언어 선택
- [ ] Apple Speech / Whisper 전환
- [ ] 캐시 전체 삭제

### 12.9 디바이스별 검증

#### iPhone
- [ ] iPhone SE (4.7"): 최소 화면 검증
- [ ] iPhone 14/15 (6.1"): 기준 화면
- [ ] iPhone 16 Pro (6.3"): 새 기준
- [ ] iPhone 16 Pro Max (6.9"): 최대 Compact
- [ ] Dynamic Island 대응 확인

#### iPad
- [ ] iPad Mini (8.3"): 최소 Regular
- [ ] iPad Air 11" (M2): 기준 Regular
- [ ] iPad Pro 13" (M4): 최대 + Stage Manager
- [ ] Split View (1/2, 1/3, 2/3)
- [ ] Slide Over
- [ ] Stage Manager 다중 윈도우

### 12.10 다국어 검증

- [ ] 한국어 (기본): 전체 문자열 검증
- [ ] 영어: 번역 완성도 + 레이아웃
- [ ] 일본어: 긴 텍스트 처리
- [ ] 중국어: 문자 렌더링

### 12.11 Dynamic Island / Live Activity 검증

#### iPhone 14 Pro+ (Dynamic Island)
- [ ] 재생 시작 → Dynamic Island Compact 표시
- [ ] Compact 탭 → Expanded 확장
- [ ] Expanded에서 재생/일시정지 버튼 동작
- [ ] Expanded에서 다음/이전 청크 버튼 동작
- [ ] 진행률 바 실시간 업데이트
- [ ] Gap 상태 → 타이머 카운트다운 표시
- [ ] 녹음 상태 → 빨간 점 깜빡임 표시
- [ ] 앱으로 돌아가기 탭 → 앱 전환
- [ ] 재생 종료 → Dynamic Island 사라짐

#### iPhone 14 이하 / iPad (Live Activity 배너)
- [ ] 재생 시작 → 상단 배너 표시
- [ ] 배너 탭 → 앱 전환
- [ ] 잠금화면에서 Live Activity 표시
- [ ] 잠금화면 컨트롤 동작

#### 잠금화면 컨트롤
- [ ] 재생/일시정지 버튼 동작
- [ ] 다음/이전 청크 버튼 동작
- [ ] 진행률 표시 정확성
- [ ] 제목/부제목 표시

#### 상태별 표시
- [ ] NORMAL 모드: 재생 → 반복 표시
- [ ] LR 모드: 재생 → Gap 타이머 표시
- [ ] LRLR 모드: 재생 → Gap+녹음 → 재생 → 녹음재생
- [ ] 일시정지: 일시정지 아이콘 표시
- [ ] 백그라운드: 모든 상태 정상 업데이트

---

## 13. Edge Case 테스트

### 13.1 우선순위 정의

| Level | 정의 | 예시 |
|-------|------|------|
| **CRITICAL** | 데이터 손실/손상, 앱 크래시 | 녹음 파일 손상, 재생 위치 영구 손실 |
| **HIGH** | 잘못된 동작, 심각한 UX 저하 | 오디오-텍스트 불일치, 무한 대기 |
| **MEDIUM** | 불편함, 비정상적 UI | 순간 깜빡임, 잘못된 메타데이터 표시 |
| **LOW** | 미미한 영향 | 버퍼링 시 UI 딜레이 |

### 13.2 LRLR 모드 Edge Cases

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| LRLR-1 | PlayingFirst 중 다른 청크 터치 | 재생 중단, 새 청크 PlayingFirst로 전환 | HIGH |
| LRLR-2 | GapWithRecording 중 다른 청크 터치 | **녹음 중단 및 삭제**, 새 청크 PlayingFirst로 전환 | **CRITICAL** |
| LRLR-3 | 녹음 권한 없이 LRLR 모드 진입 시도 | 권한 요청 팝업 표시, **거부 시 LR 모드로 자동 전환** | **CRITICAL** |
| LRLR-4 | 녹음 중 마이크 사용 불가 | 녹음 실패 감지, LR 모드로 자동 전환, 에러 토스트 | HIGH |

### 13.3 모드 전환 Edge Cases

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| MT-1 | Playing 중 NORMAL → LR | 현재 청크 Playing 상태 유지, 완료 시 Gap 진입 | HIGH |
| MT-2 | GapWithRecording 중 LRLR → LR | **녹음 중단 및 삭제**, Gap 상태로 전환 | **CRITICAL** |
| MT-3 | 빠른 모드 토글 연타 (5회 이상) | 최종 모드만 적용, 중간 상태 정리 완료 | HIGH |

### 13.4 일시정지/재개 Edge Cases

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| PR-1 | Playing 중 일시정지 → 재개 | 정확한 위치에서 재개 (±30ms) | HIGH |
| PR-2 | Gap 중 일시정지 → 재개 | Gap 타이머 재개, 남은 시간 계속 | HIGH |
| PR-3 | GapWithRecording 중 일시정지 | 녹음 일시정지, Gap 타이머 정지 | HIGH |
| PR-4 | 일시정지 후 청크 변경 → 재개 | **새 청크 처음부터 재생** | **CRITICAL** |

### 13.5 앱 중단 Edge Cases

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| AS-1 | 홈 버튼 (앱 배경) | 재생 계속 (Background Audio) | **CRITICAL** |
| AS-2 | 화면 끄기 중 재생 | 재생 계속 | **CRITICAL** |
| AS-3 | 화면 끄기 중 녹음 (LRLR) | 녹음 계속, AVAudioSession 유지 | **CRITICAL** |
| AS-4 | 앱 스와이프 종료 | 진행 상황 저장 | HIGH |
| AS-5 | 배경 10분 이상 유지 | 서비스 유지 | **CRITICAL** |

### 13.6 오디오 포커스 Edge Cases

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| AF-1 | 전화 수신 중 재생 | 자동 일시정지, 통화 종료 후 재개 | HIGH |
| AF-2 | 다른 미디어 앱 재생 시작 | 자동 일시정지 | HIGH |
| AF-3 | 블루투스 연결 끊김 | 자동 일시정지 | HIGH |
| AF-4 | 이어폰 뽑힘 | 자동 일시정지 | HIGH |

### 13.7 비활성화 상태 모드 연속성

> **핵심 원칙**: 화면 꺼짐/앱 백그라운드 상태에서도 **현재 모드의 전체 사이클이 끊김 없이 완료**되어야 함

| ID | 시나리오 | 기대 동작 | 우선순위 |
|----|----------|----------|---------|
| BG-N1 | Playing 중 화면 끄기 | 재생 계속 → 청크 완료 → 자동 다음 청크 이동 | **CRITICAL** |
| BG-LR1 | LR 모드 Playing 중 화면 끄기 | Playing 완료 → Gap 진입 → Gap 완료 → 다음 사이클 | **CRITICAL** |
| BG-LRLR1 | LRLR 모드 PlayingFirst 중 화면 끄기 | 전체 사이클 (녹음 포함) 완료 | **CRITICAL** |
| BG-P1 | 화면 끄기 상태에서 플레이리스트 아이템 자동 전환 | 다음 아이템 로드 → 현재 모드로 자동 재생 시작 | **CRITICAL** |

---

## 위험 요소 및 대응

| 위험 | 영향 | 대응 |
|------|------|------|
| AVPlayer seeking 정밀도 | 청크 싱크 오차 | seek tolerance 최소화, 실기기 테스트 |
| Apple Speech 타임스탬프 정확도 | Whisper 대비 낮음 | Whisper 우선 권장, 설정에서 선택 |
| 백그라운드 재생 제한 | iOS 정책 | AVAudioSession 올바른 설정 |
| SwiftData 마이그레이션 | 스키마 변경 시 | 초기부터 버전 관리 |
| iPhone SE 작은 화면 | UI 잘림, 터치 영역 부족 | 최소 44pt 터치 영역, 스크롤 활용 |
| iPad Split View | 레이아웃 깨짐 | Size class 기반 적응형 레이아웃 |
| Stage Manager 다중 윈도우 | 상태 동기화 | 단일 PlaybackService 싱글톤 |
| Dynamic Type 극단적 크기 | 레이아웃 깨짐 | ScrollView + 최소/최대 크기 설정 |

---

## 14. 개발 방법론

### 14.1 Subagent 기반 개발 원칙

> **핵심 원칙**: 모든 구현 작업은 Subagent가 수행하고, Main Thread는 검증/조율만 담당

```
┌─────────────────────────────────────────────────────────────────┐
│                      Main Thread (조율자)                        │
│  - 전체 진행 상황 확인                                            │
│  - 구현 결과 검증 및 피드백                                        │
│  - 다음 작업 지시 및 우선순위 결정                                  │
│  - 병렬 작업 조율 (의존성 없는 작업은 동시 진행)                     │
└─────────────────────────────────────────────────────────────────┘
                              │
           ┌──────────────────┼──────────────────┐
           ▼                  ▼                  ▼
    ┌────────────┐     ┌────────────┐     ┌────────────┐
    │ Subagent A │     │ Subagent B │     │ Subagent C │
    │ (구현 담당) │     │ (구현 담당) │     │ (구현 담당) │
    └────────────┘     └────────────┘     └────────────┘
```

### 14.2 역할 분리

| 역할 | Main Thread | Subagent |
|------|-------------|----------|
| **코드 작성** | ❌ | ✅ |
| **테스트 작성** | ❌ | ✅ |
| **파일 탐색/분석** | ❌ (Subagent 위임) | ✅ |
| **빌드 실행** | ✅ (결과 확인) | ✅ (실제 실행) |
| **결과 검증** | ✅ | ❌ |
| **다음 작업 결정** | ✅ | ❌ |
| **에러 분석 지시** | ✅ | ❌ |
| **에러 해결 구현** | ❌ | ✅ |

### 14.3 작업 흐름

```
1. Main Thread: 작업 지시 (Phase X.X 구현)
       │
       ▼
2. Subagent: 코드 작성 + 테스트 작성
       │
       ▼
3. Main Thread: 빌드/테스트 결과 확인
       │
       ├── 성공 → 다음 작업 지시
       │
       └── 실패 → 에러 내용과 함께 수정 지시
              │
              ▼
       4. Subagent: 에러 수정
              │
              ▼
       5. Main Thread: 재검증 (반복)
```

### 14.4 병렬 작업 규칙

**병렬 가능 (의존성 없음)**
```
Phase 2 (청킹 알고리즘)  ←→  Phase 3 (데이터 계층)
Phase 6 (재생 서비스)   ←→  Phase 7 (녹음 서비스)
화면 A 구현             ←→  화면 B 구현 (공유 상태 없을 때)
```

**순차 필수 (의존성 있음)**
```
Phase 5 (상태 머신) → Phase 6 (재생 서비스)  // 상태 머신이 재생에 필수
Phase 4 (전사)     → Phase 8.6 (전사 화면)  // 전사 서비스가 화면에 필수
Model 정의         → Repository 구현        // Model이 Repository에 필수
```

### 14.5 검증 체크포인트

각 Phase 완료 시 Main Thread가 확인할 항목:

```
□ 빌드 성공: xcodebuild build 통과
□ 테스트 통과: xcodebuild test 통과
□ 린트 통과: swiftlint lint --strict 통과
□ 기능 동작: 시뮬레이터에서 수동 확인
□ 코드 품질: @Observable 사용, @MainActor 적용
```

### 14.6 Subagent 지시 템플릿

```markdown
## 작업 지시: [Phase X.X 이름]

### 목표
- [구체적 구현 목표]

### 참조 파일
- Android: `path/to/android/file.kt`
- iOS 위치: `ListenerIOS/path/to/file.swift`

### 요구사항
1. [세부 요구사항 1]
2. [세부 요구사항 2]

### 테스트 케이스
- [ ] [테스트 1]
- [ ] [테스트 2]

### 완료 조건
- [ ] 빌드 성공
- [ ] 테스트 통과
- [ ] 지정된 기능 동작
```

### 14.7 에러 처리 프로토콜

```
1. Main Thread: 에러 로그 전달
   "빌드 에러 발생: [에러 메시지]"

2. Subagent: 원인 분석 + 수정
   - 에러 원인 설명
   - 수정 코드 제출

3. Main Thread: 재빌드 + 결과 확인
   - 성공: 다음 단계 진행
   - 실패: 추가 컨텍스트와 함께 재지시
```

### 14.8 금지 사항

**Main Thread 금지**
- ❌ 직접 코드 작성 (한 줄도 안 됨)
- ❌ 직접 파일 탐색 (Subagent에 위임)
- ❌ 테스트 코드 작성
- ❌ 에러 직접 수정

**Subagent 금지**
- ❌ 작업 범위 임의 확장
- ❌ 지시받지 않은 리팩토링
- ❌ 다음 Phase 선행 진행
- ❌ 검증 없이 완료 선언

### 14.9 필수 참조 문서

> **중요**: 구현 전/중/후에 반드시 아래 문서를 확인하여 요구사항 누락 방지

#### 구현 전 확인 (Must Read)

| 문서 | 위치 | 확인 내용 |
|------|------|----------|
| **SPEC.md** | `listener/SPEC.md` | 기능 명세, UI 와이어프레임, 데이터 모델 |
| **IMPLEMENTATION_PLAN.md** | `listener/ios/IMPLEMENTATION_PLAN.md` | 해당 Phase 상세 요구사항 |
| **Android CLAUDE.md** | `listener/CLAUDE.md` | 핵심 불변조건, 알려진 문제점 |

#### 영역별 세부 문서 (해당 영역 작업 시)

| 영역 | 문서 | 핵심 내용 |
|------|------|----------|
| **청킹 알고리즘** | `listener/app/.../chunking/CLAUDE.md` | 동기화 규칙, 드리프트 방지, 94개 테스트 |
| **서비스 계층** | `listener/app/.../service/CLAUDE.md` | 경합 조건, 상태 머신 전이 |
| **데이터 계층** | `listener/app/.../data/CLAUDE.md` | 트랜잭션, 고아 데이터 방지 |

#### 구현 후 검증 (Must Check)

| 문서 | 위치 | 검증 내용 |
|------|------|----------|
| **E2E 검증 체크리스트** | `listener/docs/E2E_VERIFICATION_CHECKLIST.md` | 사용자 시나리오별 전체 플로우 |
| **Edge Case 테스트** | `listener/EDGE_E2E.md` | 46개 엣지 케이스 시나리오 |
| **본 문서 12장** | 섹션 12 | iOS 맞춤 E2E 체크리스트 |
| **본 문서 13장** | 섹션 13 | iOS 맞춤 Edge Case 테스트 |

#### 문서 확인 체크리스트

```
구현 시작 전:
□ SPEC.md에서 해당 기능 명세 확인
□ IMPLEMENTATION_PLAN.md에서 Phase 요구사항 확인
□ 관련 CLAUDE.md에서 주의사항/불변조건 확인

구현 완료 후:
□ E2E_VERIFICATION_CHECKLIST.md 해당 항목 통과
□ EDGE_E2E.md 관련 시나리오 통과
□ 본 문서 12장/13장 체크리스트 통과
```

#### Subagent 지시 시 문서 참조 예시

```markdown
## 작업 지시: Phase 2.1 SentenceSplitter 구현

### 필수 참조 문서
1. `listener/SPEC.md` - 섹션 8 분절 알고리즘
2. `listener/app/.../chunking/CLAUDE.md` - 불변조건, 테스트 목록
3. `listener/app/.../chunking/SentenceSplitter.kt` - Android 구현체

### 검증 문서
- `listener/docs/E2E_VERIFICATION_CHECKLIST.md` - 섹션 6.9
- `listener/EDGE_E2E.md` - CH-* 시나리오
```

---

*Last Updated: 2026-01-23*
*Generated for iOS Migration from Android Listener App*
