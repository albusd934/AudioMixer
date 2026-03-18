package com.audiomixer.app.utils

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import androidx.annotation.RequiresApi

class AppVolumeManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sessionGainMap = mutableMapOf<String, Float>()
    private val prefs = context.getSharedPreferences("volumes", Context.MODE_PRIVATE)

    init {
        @Suppress("UNCHECKED_CAST")
        (prefs.all as Map<String, Any>).forEach { (key, value) ->
            if (value is Float) sessionGainMap[key] = value
        }
    }

    fun getActiveAudioPackages(): List<String> {
        val packages = mutableSetOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getActivePackagesApi29(packages)
        }
        packages.addAll(getInstalledMediaApps())
        return packages.toList()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getActivePackagesApi29(packages: MutableSet<String>) {
        val configs: List<AudioPlaybackConfiguration> = audioManager.activePlaybackConfigurations
        for (config in configs) {
            val uid = getClientUid(config) ?: continue
            val pkgs = context.packageManager.getPackagesForUid(uid)
            pkgs?.forEach { packages.add(it) }
        }
    }

    private fun getClientUid(config: AudioPlaybackConfiguration): Int? {
        return try {
            val method = AudioPlaybackConfiguration::class.java.getDeclaredMethod("getClientUid")
            method.isAccessible = true
            method.invoke(config) as? Int
        } catch (e: Exception) { null }
    }

    private fun getAudioSessionId(config: AudioPlaybackConfiguration): Int? {
        return try {
            val method = AudioPlaybackConfiguration::class.java.getDeclaredMethod("getSessionId")
            method.isAccessible = true
            method.invoke(config) as? Int
        } catch (e: Exception) { null }
    }

    fun isAppPlaying(packageName: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val configs = audioManager.activePlaybackConfigurations
            for (config in configs) {
                val uid = getClientUid(config) ?: continue
                val pkgs = context.packageManager.getPackagesForUid(uid)
                if (pkgs?.contains(packageName) == true) return true
            }
        }
        return false
    }

    fun setAppVolume(packageName: String, volume: Float) {
        sessionGainMap[packageName] = volume
        prefs.edit().putFloat(packageName, volume).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setVolumeViaAudioEffect(packageName, volume)
        } else {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * maxVol).toInt(), 0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setVolumeViaAudioEffect(packageName: String, volume: Float) {
        val configs = audioManager.activePlaybackConfigurations
        for (config in configs) {
            val uid = getClientUid(config) ?: continue
            val pkgs = context.packageManager.getPackagesForUid(uid) ?: continue
            if (pkgs.contains(packageName)) {
                val sessionId = getAudioSessionId(config) ?: continue
                applyGainToSession(sessionId, volume)
            }
        }
    }

    private fun applyGainToSession(sessionId: Int, volume: Float) {
        if (sessionId == AudioManager.AUDIO_SESSION_ID_GENERATE) return
        try {
            val enhancer = android.media.audiofx.LoudnessEnhancer(sessionId)
            if (volume < 1.0f) {
                val gainMB = if (volume <= 0f) -9600
                    else (2000 * Math.log10(volume.toDouble())).toInt() * 100
                enhancer.setTargetGain(gainMB)
                enhancer.enabled = true
            } else {
                enhancer.enabled = false
                enhancer.release()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getAppVolume(packageName: String): Float = sessionGainMap[packageName] ?: 1.0f

    private fun getInstalledMediaApps(): List<String> {
        val wellKnownMediaApps = listOf(
            "com.spotify.music", "com.google.android.youtube",
            "com.google.android.apps.youtube.music", "com.amazon.music",
            "com.apple.android.music", "com.soundcloud.android",
            "com.pandora.android", "com.netflix.mediaclient",
            "com.tidal.music", "com.deezer.android",
            "com.audible.application", "com.google.android.apps.podcasts",
            "com.bamtechMedia.disneyplus", "com.hbo.hbonow",
            "com.twitch.android.app", "com.discord",
            "com.whatsapp", "org.telegram.messenger",
            "com.google.android.apps.tachyon",
            "us.zoom.videomeetings", "com.microsoft.teams"
        )
        val pm = context.packageManager
        return wellKnownMediaApps.filter { pkg ->
            try { pm.getApplicationInfo(pkg, 0); true }
            catch (e: PackageManager.NameNotFoundException) { false }
        }
    }
}
