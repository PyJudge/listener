package com.listener.presentation.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.entity.PodcastEpisodeEntity
import com.listener.data.local.db.entity.SubscribedPodcastEntity
import com.listener.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

sealed interface PodcastDetailUiState {
    data object Loading : PodcastDetailUiState
    data class Success(
        val podcast: SubscribedPodcastEntity,
        val episodes: List<PodcastEpisodeEntity>,
        val isRefreshing: Boolean = false
    ) : PodcastDetailUiState
    data class Error(val message: String) : PodcastDetailUiState
}

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastRepository: PodcastRepository
) : ViewModel() {

    private val encodedFeedUrl: String = checkNotNull(savedStateHandle["feedUrl"])
    val feedUrl: String = URLDecoder.decode(encodedFeedUrl, "UTF-8")

    private val _uiState = MutableStateFlow<PodcastDetailUiState>(PodcastDetailUiState.Loading)
    val uiState: StateFlow<PodcastDetailUiState> = _uiState.asStateFlow()

    init {
        loadPodcastDetail()
    }

    private fun loadPodcastDetail() {
        viewModelScope.launch {
            val podcast = podcastRepository.getSubscription(feedUrl)

            if (podcast == null) {
                _uiState.value = PodcastDetailUiState.Error("Podcast not found")
                return@launch
            }

            // Load local episodes first (offline-first)
            podcastRepository.getEpisodes(feedUrl).collect { episodes ->
                _uiState.update { currentState ->
                    when (currentState) {
                        is PodcastDetailUiState.Success -> currentState.copy(episodes = episodes)
                        else -> PodcastDetailUiState.Success(
                            podcast = podcast,
                            episodes = episodes,
                            isRefreshing = currentState is PodcastDetailUiState.Loading
                        )
                    }
                }
            }
        }

        // Refresh from RSS in background
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                when (currentState) {
                    is PodcastDetailUiState.Success -> currentState.copy(isRefreshing = true)
                    else -> currentState
                }
            }

            podcastRepository.refreshEpisodes(feedUrl)
                .onSuccess {
                    // Reload podcast to get updated description from RSS
                    val updatedPodcast = podcastRepository.getSubscription(feedUrl)
                    _uiState.update { currentState ->
                        when (currentState) {
                            is PodcastDetailUiState.Success -> currentState.copy(
                                podcast = updatedPodcast ?: currentState.podcast,
                                isRefreshing = false
                            )
                            is PodcastDetailUiState.Loading -> {
                                if (updatedPodcast != null) {
                                    PodcastDetailUiState.Success(
                                        podcast = updatedPodcast,
                                        episodes = it,
                                        isRefreshing = false
                                    )
                                } else {
                                    PodcastDetailUiState.Error("Podcast not found")
                                }
                            }
                            else -> currentState
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { currentState ->
                        when (currentState) {
                            is PodcastDetailUiState.Success -> currentState.copy(isRefreshing = false)
                            is PodcastDetailUiState.Loading -> PodcastDetailUiState.Error(
                                error.message ?: "Failed to load episodes"
                            )
                            else -> currentState
                        }
                    }
                }
        }
    }

    fun markAsRead(episodeId: String) {
        viewModelScope.launch {
            podcastRepository.markEpisodeAsRead(episodeId)
        }
    }

    fun unsubscribe(onComplete: () -> Unit) {
        viewModelScope.launch {
            podcastRepository.unsubscribe(feedUrl)
            onComplete()
        }
    }
}
