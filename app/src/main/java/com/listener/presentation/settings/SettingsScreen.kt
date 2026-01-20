package com.listener.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.listener.service.AudioCacheManager
import com.listener.presentation.theme.ListenerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Learning Section
            item {
                SettingsSectionHeader(title = "Learning")
            }

            item {
                SettingsSliderItem(
                    title = "Default Repeat Count",
                    value = settings.defaultRepeatCount.toFloat(),
                    valueRange = 1f..5f,
                    steps = 3,
                    valueLabel = "${settings.defaultRepeatCount}x",
                    onValueChange = { viewModel.setRepeatCount(it.toInt()) }
                )
            }

            item {
                SettingsSliderItem(
                    title = "Default Gap Ratio",
                    value = settings.defaultGapRatio,
                    valueRange = 0.2f..1.0f,
                    steps = 3,
                    valueLabel = "${(settings.defaultGapRatio * 100).toInt()}%",
                    onValueChange = { viewModel.setGapRatio(it) }
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Auto Recording",
                    subtitle = "Automatically record during gap",
                    checked = settings.autoRecordingEnabled,
                    onCheckedChange = { viewModel.setAutoRecording(it) }
                )
            }

            // Transcription Section
            item {
                SettingsSectionHeader(title = "Transcription")
            }

            item {
                SettingsClickableItem(
                    title = "Language",
                    value = getLanguageName(settings.transcriptionLanguage),
                    onClick = { viewModel.showLanguageDialog() }
                )
            }

            item {
                SettingsSliderItem(
                    title = "Minimum Chunk Duration",
                    value = settings.minChunkMs / 1000f,
                    valueRange = 0.5f..3.0f,
                    steps = 4,
                    valueLabel = "${settings.minChunkMs / 1000.0}s",
                    onValueChange = { viewModel.setMinChunkDuration(it) }
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Sentence Unit Only",
                    subtitle = "Split only at sentence endings (. ! ?)",
                    checked = settings.sentenceOnly,
                    onCheckedChange = { viewModel.setSentenceOnly(it) }
                )
            }

            // Storage Section
            item {
                SettingsSectionHeader(title = "Storage")
            }

            item {
                SettingsStorageItem(
                    title = "Audio Cache",
                    currentSize = uiState.audioCacheSize,
                    maxSize = AudioCacheManager.MAX_CACHE_SIZE_BYTES,
                    onClick = { viewModel.showClearCacheDialog() }
                )
            }

            item {
                SettingsClickableItem(
                    title = "Clear All Cache",
                    value = "",
                    onClick = { viewModel.showClearCacheDialog() },
                    showArrow = false,
                    textColor = MaterialTheme.colorScheme.error
                )
            }

            // API Section
            item {
                SettingsSectionHeader(title = "API")
            }

            item {
                SettingsClickableItem(
                    title = "OpenAI API Key",
                    value = if (settings.openAiApiKey.isNotEmpty()) "Configured" else "Not set",
                    onClick = { viewModel.showApiKeyDialog() }
                )
            }

            // Info Section
            item {
                SettingsSectionHeader(title = "Info")
            }

            item {
                SettingsInfoItem(
                    title = "Version",
                    value = "1.0.0"
                )
            }
        }
    }

    // Dialogs
    if (uiState.showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = settings.openAiApiKey,
            onSave = { viewModel.setApiKey(it) },
            onDismiss = { viewModel.dismissApiKeyDialog() }
        )
    }

    if (uiState.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearCacheDialog() },
            title = { Text("Clear Cache") },
            text = {
                Text("This will delete all cached audio files (${formatSize(uiState.audioCacheSize)}). Downloaded content will need to be re-downloaded.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearAudioCache() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearCacheDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showLanguageDialog) {
        LanguageDialog(
            currentLanguage = settings.transcriptionLanguage,
            onSelect = { viewModel.setTranscriptionLanguage(it) },
            onDismiss = { viewModel.dismissLanguageDialog() }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsClickableItem(
    title: String,
    value: String,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showArrow) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SettingsSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsStorageItem(
    title: String,
    currentSize: Long,
    maxSize: Long,
    onClick: () -> Unit
) {
    val progress = (currentSize.toFloat() / maxSize).coerceIn(0f, 1f)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${formatSize(currentSize)} / ${formatSize(maxSize)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (progress > 0.9f) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("OpenAI API Key") },
        text = {
            Column {
                Text(
                    text = "Enter your OpenAI API key for transcription",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(key) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LanguageDialog(
    currentLanguage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "ko" to "Korean",
        "ja" to "Japanese",
        "zh" to "Chinese",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = name)
                        if (code == currentLanguage) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun getLanguageName(code: String): String {
    return when (code) {
        "en" -> "English"
        "ko" -> "Korean"
        "ja" -> "Japanese"
        "zh" -> "Chinese"
        "es" -> "Spanish"
        "fr" -> "French"
        "de" -> "German"
        else -> code
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ListenerTheme {
        // Preview without ViewModel
    }
}
