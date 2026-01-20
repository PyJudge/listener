package chromahub.rhythm.app.features.streaming.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Streaming Search Screen - Shows Rhythm logo with name
 */
@Composable
fun StreamingSearchScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    RhythmStreamingLogoScreen(onSwitchToLocal = {}, modifier = modifier)
}
