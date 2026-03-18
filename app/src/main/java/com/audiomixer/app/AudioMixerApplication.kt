package com.audiomixer.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class AudioMixerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Main service channel
        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Audio Mixer Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Audio Mixer active in the background"
            setSound(null, null)
            enableVibration(false)
        }

        // Alert channel for permission warnings
        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Audio Mixer Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications about permissions and important updates"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(serviceChannel, alertChannel))
    }

    companion object {
        const val CHANNEL_SERVICE = "AudioMixerChannel"
        const val CHANNEL_ALERTS  = "AudioMixerAlerts"
    }
}
