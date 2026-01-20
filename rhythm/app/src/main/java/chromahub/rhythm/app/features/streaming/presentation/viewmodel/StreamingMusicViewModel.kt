package chromahub.rhythm.app.features.streaming.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import chromahub.rhythm.app.core.domain.model.SourceType
import chromahub.rhythm.app.core.domain.model.StreamingConfig
import chromahub.rhythm.app.core.domain.model.StreamingQuality
import chromahub.rhythm.app.features.streaming.domain.model.BrowseCategory
import chromahub.rhythm.app.features.streaming.domain.model.StreamingAlbum
import chromahub.rhythm.app.features.streaming.domain.model.StreamingArtist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingPlaylist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing streaming music playback and library.
 * Handles authentication, browsing, and playback for streaming services.
 */
class StreamingMusicViewModel(application: Application) : AndroidViewModel(application) {
    
    // Authentication state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _currentService = MutableStateFlow(SourceType.SPOTIFY)
    val currentService: StateFlow<SourceType> = _currentService.asStateFlow()
    
    private val _streamingConfig = MutableStateFlow(StreamingConfig())
    val streamingConfig: StateFlow<StreamingConfig> = _streamingConfig.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Home content
    private val _recommendations = MutableStateFlow<List<StreamingSong>>(emptyList())
    val recommendations: StateFlow<List<StreamingSong>> = _recommendations.asStateFlow()
    
    private val _newReleases = MutableStateFlow<List<StreamingAlbum>>(emptyList())
    val newReleases: StateFlow<List<StreamingAlbum>> = _newReleases.asStateFlow()
    
    private val _featuredPlaylists = MutableStateFlow<List<StreamingPlaylist>>(emptyList())
    val featuredPlaylists: StateFlow<List<StreamingPlaylist>> = _featuredPlaylists.asStateFlow()
    
    // Browse content
    private val _browseCategories = MutableStateFlow<List<BrowseCategory>>(emptyList())
    val browseCategories: StateFlow<List<BrowseCategory>> = _browseCategories.asStateFlow()
    
    private val _topCharts = MutableStateFlow<List<StreamingSong>>(emptyList())
    val topCharts: StateFlow<List<StreamingSong>> = _topCharts.asStateFlow()
    
    // Library content
    private val _likedSongs = MutableStateFlow<List<StreamingSong>>(emptyList())
    val likedSongs: StateFlow<List<StreamingSong>> = _likedSongs.asStateFlow()
    
    private val _savedAlbums = MutableStateFlow<List<StreamingAlbum>>(emptyList())
    val savedAlbums: StateFlow<List<StreamingAlbum>> = _savedAlbums.asStateFlow()
    
    private val _followedArtists = MutableStateFlow<List<StreamingArtist>>(emptyList())
    val followedArtists: StateFlow<List<StreamingArtist>> = _followedArtists.asStateFlow()
    
    private val _savedPlaylists = MutableStateFlow<List<StreamingPlaylist>>(emptyList())
    val savedPlaylists: StateFlow<List<StreamingPlaylist>> = _savedPlaylists.asStateFlow()
    
    private val _downloadedSongs = MutableStateFlow<List<StreamingSong>>(emptyList())
    val downloadedSongs: StateFlow<List<StreamingSong>> = _downloadedSongs.asStateFlow()
    
    // Current playback state
    private val _currentSong = MutableStateFlow<StreamingSong?>(null)
    val currentSong: StateFlow<StreamingSong?> = _currentSong.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    // Queue
    private val _queue = MutableStateFlow<List<StreamingSong>>(emptyList())
    val queue: StateFlow<List<StreamingSong>> = _queue.asStateFlow()
    
    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<StreamingSearchResults>(StreamingSearchResults())
    val searchResults: StateFlow<StreamingSearchResults> = _searchResults.asStateFlow()
    
    init {
        checkAuthenticationStatus()
    }
    
    /**
     * Check if user is authenticated with the current service.
     */
    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            // TODO: Implement actual authentication check via repository
            _isAuthenticated.value = false
        }
    }
    
    /**
     * Select a streaming service.
     */
    fun selectService(service: SourceType) {
        _currentService.value = service
        checkAuthenticationStatus()
    }
    
    /**
     * Authenticate with the current streaming service.
     */
    fun authenticate() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // TODO: Implement actual authentication via repository
                // val success = repository.authenticate()
                // _isAuthenticated.value = success
                
                // Placeholder for now
                _isAuthenticated.value = true
                
                if (_isAuthenticated.value) {
                    loadHomeContent()
                }
            } catch (e: Exception) {
                _error.value = "Authentication failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Log out from the current streaming service.
     */
    fun logout() {
        viewModelScope.launch {
            // TODO: Implement actual logout via repository
            _isAuthenticated.value = false
            clearContent()
        }
    }
    
    /**
     * Load home screen content.
     */
    fun loadHomeContent() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // TODO: Load content from repository
                // _recommendations.value = repository.getRecommendations()
                // _newReleases.value = repository.getNewReleases()
                // _featuredPlaylists.value = repository.getFeaturedPlaylists()
            } catch (e: Exception) {
                _error.value = "Failed to load content: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh home screen content.
     */
    fun refreshHome() {
        loadHomeContent()
    }
    
    /**
     * Load browse categories.
     */
    fun loadBrowseCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // TODO: Load from repository
                // _browseCategories.value = repository.getBrowseCategories()
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load top charts.
     */
    fun loadTopCharts() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // TODO: Load from repository
                // _topCharts.value = repository.getTopCharts()
            } catch (e: Exception) {
                _error.value = "Failed to load charts: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load user's library content.
     */
    fun loadLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // TODO: Load from repository
                // _likedSongs.value = repository.getLikedSongs()
                // _savedAlbums.value = repository.getSavedAlbums()
                // _followedArtists.value = repository.getFollowedArtists()
                // _savedPlaylists.value = repository.getPlaylists()
            } catch (e: Exception) {
                _error.value = "Failed to load library: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Search across the streaming service.
     */
    fun search(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            _searchResults.value = StreamingSearchResults()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // TODO: Search via repository
                // val songs = repository.searchSongs(query)
                // val albums = repository.searchAlbums(query)
                // val artists = repository.searchArtists(query)
                // val playlists = repository.searchPlaylists(query)
                // 
                // _searchResults.value = StreamingSearchResults(
                //     songs = songs.filterIsInstance<StreamingSong>(),
                //     albums = albums.filterIsInstance<StreamingAlbum>(),
                //     artists = artists.filterIsInstance<StreamingArtist>(),
                //     playlists = playlists.filterIsInstance<StreamingPlaylist>()
                // )
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Play a streaming song.
     */
    fun playSong(song: StreamingSong) {
        viewModelScope.launch {
            if (!song.isPlayable) {
                _error.value = "This track is not available for playback"
                return@launch
            }
            
            _currentSong.value = song
            _isPlaying.value = true
            
            // TODO: Get streaming URL and start playback via MediaPlaybackService
        }
    }
    
    /**
     * Play an album.
     */
    fun playAlbum(album: StreamingAlbum) {
        viewModelScope.launch {
            val tracks = album.getTracks()
            if (tracks.isNotEmpty()) {
                _queue.value = tracks
                playSong(tracks.first())
            }
        }
    }
    
    /**
     * Play a playlist.
     */
    fun playPlaylist(playlist: StreamingPlaylist) {
        viewModelScope.launch {
            val tracks = playlist.getTracks()
            if (tracks.isNotEmpty()) {
                _queue.value = tracks
                playSong(tracks.first())
            }
        }
    }
    
    /**
     * Toggle play/pause.
     */
    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
        // TODO: Connect to MediaPlaybackService
    }
    
    /**
     * Skip to next song.
     */
    fun skipToNext() {
        val currentIndex = _queue.value.indexOf(_currentSong.value)
        if (currentIndex >= 0 && currentIndex < _queue.value.size - 1) {
            playSong(_queue.value[currentIndex + 1])
        }
    }
    
    /**
     * Skip to previous song.
     */
    fun skipToPrevious() {
        val currentIndex = _queue.value.indexOf(_currentSong.value)
        if (currentIndex > 0) {
            playSong(_queue.value[currentIndex - 1])
        }
    }
    
    /**
     * Seek to a position.
     */
    fun seekTo(progress: Float) {
        _progress.value = progress
        // TODO: Connect to MediaPlaybackService
    }
    
    /**
     * Like/save a song.
     */
    fun likeSong(song: StreamingSong) {
        viewModelScope.launch {
            try {
                // TODO: Implement via repository
                // repository.likeSong(song.id)
            } catch (e: Exception) {
                _error.value = "Failed to save song: ${e.message}"
            }
        }
    }
    
    /**
     * Unlike/unsave a song.
     */
    fun unlikeSong(song: StreamingSong) {
        viewModelScope.launch {
            try {
                // TODO: Implement via repository
                // repository.unlikeSong(song.id)
            } catch (e: Exception) {
                _error.value = "Failed to remove song: ${e.message}"
            }
        }
    }
    
    /**
     * Follow an artist.
     */
    fun followArtist(artist: StreamingArtist) {
        viewModelScope.launch {
            try {
                // TODO: Implement via repository
            } catch (e: Exception) {
                _error.value = "Failed to follow artist: ${e.message}"
            }
        }
    }
    
    /**
     * Save an album.
     */
    fun saveAlbum(album: StreamingAlbum) {
        viewModelScope.launch {
            try {
                // TODO: Implement via repository
            } catch (e: Exception) {
                _error.value = "Failed to save album: ${e.message}"
            }
        }
    }
    
    /**
     * Download a song for offline playback.
     */
    fun downloadSong(song: StreamingSong) {
        viewModelScope.launch {
            try {
                // TODO: Implement via repository
                // repository.downloadSong(song.id)
            } catch (e: Exception) {
                _error.value = "Download failed: ${e.message}"
            }
        }
    }
    
    /**
     * Set streaming quality.
     */
    fun setStreamingQuality(quality: StreamingQuality) {
        viewModelScope.launch {
            _streamingConfig.value = _streamingConfig.value.copy(streamingQuality = quality)
            // TODO: Persist via repository
        }
    }
    
    /**
     * Clear all loaded content.
     */
    private fun clearContent() {
        _recommendations.value = emptyList()
        _newReleases.value = emptyList()
        _featuredPlaylists.value = emptyList()
        _browseCategories.value = emptyList()
        _topCharts.value = emptyList()
        _likedSongs.value = emptyList()
        _savedAlbums.value = emptyList()
        _followedArtists.value = emptyList()
        _savedPlaylists.value = emptyList()
        _downloadedSongs.value = emptyList()
        _queue.value = emptyList()
        _currentSong.value = null
    }
    
    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }
}

/**
 * Container for streaming search results.
 */
data class StreamingSearchResults(
    val songs: List<StreamingSong> = emptyList(),
    val albums: List<StreamingAlbum> = emptyList(),
    val artists: List<StreamingArtist> = emptyList(),
    val playlists: List<StreamingPlaylist> = emptyList()
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
    
    val totalCount: Int
        get() = songs.size + albums.size + artists.size + playlists.size
}
