package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asAndroidPath
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs

/**
 * Material Design 3 Expressive Linear Progress Indicator
 * Features dynamic breathing animations, enhanced visual feedback, and smart color transitions
 */
@Composable
fun M3LinearLoader(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    showTrackGap: Boolean = true,
    showStopIndicator: Boolean = true,
    fourColor: Boolean = false,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        M3ExpressiveLinearIndicator(
            progress = progress,
            modifier = modifier,
            primaryColor = color,
//            trackColor = trackColor,
            fourColor = fourColor
        )
    } else {
        M3WaveProgressIndicator(
            progress = progress ?: 1f,
            modifier = modifier,
            waveColor = color,
            trackColor = trackColor,
            showLabel = false
        )
    }
}

/**
 * Material Design 3 Expressive Circular Progress Indicator
 * Features breathing animations, dynamic stroke variations, and enhanced visual impact
 */
@Composable
fun M3CircularLoader(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
    strokeWidth: Float = 4f,
    showTrackGap: Boolean = true,
    fourColor: Boolean = false,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        M3ExpressiveCircularIndicator(
            progress = progress,
            modifier = modifier,
            primaryColor = color,
//            trackColor = trackColor,
            baseStrokeWidth = strokeWidth,
            fourColor = fourColor
        )
    } else {
        M3CircularWaveProgressIndicator(
            progress = progress ?: 1f,
            modifier = modifier,
            colors = if (fourColor) {
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                listOf(color, color)
            },
//            trackColor = trackColor,
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Material Design 3 Expressive Linear Progress Indicator
 * Features breathing animations, smart color transitions, and enhanced visual feedback
 */
@Composable
fun M3ExpressiveLinearIndicator(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
//    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    fourColor: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "expressiveLinearAnimation")
    
    // Breathing animation for indeterminate state
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    
    // Shimmer effect for indeterminate
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    // Color pulsing for four-color mode
    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorPhase"
    )
    
    // Animate determinate progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progressAnimation"
    )
    
    val isIndeterminate = progress == null
    
    // Pre-calculate colors outside of Canvas context
    val fourColorPalette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .graphicsLayer {
                if (isIndeterminate) {
                    scaleY = breathingScale
                }
            },
//        color = trackColor,
        shadowElevation = if (isIndeterminate) 2.dp else 0.dp
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            if (isIndeterminate) {
                // Indeterminate shimmer effect
                val shimmerWidth = width * 0.3f
                val shimmerStart = (width + shimmerWidth) * shimmerOffset - shimmerWidth
                val shimmerEnd = shimmerStart + shimmerWidth
                
                if (fourColor) {
                    // Four-color cycling animation
                    val currentColorIndex = (colorPhase % 4).toInt()
                    val nextColorIndex = ((colorPhase % 4).toInt() + 1) % 4
                    val colorProgress = colorPhase % 1f
                    
                    val currentColor = fourColorPalette[currentColorIndex]
                    val nextColor = fourColorPalette[nextColorIndex]
                    val blendedColor = Color(
                        red = currentColor.red * (1f - colorProgress) + nextColor.red * colorProgress,
                        green = currentColor.green * (1f - colorProgress) + nextColor.green * colorProgress,
                        blue = currentColor.blue * (1f - colorProgress) + nextColor.blue * colorProgress,
                        alpha = currentColor.alpha
                    )
                    
                    val gradient = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            blendedColor.copy(alpha = 0.3f),
                            blendedColor,
                            blendedColor.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        startX = shimmerStart,
                        endX = shimmerEnd
                    )
                    
                    drawRect(
                        brush = gradient,
                        size = size
                    )
                } else {
                    val gradient = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.3f),
                            primaryColor,
                            primaryColor.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        startX = shimmerStart,
                        endX = shimmerEnd
                    )
                    
                    drawRect(
                        brush = gradient,
                        size = size
                    )
                }
            } else {
                // Determinate progress with gradient
                val progressWidth = width * animatedProgress
                
                if (progressWidth > 0) {
                    val gradient = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.8f),
                            primaryColor,
                            primaryColor.copy(alpha = 0.9f)
                        ),
                        startX = 0f,
                        endX = progressWidth
                    )
                    
                    drawRect(
                        brush = gradient,
                        size = androidx.compose.ui.geometry.Size(progressWidth, height)
                    )
                }
            }
        }
    }
}

/**
 * Material Design 3 Expressive Circular Progress Indicator
 * Features breathing animations, dynamic stroke variations, and enhanced visual impact
 */
@Composable
fun M3ExpressiveCircularIndicator(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
//    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    baseStrokeWidth: Float = 4f,
    fourColor: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "expressiveCircularAnimation")
    
    // Rotation for indeterminate
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Stroke width breathing
    val strokeMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strokeBreathing"
    )
    
    // Arc length pulsing for indeterminate
    val arcLength by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arcLength"
    )
    
    // Color cycling for four-color mode
    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorPhase"
    )
    
    // Animate determinate progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progressAnimation"
    )
    
    val isIndeterminate = progress == null
    
    // Pre-calculate colors outside of Canvas context
    val fourColorPalette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error
    )
    
    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    if (isIndeterminate) {
                        rotationZ = rotation
                    }
                }
        ) {
            val strokeWidth = baseStrokeWidth.dp.toPx() * if (isIndeterminate) strokeMultiplier else 1f
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw track
//            drawCircle(
//                color = trackColor,
//                radius = radius,
//                center = center,
//                style = Stroke(width = strokeWidth * 0.6f, cap = StrokeCap.Round)
//            )
            
            if (isIndeterminate) {
                // Indeterminate mode
                val sweepAngle = arcLength
                val startAngle = -90f
                
                val drawColor = if (fourColor) {
                    val currentColorIndex = (colorPhase % 4).toInt()
                    val nextColorIndex = ((colorPhase % 4).toInt() + 1) % 4
                    val colorProgress = colorPhase % 1f
                    
                    val currentColor = fourColorPalette[currentColorIndex]
                    val nextColor = fourColorPalette[nextColorIndex]
                    Color(
                        red = currentColor.red * (1f - colorProgress) + nextColor.red * colorProgress,
                        green = currentColor.green * (1f - colorProgress) + nextColor.green * colorProgress,
                        blue = currentColor.blue * (1f - colorProgress) + nextColor.blue * colorProgress,
                        alpha = currentColor.alpha
                    )
                } else {
                    primaryColor
                }
                
                drawArc(
                    color = drawColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            } else {
                // Determinate mode
                val sweepAngle = animatedProgress * 360f
                
                if (sweepAngle > 0f) {
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }
            }
        }
    }
}
/**
 * Material Design 3 four-color circular progress indicator
 * Uses four colors from the Material 3 color scheme for indeterminate progress
 */
@Composable
fun M3FourColorCircularLoader(
    modifier: Modifier = Modifier,
    strokeWidth: Float = 4f,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        M3ExpressiveCircularIndicator(
            progress = null, // Indeterminate
            modifier = modifier,
            primaryColor = MaterialTheme.colorScheme.primary,
//            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            baseStrokeWidth = strokeWidth,
            fourColor = true
        )
    } else {
        M3CircularWaveProgressIndicator(
            progress = 1f,
            modifier = modifier,
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Material Design 3 four-color linear progress indicator
 * Uses four colors from the Material 3 color scheme for indeterminate progress
 */
@Composable
fun M3FourColorLinearLoader(
    modifier: Modifier = Modifier,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        M3ExpressiveLinearIndicator(
            progress = null, // Indeterminate
            modifier = modifier,
            primaryColor = MaterialTheme.colorScheme.primary,
//            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            fourColor = true
        )
    } else {
        M3WaveProgressIndicator(
            progress = 1f,
            modifier = modifier,
            waveColor = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            showLabel = false
        )
    }
}

/**
 * Material Design 3 linear progress indicator with buffer
 * Shows both determinate progress and buffer progress with expressive animations
 */
@Composable
fun M3BufferedLinearLoader(
    progress: Float,
    buffer: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    bufferColor: Color = color.copy(alpha = 0.4f),
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        M3ExpressiveBufferedIndicator(
            progress = progress,
            buffer = buffer,
            modifier = modifier,
            primaryColor = color,
            trackColor = trackColor,
            bufferColor = bufferColor
        )
    } else {
        M3WaveProgressIndicator(
            progress = progress,
            modifier = modifier,
            waveColor = color,
            trackColor = trackColor,
            showLabel = false
        )
    }
}

/**
 * Material Design 3 Expressive Buffered Linear Indicator
 * Shows progress and buffer with sophisticated animations
 */
@Composable
fun M3ExpressiveBufferedIndicator(
    progress: Float,
    buffer: Float,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    bufferColor: Color = primaryColor.copy(alpha = 0.4f)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progressAnimation"
    )
    
    val animatedBuffer by animateFloatAsState(
        targetValue = buffer,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bufferAnimation"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = trackColor,
        shadowElevation = 1.dp
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Draw buffer
            val bufferWidth = width * animatedBuffer.coerceIn(0f, 1f)
            if (bufferWidth > 0) {
                drawRect(
                    color = bufferColor,
                    size = androidx.compose.ui.geometry.Size(bufferWidth, height)
                )
            }
            
            // Draw progress with gradient
            val progressWidth = width * animatedProgress.coerceIn(0f, 1f)
            if (progressWidth > 0) {
                val gradient = Brush.horizontalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.8f),
                        primaryColor,
                        primaryColor.copy(alpha = 0.9f)
                    ),
                    startX = 0f,
                    endX = progressWidth
                )
                
                drawRect(
                    brush = gradient,
                    size = androidx.compose.ui.geometry.Size(progressWidth, height)
                )
            }
        }
    }
}

/**
 * Enhanced wave slider with Material Design 3 styling
 * Improved from the original WaveSlider with better animations and styling
 */
@Composable
fun M3WaveProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showLabel: Boolean = false
) {
    // Animation for wave movement
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnimation")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing), // Slightly slower for smoother effect
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val animatedWaveAmplitude by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveAmplitude"
    )

    // Animate progress changes
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400),
        label = "progressAnimation"
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 16.dp)
        ) {
            // Draw the wave
            Canvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(24.dp)
                    .drawBehind {
                        if (animatedProgress > 0f) {
                            val shadowColor = Color.Black.copy(alpha = 0.2f)
                            val shadowRadius = 4.dp.toPx()
                            val shadowOffset = Offset(0f, 2.dp.toPx())

                            drawIntoCanvas { canvas ->
                                val paint = Paint().apply {
                                    color = shadowColor.toArgb()
                                    maskFilter = BlurMaskFilter(shadowRadius, BlurMaskFilter.Blur.NORMAL)
                                }
                                val path = Path()
                                val width = size.width
                                val height = size.height
                                val centerY = height / 2
                                val progressWidth = width * animatedProgress
                                val amplitude = 4.dp.toPx() * animatedWaveAmplitude
                                val period = 25.dp.toPx()

                                path.moveTo(0f, centerY + shadowOffset.y)
                                var x = 0f
                                while (x <= progressWidth) {
                                    val waveY = centerY + amplitude * sin((x / period) * 2 * PI.toFloat() + animatedOffset) + shadowOffset.y
                                    path.lineTo(x, waveY)
                                    x += 1.5f
                                }
                                canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val centerY = height / 2

                // Draw background track
                drawLine(
                    color = trackColor,
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                if (animatedProgress > 0f) {
                    // Create gradient for wave
                    val gradient = Brush.horizontalGradient(
                        colors = listOf(
                            waveColor.copy(alpha = 0.7f),
                            waveColor
                        )
                    )

                    // Draw wavy progress line
                    val progressWidth = width * animatedProgress
                    val path = Path()
                    val amplitude = 4.dp.toPx() * animatedWaveAmplitude // Height of the wave, animated
                    val period = 25.dp.toPx() // Length of one wave cycle

                    // Move to the start point
                    path.moveTo(0f, centerY)

                    // Create the wavy path
                    var x = 0f
                    while (x <= progressWidth) {
                        val waveY = centerY + amplitude * sin((x / period) * 2 * PI.toFloat() + animatedOffset)
                        path.lineTo(x, waveY)
                        x += 1.5f // Smaller increment for smoother curve
                    }

                    // Draw the wavy path
                    drawPath(
                        path = path,
                        brush = gradient,
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.cornerPathEffect(8f)
                        )
                    )
                }
            }
        }

        if (showLabel) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Material Design 3 compliant circular wavy progress indicator
 */
@Composable
fun M3CircularWaveProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary), // Ensure default has at least two colors
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Float = 4f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circularWaveAnimation")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing), // Slightly slower for smoother effect
            repeatMode = RepeatMode.Restart
        ),
        label = "circularWaveOffset"
    )

    val animatedWaveAmplitude by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circularWaveAmplitude"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400),
        label = "circularProgressAnimation"
    )

    // Ensure colors list has at least two elements for sweepGradient
    val safeColors = if (colors.size < 2) {
        // Fallback to a default gradient if not enough colors are provided
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
    } else {
        colors
    }

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(48.dp)
                .drawBehind {
                    if (animatedProgress > 0f) {
                        val shadowColor = Color.Black.copy(alpha = 0.2f)
                        val shadowRadius = 4.dp.toPx()
                        val shadowOffset = Offset(2.dp.toPx(), 2.dp.toPx()) // Slight offset for circular shadow

                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                color = shadowColor.toArgb()
                                maskFilter = BlurMaskFilter(shadowRadius, BlurMaskFilter.Blur.NORMAL)
                            }
                            val path = Path()
                            val radius = size.minDimension / 2 - strokeWidth.dp.toPx() / 2
                            val center = Offset(size.width / 2, size.height / 2)

                            val startAngle = -90f
                            val sweepAngle = animatedProgress * 360f
                            val amplitude = 2.dp.toPx() * animatedWaveAmplitude

                            for (angle in 0..sweepAngle.toInt()) {
                                val currentAngleRad = Math.toRadians(startAngle + angle.toDouble()).toFloat()
                                val waveOffset = amplitude * sin(currentAngleRad * 5 + animatedOffset)

                                val x = center.x + (radius + waveOffset) * cos(currentAngleRad) + shadowOffset.x
                                val y = center.y + (radius + waveOffset) * sin(currentAngleRad) + shadowOffset.y

                                if (angle == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                            canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
                        }
                    }
                }
        ) {
            val radius = size.minDimension / 2 - strokeWidth.dp.toPx() / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Draw background track
            drawCircle(
                color = trackColor,
                radius = radius,
                style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round)
            )

            if (animatedProgress > 0f) {
                val gradient = Brush.sweepGradient(
                    colors = safeColors, // Use safeColors here
                    center = center
                )

                val path = Path()
                val startAngle = -90f // Start from top
                val sweepAngle = animatedProgress * 360f
                val amplitude = 2.dp.toPx() * animatedWaveAmplitude // Height of the wave, animated

                for (angle in 0..sweepAngle.toInt()) {
                    val currentAngleRad = Math.toRadians(startAngle + angle.toDouble()).toFloat()
                    val waveOffset = amplitude * sin(currentAngleRad * 6 + animatedOffset) // 6 waves around the circle

                    val x = center.x + (radius + waveOffset) * cos(currentAngleRad)
                    val y = center.y + (radius + waveOffset) * sin(currentAngleRad)

                    if (angle == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    brush = gradient,
                    style = Stroke(
                        width = strokeWidth.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.cornerPathEffect(8f)
                    )
                )
            }
        }
    }
}

/**
 * Pulsating circular loader that follows Material Design 3 expressive guidelines
 */
@Composable
fun M3PulseLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        M3ExpressivePulseIndicator(
            modifier = modifier,
            primaryColor = color
        )
    } else {
        M3CircularWaveProgressIndicator(
            progress = 1f,
            modifier = modifier,
            colors = listOf(color, color),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 4f
        )
    }
}

/**
 * Material Design 3 Expressive Pulse Indicator
 * Features sophisticated pulsing animations with breathing effects
 */
@Composable
fun M3ExpressivePulseIndicator(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnimation")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // Multiple pulse rings for enhanced effect
        repeat(3) { index ->
            val delay = index * 500
            val ringScale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, delayMillis = delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ringScale$index"
            )
            
            val ringAlpha by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, delayMillis = delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ringAlpha$index"
            )
            
            Canvas(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
            ) {
                drawCircle(
                    color = primaryColor,
                    radius = size.minDimension / 4,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        
        // Central pulsing dot
        Canvas(
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
        ) {
            drawCircle(
                color = primaryColor,
                radius = size.minDimension / 2
            )
        }
    }
}

/**
 * Material Design 3 segmented progress indicator
 * Shows progress as a series of segments that fill up
 */
@Composable
fun M3SegmentedLoader(
    progress: Float,
    modifier: Modifier = Modifier,
    segmentCount: Int = 5, // This parameter will be ignored for wavy loader
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showLabel: Boolean = false
) {
    M3WaveProgressIndicator(
        progress = progress,
        modifier = modifier,
        waveColor = activeColor, // Use activeColor as waveColor
        trackColor = inactiveColor, // Use inactiveColor as trackColor
        showLabel = showLabel
    )
}

/**
 * Material Design 3 dot progress indicator
 * Shows progress as a series of animated dots
 */
@Composable
fun M3DotLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotSize: Float = 8f, // This parameter will be ignored for wavy loader
    dotCount: Int = 3 // This parameter will be ignored for wavy loader
) {
    // Dot animation will be replaced by circular wave animation
    M3CircularWaveProgressIndicator(
        progress = 1f, // Indeterminate, so full wave
        modifier = modifier,
        colors = listOf(color, color), // Ensure at least two colors for sweepGradient
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeWidth = 4f
    )
}

/**
 * Material Design 3 branded loader with surface elevation
 * Shows a circular progress indicator on an elevated surface
 */
@Composable
fun M3BrandedLoader(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    surfaceColor: Color = MaterialTheme.colorScheme.surface, // This parameter will be ignored for wavy loader
    showLabel: Boolean = false
) {
    // Branded loader will be replaced by circular wave animation
    M3CircularWaveProgressIndicator(
        progress = progress ?: 1f, // Use 1f for indeterminate
        modifier = modifier,
        colors = listOf(color, color), // Ensure at least two colors for sweepGradient
        trackColor = trackColor,
        strokeWidth = 5f // Use a slightly thicker stroke for branded loader
    )
}

/**
 * Material Design 3 step progress indicator
 * Shows progress as a series of steps with the current step highlighted
 */
@Composable
fun M3StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    completedColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    // Step progress will be replaced by linear wave animation
    val progress = if (totalSteps > 0) currentStep.toFloat() / totalSteps.toFloat() else 0f
    M3WaveProgressIndicator(
        progress = progress,
        modifier = modifier,
        waveColor = activeColor,
        trackColor = inactiveColor,
        showLabel = true // Show label for step progress
    )
}

/**
 * Simple circular loading indicator for quick actions like blacklisting/whitelisting
 * Enhanced with Material 3 expressive design
 */
@Composable
fun SimpleCircularLoader(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 16.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        SimpleExpressiveCircularLoader(
            modifier = modifier,
            size = size,
            strokeWidth = strokeWidth,
            color = color
        )
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "simpleCircularLoader")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        Canvas(
            modifier = modifier
                .size(size)
                .graphicsLayer { rotationZ = rotation }
        ) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (size.toPx() - strokeWidthPx) / 2
            val center = androidx.compose.ui.geometry.Offset(size.toPx() / 2, size.toPx() / 2)
            
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }
    }
}

/**
 * Simple Expressive Circular Loader with breathing and rotation
 */
@Composable
fun SimpleExpressiveCircularLoader(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 16.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "simpleExpressiveLoader")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val strokeMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strokeBreathing"
    )
    
    val arcLength by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arcLength"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { 
                rotationZ = rotation
            }
    ) {
        val dynamicStrokeWidth = strokeWidth.toPx() * strokeMultiplier
        val radius = (size.toPx() - dynamicStrokeWidth) / 2
        val center = androidx.compose.ui.geometry.Offset(size.toPx() / 2, size.toPx() / 2)
        
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = arcLength,
            useCenter = false,
            style = Stroke(width = dynamicStrokeWidth, cap = StrokeCap.Round),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
    }
}
