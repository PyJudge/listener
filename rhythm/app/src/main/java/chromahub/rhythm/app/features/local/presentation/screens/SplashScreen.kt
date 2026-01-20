package chromahub.rhythm.app.features.local.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.ui.theme.festive.FestiveConfig
import chromahub.rhythm.app.ui.theme.festive.FestiveThemeEngine
import chromahub.rhythm.app.ui.theme.festive.FestiveThemeType
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    musicViewModel: MusicViewModel,
    onMediaScanComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }

    // Festive theme configuration
    val festiveEnabled by appSettings.festiveThemeEnabled.collectAsState()
    val festiveTypeString by appSettings.festiveThemeType.collectAsState()
    val festiveAutoDetect by appSettings.festiveThemeAutoDetect.collectAsState()

    val festiveConfig = remember(festiveEnabled, festiveTypeString, festiveAutoDetect) {
        FestiveConfig(
            enabled = festiveEnabled,
            type = try {
                FestiveThemeType.valueOf(festiveTypeString)
            } catch (e: IllegalArgumentException) {
                FestiveThemeType.NONE
            },
            autoDetect = festiveAutoDetect
        )
    }
    val activeFestiveTheme = FestiveThemeEngine.getActiveFestiveTheme(festiveConfig)

    // Animation states
    var showContent by remember { mutableStateOf(false) }
    var showLoader by remember { mutableStateOf(false) }
    var exitSplash by remember { mutableStateOf(false) }

    // Animatable values
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.8f) }
    val loaderOffsetY = remember { Animatable(100f) } // Start below screen
    val loaderAlpha = remember { Animatable(0f) }
    val exitScale = remember { Animatable(1f) }
    val exitAlpha = remember { Animatable(1f) }

    // Subtle pulse animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    // Monitor media scanning completion
    val isInitialized by musicViewModel.isInitialized.collectAsState()

    // Entrance animation
    LaunchedEffect(Unit) {
        delay(200) // Brief delay

        // Show content with smooth fade and scale
        showContent = true
        launch {
            contentAlpha.animateTo(1f, animationSpec = tween(800, easing = EaseOut))
        }
        launch {
            contentScale.animateTo(1f, animationSpec = spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            ))
        }

        delay(600) // Let content settle

        // Show loader sliding up from bottom
        showLoader = true
        launch {
            loaderOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                )
            )
        }
        loaderAlpha.animateTo(1f, animationSpec = tween(400))
    }

    // Exit animation when ready
    LaunchedEffect(isInitialized) {
        if (isInitialized && !exitSplash) {
            delay(1500) // Brief hold
            exitSplash = true

            launch {
                exitScale.animateTo(0.9f, animationSpec = tween(400))
            }
            launch {
                exitAlpha.animateTo(0f, animationSpec = tween(400))
            }

            delay(100)
            onMediaScanComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer {
                scaleX = exitScale.value
                scaleY = exitScale.value
                alpha = exitAlpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer {
                    alpha = contentAlpha.value
                    scaleX = contentScale.value
                    scaleY = contentScale.value
                }
                .padding(horizontal = 32.dp)
        ) {
            // Logo and App name on same row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.rhythm_splash_logo),
                    contentDescription = "Rhythm Logo",
                    modifier = Modifier
                        .size(100.dp)
                )

                // App name
                Text(
                    text = context.getString(R.string.common_rhythm),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Tagline
            Text(
                text = context.getString(R.string.splash_tagline),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            // Festive greeting
            if (festiveEnabled && activeFestiveTheme != FestiveThemeType.NONE) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = when (activeFestiveTheme) {
                        FestiveThemeType.CHRISTMAS -> context.getString(R.string.festive_greeting_christmas)
                        FestiveThemeType.NEW_YEAR -> context.getString(R.string.festive_greeting_new_year)
                        FestiveThemeType.HALLOWEEN -> context.getString(R.string.festive_greeting_halloween)
                        FestiveThemeType.VALENTINES -> context.getString(R.string.festive_greeting_valentines)
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Loading indicator at bottom, sliding up
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp)
                .graphicsLayer {
                    translationY = loaderOffsetY.value
                    alpha = loaderAlpha.value
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            if (showLoader) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = context.getString(R.string.splash_loading),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )

                    // Modern loading dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val dotAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        800,
                                        delayMillis = index * 200,
                                        easing = EaseInOut
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dotAlpha$index"
                            )

                            Surface(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(dotAlpha),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {}
                        }
                    }
                }
            }
        }
    }
}
