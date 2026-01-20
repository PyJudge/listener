# Technology Stack

This document details the technical architecture and libraries used in Rhythm Music Player.

## 🏗️ Core Technologies

### UI & Design

| Technology | Version | Purpose |
|:---|:---:|:---|
| **Jetpack Compose** | Latest | Modern declarative UI toolkit for Android |
| **Material 3** | Latest | Material Design components and theming system |
| **Material Icons Extended** | Latest | Comprehensive icon library |
| **AndroidX Palette** | Latest | Dynamic color extraction from images |

### Audio & Media

| Technology | Version | Purpose |
|:---|:---:|:---|
| **Media3 ExoPlayer** | 1.9.0 | Professional-grade media playback engine |
| **MediaStore API** | Latest | Android media content provider |
| **AudioFocus** | Latest | Audio focus management for calls/notifications |

### Widgets

| Technology | Version | Purpose |
|:---|:---:|:---|
| **Glance** | 1.1.1 | Modern reactive widgets with Material 3 design |
| **RemoteViews** | Latest | Legacy widget support |
| **WorkManager** | Latest | Background widget updates |

### Programming Language

| Technology | Version | Purpose |
|:---|:---:|:---|
| **Kotlin** | Latest | 100% Kotlin codebase |
| **Kotlin Coroutines** | Latest | Asynchronous programming |
| **Kotlin Flow** | Latest | Reactive streams and state management |

## 🎨 Architecture

### Design Pattern

**MVVM (Model-View-ViewModel) + Clean Architecture**

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│  ┌──────────────────────────────────────────┐   │
│  │   Composables (Screens & Components)     │   │
│  └──────────────────────────────────────────┘   │
│                      ↕                           │
│  ┌──────────────────────────────────────────┐   │
│  │         ViewModels (State)               │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│                 Domain Layer                     │
│  ┌──────────────────────────────────────────┐   │
│  │      Use Cases (Business Logic)          │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │     Repository Interfaces                │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │      Models (Data Entities)              │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│                  Data Layer                      │
│  ┌──────────────────────────────────────────┐   │
│  │    Repository Implementations            │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │  Data Sources (Local & Remote)           │   │
│  │  • MediaStore                            │   │
│  │  • LRCLib API                            │   │
│  │  • Deezer API                            │   │
│  │  • Local Storage                         │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### Project Structure

```
app/
├── ui/                          # UI Layer
│   ├── screens/                 # Screen Composables
│   │   ├── home/               # Home screen
│   │   ├── player/             # Player screen
│   │   ├── library/            # Library screen
│   │   ├── search/             # Search screen
│   │   └── settings/           # Settings screen
│   ├── components/             # Reusable UI components
│   ├── navigation/             # Navigation graph
│   ├── theme/                  # Material 3 theming
│   └── viewmodels/             # ViewModels
│
├── domain/                      # Domain Layer
│   ├── models/                 # Data models
│   │   ├── Song.kt
│   │   ├── Album.kt
│   │   ├── Artist.kt
│   │   └── Playlist.kt
│   ├── repository/             # Repository interfaces
│   └── usecases/               # Business logic use cases
│
├── data/                        # Data Layer
│   ├── local/                  # Local data sources
│   │   ├── mediastore/        # MediaStore integration
│   │   └── preferences/       # SharedPreferences/DataStore
│   ├── remote/                 # Remote data sources
│   │   ├── lrclib/            # LRCLib API
│   │   └── deezer/            # Deezer API
│   └── repository/             # Repository implementations
│
├── services/                    # Background Services
│   ├── MusicService.kt         # Playback service
│   └── MediaNotification.kt    # Notification handler
│
├── widgets/                     # Home Screen Widgets
│   ├── glance/                 # Modern Glance widgets
│   └── legacy/                 # RemoteViews widgets
│
└── utils/                       # Utility classes
    ├── Extensions.kt
    ├── Constants.kt
    └── Helpers.kt
```

## 📦 Libraries & Dependencies

### AndroidX & Jetpack

```kotlin
// Core
androidx.core:core-ktx
androidx.lifecycle:lifecycle-runtime-ktx
androidx.lifecycle:lifecycle-viewmodel-compose

// Compose
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.compose.material:material-icons-extended
androidx.compose.ui:ui-tooling

// Navigation
androidx.navigation:navigation-compose

// Media
androidx.media3:media3-exoplayer
androidx.media3:media3-session
androidx.media3:media3-ui

// Widgets
androidx.glance:glance-appwidget
androidx.work:work-runtime-ktx

// Other
androidx.palette:palette-ktx
```

### Networking

```kotlin
// HTTP Client
com.squareup.retrofit2:retrofit
com.squareup.retrofit2:converter-gson
com.squareup.okhttp3:okhttp
com.squareup.okhttp3:logging-interceptor

// JSON
com.google.code.gson:gson
```

### Image Loading

```kotlin
// Coil for Compose
io.coil-kt:coil-compose
```

### Utilities

```kotlin
// Permissions
com.google.accompanist:accompanist-permissions

// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-android
```

## 🔄 State Management

### StateFlow & Compose State

Rhythm uses Kotlin Flow and Compose state for reactive UI updates:

```kotlin
// ViewModel
class PlayerViewModel : ViewModel() {
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    // Business logic...
}

// Composable
@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    
    // UI updates automatically when state changes
}
```

### Repository Pattern

Data access abstracted through repositories:

```kotlin
interface MusicRepository {
    fun getAllSongs(): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
}

class MusicRepositoryImpl(
    private val mediaStore: MediaStoreDataSource
) : MusicRepository {
    override fun getAllSongs(): Flow<List<Song>> = 
        mediaStore.queryAllSongs()
}
```

## 🎵 Audio Playback Architecture

### ExoPlayer Integration

```
┌─────────────────────────────────────┐
│         MusicService                │
│   (Foreground Service)              │
│                                     │
│  ┌──────────────────────────────┐  │
│  │      ExoPlayer               │  │
│  │  • Media3 ExoPlayer 1.9.0    │  │
│  │  • Gapless playback          │  │
│  │  • Audio focus handling      │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   MediaSession               │  │
│  │  • Playback state            │  │
│  │  • Queue management          │  │
│  │  • Media buttons             │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   MediaNotification          │  │
│  │  • Playback controls         │  │
│  │  • Album art                 │  │
│  │  • Metadata display          │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
            ↕
┌─────────────────────────────────────┐
│     UI (Player Composables)         │
│  • Observe playback state           │
│  • Send playback commands           │
│  • Display metadata                 │
└─────────────────────────────────────┘
```

## 📱 Widget Architecture

### Glance Widgets (Modern)

```kotlin
class RhythmWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            RhythmWidgetContent()
        }
    }
}

@Composable
fun RhythmWidgetContent() {
    val playerState = currentState<PlayerState>()
    
    // Material 3 widget UI
    MaterialTheme {
        // Widget content...
    }
}
```

### Background Updates

```kotlin
class WidgetUpdateWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        // Update widget data
        GlanceAppWidgetManager(context)
            .getGlanceIds(RhythmWidget::class.java)
            .forEach { glanceId ->
                RhythmWidget().update(context, glanceId)
            }
        return Result.success()
    }
}
```

## 🔧 Build System

### Gradle Kotlin DSL

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "chromahub.rhythm.app"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "chromahub.rhythm.app"
        minSdk = 26
        targetSdk = 36
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}
```

### Version Catalog

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "1.9.22"
compose = "1.6.0"
material3 = "1.2.0"
exoplayer = "1.9.0"

[libraries]
compose-ui = { module = "androidx.compose.ui:ui", version.ref = "compose" }
material3 = { module = "androidx.compose.material3:material3", version.ref = "material3" }
media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "exoplayer" }
```

## 🧪 Testing (Planned)

### Unit Tests
- ViewModel logic testing
- Repository testing
- Use case testing

### UI Tests
- Compose UI testing
- Navigation testing
- Integration testing

```kotlin
// Example unit test
class PlayerViewModelTest {
    @Test
    fun `test play pause toggle`() = runTest {
        val viewModel = PlayerViewModel()
        viewModel.playPause()
        assert(viewModel.playerState.value is PlayerState.Playing)
    }
}
```

## 🔐 Security & Privacy

- **No Analytics**: Zero tracking code
- **Local Storage**: All data stored on device
- **Minimal Permissions**: Only essential permissions
- **FOSS Compliance**: Fully open source
- **Reproducible Builds**: Consistent APK generation

## 📊 Performance Optimizations

- **Lazy Loading**: Load music library on demand
- **Image Caching**: Coil caches album art efficiently
- **Background Processing**: WorkManager for non-urgent tasks
- **Compose Optimization**: Remember, derivedStateOf, keys
- **ExoPlayer Buffering**: Optimized buffer sizes

## 🔄 CI/CD (Planned)

- GitHub Actions for builds
- Automated testing
- Release automation
- Code quality checks

---

**Want to contribute?** Check the [Contributing Guide](https://github.com/cromaguy/Rhythm/wiki/Contributing)!
