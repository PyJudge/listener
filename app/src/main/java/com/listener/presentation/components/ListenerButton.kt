package com.listener.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.listener.presentation.theme.ListenerTheme

@Composable
fun ListenerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.Primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val colors = when (style) {
        ButtonStyle.Primary -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        ButtonStyle.Secondary -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        ButtonStyle.Outline -> ButtonDefaults.outlinedButtonColors()
    }

    when (style) {
        ButtonStyle.Outline -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.scale(scale),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                interactionSource = interactionSource
            ) {
                ButtonContent(icon = icon, text = text)
            }
        }
        else -> {
            Button(
                onClick = onClick,
                modifier = modifier.scale(scale),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = colors,
                interactionSource = interactionSource
            ) {
                ButtonContent(icon = icon, text = text)
            }
        }
    }
}

@Composable
private fun ButtonContent(icon: ImageVector?, text: String) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text(text = text)
}

enum class ButtonStyle {
    Primary, Secondary, Outline
}

@Preview(showBackground = true)
@Composable
private fun ListenerButtonPreview() {
    ListenerTheme {
        ListenerButton(
            text = "Play Now",
            onClick = {},
            icon = Icons.Default.PlayArrow
        )
    }
}
