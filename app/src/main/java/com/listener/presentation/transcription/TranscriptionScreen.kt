package com.listener.presentation.transcription

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.listener.presentation.components.EmptyState
import com.listener.presentation.components.ListenerButton
import com.listener.presentation.components.ButtonStyle
import com.listener.presentation.theme.ListenerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(
    sourceId: String,
    onNavigateBack: () -> Unit = {},
    onTranscriptionComplete: () -> Unit = {},
    viewModel: TranscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.overallProgress,
        label = "progress"
    )

    // Navigate when complete or error
    LaunchedEffect(uiState.step) {
        if (uiState.step == UiTranscriptionStep.COMPLETE) {
            onTranscriptionComplete()
        }
        if (uiState.step == UiTranscriptionStep.ERROR) {
            // 에러 메시지 확인 시간 후 자동 뒤로가기
            delay(2000)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.step) {
                            UiTranscriptionStep.IDLE -> "Ready"
                            UiTranscriptionStep.DOWNLOADING -> "Downloading..."
                            UiTranscriptionStep.PREPROCESSING -> "Compressing..."
                            UiTranscriptionStep.TRANSCRIBING -> "Transcribing"
                            UiTranscriptionStep.PROCESSING -> "Processing"
                            UiTranscriptionStep.COMPLETE -> "Complete"
                            UiTranscriptionStep.ERROR -> "Error"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState.step) {
                UiTranscriptionStep.ERROR -> {
                    ErrorContent(
                        errorMessage = uiState.errorMessage ?: "Unknown error",
                        onRetry = { viewModel.retry() }
                    )
                }
                UiTranscriptionStep.COMPLETE -> {
                    CompleteContent(chunkCount = uiState.chunkCount)
                }
                else -> {
                    ProgressContent(
                        progress = animatedProgress,
                        step = uiState.step
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressContent(
    progress: Float,
    step: UiTranscriptionStep
) {
    CircularProgressIndicator(
        progress = { progress },
        modifier = Modifier.size(120.dp),
        strokeWidth = 8.dp,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "${(progress * 100).toInt()}%",
        style = MaterialTheme.typography.headlineLarge
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = when (step) {
            UiTranscriptionStep.DOWNLOADING -> "Downloading audio file..."
            UiTranscriptionStep.PREPROCESSING -> "Compressing audio for upload..."
            UiTranscriptionStep.TRANSCRIBING -> "Processing audio with Whisper AI"
            UiTranscriptionStep.PROCESSING -> "Creating learning chunks..."
            else -> "Getting ready..."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    if (step != UiTranscriptionStep.DOWNLOADING) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when (step) {
                UiTranscriptionStep.PREPROCESSING -> "Compressing to optimize for transcription..."
                else -> "This may take a few minutes depending on the audio length"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit
) {
    EmptyState(
        icon = Icons.Outlined.Error,
        title = "Transcription Failed",
        description = errorMessage,
        actionLabel = "Try Again",
        onAction = onRetry
    )
}

@Composable
private fun CompleteContent(
    chunkCount: Int
) {
    Text(
        text = "Transcription Complete",
        style = MaterialTheme.typography.headlineMedium
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "$chunkCount learning chunks created",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(showBackground = true)
@Composable
private fun TranscriptionScreenPreview() {
    ListenerTheme {
        // Preview without ViewModel
    }
}
