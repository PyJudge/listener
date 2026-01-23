# CLAUDE.md - Data 계층

## ⚠️ 수정 시 주의사항

### 청크 저장/삭제
- 청크 삭제와 삽입은 반드시 하나의 트랜잭션 안에서 실행
- 트랜잭션 없으면: 삭제 직후 다른 스레드가 읽으면 0개 청크 반환
- 청크 0개 = 플레이어 "no content" 에러

### 에피소드/파일 삭제
- 에피소드 삭제 전 관련 플레이리스트 항목 먼저 삭제
- 안 하면: 플레이리스트에 "유령 항목" 남음 (재생 시 에러)

### DB 스키마 변경
- Room version 올리면 기존 사용자 데이터 손실 가능
- 반드시 Migration 정의하거나 fallbackToDestructiveMigration 명시

### API 키 저장
- 현재: DataStore에 평문 저장 (보안 취약)
- 루팅 기기에서 키 탈취 가능

---

## 핵심 불변조건 (INVARIANTS)

```
1. Chunk의 sourceId는 반드시 존재하는 TranscriptionResult를 참조
2. PlaylistItem의 sourceId는 Episode 또는 LocalFile 중 하나와 일치
3. DB 트랜잭션 내에서 Chunk 삭제/삽입은 원자적으로 수행
```

---

## 알려진 문제점

### CRITICAL - Orphaned PlaylistItem

**현상:** 에피소드 삭제 시 PlaylistItem이 고아 데이터로 남음

**원인:** PlaylistItem.sourceId에 FK 제약 없음 (Polymorphic Type)

**영향:** 플레이리스트 재생 시 Chunk 없음 오류

**위치:** `PlaylistItemEntity.kt`, `FolderItemEntity.kt`

**당장 해결책:**
```sql
-- 고아 데이터 정리 쿼리
DELETE FROM playlist_items
WHERE sourceId NOT IN (
    SELECT id FROM podcast_episodes
    UNION
    SELECT contentHash FROM local_audio_files
)
```

**근본 해결책:**
- 에피소드/파일 삭제 전 관련 PlaylistItem 먼저 삭제
- 또는 스키마 정규화 (Polymorphic → 상속 구조)

### HIGH - saveChunks 트랜잭션 부재

**현상:** 청크 저장 중 읽기 요청 시 0 chunks 반환 가능

**원인:** DELETE와 INSERT가 별도 트랜잭션

**위치:** `TranscriptionRepositoryImpl.kt` 라인 494-505

**해결책:**
```kotlin
@Transaction
override suspend fun saveChunks(sourceId: String, chunks: List<Chunk>) {
    transcriptionDao.deleteChunks(sourceId)
    transcriptionDao.insertChunks(entities)
}
```

### HIGH - TranscriptionResult 다국어 지원 불가

**현상:** 동일 sourceId로 여러 언어 전사 결과 저장 불가

**원인:** `sourceId`가 단일 PK (복합 PK 아님)

**위치:** `TranscriptionResultEntity.kt`

**해결책:**
```kotlin
@Entity(
    tableName = "transcription_results",
    primaryKeys = ["sourceId", "language"]  // 복합 PK
)
```

### MEDIUM - N+1 쿼리 문제

**현상:** 플레이리스트 duration 계산 시 LocalFile JOIN 누락

**위치:** `PlaylistDao.kt` 라인 48-54

**해결책:**
```sql
SELECT COALESCE(SUM(duration), 0)
FROM (
    SELECT pe.durationMs as duration
    FROM playlist_items pi
    JOIN podcast_episodes pe ON pi.sourceId = pe.id AND pi.sourceType = 'PODCAST_EPISODE'
    WHERE pi.playlistId = :playlistId
    UNION ALL
    SELECT laf.durationMs as duration
    FROM playlist_items pi
    JOIN local_audio_files laf ON pi.sourceId = laf.contentHash AND pi.sourceType = 'LOCAL_FILE'
    WHERE pi.playlistId = :playlistId
)
```

### MEDIUM - INDEX 누락

**누락된 인덱스:**
- `LearningProgressEntity.sourceId`
- `RecentLearningEntity.lastAccessedAt` (DESC)
- `UserRecordingEntity` (sourceId, chunkIndex)

**권장:**
```kotlin
@Entity(
    tableName = "learning_progress",
    indices = [
        Index("sourceId"),
        Index(value = ["updatedAt"], orders = [Index.Order.DESC])
    ]
)
```

### MEDIUM - 마이그레이션 미정의

**현상:** `exportSchema = false`로 마이그레이션 경로 불명확

**위치:** `ListenerDatabase.kt`

**권장:**
```kotlin
@Database(
    entities = [...],
    version = 6,
    exportSchema = true,  // 변경
    autoMigrations = [
        AutoMigration(from = 5, to = 6)
    ]
)
```

---

## Entity 관계도

```
SubscribedPodcast (feedUrl PK)
       ↓ (FK, CASCADE)
PodcastEpisode (id PK, feedUrl FK)
       ↓ (sourceId로 참조, FK 없음)
TranscriptionResult (sourceId PK)
       ↓ (FK, CASCADE)
ChunkEntity (id PK, sourceId FK)

Playlist (id PK)
       ↓ (FK, CASCADE)
PlaylistItem (id PK, playlistId FK, sourceId - FK 없음!)

LocalAudioFile (contentHash PK)
       ↓ (sourceId로 참조, FK 없음)
TranscriptionResult
```

---

## 수정 시 체크리스트

```bash
# 1. DAO 테스트
./gradlew connectedAndroidTest --tests "*.PlaylistDaoTest"

# 2. 고아 데이터 검증 (실기기)
adb exec-out run-as com.listener cat databases/listener_database > /tmp/db.db
sqlite3 /tmp/db.db "
SELECT COUNT(*) FROM playlist_items
WHERE sourceId NOT IN (
    SELECT id FROM podcast_episodes
    UNION
    SELECT contentHash FROM local_audio_files
)"
# 결과: 0이어야 정상

# 3. 마이그레이션 테스트 (스키마 변경 시)
./gradlew test --tests "*Migration*"
```

---

## 핵심 파일

| 파일 | 역할 |
|------|------|
| `ListenerDatabase.kt` | Room Database 정의 |
| `TranscriptionRepositoryImpl.kt` | 전사 결과 저장/로드 |
| `PodcastRepositoryImpl.kt` | 팟캐스트 구독/에피소드 관리 |
| `SettingsRepository.kt` | 사용자 설정 (DataStore) |
| `entity/*.kt` | Room Entity 정의 |
| `dao/*.kt` | DAO 인터페이스 |

---

## 보안 주의사항

### API 키 저장 (SettingsRepository)

**현재:** DataStore에 평문 저장 (위험)

**권장:** EncryptedSharedPreferences 사용

```kotlin
// build.gradle.kts
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```
