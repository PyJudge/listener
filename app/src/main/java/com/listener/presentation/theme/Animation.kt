package com.listener.presentation.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

object ListenerAnimation {
    val FastOutSlowIn = tween<Float>(300, easing = FastOutSlowInEasing)
    val SlowOutFastIn = tween<Float>(300, easing = LinearOutSlowInEasing)

    val ScreenEnter: EnterTransition = fadeIn(animationSpec = tween(300)) + slideInHorizontally(
        initialOffsetX = { it / 4 },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )

    val ScreenExit: ExitTransition = fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
        targetOffsetX = { -it / 4 },
        animationSpec = tween(200, easing = FastOutSlowInEasing)
    )

    val BottomSheetEnter: EnterTransition = fadeIn(animationSpec = tween(200)) + slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )

    val BottomSheetExit: ExitTransition = fadeOut(animationSpec = tween(150)) + slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(200, easing = FastOutSlowInEasing)
    )

    val FadeThrough: Pair<EnterTransition, ExitTransition> =
        fadeIn(animationSpec = tween(200)) to fadeOut(animationSpec = tween(200))
}

fun Modifier.pressAnimation(pressed: Boolean): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "press"
    )
    this.scale(scale)
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    graphicsLayer { translationX = translateAnim }
}
