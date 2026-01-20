package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Material 3 placeholder types
 */
enum class M3PlaceholderType {
    ALBUM,
    ARTIST,
    TRACK,
    PLAYLIST,
    GENERAL
}

/**
 * A Material 3 placeholder for media content
 */
@Composable
fun M3Placeholder(
    type: M3PlaceholderType,
    name: String? = null,
    modifier: Modifier = Modifier,
) {
    val (icon, shape, containerColor) = when (type) {
        M3PlaceholderType.ALBUM -> Triple(
            Icons.Filled.Album,
            RoundedCornerShape(8.dp),
            getColorForName(name, MaterialTheme.colorScheme.surfaceVariant)
        )
        M3PlaceholderType.ARTIST -> Triple(
            Icons.Filled.Person,
            CircleShape,
            getColorForName(name, MaterialTheme.colorScheme.surfaceVariant)
        )
        M3PlaceholderType.TRACK -> Triple(
            Icons.Filled.MusicNote,
            RoundedCornerShape(4.dp),
            getColorForName(name, MaterialTheme.colorScheme.surfaceVariant)
        )
        M3PlaceholderType.PLAYLIST -> Triple(
            Icons.AutoMirrored.Filled.QueueMusic,
            RoundedCornerShape(8.dp),
            getColorForName(name, MaterialTheme.colorScheme.surfaceVariant)
        )
        M3PlaceholderType.GENERAL -> Triple(
            Icons.Filled.AudioFile,
            RoundedCornerShape(8.dp),
            getColorForName(name, MaterialTheme.colorScheme.surfaceVariant)
        )
    }

    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = shape,
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Generate a consistent color based on the name
 */
@Composable
private fun getColorForName(name: String?, fallbackColor: Color): Color {
    if (name.isNullOrBlank()) {
        return fallbackColor
    }
    
    val seed = name.hashCode()
    val random = Random(seed)
    
    // Generate a color in the same palette as the theme
    val baseColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant
    )
    
    return baseColors[random.nextInt(baseColors.size)]
}

/**
 * Album art placeholder
 */
@Composable
fun AlbumPlaceholder(name: String? = null, modifier: Modifier = Modifier) {
    M3Placeholder(
        type = M3PlaceholderType.ALBUM,
        name = name,
        modifier = modifier
    )
}

/**
 * Artist image placeholder
 */
@Composable
fun ArtistPlaceholder(name: String? = null, modifier: Modifier = Modifier) {
    M3Placeholder(
        type = M3PlaceholderType.ARTIST,
        name = name,
        modifier = modifier
    )
}

/**
 * Track placeholder
 */
@Composable
fun TrackPlaceholder(name: String? = null, modifier: Modifier = Modifier) {
    M3Placeholder(
        type = M3PlaceholderType.TRACK,
        name = name,
        modifier = modifier
    )
}

/**
 * Playlist placeholder
 */
@Composable
fun PlaylistPlaceholder(name: String? = null, modifier: Modifier = Modifier) {
    M3Placeholder(
        type = M3PlaceholderType.PLAYLIST,
        name = name,
        modifier = modifier
    )
}
