# Phase 5: 콘텐츠 UI

## 비즈니스 로직

### 해결하는 문제
SPEC.md 3.3~3.4, 3.6장의 팟캐스트 탭, 미디어 파일 탭, 전사 진행 화면을 구현합니다.

### 핵심 규칙
1. **팟캐스트 검색**: 300ms debounce, iTunes API
2. **에피소드 바텀시트**: 학습 시작, 플레이리스트 추가
3. **파일 추가**: `ACTION_OPEN_DOCUMENT`, `audio/*`
4. **전사 진행**: 다운로드 -> 전사 -> 분절 -> 완료

### 제약/예외
- 새 에피소드 배지 표시 (빨간 점)
- 빈 상태 시 추천 또는 유도 버튼

---

## 설계

### 팟캐스트 관련
```
presentation/podcast/
├── PodcastScreen.kt            # 구독 목록 + 빈 상태
├── PodcastSearchScreen.kt      # 검색 화면
├── PodcastDetailScreen.kt      # 에피소드 목록
├── EpisodeBottomSheet.kt       # 에피소드 상세
└── PodcastViewModel.kt
```

### 미디어 파일 관련
```
presentation/media/
├── MediaFileScreen.kt          # 파일 목록 + 빈 상태
├── MediaFileBottomSheet.kt     # 파일 상세
└── MediaFileViewModel.kt
```

### 전사 진행
```
presentation/transcription/
├── TranscriptionScreen.kt      # 다운로드 + 전사 진행
└── TranscriptionViewModel.kt
```

---

## 테스트 케이스

### PodcastViewModel 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 구독 목록 로드 | loadSubscriptions() | List<Podcast> |
| 정상 | 검색 (debounce) | "english" 입력 | 300ms 후 API 호출 |
| 정상 | 구독 추가 | subscribe(podcast) | DB 저장 |
| 정상 | 구독 해제 | unsubscribe(feedUrl) | DB 삭제 |
| 정상 | 에피소드 로드 | loadEpisodes(feedUrl) | RSS 파싱 결과 |
| 정상 | 새 에피소드 표시 | isNew = true | 배지 표시 |
| 엣지 | 검색 빈 결과 | "ㅁㄴㅇㄹ" | 빈 목록 |
| 예외 | 네트워크 오류 | 오프라인 | 에러 상태 |

### MediaFileViewModel 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 파일 목록 로드 | loadFiles() | List<LocalAudioFile> |
| 정상 | 파일 추가 | addFile(uri) | DB 저장 + 해시 생성 |
| 정상 | 파일 삭제 | deleteFile(hash) | DB 삭제 |
| 엣지 | 동일 파일 재추가 | 같은 해시 | 중복 무시 |

### TranscriptionViewModel 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 다운로드 시작 | startTranscription() | 다운로드 진행 |
| 정상 | 다운로드 완료 | 100% | 전사 단계 전환 |
| 정상 | 전사 완료 | API 응답 | 분절 결과 저장 |
| 정상 | 취소 | cancel() | 부분 데이터 삭제 |
| 예외 | API 오류 | 401 | 에러 메시지 표시 |

---

## 실행 계획

### TDD 순서
1. **PodcastViewModel** 테스트 -> 구현
2. **PodcastScreen** UI 구현
3. **PodcastSearchScreen** 검색 + debounce
4. **MediaFileViewModel** 테스트 -> 구현
5. **MediaFileScreen** UI 구현
6. **TranscriptionViewModel** 테스트 -> 구현
7. **TranscriptionScreen** UI 구현

### 병렬/순차
- **병렬**: Podcast UI, MediaFile UI
- **순차**: ViewModel -> Screen

---

## 완료 체크리스트

- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew test` 모든 테스트 통과
- [ ] `./gradlew lint` 경고/오류 없음
- [ ] Android Emulator 실행 확인 (스크린샷)
- [ ] 팟캐스트 검색 동작 (debounce)
- [ ] 구독 추가/해제 동작
- [ ] 에피소드 목록 로드
- [ ] 미디어 파일 추가/삭제
- [ ] 전사 진행률 표시
- [ ] 전사 완료 -> 플레이어 이동

---

## 학습 내용 (Phase 5 완료 후)

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
