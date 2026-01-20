package chromahub.rhythm.app.shared.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import chromahub.rhythm.app.core.domain.model.AppMode
import chromahub.rhythm.app.core.domain.model.SourceType
import chromahub.rhythm.app.core.domain.model.StreamingConfig
import chromahub.rhythm.app.core.domain.model.StreamingQuality
import chromahub.rhythm.app.shared.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the application mode (Local vs Streaming).
 * Handles mode switching and persists user preferences.
 */
class AppModeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val userPreferencesRepository = UserPreferencesRepository.getInstance(application)
    
    /**
     * Current application mode (Local or Streaming).
     */
    val currentMode: StateFlow<AppMode> = userPreferencesRepository.appMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = userPreferencesRepository.getCurrentAppMode()
        )
    
    /**
     * Current streaming configuration.
     */
    val streamingConfig: StateFlow<StreamingConfig> = userPreferencesRepository.streamingConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StreamingConfig()
        )
    
    private val _isModeSwitching = MutableStateFlow(false)
    
    /**
     * Whether mode is currently being switched (for loading UI).
     */
    val isModeSwitching: StateFlow<Boolean> = _isModeSwitching.asStateFlow()
    
    /**
     * Switch to a new application mode.
     */
    fun switchMode(newMode: AppMode) {
        viewModelScope.launch {
            _isModeSwitching.value = true
            
            try {
                userPreferencesRepository.setAppMode(newMode)
                
                // Add a small delay for smooth transition
                kotlinx.coroutines.delay(200)
            } finally {
                _isModeSwitching.value = false
            }
        }
    }
    
    /**
     * Toggle between Local and Streaming modes.
     */
    fun toggleMode() {
        val newMode = when (currentMode.value) {
            AppMode.LOCAL -> AppMode.STREAMING
            AppMode.STREAMING -> AppMode.LOCAL
        }
        switchMode(newMode)
    }
    
    /**
     * Set the active streaming service.
     */
    fun setActiveStreamingService(service: SourceType) {
        viewModelScope.launch {
            userPreferencesRepository.setActiveService(service)
        }
    }
    
    /**
     * Set streaming quality preference.
     */
    fun setStreamingQuality(quality: StreamingQuality) {
        viewModelScope.launch {
            userPreferencesRepository.setStreamingQuality(quality)
        }
    }
    
    /**
     * Update authentication status for the current streaming service.
     */
    fun setAuthenticated(authenticated: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAuthenticated(authenticated)
        }
    }
    
    /**
     * Check if streaming mode is available (authenticated with at least one service).
     */
    fun isStreamingAvailable(): Boolean {
        return streamingConfig.value.isAuthenticated
    }
}
