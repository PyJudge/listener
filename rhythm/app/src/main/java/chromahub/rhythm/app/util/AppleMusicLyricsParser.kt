package chromahub.rhythm.app.util

import android.util.Log
import chromahub.rhythm.app.network.AppleMusicLyricsLine
import chromahub.rhythm.app.network.AppleMusicWord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AppleMusicLyricsParser {
    private const val TAG = "AppleMusicLyricsParser"
    
    // Pattern to detect voice tags in lyrics text (e.g., "v1: text" or "v2: text")
    private val voiceTagPattern = java.util.regex.Pattern.compile("^(v\\d+):\\s*(.*)$", java.util.regex.Pattern.CASE_INSENSITIVE)

    /**
     * Parses Apple Music word-by-word lyrics JSON into structured format
     * @param jsonContent JSON string containing Apple Music lyrics data
     * @return List of parsed word-level lyrics, or empty if parsing fails
     */
    fun parseWordByWordLyrics(jsonContent: String): List<WordByWordLyricLine> {
        if (jsonContent.isBlank()) return emptyList()
        
        return try {
            val gson = Gson()
            val listType = object : TypeToken<List<AppleMusicLyricsLine>>() {}.type
            val appleMusicLines: List<AppleMusicLyricsLine> = gson.fromJson(jsonContent, listType)
            
            appleMusicLines.mapNotNull { line ->
                var words = line.text?.map { word ->
                    WordByWordWord(
                        text = word.text,
                        isPart = word.part ?: false,
                        timestamp = word.timestamp,
                        endtime = word.endtime
                    )
                } ?: emptyList()
                
                // Check if first word contains voice tag and extract it
                var voiceTag: String? = null
                if (words.isNotEmpty()) {
                    val firstWordText = words.first().text
                    val matcher = voiceTagPattern.matcher(firstWordText)
                    if (matcher.matches()) {
                        voiceTag = matcher.group(1)?.lowercase()
                        val cleanedText = matcher.group(2)?.trim() ?: ""
                        // Replace first word with cleaned text (without voice tag)
                        if (cleanedText.isNotEmpty()) {
                            words = listOf(
                                words.first().copy(text = cleanedText)
                            ) + words.drop(1)
                        } else {
                            // If cleaned text is empty, remove the first word entirely
                            words = words.drop(1)
                        }
                    }
                }
                
                if (words.isNotEmpty()) {
                    WordByWordLyricLine(
                        words = words,
                        lineTimestamp = line.timestamp ?: 0L,
                        lineEndtime = line.endtime ?: 0L,
                        background = line.background ?: false,
                        voiceTag = voiceTag
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Apple Music word-by-word lyrics", e)
            emptyList()
        }
    }
    
    /**
     * Convert word-by-word lyrics to plain text (for display when word highlighting is not needed)
     */
    fun toPlainText(wordByWordLines: List<WordByWordLyricLine>): String {
        return wordByWordLines.joinToString("\n") { line ->
            line.words.joinToString("") { word ->
                if (word.isPart && word.text.isNotEmpty()) {
                    word.text // syllable, no space before
                } else {
                    " ${word.text}"
                }
            }.trim()
        }
    }
    
    /**
     * Convert word-by-word lyrics to LRC format (for compatibility)
     */
    fun toLRCFormat(wordByWordLines: List<WordByWordLyricLine>): String {
        return wordByWordLines.joinToString("\n") { line ->
            val timestamp = formatLRCTimestamp(line.lineTimestamp)
            val text = line.words.joinToString("") { word ->
                if (word.isPart && word.text.isNotEmpty()) {
                    word.text
                } else {
                    " ${word.text}"
                }
            }.trim()
            "[$timestamp]$text"
        }
    }
    
    private fun formatLRCTimestamp(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = (milliseconds % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, millis)
    }
}

/**
 * Represents a line of lyrics with word-level timing
 */
data class WordByWordLyricLine(
    val words: List<WordByWordWord>,
    val lineTimestamp: Long,
    val lineEndtime: Long,
    val background: Boolean = false,
    val voiceTag: String? = null // Voice tag (v1, v2, v3, etc.) for multi-voice lyrics
)

/**
 * Represents a single word with precise timing
 */
data class WordByWordWord(
    val text: String,
    val isPart: Boolean, // true if this is a syllable/part of a split word
    val timestamp: Long, // start time in milliseconds
    val endtime: Long // end time in milliseconds
)
