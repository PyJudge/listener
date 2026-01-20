package com.listener.presentation.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listener.data.local.db.dao.LocalFileDao
import com.listener.data.local.db.entity.LocalAudioFileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject

data class MediaFileUiState(
    val files: List<LocalAudioFileEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MediaFileViewModel @Inject constructor(
    private val localFileDao: LocalFileDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaFileUiState())
    val uiState: StateFlow<MediaFileUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            localFileDao.getAllFiles().collect { files ->
                _uiState.update { it.copy(files = files, isLoading = false) }
            }
        }
    }

    fun addFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val entity = withContext(Dispatchers.IO) {
                    val displayName = getFileName(uri)
                    val hash = computeFileHash(uri)

                    LocalAudioFileEntity(
                        contentHash = hash,
                        uri = uri.toString(),
                        displayName = displayName,
                        durationMs = null,
                        addedAt = System.currentTimeMillis()
                    )
                }
                localFileDao.insertFile(entity)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun deleteFile(hash: String) {
        viewModelScope.launch {
            localFileDao.deleteFileByHash(hash)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun getFileName(uri: Uri): String {
        var name = "Unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun computeFileHash(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
