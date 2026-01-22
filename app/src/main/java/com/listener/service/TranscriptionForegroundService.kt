package com.listener.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.listener.ListenerApp
import com.listener.MainActivity
import com.listener.R
import com.listener.domain.repository.TranscriptionRepository
import com.listener.domain.repository.TranscriptionState
import com.listener.domain.repository.TranscriptionStep
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service for background transcription
 * - Keeps CPU awake with WakeLock
 * - Shows progress notification
 * - Protected from system killing when app is in background
 */
@AndroidEntryPoint
class TranscriptionForegroundService : Service() {

    companion object {
        private const val TAG = "TranscriptionService"
        private const val NOTIFICATION_ID = 2001
        private val CHANNEL_ID = ListenerApp.CHANNEL_TRANSCRIPTION

        const val ACTION_START = "com.listener.action.START_TRANSCRIPTION"
        const val ACTION_CANCEL = "com.listener.action.CANCEL_TRANSCRIPTION"
        const val EXTRA_SOURCE_ID = "sourceId"
        const val EXTRA_LANGUAGE = "language"

        private const val WAKE_LOCK_TAG = "Listener::TranscriptionWakeLock"
        private const val WAKE_LOCK_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes

        fun createStartIntent(context: Context, sourceId: String, language: String): Intent {
            return Intent(context, TranscriptionForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SOURCE_ID, sourceId)
                putExtra(EXTRA_LANGUAGE, language)
            }
        }

        fun createCancelIntent(context: Context): Intent {
            return Intent(context, TranscriptionForegroundService::class.java).apply {
                action = ACTION_CANCEL
            }
        }
    }

    @Inject
    lateinit var transcriptionRepository: TranscriptionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var transcriptionJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Notification channel is created in ListenerApp.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                val sourceId = intent.getStringExtra(EXTRA_SOURCE_ID)
                val language = intent.getStringExtra(EXTRA_LANGUAGE)

                if (sourceId != null && language != null) {
                    startTranscription(sourceId, language)
                } else {
                    Log.e(TAG, "Missing sourceId or language")
                    stopSelf()
                }
            }
            ACTION_CANCEL -> {
                cancelTranscription()
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startTranscription(sourceId: String, language: String) {
        Log.i(TAG, "Foreground service started for transcription: sourceId=$sourceId")

        // Start foreground with initial notification
        startForeground(NOTIFICATION_ID, createNotification("Preparing...", 0))

        // Acquire wake lock to keep CPU running
        acquireWakeLock()

        // Cancel any existing observation job
        transcriptionJob?.cancel()

        // Observe transcription state and update notification
        // Note: The actual transcription is started by Repository, not here
        transcriptionJob = serviceScope.launch {
            var hasSeenProgress = false
            transcriptionRepository.transcriptionState.collect { state ->
                when (state) {
                    is TranscriptionState.InProgress -> {
                        hasSeenProgress = true
                        val (stepText, progress) = getProgressInfo(state)
                        updateNotification(stepText, progress)
                    }
                    is TranscriptionState.Complete -> {
                        Log.i(TAG, "Transcription complete: ${state.chunkCount} chunks")
                        showCompletionNotification(state.chunkCount)
                        stopSelfDelayed()
                    }
                    is TranscriptionState.Error -> {
                        Log.e(TAG, "Transcription error: ${state.message}")
                        showErrorNotification(state.message)
                        stopSelfDelayed()
                    }
                    TranscriptionState.Idle -> {
                        // Only stop if we've seen progress before (meaning transcription was cancelled)
                        // Ignore initial Idle state
                        if (hasSeenProgress) {
                            Log.d(TAG, "Transcription cancelled, stopping service")
                            stopSelfDelayed()
                        } else {
                            Log.d(TAG, "Initial Idle state, waiting for transcription to start")
                        }
                    }
                }
            }
        }
    }

    private fun cancelTranscription() {
        Log.i(TAG, "Cancelling transcription")
        transcriptionJob?.cancel()
        transcriptionRepository.cancelTranscription()
        releaseWakeLock()
        stopSelf()
    }

    private fun stopSelfDelayed() {
        serviceScope.launch {
            kotlinx.coroutines.delay(1000) // Short delay to ensure notification is shown
            releaseWakeLock()
            stopSelf()
        }
    }

    private fun getProgressInfo(state: TranscriptionState.InProgress): Pair<String, Int> {
        return when (state.step) {
            TranscriptionStep.DOWNLOADING -> {
                "Downloading..." to (state.downloadProgress * 100).toInt()
            }
            TranscriptionStep.PREPROCESSING -> {
                "Compressing..." to (state.preprocessProgress * 100).toInt()
            }
            TranscriptionStep.TRANSCRIBING -> {
                "Transcribing..." to (state.transcriptionProgress * 100).toInt()
            }
            TranscriptionStep.PROCESSING -> {
                "Processing..." to 95
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            ).apply {
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
            Log.d(TAG, "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }

    private fun createNotification(text: String, progress: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = PendingIntent.getService(
            this,
            1,
            createCancelIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Transcription")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .addAction(0, "Cancel", cancelIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val notification = createNotification(text, progress)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(chunkCount: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Transcription Complete")
            .setContentText("$chunkCount chunks created")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Transcription Failed")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}
