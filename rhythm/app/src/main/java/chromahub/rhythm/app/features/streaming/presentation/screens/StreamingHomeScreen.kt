package chromahub.rhythm.app.features.streaming.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Streaming Home Screen - Shows Rhythm logo with name
 */
@Composable
fun StreamingHomeScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSwitchToLocalMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    RhythmStreamingLogoScreen(onSwitchToLocal = onSwitchToLocalMode, modifier = modifier)
}
