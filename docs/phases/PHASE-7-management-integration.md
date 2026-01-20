# Phase 7: 관리 UI & 통합

## 비즈니스 로직

### 해결하는 문제
SPEC.md 3.1~3.2, 3.5장의 홈 화면, 플레이리스트, 설정 화면을 구현하고,
전체 앱 E2E 테스트로 통합을 검증합니다.

### 핵심 규칙
1. **홈 화면**: 이어서 학습하기 (최근 5개), 새 에피소드 (최근 10개)
2. **플레이리스트**: 드래그 정렬, 이어서 학습
3. **설정**: 학습/전사/저장공간/API 설정

### 제약/예외
- 빈 상태 시 유도 버튼
- 설정 변경 즉시 적용

---

## 설계

### 홈 화면
```
presentation/home/
├── HomeScreen.kt
├── RecentLearningSection.kt    # 이어서 학습하기
├── NewEpisodesSection.kt       # 새 에피소드
└── HomeViewModel.kt
```

### 플레이리스트
```
presentation/playlist/
├── PlaylistListScreen.kt       # 목록
├── PlaylistDetailScreen.kt     # 상세 (드래그 정렬)
├── CreatePlaylistDialog.kt     # 생성 다이얼로그
└── PlaylistViewModel.kt
```

### 설정
```
presentation/settings/
├── SettingsScreen.kt
├── LearningSettingsSection.kt  # 반복, 공백, 자동녹음
├── TranscriptionSettingsSection.kt  # 언어, 최소구간, 문장단위
├── StorageSettingsSection.kt   # 캐시, 녹음 파일
├── ApiSettingsSection.kt       # OpenAI API 키
└── SettingsViewModel.kt
```

---

## 테스트 케이스

### HomeViewModel 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 최근 학습 로드 | load() | lastAccessedAt DESC, 5개 |
| 정상 | 새 에피소드 로드 | load() | pubDate DESC, 10개 |
| 정상 | 빈 상태 | 구독 없음 | 유도 버튼 표시 |

### PlaylistViewModel 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 목록 로드 | loadPlaylists() | List<Playlist> |
| 정상 | 생성 | create("영어 회화") | DB 저장 |
| 정상 | 삭제 | delete(playlistId) | DB 삭제 |
| 정상 | 순서 변경 | reorder(from, to) | orderIndex 업데이트 |
| 정상 | 이어서 학습 | resume(playlistId) | 첫 미완료 항목 |
| 정상 | 항목 추가 | addItem(sourceId) | DB 저장 |
| 정상 | 항목 제거 | removeItem(itemId) | DB 삭제 |

### SettingsViewModel 테스트

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 설정 로드 | load() | DataStore 값 |
| 정상 | 반복 횟수 저장 | setRepeatCount(3) | DataStore 저장 |
| 정상 | API 키 저장 | setApiKey("sk-...") | 암호화 저장 |
| 정상 | 캐시 삭제 | clearCache() | 캐시 폴더 삭제 |
| 정상 | 캐시 용량 조회 | getCacheSize() | 바이트 값 |

### E2E 테스트

| 구분 | 테스트명 | 시나리오 | 기대값 |
|------|---------|---------|--------|
| 정상 | 팟캐스트 학습 플로우 | 검색 -> 구독 -> 에피소드 선택 -> 전사 -> 학습 | 전체 성공 |
| 정상 | 로컬 파일 학습 플로우 | 파일 추가 -> 전사 -> 학습 | 전체 성공 |
| 정상 | 플레이리스트 플로우 | 생성 -> 항목 추가 -> 이어서 학습 | 전체 성공 |
| 정상 | HARD 모드 플로우 | HARD 모드 -> 녹음 -> 비교 | 전체 성공 |
| 정상 | 백그라운드 재생 | 앱 백그라운드 -> 알림 컨트롤 | 재생 유지 |

---

## 실행 계획

### TDD 순서
1. **HomeViewModel** 테스트 -> 구현
2. **HomeScreen** UI 구현
3. **PlaylistViewModel** 테스트 -> 구현
4. **PlaylistScreens** UI 구현
5. **SettingsViewModel** 테스트 -> 구현
6. **SettingsScreen** UI 구현
7. **E2E 테스트** 작성 및 실행

### 병렬/순차
- **병렬**: Home, Playlist, Settings (독립적)
- **순차**: 모든 UI -> E2E 테스트

---

## 완료 체크리스트

- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew test` 모든 테스트 통과
- [ ] `./gradlew connectedAndroidTest` E2E 테스트 통과
- [ ] `./gradlew lint` 경고/오류 없음
- [ ] Android Emulator 실행 확인 (스크린샷)
- [ ] 홈 화면 최근 학습 + 새 에피소드
- [ ] 플레이리스트 CRUD + 드래그 정렬
- [ ] 설정 모든 항목 동작
- [ ] E2E: 팟캐스트 학습 플로우
- [ ] E2E: 로컬 파일 학습 플로우
- [ ] E2E: 플레이리스트 플로우
- [ ] E2E: HARD 모드 플로우
- [ ] E2E: 백그라운드 재생

---

## 학습 내용 (Phase 7 완료 후)

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

---

## 전체 프로젝트 회고 (Phase 7 완료 후)

> 모든 Phase 완료 후 전체 프로젝트에 대한 회고를 작성하세요.

### 잘된 점
-

### 개선할 점
-

### 다음 프로젝트에 적용할 교훈
-
