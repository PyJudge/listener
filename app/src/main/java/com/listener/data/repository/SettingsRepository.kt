package com.listener.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    // Learning
    val defaultRepeatCount: Int = 2,
    val defaultGapRatio: Float = 0.4f,
    val playMode: String = "LR", // "NORMAL" | "LR" | "LRLR"
    val autoRecordingEnabled: Boolean = true,

    // Transcription
    val transcriptionLanguage: String = "en",
    val minChunkMs: Long = 1200L,
    val sentenceOnly: Boolean = true,
    val skipPreprocessingForSmallFiles: Boolean = true,

    // API
    val transcriptionProvider: String = "groq", // "groq" | "openai"
    val openAiApiKey: String = "",
    val groqApiKey: String = ""
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val DEFAULT_REPEAT_COUNT = intPreferencesKey("default_repeat_count")
        val DEFAULT_GAP_RATIO = floatPreferencesKey("default_gap_ratio")
        val PLAY_MODE = stringPreferencesKey("play_mode")
        val AUTO_RECORDING_ENABLED = booleanPreferencesKey("auto_recording_enabled")
        val TRANSCRIPTION_LANGUAGE = stringPreferencesKey("transcription_language")
        val MIN_CHUNK_MS = longPreferencesKey("min_chunk_ms")
        val SENTENCE_ONLY = booleanPreferencesKey("sentence_only")
        val SKIP_PREPROCESSING = booleanPreferencesKey("skip_preprocessing_for_small_files")
        val TRANSCRIPTION_PROVIDER = stringPreferencesKey("transcription_provider")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val PENDING_RECHUNK = booleanPreferencesKey("pending_rechunk")
    }

    val settings: Flow<AppSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AppSettings(
                defaultRepeatCount = prefs[Keys.DEFAULT_REPEAT_COUNT] ?: 2,
                defaultGapRatio = prefs[Keys.DEFAULT_GAP_RATIO] ?: 0.4f,
                playMode = prefs[Keys.PLAY_MODE] ?: "LR",
                autoRecordingEnabled = prefs[Keys.AUTO_RECORDING_ENABLED] ?: true,
                transcriptionLanguage = prefs[Keys.TRANSCRIPTION_LANGUAGE] ?: "en",
                minChunkMs = prefs[Keys.MIN_CHUNK_MS] ?: 1200L,
                sentenceOnly = prefs[Keys.SENTENCE_ONLY] ?: true,
                skipPreprocessingForSmallFiles = prefs[Keys.SKIP_PREPROCESSING] ?: true,
                transcriptionProvider = prefs[Keys.TRANSCRIPTION_PROVIDER] ?: "groq",
                openAiApiKey = prefs[Keys.OPENAI_API_KEY] ?: "",
                groqApiKey = prefs[Keys.GROQ_API_KEY] ?: ""
            )
        }

    val pendingRechunk: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.PENDING_RECHUNK] ?: false }

    suspend fun setDefaultRepeatCount(count: Int) {
        dataStore.edit { it[Keys.DEFAULT_REPEAT_COUNT] = count.coerceIn(1, 5) }
    }

    suspend fun setDefaultGapRatio(ratio: Float) {
        dataStore.edit { it[Keys.DEFAULT_GAP_RATIO] = ratio.coerceIn(0.2f, 1.0f) }
    }

    suspend fun setPlayMode(mode: String) {
        dataStore.edit { it[Keys.PLAY_MODE] = mode }
    }

    suspend fun setAutoRecordingEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_RECORDING_ENABLED] = enabled }
    }

    suspend fun setTranscriptionLanguage(language: String) {
        dataStore.edit { it[Keys.TRANSCRIPTION_LANGUAGE] = language }
    }

    suspend fun setMinChunkMs(ms: Long) {
        dataStore.edit {
            it[Keys.MIN_CHUNK_MS] = ms.coerceIn(500L, 3000L)
            it[Keys.PENDING_RECHUNK] = true
        }
    }

    suspend fun setSentenceOnly(enabled: Boolean) {
        dataStore.edit {
            it[Keys.SENTENCE_ONLY] = enabled
            it[Keys.PENDING_RECHUNK] = true
        }
    }

    suspend fun setOpenAiApiKey(key: String) {
        dataStore.edit { it[Keys.OPENAI_API_KEY] = key }
    }

    suspend fun setGroqApiKey(key: String) {
        dataStore.edit { it[Keys.GROQ_API_KEY] = key }
    }

    suspend fun setTranscriptionProvider(provider: String) {
        dataStore.edit { it[Keys.TRANSCRIPTION_PROVIDER] = provider }
    }

    suspend fun setSkipPreprocessing(skip: Boolean) {
        dataStore.edit { it[Keys.SKIP_PREPROCESSING] = skip }
    }

    suspend fun setPendingRechunk(pending: Boolean) {
        dataStore.edit { it[Keys.PENDING_RECHUNK] = pending }
    }
}
