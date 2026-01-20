package chromahub.rhythm.app.util

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Data class to store extracted colors from album artwork
 * Stores full Material 3 color scheme for both light and dark themes
 */
data class ExtractedColors(
    // Primary colors
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    
    // Secondary colors
    val secondary: Int,
    val onSecondary: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,
    
    // Tertiary colors
    val tertiary: Int,
    val onTertiary: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,
    
    // Surface colors
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int
)

/**
 * Utility object for extracting color palettes from album artwork using AndroidX Palette library
 */
object ColorExtractor {
    
    private const val TAG = "ColorExtractor"
    private val gson = Gson()
    
    /**
     * Extract a Material 3 color palette from album artwork bitmap
     * Returns null if extraction fails
     */
    suspend fun extractColorsFromBitmap(bitmap: Bitmap?): ExtractedColors? = withContext(Dispatchers.Default) {
        try {
            if (bitmap == null) {
                Log.w(TAG, "Bitmap is null, cannot extract colors")
                return@withContext null
            }
            
            // Generate palette with more samples for better accuracy
            val palette = Palette.from(bitmap)
                .maximumColorCount(16)
                .generate()
            
            // Extract base colors with fallbacks
            val vibrantSwatch = palette.vibrantSwatch
            val lightVibrantSwatch = palette.lightVibrantSwatch
            val darkVibrantSwatch = palette.darkVibrantSwatch
            val mutedSwatch = palette.mutedSwatch
            val lightMutedSwatch = palette.lightMutedSwatch
            val darkMutedSwatch = palette.darkMutedSwatch
            val dominantSwatch = palette.dominantSwatch
            
            // PRIMARY: Use vibrant color with good saturation
            val primaryBase = vibrantSwatch?.rgb 
                ?: darkVibrantSwatch?.rgb 
                ?: dominantSwatch?.rgb
                ?: 0xFF6750A4.toInt() // Material default purple
            
            // Ensure primary has good saturation and appropriate brightness
            val primary = ensureMinimumSaturation(primaryBase, 0.7f) // Increased saturation for more vibrancy
            val onPrimary = getContrastingTextColor(primary)
            // Better container colors with proper lightness and saturation
            val primaryContainer = createContainerColor(primary, true) // Light container
            val onPrimaryContainer = createOnContainerColor(primary, true)
            
            // SECONDARY: Use muted or complementary color
            val secondaryBase = mutedSwatch?.rgb
                ?: lightMutedSwatch?.rgb
                ?: rotateHue(primary, 30) // Analogous color
            
            val secondary = ensureMinimumSaturation(secondaryBase, 0.6f) // Increased saturation for more vibrancy
            val onSecondary = getContrastingTextColor(secondary)
            val secondaryContainer = createContainerColor(secondary, true)
            val onSecondaryContainer = createOnContainerColor(secondary, true)
            
            // TERTIARY: Use accent color (complementary or triadic)
            val tertiaryBase = lightVibrantSwatch?.rgb
                ?: darkMutedSwatch?.rgb
                ?: rotateHue(primary, 120) // Triadic color
            
            val tertiary = ensureMinimumSaturation(tertiaryBase, 0.65f) // Increased saturation for more vibrancy
            val onTertiary = getContrastingTextColor(tertiary)
            val tertiaryContainer = createContainerColor(tertiary, true)
            val onTertiaryContainer = createOnContainerColor(tertiary, true)
            
            // SURFACE: Use very desaturated version of dominant color
            val surfaceBase = dominantSwatch?.rgb ?: 0xFFFFFBFE.toInt()
            val surface = desaturateColor(lightenColor(surfaceBase, 0.90f), 0.95f) // More desaturated for better readability
            val onSurface = if (isColorLight(surface)) 0xFF1C1B1F.toInt() else 0xFFE6E1E5.toInt()
            val surfaceVariant = adjustBrightness(surface, 0.96f) // Slightly lighter
            val onSurfaceVariant = if (isColorLight(surfaceVariant)) 0xFF49454F.toInt() else 0xFFCAC4D0.toInt()
            
            val extractedColors = ExtractedColors(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary,
                onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = onTertiaryContainer,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant
            )
            
            Log.d(TAG, "Successfully extracted colors from album artwork")
            extractedColors
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract colors from bitmap", e)
            null
        }
    }
    
    /**
     * Convert ExtractedColors to JSON string for storage
     */
    fun colorsToJson(colors: ExtractedColors): String {
        return gson.toJson(colors)
    }
    
    /**
     * Convert JSON string back to ExtractedColors
     * Returns null if parsing fails
     */
    fun jsonToColors(json: String?): ExtractedColors? {
        if (json == null) return null
        return try {
            gson.fromJson(json, ExtractedColors::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse colors JSON", e)
            null
        }
    }
    
    /**
     * Lighten a color by a factor (0.0 to 1.0)
     */
    private fun lightenColor(color: Int, factor: Float): Int {
        val r = (android.graphics.Color.red(color) + (255 - android.graphics.Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (android.graphics.Color.green(color) + (255 - android.graphics.Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (android.graphics.Color.blue(color) + (255 - android.graphics.Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return android.graphics.Color.rgb(r, g, b)
    }
    
    /**
     * Darken a color by a factor (0.0 to 1.0)
     */
    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (android.graphics.Color.red(color) * (1 - factor)).toInt().coerceIn(0, 255)
        val g = (android.graphics.Color.green(color) * (1 - factor)).toInt().coerceIn(0, 255)
        val b = (android.graphics.Color.blue(color) * (1 - factor)).toInt().coerceIn(0, 255)
        return android.graphics.Color.rgb(r, g, b)
    }
    
    /**
     * Adjust brightness while preserving hue and saturation
     */
    private fun adjustBrightness(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return android.graphics.Color.HSVToColor(hsv)
    }
    
    /**
     * Desaturate a color by a factor (0.0 = full saturation, 1.0 = grayscale)
     */
    private fun desaturateColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * (1 - factor)).coerceIn(0f, 1f)
        return android.graphics.Color.HSVToColor(hsv)
    }
    
    /**
     * Ensure color has minimum saturation for visibility
     */
    private fun ensureMinimumSaturation(color: Int, minSaturation: Float): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        if (hsv[1] < minSaturation) {
            hsv[1] = minSaturation
        }
        // Also ensure reasonable brightness for vibrancy
        if (hsv[2] < 0.3f) {
            hsv[2] = 0.4f // Allow slightly darker colors to preserve vibrancy
        } else if (hsv[2] > 0.95f) {
            hsv[2] = 0.8f // Allow somewhat brighter colors for more vibrancy
        }
        return android.graphics.Color.HSVToColor(hsv)
    }
    
    /**
     * Rotate hue by degrees (0-360)
     */
    private fun rotateHue(color: Int, degrees: Int): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + degrees) % 360f
        return android.graphics.Color.HSVToColor(hsv)
    }
    
    /**
     * Create a proper container color from a base color
     * Containers should be lighter and less saturated for better readability
     */
    private fun createContainerColor(baseColor: Int, isLight: Boolean): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor, hsv)
        
        if (isLight) {
            // Light container: high brightness, moderate saturation to preserve vibrancy
            hsv[1] = (hsv[1] * 0.5f).coerceIn(0.25f, 0.6f) // Reduce saturation to 50%, min 25%, max 60%
            hsv[2] = 0.90f // High brightness for light backgrounds
        } else {
            // Dark container: lower brightness, moderate saturation
            hsv[1] = (hsv[1] * 0.5f).coerceIn(0.2f, 0.6f)
            hsv[2] = 0.30f // Lower brightness for dark backgrounds
        }
        
        return android.graphics.Color.HSVToColor(hsv)
    }
    
    /**
     * Create proper onContainer color with good contrast
     */
    private fun createOnContainerColor(baseColor: Int, isLight: Boolean): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor, hsv)
        
        if (isLight) {
            // Dark text on light container
            hsv[1] = min(hsv[1] * 1.2f, 1.0f) // Slightly boost saturation
            hsv[2] = 0.20f // Very dark for good contrast
        } else {
            // Light text on dark container
            hsv[1] = (hsv[1] * 0.6f).coerceIn(0.1f, 0.5f)
            hsv[2] = 0.90f // Very light for good contrast
        }
        
        return android.graphics.Color.HSVToColor(hsv)
    }
    
    /**
     * Get contrasting text color (black or white) for a background
     */
    private fun getContrastingTextColor(backgroundColor: Int): Int {
        return if (isColorLight(backgroundColor)) {
            0xFF000000.toInt() // Black text on light background
        } else {
            0xFFFFFFFF.toInt() // White text on dark background
        }
    }
    
    /**
     * Check if a color is light or dark using relative luminance
     */
    fun isColorLight(color: Int): Boolean {
        val red = android.graphics.Color.red(color) / 255.0
        val green = android.graphics.Color.green(color) / 255.0
        val blue = android.graphics.Color.blue(color) / 255.0
        
        // Calculate relative luminance (sRGB)
        val r = if (red <= 0.03928) red / 12.92 else Math.pow((red + 0.055) / 1.055, 2.4)
        val g = if (green <= 0.03928) green / 12.92 else Math.pow((green + 0.055) / 1.055, 2.4)
        val b = if (blue <= 0.03928) blue / 12.92 else Math.pow((blue + 0.055) / 1.055, 2.4)
        
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return luminance > 0.5 // Light if luminance > 50%
    }
    
    /**
     * Convert Android Color int to Compose Color
     */
    fun intToComposeColor(colorInt: Int): Color {
        return Color(colorInt)
    }
}
