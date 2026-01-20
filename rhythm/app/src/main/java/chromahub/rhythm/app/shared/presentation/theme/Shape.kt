package chromahub.rhythm.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material Design 3 Shape System
// Defines the corner radius values for different component categories
val Shapes = Shapes(
    // Extra small components (chips, etc.)
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small components (buttons, cards, etc.)
    small = RoundedCornerShape(8.dp),
    
    // Medium components (extended FABs, larger cards, etc.)
    medium = RoundedCornerShape(12.dp),
    
    // Large components (sheets, dialogs, etc.)
    large = RoundedCornerShape(16.dp),
    
    // Extra large components (large modals, etc.)
    extraLarge = RoundedCornerShape(28.dp)
)

// Custom shapes for music app specific components
object MusicShapes {
    val PlayerCard = RoundedCornerShape(20.dp)
    val MiniPlayer = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val AlbumCover = RoundedCornerShape(12.dp)
    val PlaylistCard = RoundedCornerShape(16.dp)
    val SearchBar = RoundedCornerShape(28.dp)
}
