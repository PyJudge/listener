package com.listener

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ListenerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Transcription channel - for background transcription progress
            val transcriptionChannel = NotificationChannel(
                CHANNEL_TRANSCRIPTION,
                "Transcription",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows transcription progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(transcriptionChannel)
        }
    }

    companion object {
        const val CHANNEL_TRANSCRIPTION = "transcription"
    }
}
