package com.listener.presentation.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.dao.PodcastDao
import com.listener.data.local.db.entity.SubscribedPodcastEntity
import com.listener.data.remote.api.ITunesApi
import com.listener.data.remote.dto.ITunesPodcast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PodcastUiState(
    val subscriptions: List<SubscribedPodcastEntity> = emptyList(),
    val searchResults: List<ITunesPodcast> = emptyList(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val podcastDao: PodcastDao,
    private val iTunesApi: ITunesApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(PodcastUiState())
    val uiState: StateFlow<PodcastUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadSubscriptions()
        observeSearchQuery()
    }

    private fun loadSubscriptions() {
        viewModelScope.launch {
            podcastDao.getAllSubscriptions().collect { subscriptions ->
                _uiState.update { it.copy(subscriptions = subscriptions, isLoading = false) }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collectLatest { query ->
                    searchPodcasts(query)
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    private suspend fun searchPodcasts(query: String) {
        _uiState.update { it.copy(isSearching = true, error = null) }
        try {
            val response = iTunesApi.searchPodcasts(term = query)
            _uiState.update {
                it.copy(
                    searchResults = response.results,
                    isSearching = false
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    error = e.message,
                    isSearching = false
                )
            }
        }
    }

    fun subscribe(podcast: ITunesPodcast) {
        viewModelScope.launch {
            val feedUrl = podcast.feedUrl ?: return@launch
            val maxOrder = podcastDao.getMaxOrderIndex() ?: -1
            val entity = SubscribedPodcastEntity(
                feedUrl = feedUrl,
                collectionId = podcast.collectionId,
                title = podcast.collectionName,
                description = podcast.description,
                artworkUrl = podcast.artworkUrl600 ?: podcast.artworkUrl100,
                lastCheckedAt = System.currentTimeMillis(),
                addedAt = System.currentTimeMillis(),
                orderIndex = maxOrder + 1
            )
            podcastDao.insertSubscription(entity)
        }
    }

    fun unsubscribe(feedUrl: String) {
        viewModelScope.launch {
            podcastDao.deleteSubscriptionByFeedUrl(feedUrl)
        }
    }

    fun reorderPodcasts(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = _uiState.value.subscriptions.toMutableList()
            if (fromIndex < 0 || fromIndex >= currentList.size ||
                toIndex < 0 || toIndex >= currentList.size) {
                return@launch
            }

            val movedItem = currentList.removeAt(fromIndex)
            currentList.add(toIndex, movedItem)

            // Update order indices in database
            currentList.forEachIndexed { index, podcast ->
                podcastDao.updateOrderIndex(podcast.feedUrl, index)
            }
        }
    }

    fun isSubscribed(feedUrl: String?): Boolean {
        return feedUrl != null && _uiState.value.subscriptions.any { it.feedUrl == feedUrl }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
