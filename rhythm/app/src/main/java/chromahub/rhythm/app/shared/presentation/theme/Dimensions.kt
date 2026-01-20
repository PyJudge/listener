package chromahub.rhythm.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Material Design 3 Spacing System
 * Consistent spacing values following the 4dp grid system
 */
object Spacing {
    val none = 0.dp
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val huge = 48.dp
    val giant = 64.dp
}

/**
 * Music app specific dimensions
 */
object MusicDimensions {
    // Player components
    val miniPlayerHeight = 56.dp
    val fullPlayerHeight = 480.dp
    val albumCoverSize = 280.dp
    val albumCoverSizeMini = 48.dp
    val playerControlSize = 56.dp
    val playerControlSizeSmall = 40.dp
    
    // Navigation and layout
    val bottomNavigationHeight = 80.dp
    val topAppBarHeight = 64.dp
    val listItemHeight = 72.dp
    val listItemHeightCompact = 56.dp
    
    // Cards and containers
    val cardElevation = 2.dp
    val cardElevationPressed = 8.dp
    
    // Buttons and interactive elements
    val buttonHeight = 40.dp
    val buttonHeightLarge = 48.dp
    val fabSize = 56.dp
    val fabSizeSmall = 40.dp
}
