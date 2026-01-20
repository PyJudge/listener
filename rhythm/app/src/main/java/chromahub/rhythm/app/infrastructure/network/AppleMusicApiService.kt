package chromahub.rhythm.app.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Service interface for Apple Music lyrics API
 * API Documentation: https://paxsenix.alwaysdata.net/
 */
interface AppleMusicApiService {
    /**
     * Search for songs on Apple Music by query string
     * @param query Search query (artist + song name)
     * @return List of matching songs
     */
    @GET("searchAppleMusic.php")
    suspend fun searchSongs(
        @Query("q") query: String
    ): List<AppleMusicSearchResult>
    
    /**
     * Get word-by-word synchronized lyrics for a specific song
     * @param id Apple Music song ID
     * @return Lyrics response with word-level timing
     */
    @GET("getAppleMusicLyrics.php")
    suspend fun getLyrics(
        @Query("id") id: String
    ): AppleMusicLyricsResponse
}
