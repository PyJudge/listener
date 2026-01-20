package com.listener.presentation.theme

import androidx.compose.ui.graphics.Color

// ===== Purple Accent Theme =====

// Primary Purple Palette (진하고 채도 높은 보라색)
val PurplePrimary = Color(0xFF7C4DFF)       // 밝은 보라 (메인 액센트)
val PurpleDark = Color(0xFF651FFF)          // 진한 보라
val PurpleDeep = Color(0xFF6200EA)          // 가장 진한 보라
val PurpleLight = Color(0xFFB388FF)         // 연한 보라 (하이라이트)
val PurpleAlpha = Color(0x337C4DFF)         // 투명 보라 (배경용)

// Surface Colors (다크 테마 최적화)
val SurfaceDark = Color(0xFF121212)         // 가장 어두운 배경
val SurfaceContainer = Color(0xFF1E1E2E)    // 카드 배경
val SurfaceElevated = Color(0xFF2D2D3F)     // 강조된 표면
val SurfaceHighlight = Color(0xFF363650)    // 선택된 항목 배경

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)         // 주요 텍스트
val TextSecondary = Color(0xFFB3B3B3)       // 보조 텍스트
val TextMuted = Color(0xFF666666)           // 비활성 텍스트

// State Colors
val ErrorRed = Color(0xFFCF6679)            // 에러/녹음 중
val SuccessGreen = Color(0xFF4CAF50)        // 완료/성공
val WarningAmber = Color(0xFFFFB74D)        // 경고

// Player Specific
val PlayerBackground = Color(0xFF0F0F1A)    // 플레이어 배경
val PlayerSurface = Color(0xFF1A1A2E)       // 플레이어 표면
val PlayerAccent = PurplePrimary            // 플레이어 액센트

// Chunk Highlight
val ChunkHighlight = PurplePrimary          // 현재 청크 강조 색상
val ChunkHighlightAlpha = PurpleAlpha       // 현재 청크 배경 (투명)
val ChunkActiveBg = SurfaceElevated         // 현재 청크 배경 (불투명)

// Progress/SeekBar
val ProgressTrack = Color(0xFF3D3D3D)       // 시크바 트랙 (비활성)
val ProgressActive = PurplePrimary          // 시크바 진행률

// ===== Legacy Colors (하위 호환성) =====

// Primary - Deep Blue → Purple로 대체
val Primary = PurpleDeep
val PrimaryLight = PurplePrimary
val PrimaryDark = PurpleDark
val OnPrimary = Color.White

// Secondary - Teal → Purple Light로 대체
val Secondary = PurpleLight
val SecondaryLight = PurpleLight
val SecondaryDark = PurpleDark
val OnSecondary = Color.White

// Background
val Background = Color(0xFFFAFAFA)
val BackgroundDark = SurfaceDark
val Surface = Color.White
val SurfaceDarkLegacy = SurfaceContainer

// Text (Legacy)
val TextPrimaryLegacy = Color(0xFF212121)
val TextSecondaryLegacy = Color(0xFF757575)
val TextPrimaryDark = TextPrimary
val TextSecondaryDark = TextSecondary

// Accents
val Success = SuccessGreen
val Warning = WarningAmber
val Error = ErrorRed
