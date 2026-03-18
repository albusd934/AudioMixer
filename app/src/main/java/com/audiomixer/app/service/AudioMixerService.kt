package com.audiomixer.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.audiomixer.app.R
import com.audiomixer.app.ui.MainActivity
import com.audiomixer.app.utils.AppVolumeManager
import com.audiomixer.app.utils.VolumePreferencesManager

class AudioMixerService : Service() {

    private val binder = LocalBinder()
    private lateinit var audioManager: AudioManager
    private lateinit var appVolumeManager: AppVolumeManager
    private lateinit var volumePrefs: VolumePreferencesManager

    // Per-app volume & mute state (in-memory, synced to prefs)
    private val appVolumes = mutableMapOf<String, Float>()
    private val appMutes = mutableMapOf<String, Boolean>()

    companion object {
        const val CHANNEL_ID = "AudioMixerChannel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.audiomixer.STOP"
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioMixerService = this@AudioMixerService
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        appVolumeManager = AppVolumeManager(this)
        volumePrefs = VolumePreferencesManager(this)

        // Restore persisted state into in-memory maps
        appVolumes.putAll(volumePrefs.getAllSavedVolumes())
        appMutes.putAll(volumePrefs.getAllSavedMutes())

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    /**
     * Set volume for a specific app (0.0 – 1.0).
     * Uses AudioManager's setStreamVolume for system-level streams and
     * AudioEffect / VolumeShaper APIs for per-UID adjustments on Android 10+.
     */
    fun setAppVolume(packageName: String, volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        appVolumes[packageName] = clampedVolume
        volumePrefs.saveVolume(packageName, clampedVolume)

        // Apply via AppVolumeManager which handles API level differences
        if (appMutes[packageName] != true) {
            appVolumeManager.setAppVolume(packageName, clampedVolume)
        }
    }

    /**
     * Mute or unmute a specific app.
     */
    fun setAppMute(packageName: String, muted: Boolean) {
        appMutes[packageName] = muted
        volumePrefs.saveMute(packageName, muted)
        if (muted) {
            appVolumeManager.setAppVolume(packageName, 0f)
        } else {
            val savedVolume = appVolumes[packageName] ?: 1.0f
            appVolumeManager.setAppVolume(packageName, savedVolume)
        }
    }

    fun getAppVolume(packageName: String): Float = appVolumes[packageName] ?: 1.0f
    fun isAppMuted(packageName: String): Boolean = appMutes[packageName] ?: false

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audio Mixer Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Audio Mixer running in the background"
            setSound(null, null)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioMixerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Mixer Active")
            .setContentText("Managing audio for multiple apps")
            .setSmallIcon(R.drawable.ic_mixer_notification)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Restore all volumes to full when service stops
        appVolumes.keys.forEach { pkg ->
            appVolumeManager.setAppVolume(pkg, 1.0f)
        }
    }
}
