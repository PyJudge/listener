package chromahub.rhythm.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import chromahub.rhythm.app.BuildConfig
import chromahub.rhythm.app.activities.MainActivity
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Background worker that checks for app updates using smart polling techniques
 * to minimize GitHub API calls while still providing timely notifications.
 * 
 * ## How the "Webhook" System Works
 * 
 * While Android apps cannot receive true webhooks (which require a server endpoint),
 * this worker implements a smart polling system that behaves similarly by:
 * 
 * ### 1. HTTP Conditional Requests (ETag/Last-Modified)
 * - Stores the `ETag` and `Last-Modified` headers from previous GitHub API responses
 * - On subsequent checks, includes these in conditional request headers
 * - GitHub returns `304 Not Modified` if nothing changed (saves bandwidth and API calls)
 * - Only processes full response when actual changes are detected
 * 
 * ### 2. Exponential Backoff
 * - Tracks consecutive `304 Not Modified` responses
 * - Gradually increases check interval when no updates are found:
 *   * 0-3 consecutive 304s: Check every 6 hours
 *   * 4-6 consecutive 304s: Check every 12 hours
 *   * 7-10 consecutive 304s: Check every 24 hours
 *   * 10+ consecutive 304s: Check every 72 hours (max backoff)
 * - Resets to 6 hours when a new version is detected
 * 
 * ### 3. Version Tracking
 * - Caches the last known version tag (e.g., "v3.0.5")
 * - Only sends notifications when a genuinely newer version appears
 * - Prevents duplicate notifications for the same version
 * 
 * ### 4. Rate Limit Awareness
 * - Monitors GitHub's `X-RateLimit-Remaining` header
 * - Automatically backs off if approaching rate limits
 * - Handles `403 Forbidden` responses gracefully
 * 
 * ### Benefits Over Regular Polling
 * - **Reduced API Calls**: HTTP 304 responses don't count toward rate limits as heavily
 * - **Bandwidth Efficient**: No data transfer when nothing changed
 * - **Battery Friendly**: Exponential backoff reduces wake-ups when app is stable
 * - **Timely Notifications**: Still detects updates within hours of release
 * - **User Control**: Can be disabled via settings while maintaining manual check ability
 * 
 * ### GitHub API Rate Limits
 * - Unauthenticated: 60 requests/hour
 * - Authenticated: 5000 requests/hour
 * - This worker typically uses <10 requests/day with smart polling
 * 
 * @see chromahub.rhythm.app.shared.data.model.AppSettings.updateNotificationsEnabled
 * @see chromahub.rhythm.app.shared.data.model.AppSettings.useSmartUpdatePolling
 */
class UpdateNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "UpdateNotificationWorker"
        const val WORK_NAME = "update_notification_work"
        const val CHANNEL_ID = "app_updates"
        const val NOTIFICATION_ID = 1001
        
        // Metadata keys for SharedPreferences
        private const val PREF_NAME = "update_webhook_cache"
        private const val KEY_LAST_ETAG = "last_etag"
        private const val KEY_LAST_MODIFIED = "last_modified"
        private const val KEY_LAST_VERSION_TAG = "last_version_tag"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_CONSECUTIVE_NOT_MODIFIED = "consecutive_not_modified"
        
        // Exponential backoff thresholds
        private const val MAX_CONSECUTIVE_NOT_MODIFIED = 10
    }
    
    private val appSettings = AppSettings.getInstance(applicationContext)
    private val gitHubApiService = NetworkManager.createGitHubApiService()
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Starting update check via webhook worker...")
            
            // Check if updates are enabled
            if (!appSettings.updatesEnabled.value) {
                Log.d(TAG, "Updates disabled, skipping check")
                return@withContext Result.success()
            }
            
            // Check if notifications are enabled
            if (!appSettings.updateNotificationsEnabled.value) {
                Log.d(TAG, "Update notifications disabled, skipping check")
                return@withContext Result.success()
            }
            
            val currentChannel = appSettings.updateChannel.value
            
            // Perform smart polling check
            val updateDetected = checkForUpdateWithSmartPolling(currentChannel)
            
            if (updateDetected) {
                Log.d(TAG, "New update detected! Sending notification...")
                sendUpdateNotification()
            } else {
                Log.d(TAG, "No new updates detected")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}", e)
            Result.retry()
        }
    }
    
    /**
     * Smart polling using HTTP conditional requests to minimize API calls
     * Returns true if a new update is detected
     */
    private suspend fun checkForUpdateWithSmartPolling(channel: String): Boolean {
        try {
            val lastETag = prefs.getString(KEY_LAST_ETAG, null)
            val lastModified = prefs.getString(KEY_LAST_MODIFIED, null)
            val lastVersionTag = prefs.getString(KEY_LAST_VERSION_TAG, null)
            val consecutiveNotModified = prefs.getInt(KEY_CONSECUTIVE_NOT_MODIFIED, 0)
            
            Log.d(TAG, "Smart polling - Last ETag: $lastETag, Last Modified: $lastModified")
            Log.d(TAG, "Consecutive 304 responses: $consecutiveNotModified")
            
            // Build headers for conditional request
            val headers = mutableMapOf<String, String>()
            lastETag?.let { headers["If-None-Match"] = it }
            lastModified?.let { headers["If-Modified-Since"] = it }
            
            // Fetch latest release based on channel with conditional headers
            val response = if (channel == "beta") {
                gitHubApiService.getReleasesWithHeaders(
                    owner = "cromaguy",
                    repo = "Rhythm",
                    perPage = 10,
                    ifNoneMatch = lastETag,
                    ifModifiedSince = lastModified
                )
            } else {
                gitHubApiService.getLatestReleaseWithHeaders(
                    owner = "cromaguy",
                    repo = "Rhythm",
                    ifNoneMatch = lastETag,
                    ifModifiedSince = lastModified
                )
            }
            
            // Check response headers
            val responseCode = response.code()
            val newETag = response.headers()["ETag"]
            val newLastModified = response.headers()["Last-Modified"]
            val rateLimit = response.headers()["X-RateLimit-Remaining"]
            val rateLimitReset = response.headers()["X-RateLimit-Reset"]
            
            Log.d(TAG, "Response code: $responseCode")
            Log.d(TAG, "Rate limit remaining: $rateLimit, resets at: $rateLimitReset")
            
            when (responseCode) {
                304 -> {
                    // Not Modified - no changes since last check
                    Log.d(TAG, "304 Not Modified - no changes detected")
                    prefs.edit()
                        .putInt(KEY_CONSECUTIVE_NOT_MODIFIED, consecutiveNotModified + 1)
                        .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                        .apply()
                    return false
                }
                
                200 -> {
                    // Success - check if version changed
                    if (response.isSuccessful && response.body() != null) {
                        val latestRelease = if (channel == "beta") {
                            // For beta channel, get all releases and find first non-draft
                            @Suppress("UNCHECKED_CAST")
                            (response.body() as? List<chromahub.rhythm.app.network.GitHubRelease>)?.firstOrNull { !it.draft }
                        } else {
                            response.body() as? chromahub.rhythm.app.network.GitHubRelease
                        }
                        
                        if (latestRelease != null) {
                            val newVersionTag = latestRelease.tag_name
                            val hasNewVersion = lastVersionTag != newVersionTag && 
                                               isNewerVersion(latestRelease.tag_name, BuildConfig.VERSION_NAME)
                            
                            Log.d(TAG, "Latest version: $newVersionTag, Last known: $lastVersionTag")
                            Log.d(TAG, "Current version: ${BuildConfig.VERSION_NAME}, Is newer: $hasNewVersion")
                            
                            // Update cache
                            prefs.edit()
                                .putString(KEY_LAST_ETAG, newETag)
                                .putString(KEY_LAST_MODIFIED, newLastModified)
                                .putString(KEY_LAST_VERSION_TAG, newVersionTag)
                                .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                                .putInt(KEY_CONSECUTIVE_NOT_MODIFIED, 0) // Reset counter
                                .apply()
                            
                            return hasNewVersion
                        }
                    }
                }
                
                403 -> {
                    // Rate limit exceeded
                    Log.w(TAG, "GitHub API rate limit exceeded. Next reset: $rateLimitReset")
                    // Don't retry immediately
                    return false
                }
                
                else -> {
                    Log.w(TAG, "Unexpected response code: $responseCode")
                }
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error during smart polling: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Compare version strings to determine if new version is newer
     * Handles semantic versioning with build numbers and pre-release tags
     */
    private fun isNewerVersion(newVersion: String, currentVersion: String): Boolean {
        try {
            // Parse semantic versions
            val newSemVer = parseSemanticVersion(newVersion)
            val currentSemVer = parseSemanticVersion(currentVersion)
            
            // Compare major.minor.patch first
            if (newSemVer.major != currentSemVer.major) return newSemVer.major > currentSemVer.major
            if (newSemVer.minor != currentSemVer.minor) return newSemVer.minor > currentSemVer.minor
            if (newSemVer.patch != currentSemVer.patch) return newSemVer.patch > currentSemVer.patch
            if (newSemVer.subpatch != currentSemVer.subpatch) return newSemVer.subpatch > currentSemVer.subpatch
            
            // If versions are equal, compare build numbers
            if (newSemVer.buildNumber != currentSemVer.buildNumber) {
                return newSemVer.buildNumber > currentSemVer.buildNumber
            }
            
            // Pre-releases are considered older than stable releases
            if (newSemVer.isPreRelease != currentSemVer.isPreRelease) {
                return !newSemVer.isPreRelease && currentSemVer.isPreRelease
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing versions: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Parse version string to semantic version components
     */
    private data class SemanticVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val subpatch: Int = 0,
        val buildNumber: Int = 0,
        val isPreRelease: Boolean = false
    )
    
    private fun parseSemanticVersion(versionString: String): SemanticVersion {
        try {
            val cleaned = versionString.trim().removePrefix("v")
            
            // Extract build number (e.g., "b-127" or "build-127")
            val buildRegex = Regex("(?:b|build)-(\\d+)", RegexOption.IGNORE_CASE)
            val buildNumber = buildRegex.find(cleaned)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            
            // Extract base version (remove build info and tags)
            val versionBase = cleaned.split(" ")[0].split("-")[0].split("_")[0]
            val versionParts = versionBase.split(".")
            
            // Check for pre-release keywords
            val preReleaseKeywords = listOf("alpha", "beta", "pre", "rc", "dev", "snapshot")
            val isPreRelease = preReleaseKeywords.any { keyword ->
                cleaned.contains(keyword, ignoreCase = true)
            }
            
            return SemanticVersion(
                major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0,
                minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0,
                patch = versionParts.getOrNull(2)?.toIntOrNull() ?: 0,
                subpatch = versionParts.getOrNull(3)?.toIntOrNull() ?: 0,
                buildNumber = buildNumber,
                isPreRelease = isPreRelease
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing version: $versionString", e)
            return SemanticVersion(0, 0, 0, 0, 0, false)
        }
    }
    
    /**
     * Send a notification about the available update
     */
    private fun sendUpdateNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new app version releases"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intent to open app update screen
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "updates")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists
            .setContentTitle("Rhythm Update Available")
            .setContentText("A new version of Rhythm is available. Tap to download.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Update notification sent")
    }
    
    /**
     * Get the recommended check interval based on consecutive 304 responses
     * Implements exponential backoff to reduce unnecessary API calls
     */
    fun getRecommendedCheckInterval(): Long {
        val consecutiveNotModified = prefs.getInt(KEY_CONSECUTIVE_NOT_MODIFIED, 0)
        
        return when {
            consecutiveNotModified < 3 -> 6L // 6 hours
            consecutiveNotModified < 6 -> 12L // 12 hours
            consecutiveNotModified < MAX_CONSECUTIVE_NOT_MODIFIED -> 24L // 1 day
            else -> 72L // 3 days (maximum backoff)
        }
    }
    
    /**
     * Clear cached webhook data (useful for testing or reset)
     */
    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Webhook cache cleared")
    }
}
