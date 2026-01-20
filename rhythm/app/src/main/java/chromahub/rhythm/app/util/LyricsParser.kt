package chromahub.rhythm.app.util

import android.util.Log
import java.util.regex.Pattern

object LyricsParser {

    // Enhanced regex pattern to support various LRC timestamp formats
    // Supports: [mm:ss.xx], [mm:ss:xx], [mm:ss.xxx], [mm:ss], and even [hh:mm:ss.xxx]
    private val timestampPattern = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:]?(\\d{0,3}))?\\]")
    
    // Metadata pattern includes:
    // - Standard LRC metadata: ar (artist), ti (title), al (album), by (creator), offset, re (editor), ve (version), length
    // - Voice/part tags: v1, v2, v3, etc. (used in duets/multi-voice songs to indicate different singers)
    // - Tool tags: editor, tool, version (lyrics editor/tool information)
    // Voice tags (v1:, v2:, etc.) are commonly used to label different singers in duets or harmonies
    private val metadataPattern = Pattern.compile("\\[(ar|ti|al|by|offset|re|ve|length|v\\d+|version|editor|tool):[^\\]]*\\]", Pattern.CASE_INSENSITIVE)
    
    // Enhanced LRC word-level timestamp pattern: <mm:ss.xx> or <mm:ss.xxx>
    private val wordTimestampPattern = Pattern.compile("<(\\d{1,3}):(\\d{2})(?:\\.(\\d{2,3}))?>")
    
    // Pattern to detect voice tags in lyrics text (e.g., "v1: text" or "v2: text")
    private val voiceTagInLinePattern = Pattern.compile("^(v\\d+):\\s*(.*)$", Pattern.CASE_INSENSITIVE)
    
    /**
     * Check if lyrics contain word-level timestamps (Enhanced LRC format)
     */
    fun hasWordTimestamps(lrcContent: String): Boolean {
        return wordTimestampPattern.matcher(lrcContent).find()
    }
    
    /**
     * Extract voice tag and text from a line
     * @param text The lyrics text that may contain voice tag
     * @return Pair of (voiceTag, cleanedText) or (null, originalText)
     */
    private fun extractVoiceTag(text: String): Pair<String?, String> {
        val matcher = voiceTagInLinePattern.matcher(text.trim())
        return if (matcher.matches()) {
            val voiceTag = matcher.group(1) ?: ""
            val cleanedText = matcher.group(2) ?: text
            Pair(voiceTag.lowercase(), cleanedText.trim())
        } else {
            Pair(null, text)
        }
    }

    /**
     * Parse LRC format lyrics into structured lyric lines with timestamps.
     * 
     * Supports multi-line lyrics where untimestamped lines following a timestamped line
     * are combined with the timestamped line using newline separators. This enables:
     * - Translations: [00:00.74]Original text\n(Translation)
     * - Romanizations: [00:00.74]Original text\nRomanization
     * - Multi-line verses: [00:00.74]Line 1\nLine 2\nLine 3
     * 
     * Example input:
     * ```
     * [00:00.74]I've been trying to be every man you saw in me
     * (He estado intentando ser cada hombre que viste en mí)
     * [00:05.20]But in my eyes I just flicker out
     * (Pero en mis ojos solo parpadeo)
     * ```
     * 
     * Output:
     * - LyricLine(timestamp=740, text="I've been trying...\n(He estado...)")
     * - LyricLine(timestamp=5200, text="But in my eyes...\n(Pero en mis ojos...)")
     * 
     * @param lrcContent The LRC format lyrics string
     * @return List of parsed lyric lines sorted by timestamp
     */
    fun parseLyrics(lrcContent: String): List<LyricLine> {
        if (lrcContent.isBlank()) return emptyList()
        
        val lyricLines = mutableListOf<LyricLine>()
        val lines = lrcContent.trim().split("\n", "\r\n", "\r")
        
        var pendingTimestamps = mutableListOf<Long>()
        val pendingTextLines = mutableListOf<String>()

        for (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            // Skip metadata lines (artist, title, album, etc.)
            if (metadataPattern.matcher(trimmedLine).find()) continue
            
            val matcher = timestampPattern.matcher(trimmedLine)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            // Find all timestamps in the line
            while (matcher.find()) {
                try {
                    val timeValue1 = matcher.group(1)?.toLongOrNull() ?: 0
                    val timeValue2 = matcher.group(2)?.toLongOrNull() ?: 0
                    val millisecondsStr = matcher.group(3) ?: ""
                    
                    // Determine if this is HH:MM:SS or MM:SS format
                    // If timeValue1 > 59, it's likely minutes in MM:SS format
                    val (hours, minutes, seconds) = if (timeValue1 > 59) {
                        // Extended format MM:SS where MM can be > 59 (some songs are very long)
                        Triple(0L, timeValue1, timeValue2)
                    } else {
                        // Could be HH:MM:SS or MM:SS - check for another colon
                        val remainingText = trimmedLine.substring(matcher.start())
                        val colonCount = remainingText.substring(0, matcher.end() - matcher.start()).count { it == ':' }
                        if (colonCount >= 2) {
                            // HH:MM:SS format
                            Triple(timeValue1, timeValue2, millisecondsStr.toLongOrNull() ?: 0)
                        } else {
                            // MM:SS format
                            Triple(0L, timeValue1, timeValue2)
                        }
                    }
                    
                    // Handle different millisecond formats more robustly
                    val milliseconds = when {
                        millisecondsStr.isEmpty() -> 0L
                        millisecondsStr.length == 1 -> millisecondsStr.toLong() * 100  // [mm:ss.x] -> x00ms
                        millisecondsStr.length == 2 -> millisecondsStr.toLong() * 10   // [mm:ss.xx] -> xx0ms
                        millisecondsStr.length == 3 -> millisecondsStr.toLong()        // [mm:ss.xxx] -> xxxms
                        else -> millisecondsStr.substring(0, 3).toLong() // Truncate if too long
                    }
                    
                    // Calculate total timestamp in milliseconds
                    val timestamp = (hours * 3600 * 1000) + (minutes * 60 * 1000) + (seconds * 1000) + milliseconds
                    
                    // Only add valid timestamps (prevent negative or unreasonably large values)
                    if (timestamp >= 0 && timestamp < 86400000) { // Less than 24 hours
                        timestamps.add(timestamp)
                    }
                    lastMatchEnd = matcher.end()
                } catch (e: Exception) {
                    Log.w("LyricsParser", "Error parsing timestamp in line: $trimmedLine", e)
                    continue
                }
            }

            if (timestamps.isNotEmpty()) {
                // This line has timestamps - process any pending text first
                if (pendingTimestamps.isNotEmpty() && pendingTextLines.isNotEmpty()) {
                    // Combine all pending text lines with newline separator
                    val combinedText = pendingTextLines.joinToString("\n")
                    val (voiceTag, cleanedText) = extractVoiceTag(combinedText)
                    
                    for (timestamp in pendingTimestamps) {
                        lyricLines.add(LyricLine(timestamp, cleanedText, voiceTag))
                    }
                    pendingTextLines.clear()
                }
                
                // Extract lyrics text after all timestamps
                val text = if (lastMatchEnd < trimmedLine.length) {
                    trimmedLine.substring(lastMatchEnd).trim()
                } else {
                    ""
                }
                
                // Store timestamps and initial text
                pendingTimestamps = timestamps
                if (text.isNotEmpty()) {
                    pendingTextLines.add(text)
                }
            } else {
                // Line without timestamp - add to pending text if we have a pending timestamp
                // This handles translations, romanizations, or multi-line lyrics
                if (pendingTimestamps.isNotEmpty()) {
                    pendingTextLines.add(trimmedLine)
                }
            }
        }
        
        // Process any remaining pending text
        if (pendingTimestamps.isNotEmpty() && pendingTextLines.isNotEmpty()) {
            val combinedText = pendingTextLines.joinToString("\n")
            val (voiceTag, cleanedText) = extractVoiceTag(combinedText)
            
            for (timestamp in pendingTimestamps) {
                lyricLines.add(LyricLine(timestamp, cleanedText, voiceTag))
            }
        }

        // Sort by timestamp and remove duplicates
        return lyricLines
            .sortedBy { it.timestamp }
            .distinctBy { "${it.timestamp}_${it.text}" } // Remove exact duplicates
    }
    
    /**
     * Validates if the provided content contains valid LRC format timestamps
     */
    fun isValidLrcFormat(content: String): Boolean {
        if (content.isBlank()) return false
        
        val lines = content.trim().split("\n")
        var timestampCount = 0
        
        for (line in lines) {
            if (timestampPattern.matcher(line.trim()).find()) {
                timestampCount++
                if (timestampCount >= 2) return true // At least 2 timestamped lines
            }
        }
        
        return false
    }
    
    /**
     * Parses Enhanced LRC format with word-level timestamps into structured format
     * Enhanced LRC format example: [00:12.00]Hello <00:12.50>world <00:13.00>of <00:13.50>lyrics
     * 
     * @param lrcContent Enhanced LRC content with word-level timestamps
     * @return List of lines with word-level timing, or empty if parsing fails
     */
    fun parseEnhancedLRC(lrcContent: String): List<EnhancedLyricLine> {
        if (lrcContent.isBlank()) return emptyList()
        
        val enhancedLines = mutableListOf<EnhancedLyricLine>()
        val lines = lrcContent.trim().split("\n", "\r\n", "\r")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            // Skip metadata lines
            if (metadataPattern.matcher(trimmedLine).find()) continue
            
            // Extract line-level timestamp [mm:ss.xx]
            val lineTimestampMatcher = timestampPattern.matcher(trimmedLine)
            if (!lineTimestampMatcher.find()) continue
            
            val lineTimestamp = try {
                val timeValue1 = lineTimestampMatcher.group(1)?.toLongOrNull() ?: 0
                val timeValue2 = lineTimestampMatcher.group(2)?.toLongOrNull() ?: 0
                val millisecondsStr = lineTimestampMatcher.group(3) ?: ""
                
                val (hours, minutes, seconds) = if (timeValue1 > 59) {
                    Triple(0L, timeValue1, timeValue2)
                } else {
                    val remainingText = trimmedLine.substring(lineTimestampMatcher.start())
                    val colonCount = remainingText.substring(0, lineTimestampMatcher.end() - lineTimestampMatcher.start()).count { it == ':' }
                    if (colonCount >= 2) {
                        Triple(timeValue1, timeValue2, millisecondsStr.toLongOrNull() ?: 0)
                    } else {
                        Triple(0L, timeValue1, timeValue2)
                    }
                }
                
                val milliseconds = when {
                    millisecondsStr.isEmpty() -> 0L
                    millisecondsStr.length == 1 -> millisecondsStr.toLong() * 100
                    millisecondsStr.length == 2 -> millisecondsStr.toLong() * 10
                    millisecondsStr.length == 3 -> millisecondsStr.toLong()
                    else -> millisecondsStr.substring(0, 3).toLong()
                }
                
                (hours * 3600 * 1000) + (minutes * 60 * 1000) + (seconds * 1000) + milliseconds
            } catch (e: Exception) {
                Log.w("LyricsParser", "Error parsing line timestamp: $trimmedLine", e)
                continue
            }
            
            // Extract text after line timestamp
            val textAfterLineTimestamp = trimmedLine.substring(lineTimestampMatcher.end()).trim()
            if (textAfterLineTimestamp.isEmpty()) continue
            
            // Parse word-level timestamps <mm:ss.xx>
            val words = mutableListOf<EnhancedWord>()
            val wordMatcher = wordTimestampPattern.matcher(textAfterLineTimestamp)
            var lastWordEnd = 0
            var previousWordTimestamp = lineTimestamp
            
            while (wordMatcher.find()) {
                // Get text before this word timestamp
                val textBeforeTimestamp = textAfterLineTimestamp.substring(lastWordEnd, wordMatcher.start()).trim()
                
                // Parse word timestamp
                val wordTimestamp = try {
                    val minutes = wordMatcher.group(1)?.toLongOrNull() ?: 0
                    val seconds = wordMatcher.group(2)?.toLongOrNull() ?: 0
                    val millis = wordMatcher.group(3)?.let {
                        when (it.length) {
                            1 -> it.toLong() * 100
                            2 -> it.toLong() * 10
                            3 -> it.toLong()
                            else -> it.substring(0, 3).toLong()
                        }
                    } ?: 0
                    
                    (minutes * 60 * 1000) + (seconds * 1000) + millis
                } catch (e: Exception) {
                    Log.w("LyricsParser", "Error parsing word timestamp: ${wordMatcher.group()}", e)
                    lastWordEnd = wordMatcher.end()
                    continue
                }
                
                // Add the text before timestamp as a word (if any)
                if (textBeforeTimestamp.isNotEmpty()) {
                    words.add(EnhancedWord(
                        text = textBeforeTimestamp,
                        timestamp = previousWordTimestamp,
                        endtime = wordTimestamp
                    ))
                }
                
                previousWordTimestamp = wordTimestamp
                lastWordEnd = wordMatcher.end()
            }
            
            // Add remaining text after last word timestamp
            if (lastWordEnd < textAfterLineTimestamp.length) {
                val remainingText = textAfterLineTimestamp.substring(lastWordEnd).trim()
                if (remainingText.isNotEmpty()) {
                    words.add(EnhancedWord(
                        text = remainingText,
                        timestamp = previousWordTimestamp,
                        endtime = previousWordTimestamp + 1000 // Estimate 1 second duration
                    ))
                }
            }
            
            // If no word timestamps found, treat the entire line as one word
            if (words.isEmpty() && textAfterLineTimestamp.isNotEmpty()) {
                words.add(EnhancedWord(
                    text = textAfterLineTimestamp,
                    timestamp = lineTimestamp,
                    endtime = lineTimestamp + 3000 // Estimate 3 seconds duration
                ))
            }
            
            if (words.isNotEmpty()) {
                val lineEndtime = words.lastOrNull()?.endtime ?: (lineTimestamp + 3000)
                enhancedLines.add(EnhancedLyricLine(
                    words = words,
                    lineTimestamp = lineTimestamp,
                    lineEndtime = lineEndtime
                ))
            }
        }
        
        return enhancedLines
    }
    
    // TODO: Implement syllable-level timestamp parsing
    // Enhanced LRC can support syllable breakdowns using hyphenated words with individual timestamps
    // Example: [00:12.00]Hel<00:12.20>lo <00:12.50>wo<00:12.70>rld
    // This would require parsing word parts separated by syllable boundaries
    /**
     * Parse syllable-level timestamps (future enhancement)
     * @param lrcContent Enhanced LRC with syllable-level timing
     * @return List of lines with syllable-level timing
     */
    fun parseSyllableLRC(lrcContent: String): List<EnhancedLyricLine> {
        // TODO: Implement syllable parsing
        // For now, fall back to word-level parsing
        return parseEnhancedLRC(lrcContent)
    }
    
    // TODO: Implement Enhanced LRC export
    /**
     * Convert word-by-word lyrics back to Enhanced LRC format (future enhancement)
     * @param lines List of enhanced lyric lines
     * @return Enhanced LRC formatted string
     */
    fun toEnhancedLRC(lines: List<EnhancedLyricLine>): String {
        // TODO: Implement export to Enhanced LRC format
        return lines.joinToString("\n") { line ->
            val timestamp = formatLRCTimestamp(line.lineTimestamp)
            val words = line.words.joinToString("") { word ->
                val wordTimestamp = formatLRCTimestamp(word.timestamp)
                " <$wordTimestamp>${word.text}"
            }.trimStart()
            "[$timestamp]$words"
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

data class LyricLine(
    val timestamp: Long,
    val text: String,
    val voiceTag: String? = null // Voice tag (v1, v2, v3, etc.) for multi-voice lyrics
)

/**
 * Represents a lyric line with word-level timing (Enhanced LRC format)
 */
data class EnhancedLyricLine(
    val words: List<EnhancedWord>,
    val lineTimestamp: Long,
    val lineEndtime: Long
)

/**
 * Represents a word with timing in Enhanced LRC format
 * TODO: Add syllable support - break words into syllable parts with individual timing
 */
data class EnhancedWord(
    val text: String,
    val timestamp: Long, // start time in milliseconds
    val endtime: Long, // end time in milliseconds
    val syllables: List<Syllable>? = null // TODO: Future syllable-level timing
)

/**
 * Represents a syllable within a word (future enhancement)
 * TODO: Implement syllable parsing and display
 */
data class Syllable(
    val text: String,
    val timestamp: Long,
    val endtime: Long
)
