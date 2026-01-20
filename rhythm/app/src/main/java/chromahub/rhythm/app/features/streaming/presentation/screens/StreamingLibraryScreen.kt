package chromahub.rhythm.app.features.streaming.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Streaming Library Screen - Shows Rhythm logo with name
 */
@Composable
fun StreamingLibraryScreen(
    onNavigateToPlaylist: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    RhythmStreamingLogoScreen(onSwitchToLocal = {}, modifier = modifier)
}
