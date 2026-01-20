# Phase 0: 프로젝트 초기화

## 비즈니스 로직

### 해결하는 문제
앱 개발의 기반이 되는 프로젝트 구조와 핵심 의존성을 설정합니다.
올바른 초기 설정이 없으면 이후 모든 Phase에서 문제가 발생합니다.

### 핵심 규칙
1. **의존성 버전 호환**: Kotlin <-> Compose <-> Hilt <-> Room 버전 충돌 없음
2. **KSP 사용**: KAPT 대신 KSP로 annotation processing
3. **테스트 환경**: Unit Test + Instrumented Test 모두 실행 가능

### 제약/예외
- minSdk: 23 (Media3 요구)
- targetSdk: 35
- Kotlin: 2.1.10
- Java: 17
- Gradle: 8.10+

---

## 설계

### 프로젝트 구조
```
listener/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/listener/
│   │   │   │   ├── ListenerApp.kt           # @HiltAndroidApp
│   │   │   │   ├── MainActivity.kt          # @AndroidEntryPoint
│   │   │   │   ├── core/
│   │   │   │   │   ├── di/                  # Hilt Modules
│   │   │   │   │   ├── util/
│   │   │   │   │   └── extension/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── db/
│   │   │   │   │   │   ├── cache/
│   │   │   │   │   │   └── preferences/
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── api/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   └── repository/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   ├── repository/
│   │   │   │   │   └── usecase/
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── navigation/
│   │   │   │   │   ├── home/
│   │   │   │   │   ├── playlist/
│   │   │   │   │   ├── podcast/
│   │   │   │   │   ├── media/
│   │   │   │   │   ├── settings/
│   │   │   │   │   ├── transcription/
│   │   │   │   │   ├── player/
│   │   │   │   │   └── components/
│   │   │   │   └── service/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                            # Unit Tests
│   │   └── androidTest/                     # Instrumented Tests
│   └── build.gradle.kts
├── build.gradle.kts                         # Root
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml                # Version Catalog
└── docs/phases/
```

### 핵심 의존성 (libs.versions.toml)
```toml
[versions]
kotlin = "2.1.10"
agp = "8.7.0"
composeBom = "2025.01.01"
hilt = "2.57.1"
room = "2.8.4"
media3 = "1.8.0"
retrofit = "3.0.0"
okhttp = "5.0.0"
coil = "2.6.0"
datastore = "1.1.1"
ksp = "2.1.10-1.0.31"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Media3
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### Hilt 모듈 설계
```
core/di/
├── AppModule.kt          # Context, Dispatchers, CoroutineScope
├── DatabaseModule.kt     # Room DB, DAOs
├── NetworkModule.kt      # Retrofit, OkHttp
└── RepositoryModule.kt   # Repository 바인딩
```

---

## 테스트 케이스

| 구분 | 테스트명 | 입력 | 기대값 |
|------|---------|------|--------|
| 정상 | 앱 빌드 성공 | `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| 정상 | Hilt 주입 동작 | Application 시작 | @HiltAndroidApp 초기화 |
| 정상 | Room DB 생성 | DB 접근 | Database 인스턴스 |
| 정상 | Retrofit 클라이언트 | API 인스턴스 요청 | Non-null 반환 |
| 정상 | Unit Test | `./gradlew test` | 테스트 통과 |
| 정상 | Instrumented Test | `./gradlew connectedAndroidTest` | 테스트 통과 |
| 엣지 | R8 빌드 | `./gradlew assembleRelease` | 난독화 성공 |

---

## 실행 계획

### TDD 순서
1. **프로젝트 생성** -> settings.gradle.kts, build.gradle.kts
2. **libs.versions.toml 설정** -> 빌드 확인
3. **Hilt 설정** -> @HiltAndroidApp, @AndroidEntryPoint 컴파일 확인
4. **Room 설정** -> 빈 Database 생성 확인
5. **Retrofit 설정** -> 빈 API 인터페이스 생성 확인
6. **테스트 환경** -> `./gradlew test`, `./gradlew connectedAndroidTest`

### 병렬/순차
- **순차**: 모든 작업 순차 (의존성 충돌 방지)

---

## 완료 체크리스트

- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew test` 성공
- [ ] `./gradlew connectedAndroidTest` 성공
- [ ] `./gradlew lint` 경고/오류 없음
- [ ] Android Emulator 실행 확인 (스크린샷)
- [ ] Hilt @Inject 동작 확인
- [ ] Room Database 인스턴스 생성 확인
- [ ] Retrofit API 인터페이스 생성 확인
- [ ] 패키지 구조 SPEC.md 명세와 일치

---

## 학습 내용 (Phase 0 완료 후)

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
