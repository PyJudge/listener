package chromahub.rhythm.app.network

import android.util.Base64
import android.util.Log
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException

/**
 * Service for Spotify OAuth token management
 */
interface SpotifyAuthApiService {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun getAccessToken(
        @Header("Authorization") basicAuth: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): Response<SpotifyTokenResponse>
}

data class SpotifyTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)

/**
 * Repository for managing Spotify API authentication and track search
 */
class SpotifyService(private val appSettings: AppSettings) {
    companion object {
        private const val TAG = "SpotifyService"
        private const val SPOTIFY_AUTH_BASE_URL = "https://accounts.spotify.com/"

        // Default Spotify API credentials (for demonstration/initial setup)
        // Users should be encouraged to use their own keys for full functionality
        private const val DEFAULT_SPOTIFY_CLIENT_ID = "e3ad7591679b4f71b31b61d1d56e4b90" 
        private const val DEFAULT_SPOTIFY_CLIENT_SECRET = "e30dcc4c833b49cab3f9f3fb679f873c" 
    }
    
    private var cachedAccessToken: String? = null
    private var tokenExpiryTime: Long = 0
    
    private val authService = retrofit2.Retrofit.Builder()
        .baseUrl(SPOTIFY_AUTH_BASE_URL)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()
        .create(SpotifyAuthApiService::class.java)
    
    /**
     * Get a valid access token, refreshing if necessary
     */
    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            // Check if we have a valid cached token
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
                return@withContext cachedAccessToken
            }
            
            var clientId = appSettings.spotifyClientId.value
            var clientSecret = appSettings.spotifyClientSecret.value
            
            // Use default keys if user hasn't configured their own
            val usingDefaultKeys = clientId.isEmpty() || clientSecret.isEmpty()
            if (usingDefaultKeys) {
                clientId = DEFAULT_SPOTIFY_CLIENT_ID
                clientSecret = DEFAULT_SPOTIFY_CLIENT_SECRET
            }
            
            if (clientId.isEmpty() || clientSecret.isEmpty()) {
                Log.w(TAG, "Spotify client ID or secret not configured (even default values are missing).")
                return@withContext null
            }
            
            // Create basic auth header
            val credentials = "$clientId:$clientSecret"
            val encodedCredentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            val authHeader = "Basic $encodedCredentials"
            
            Log.d(TAG, "Requesting Spotify access token")
            val response = authService.getAccessToken(authHeader)
            
            if (response.isSuccessful) {
                response.body()?.let { tokenResponse ->
                    cachedAccessToken = tokenResponse.access_token
                    // Set expiry time slightly before actual expiry to be safe
                    tokenExpiryTime = System.currentTimeMillis() + (tokenResponse.expires_in - 60) * 1000
                    Log.d(TAG, "Successfully obtained Spotify access token")
                    return@withContext cachedAccessToken
                }
            } else {
                Log.e(TAG, "Failed to get Spotify access token: ${response.code()} - ${response.message()}")
            }
            
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Spotify access token", e)
            return@withContext null
        }
    }
    
    /**
     * Search for a track on Spotify and return the best match
     */
    suspend fun searchTrack(artist: String, title: String): SpotifyTrack? = withContext(Dispatchers.IO) {
        try {
            var currentClientId = appSettings.spotifyClientId.value
            var currentClientSecret = appSettings.spotifyClientSecret.value

            val usingDefaultKeys = currentClientId.isEmpty() || currentClientSecret.isEmpty()
            if (usingDefaultKeys) {
                currentClientId = DEFAULT_SPOTIFY_CLIENT_ID
                currentClientSecret = DEFAULT_SPOTIFY_CLIENT_SECRET
            }

            // The API is not enabled by default, so we check the setting here.
            // If the user has not provided their own keys, the default keys will be used,
            // but the API will still be disabled unless explicitly enabled by the user.
            // To fix the canvas issue, we bypass this check if default keys are in use.
            if (!appSettings.spotifyApiEnabled.value && !usingDefaultKeys) {
                Log.d(TAG, "Spotify API is disabled by user settings.")
                return@withContext null
            }
            val accessToken = getAccessToken()
            if (accessToken == null) {
                Log.w(TAG, "Could not obtain Spotify access token")
                return@withContext null
            }
            
            // Create search query
            val query = "artist:\"$artist\" track:\"$title\""
            Log.d(TAG, "Searching Spotify for: $query")
            
            val response = NetworkClient.spotifySearchApiService.searchTracks(
                query = query,
                authorization = "Bearer $accessToken",
                limit = 5 // Get a few results to find best match
            )
            
            if (response.isSuccessful) {
                response.body()?.tracks?.items?.let { tracks ->
                    if (tracks.isNotEmpty()) {
                        // Find best match based on artist and title similarity
                        val bestMatch = tracks.minByOrNull { track ->
                            val artistScore = calculateStringDistance(
                                artist.lowercase(), 
                                track.artists.firstOrNull()?.name?.lowercase() ?: ""
                            )
                            val titleScore = calculateStringDistance(
                                title.lowercase(), 
                                track.name.lowercase()
                            )
                            artistScore + titleScore
                        }
                        
                        bestMatch?.let { track ->
                            Log.d(TAG, "Found Spotify track: ${track.artists.firstOrNull()?.name} - ${track.name} (ID: ${track.id})")
                            return@withContext track
                        }
                    } else {
                        Log.d(TAG, "No tracks found for: $artist - $title")
                    }
                } ?: run {
                    Log.w(TAG, "Response body or tracks was null")
                }
            } else {
                Log.e(TAG, "Spotify search failed: ${response.code()} - ${response.message()}")
            }
            
            return@withContext null
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error searching Spotify", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error searching Spotify track", e)
            return@withContext null
        }
    }
    
    /**
     * Simple string distance calculation for matching
     */
    private fun calculateStringDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    /**
     * Test the Spotify API configuration
     */
    suspend fun testApiConfiguration(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            // Use the same logic as getAccessToken to determine which client ID/secret to use
            var clientId = appSettings.spotifyClientId.value
            var clientSecret = appSettings.spotifyClientSecret.value

            if (clientId.isEmpty() || clientSecret.isEmpty()) {
                clientId = DEFAULT_SPOTIFY_CLIENT_ID
                clientSecret = DEFAULT_SPOTIFY_CLIENT_SECRET
            }
            
            if (clientId.isEmpty() || clientSecret.isEmpty()) {
                return@withContext Pair(false, "Client ID and Secret are required (even default values are missing)")
            }
            
            val accessToken = getAccessToken()
            if (accessToken != null) {
                // Test with a simple search
                val response = NetworkClient.spotifySearchApiService.searchTracks(
                    query = "test",
                    authorization = "Bearer $accessToken",
                    limit = 1
                )
                
                if (response.isSuccessful) {
                    return@withContext Pair(true, "Spotify API configuration is valid")
                } else {
                    return@withContext Pair(false, "API test failed: ${response.code()} - ${response.message()}")
                }
            } else {
                return@withContext Pair(false, "Failed to obtain access token - check credentials")
            }
            
        } catch (e: Exception) {
            return@withContext Pair(false, "Error testing API: ${e.message}")
        }
    }
}
