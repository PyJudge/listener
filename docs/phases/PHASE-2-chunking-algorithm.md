# Phase 2: 분절 알고리즘

## 비즈니스 로직

### 해결하는 문제
OpenAI Whisper API 응답의 `segments`와 `words`를 조합하여 학습용 chunk를 정확하게 분절합니다.
SPEC.md 8장의 ankigpt 방식 알고리즘을 구현합니다.

### 핵심 규칙
1. **구분자 설정**:
   - 문장 단위 ON (기본): `. ! ?`
   - 문장 단위 OFF: `, . ! ?`
2. **시퀀스 매칭**: 문장 끝 3개 단어로 words 배열에서 타임스탬프 찾기
3. **하이픈 확장**: `mind-blowing` -> `["mind", "blowing"]`으로 분리 후 매칭
4. **최소 길이 병합**: minChunkMs (기본 1200ms) 미만 chunk는 다음과 병합
5. **중복 제거**: 25MB 초과 오디오 청크 분할 시 경계 중복 단어 제거

### 제약/예외
- 타임스탬프 단위: milliseconds
- 빈 segment 무시
- word가 없으면 segment 타임스탬프 폴백

---

## 설계

### 컴포넌트
```
domain/usecase/
├── ChunkingUseCase.kt              # 메인 진입점
├── SentenceSplitter.kt             # 구두점 기반 문장 분리
├── TimestampMatcher.kt             # 시퀀스 매칭 로직
├── ChunkMerger.kt                  # 최소 길이 병합
└── DuplicateRemover.kt             # 경계 중복 제거
```

### 데이터 모델
```kotlin
data class WhisperSegment(
    val start: Double,  // seconds
    val end: Double,
    val text: String
)

data class WhisperWord(
    val word: String,
    val start: Double,
    val end: Double
)

data class Chunk(
    val orderIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val displayText: String
)
```

### 알고리즘 흐름
```
WhisperResponse
    |
    v
+------------------+
| DuplicateRemover |  <- 25MB 청크 경계 중복 제거
+--------+---------+
         |
         v
+------------------+
| SentenceSplitter |  <- 구두점으로 문장 경계 탐지
+--------+---------+
         |
         v
+------------------+
| TimestampMatcher |  <- 마지막 3단어 시퀀스 매칭
+--------+---------+
         |
         v
+------------------+
|   ChunkMerger    |  <- minChunkMs 미만 병합
+--------+---------+
         |
         v
    List<Chunk>
```

---

## 테스트 케이스

### SentenceSplitter 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 마침표 분리 | "Hello. World." | ["Hello.", "World."] |
| 정상 | 느낌표 분리 | "Great! Thanks." | ["Great!", "Thanks."] |
| 정상 | 물음표 분리 | "How? Why?" | ["How?", "Why?"] |
| 정상 | 쉼표 분리 (OFF) | "First, second." | ["First,", "second."] |
| 정상 | 쉼표 유지 (ON) | "First, second." | ["First, second."] |
| 엣지 | 연속 구두점 | "Really?!" | ["Really?!"] |
| 엣지 | 약어 | "Dr. Smith said." | ["Dr. Smith said."] (개선 필요) |

### TimestampMatcher 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 3단어 매칭 | "good thing to know." | words[n].end |
| 정상 | 2단어 폴백 | (3단어 실패 시) | words[n].end |
| 정상 | 1단어 폴백 | (2단어 실패 시) | words[n].end |
| 정상 | 하이픈 확장 | "mind-blowing." | ["mind", "blowing"] 매칭 |
| 엣지 | 소문자 정규화 | "HELLO" vs "hello" | 매칭 성공 |
| 예외 | 매칭 실패 | words에 없는 단어 | 선형 할당 폴백 |

### ChunkMerger 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 짧은 chunk 병합 | [800ms, 600ms] | [1400ms] (합쳐짐) |
| 정상 | 긴 chunk 유지 | [1500ms, 1300ms] | [1500ms, 1300ms] |
| 정상 | 마지막 짧은 chunk | [..., 500ms] | 이전 chunk에 병합 |
| 엣지 | 모두 짧은 chunk | [500ms, 500ms, 500ms] | [1500ms] |
| 엣지 | 단일 chunk | [800ms] | [800ms] (그대로) |

### DuplicateRemover 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 중복 제거 | ["hello"@1.0, "hello"@1.0] | ["hello"@1.0] |
| 정상 | 비중복 유지 | ["hello"@1.0, "world"@2.0] | 그대로 |
| 엣지 | 타임스탬프 겹침 | start < prev.end 동일 단어 | 스킵 |

### 통합 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 전체 파이프라인 | WhisperResponse | List<Chunk> 정확한 타임스탬프 |
| 정상 | SPEC 예제 | 실제 팟캐스트 응답 | chunk 개수 합리적 (문장 수와 유사) |

---

## 실행 계획

### TDD 순서
1. **SentenceSplitter** 테스트 -> 구현
2. **TimestampMatcher** 테스트 -> 구현
3. **ChunkMerger** 테스트 -> 구현
4. **DuplicateRemover** 테스트 -> 구현
5. **ChunkingUseCase** 통합 테스트 -> 구현

### 병렬/순차
- **병렬**: SentenceSplitter, DuplicateRemover (독립적)
- **순차**: TimestampMatcher -> ChunkMerger -> ChunkingUseCase

---

## 완료 체크리스트

- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew test` 모든 테스트 통과
- [ ] `./gradlew lint` 경고/오류 없음
- [ ] Android Emulator 실행 확인 (스크린샷)
- [ ] SentenceSplitter 모든 테스트 통과
- [ ] TimestampMatcher 시퀀스 매칭 테스트 통과
- [ ] ChunkMerger 병합 로직 테스트 통과
- [ ] DuplicateRemover 중복 제거 테스트 통과
- [ ] 통합 테스트: 실제 Whisper 응답으로 검증
- [ ] 성능: 10분 오디오 1초 이내 분절

---

## 학습 내용 (Phase 2 완료 후)

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
