package chromahub.rhythm.app.features.streaming.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Streaming Coming Soon Screen - Shows Rhythm logo with name
 */
@Composable
fun StreamingComingSoonScreen(
    onSwitchToLocalMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    RhythmStreamingLogoScreen(onSwitchToLocal = onSwitchToLocalMode, modifier = modifier)
}
