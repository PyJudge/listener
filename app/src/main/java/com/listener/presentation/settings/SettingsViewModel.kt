package com.listener.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.repository.AppSettings
import com.listener.data.repository.SettingsRepository
import com.listener.service.AudioCacheManager
import com.listener.service.RecordingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val audioCacheSize: Long = 0L,
    val recordingsSize: Long = 0L,
    val isLoading: Boolean = false,
    val showApiKeyDialog: Boolean = false,
    val showClearCacheDialog: Boolean = false,
    val showLanguageDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val audioCacheManager: AudioCacheManager,
    private val recordingManager: RecordingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        loadStorageInfo()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun loadStorageInfo() {
        viewModelScope.launch {
            val cacheSize = audioCacheManager.getCacheSize()
            _uiState.update { it.copy(audioCacheSize = cacheSize) }
        }
    }

    fun setRepeatCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultRepeatCount(count)
        }
    }

    fun setGapRatio(ratio: Float) {
        viewModelScope.launch {
            settingsRepository.setDefaultGapRatio(ratio)
        }
    }

    fun setAutoRecording(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoRecordingEnabled(enabled)
        }
    }

    fun setTranscriptionLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setTranscriptionLanguage(language)
        }
        dismissLanguageDialog()
    }

    fun setMinChunkDuration(seconds: Float) {
        viewModelScope.launch {
            settingsRepository.setMinChunkMs((seconds * 1000).toLong())
        }
    }

    fun setSentenceOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSentenceOnly(enabled)
        }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.setOpenAiApiKey(key)
        }
        dismissApiKeyDialog()
    }

    fun clearAudioCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            audioCacheManager.clearCache()
            loadStorageInfo()
            _uiState.update { it.copy(isLoading = false, showClearCacheDialog = false) }
        }
    }

    fun showApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = true) }
    }

    fun dismissApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = false) }
    }

    fun showClearCacheDialog() {
        _uiState.update { it.copy(showClearCacheDialog = true) }
    }

    fun dismissClearCacheDialog() {
        _uiState.update { it.copy(showClearCacheDialog = false) }
    }

    fun showLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = true) }
    }

    fun dismissLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = false) }
    }
}
