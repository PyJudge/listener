# Phase 1: 데이터 계층

## 비즈니스 로직

### 해결하는 문제
앱의 모든 데이터 흐름의 기반이 되는 로컬 DB와 원격 API 클라이언트를 구현합니다.
SPEC.md에 정의된 10개 데이터 모델을 Room Entity로 정의하고, iTunes/OpenAI API 클라이언트를 구축합니다.

### 핵심 규칙
1. **sourceId 규칙**: 팟캐스트 에피소드는 `guid` 또는 `audioUrl` SHA-256, 로컬 파일은 `파일 바이트 SHA-256`
2. **재전사 방지**: 동일 sourceId + 동일 language -> 기존 결과 재사용
3. **캐시 용량**: 오디오 캐시 최대 1GB, LRU 방식 삭제
4. **녹음 파일**: chunk당 1개만 저장, 덮어쓰기

### 제약/예외
- Room Entity는 data class로 정의
- DAO는 suspend function 사용 (Flow 지원)
- API 응답은 DTO -> Domain Model 변환

---

## 설계

### Entity 목록 (SPEC.md 6장 기반)
```
data/local/db/entity/
├── SubscribedPodcastEntity.kt    # 6.1
├── PodcastEpisodeEntity.kt       # 6.2
├── LocalAudioFileEntity.kt       # 6.3
├── TranscriptionResultEntity.kt  # 6.4
├── ChunkEntity.kt                # 6.5
├── LearningProgressEntity.kt     # 6.6
├── UserRecordingEntity.kt        # 6.7
├── PlaylistEntity.kt             # 6.8
├── PlaylistItemEntity.kt         # 6.9
└── RecentLearningEntity.kt       # 6.10
```

### DAO 목록
```
data/local/db/dao/
├── PodcastDao.kt           # 팟캐스트 + 에피소드
├── LocalFileDao.kt         # 로컬 오디오 파일
├── TranscriptionDao.kt     # 전사 결과 + chunk
├── LearningProgressDao.kt  # 학습 진행
├── RecordingDao.kt         # 사용자 녹음
└── PlaylistDao.kt          # 플레이리스트
```

### API 클라이언트
```
data/remote/api/
├── ITunesApi.kt            # 팟캐스트 검색
└── OpenAiApi.kt            # Whisper 전사
```

### Repository 인터페이스
```
domain/repository/
├── PodcastRepository.kt
├── LocalFileRepository.kt
├── TranscriptionRepository.kt
├── LearningRepository.kt
└── PlaylistRepository.kt
```

---

## 테스트 케이스

### Entity & DAO 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 팟캐스트 구독 추가 | SubscribedPodcastEntity | DB 저장 성공 |
| 정상 | 에피소드 목록 조회 | feedUrl | List<PodcastEpisodeEntity> |
| 정상 | 새 에피소드 필터 | isNew = true | 새 에피소드만 |
| 정상 | 로컬 파일 추가 | LocalAudioFileEntity | DB 저장 성공 |
| 정상 | 전사 결과 저장 | TranscriptionResultEntity | DB 저장 성공 |
| 정상 | 전사 중복 확인 | sourceId + language | 존재 여부 반환 |
| 정상 | chunk 목록 조회 | sourceId | orderIndex 순 정렬 |
| 정상 | 학습 진행 저장 | LearningProgressEntity | DB 저장 성공 |
| 정상 | 녹음 덮어쓰기 | 동일 sourceId + chunkIndex | 기존 삭제 후 저장 |
| 정상 | 플레이리스트 순서 변경 | orderIndex 업데이트 | 정렬 반영 |
| 엣지 | 빈 에피소드 목록 | 존재하지 않는 feedUrl | emptyList() |
| 예외 | 중복 PK 저장 | 동일 PK | OnConflictStrategy.REPLACE |

### API 테스트 (Mock)

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | iTunes 검색 | "english learning" | SearchResponse |
| 정상 | OpenAI 전사 | audio file | TranscriptionResponse |
| 엣지 | iTunes 빈 결과 | "ㅁㄴㅇㄹ" | resultCount = 0 |
| 예외 | OpenAI 401 | 잘못된 API 키 | AuthenticationException |
| 예외 | 네트워크 오류 | 오프라인 | NetworkException |

---

## 실행 계획

### TDD 순서
1. **Entity 정의** -> 컴파일 확인
2. **DAO 정의** -> `@Query` 컴파일 확인
3. **Database 클래스** -> 마이그레이션 전략 설정
4. **DAO 테스트** -> Instrumented Test (실제 DB)
5. **API 인터페이스 정의** -> Retrofit 검증
6. **API Mock 테스트** -> MockWebServer
7. **Repository 구현** -> DAO + API 통합

### 병렬/순차
- **병렬**: Entity 정의, API 인터페이스 정의
- **순차**: DAO -> Database -> Repository

---

## 완료 체크리스트

- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew test` 모든 테스트 통과
- [ ] `./gradlew connectedAndroidTest` 통과
- [ ] `./gradlew lint` 경고/오류 없음
- [ ] Android Emulator 실행 확인 (스크린샷)
- [ ] 10개 Entity 정의 완료
- [ ] 모든 DAO CRUD 테스트 통과
- [ ] iTunes API 검색 테스트 통과
- [ ] OpenAI API Mock 테스트 통과
- [ ] Repository 통합 테스트 통과
- [ ] DB 마이그레이션 전략 설정

---

## 학습 내용 (Phase 1 완료 후)

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
