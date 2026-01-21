package com.listener.presentation.transcription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.entity.TranscriptionQueueStatus
import com.listener.service.QueueItem
import com.listener.service.TranscriptionQueueManager
import com.listener.service.TranscriptionQueueState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TranscriptionQueueUiState(
    val queueState: TranscriptionQueueState = TranscriptionQueueState(),
    val showDeleteConfirm: Long? = null // item id to delete
)

@HiltViewModel
class TranscriptionQueueViewModel @Inject constructor(
    private val queueManager: TranscriptionQueueManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranscriptionQueueUiState())
    val uiState: StateFlow<TranscriptionQueueUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            queueManager.queueState.collect { queueState ->
                _uiState.update { it.copy(queueState = queueState) }
            }
        }
    }

    fun removeItem(id: Long) {
        viewModelScope.launch {
            queueManager.removeFromQueue(id)
            _uiState.update { it.copy(showDeleteConfirm = null) }
        }
    }

    fun showDeleteConfirm(id: Long) {
        _uiState.update { it.copy(showDeleteConfirm = id) }
    }

    fun hideDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = null) }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            queueManager.reorderQueue(fromIndex, toIndex)
        }
    }

    fun retry(id: Long) {
        viewModelScope.launch {
            queueManager.retry(id)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            queueManager.clearCompleted()
        }
    }

    fun getItemsForDrag(): List<QueueItem> {
        return _uiState.value.queueState.pendingItems
    }
}
