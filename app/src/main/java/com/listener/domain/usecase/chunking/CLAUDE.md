# CLAUDE.md - Chunking 알고리즘

## 핵심 불변조건 (INVARIANT)

```
Chunk의 displayText와 (startMs, endMs)는 반드시 동기화되어야 한다.

- displayText의 각 단어는 [startMs, endMs] 범위 내에서 실제로 재생되어야 함
- 이 규칙이 깨지면 사용자가 듣는 소리와 보는 텍스트가 불일치
- 모든 수정 후 반드시 실기기에서 재생 테스트 필수
```

---

## 알고리즘 파이프라인

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
[5단계] ChunkMerger: 최소 길이 병합
        ↓
Result: List<Chunk>
```

---

## 알려진 문제점

### HIGH - 반복 단어 드리프트

**현상:** "The end. The end is near. The end."에서 세 번째 "The"가 첫 번째로 매칭

**원인:** TimestampMatcher가 첫 매칭만 반환, 시간 기반 범위 제한 없음

**회피책 (현재):** `searchStartIndex` 파라미터로 검색 시작점 제한

**해결책 (권장):**
```kotlin
// TimestampMatcher에 시간 기반 바운드 추가
fun findStartIndexWithTimeBound(
    sentenceWords: List<String>,
    allWords: List<Word>,
    previousChunkEndMs: Long,
    searchWindowMs: Long = 5000L
): Int?
```

### HIGH - 비순차 타임스탬프

**현상:** Whisper가 비순차 타임스탬프 출력 (실제 데이터에 72개 쌍 존재)

**회피책 (현재):** DuplicateRemover에서 정렬 제거, 원본 순서 유지

**검증:** `ChunkingUseCaseRealDataTest.kt` 라인 185-222

### MEDIUM - 1글자 단어 매칭

**현상:** "I", "a" 같은 1글자 단어가 Fuzzy 매칭 제외

**위치:** `TwoPointerAligner.kt` 라인 104-109

**권장 수정:**
```kotlin
if (s1.length == 1 && s2.length == 1) return s1 == s2
```

### MEDIUM - 콜론/세미콜론 분할

**현상:** "Title: Description" 형식이 분할되지 않아 청크가 너무 길어짐

**위치:** `SentenceSplitter.kt`

---

## 수정 시 체크리스트

```bash
# 1. 단위 테스트 전체 통과
./gradlew test --tests "*.chunking.*"

# 2. 실제 데이터 테스트 통과
./gradlew test --tests "*.ChunkingUseCaseRealDataTest"

# 3. 드리프트 시뮬레이션 통과
./gradlew test --tests "*drift*"

# 4. 실기기 검증
adb exec-out run-as com.listener cat databases/listener_database > /tmp/db.db
sqlite3 /tmp/db.db "SELECT MAX(endMs-startMs)/1000.0 as maxSec FROM chunks"
# 결과: 30초 이하여야 정상

# 5. 실기기에서 최소 3개 청크 재생하며 싱크 확인
```

---

## 테스트 파일

| 테스트 | 파일 | 테스트 수 |
|--------|------|----------|
| SentenceSplitter | `SentenceSplitterTest.kt` | 47개 |
| TimestampMatcher | `TimestampMatcherTest.kt` | 12개 |
| ChunkMerger | `ChunkMergerTest.kt` | 15개 |
| DuplicateRemover | `DuplicateRemoverTest.kt` | 4개 |
| ChunkingUseCase | `ChunkingUseCaseTest.kt` | 8개 |
| 실제 데이터 | `ChunkingUseCaseRealDataTest.kt` | 8개 |

---

## 핵심 파일

| 파일 | 역할 |
|------|------|
| `ChunkingUseCase.kt` | 전체 파이프라인 오케스트레이션 |
| `SentenceSplitter.kt` | 문장 경계 탐지 (약어, 도메인, 소수점 처리) |
| `TimestampMatcher.kt` | 문장 시작 위치 탐지 |
| `aligner/TwoPointerAligner.kt` | Phrase-Word 정렬 (선택됨) |
| `aligner/TimestampAssigner.kt` | 타임스탬프 할당 및 보간 |
| `ChunkMerger.kt` | 최소 길이 기준 병합 |
| `DuplicateRemover.kt` | 중복 단어 제거 (정렬 없이) |

---

## 성능 특성

| 단계 | 시간복잡도 |
|------|----------|
| SentenceSplitter | O(n) |
| TimestampMatcher | O(n) |
| TwoPointerAligner | O(n+m) |
| TimestampAssigner | O(n) |
| ChunkMerger | O(n) |
| **총합** | **O(n log n)** |

**40분 에피소드 (6500단어):** ~50ms 내 완료
