package com.listener.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light Color Scheme (보라색 테마)
private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PurpleLight,
    onPrimaryContainer = PurpleDeep,
    secondary = PurpleLight,
    onSecondary = PurpleDeep,
    secondaryContainer = PurpleAlpha,
    onSecondaryContainer = PurplePrimary,
    tertiary = PurpleDark,
    onTertiary = Color.White,
    background = Background,
    onBackground = TextPrimaryLegacy,
    surface = Surface,
    onSurface = TextPrimaryLegacy,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = TextSecondaryLegacy,
    error = ErrorRed,
    onError = Color.White
)

// Dark Color Scheme (보라색 테마)
private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PurpleDark,
    onPrimaryContainer = PurpleLight,
    secondary = PurpleLight,
    onSecondary = PurpleDeep,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = PurpleLight,
    tertiary = PurpleDark,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = SurfaceContainer,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    inverseSurface = Color.White,
    inverseOnSurface = SurfaceDark,
    inversePrimary = PurpleDeep
)

@Composable
fun ListenerTheme(
    darkTheme: Boolean = true,  // 항상 다크 테마 사용
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 항상 다크 테마 강제 적용
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false  // 다크 테마용
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ListenerTypography,
        content = content
    )
}

// Player-specific theme wrapper (항상 다크 테마)
@Composable
fun PlayerTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = PlayerBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme.copy(
            background = PlayerBackground,
            surface = PlayerSurface
        ),
        typography = ListenerTypography,
        content = content
    )
}
