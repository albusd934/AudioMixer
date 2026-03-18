package com.audiomixer.app.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.session.MediaSessionManager
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

        // Method 1: AudioPlaybackConfiguration — apps ACTIVELY playing right now
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getActivePackagesViaPlayback(packages)
        }

        // Method 2: MediaSessionManager — media sessions (Samsung-friendly)
        getActivePackagesViaMediaSession(packages)

        // Method 3: AudioAppRegistry — detected via notification listener
        packages.addAll(AudioAppRegistry.getActiveApps())

        // Method 4: Scan ALL installed apps that can handle audio/media intents
        packages.addAll(getAllAudioCapableApps())

        return packages.toList()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getActivePackagesViaPlayback(packages: MutableSet<String>) {
        try {
            val configs: List<AudioPlaybackConfiguration> = audioManager.activePlaybackConfigurations
            for (config in configs) {
                val uid = getClientUid(config) ?: continue
                val pkgs = context.packageManager.getPackagesForUid(uid)
                pkgs?.forEach { packages.add(it) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getActivePackagesViaMediaSession(packages: MutableSet<String>) {
        try {
            val mediaSessionManager =
                context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            val componentName = android.content.ComponentName(
                context,
                com.audiomixer.app.service.AudioNotificationListener::class.java
            )
            val controllers = mediaSessionManager.getActiveSessions(componentName)
            for (controller in controllers) {
                val pkg = controller.packageName
                if (pkg != null) packages.add(pkg)
            }
        } catch (e: SecurityException) {
            // Notification listener not granted yet
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * Scans ALL installed apps on the device and returns those capable of
     * playing audio. Uses multiple intent filters to catch every type of
     * media app — music players, video players, browsers, audiobooks,
     * podcasts, games, communication apps, etc.
     *
     * This means Pulsar, MX Player, Brave, Smart Audiobook Player, Voice,
     * YouTube Premium — ALL of them will appear automatically.
     */
    private fun getAllAudioCapableApps(): List<String> {
        val pm = context.packageManager
        val packages = mutableSetOf<String>()

        // System packages to always exclude
        val excludedPackages = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.providers.media",
            "com.android.bluetooth",
            "com.sec.android.app.launcher",
            "com.samsung.android.launcher",
            "com.android.launcher3",
            "com.google.android.launcher",
            context.packageName  // exclude ourselves
        )

        // Intent actions that audio/media apps respond to
        val mediaIntents = listOf(
            // Standard media playback
            Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH"),
            Intent("android.intent.action.MUSIC_PLAYER"),
            Intent("android.media.action.MEDIA_BROWSER_SERVICE"),
            // Audio/video file opening
            Intent(Intent.ACTION_VIEW).setType("audio/*"),
            Intent(Intent.ACTION_VIEW).setType("video/*"),
            Intent(Intent.ACTION_VIEW).setType("audio/mpeg"),
            Intent(Intent.ACTION_VIEW).setType("audio/mp4"),
            Intent(Intent.ACTION_VIEW).setType("audio/flac"),
            Intent(Intent.ACTION_VIEW).setType("audio/ogg"),
            Intent(Intent.ACTION_VIEW).setType("video/mp4"),
            Intent(Intent.ACTION_VIEW).setType("video/x-matroska"),
            // Streaming / browser
            Intent(Intent.ACTION_VIEW).setType("application/x-mpegURL"),
            Intent(Intent.ACTION_VIEW).setType("application/vnd.apple.mpegurl"),
        )

        for (intent in mediaIntents) {
            try {
                val resolvedApps: List<ResolveInfo> = pm.queryIntentActivities(
                    intent, PackageManager.MATCH_ALL
                )
                for (info in resolvedApps) {
                    val pkg = info.activityInfo?.packageName ?: continue
                    if (pkg !in excludedPackages && !isSystemOnlyApp(pm, pkg)) {
                        packages.add(pkg)
                    }
                }
            } catch (e: Exception) { /* ignore individual failures */ }
        }

        // Also query MediaBrowserService — covers music players that support
        // background playback (Pulsar, Poweramp, Samsung Music, etc.)
        try {
            val browserIntent = Intent("android.media.browse.MediaBrowserService")
            val browserApps = pm.queryIntentServices(browserIntent, PackageManager.MATCH_ALL)
            for (info in browserApps) {
                val pkg = info.serviceInfo?.packageName ?: continue
                if (pkg !in excludedPackages) packages.add(pkg)
            }
        } catch (e: Exception) { /* ignore */ }

        // Also add any app that has declared the MEDIA_CONTENT_CONTROL permission
        // (these are apps that explicitly want media session access)
        try {
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in allApps) {
                if (app.packageName in excludedPackages) continue
                if (isSystemOnlyApp(pm, app.packageName)) continue
                // Check if this app has audio-related permissions declared
                val permInfo = pm.getPackageInfo(
                    app.packageName,
                    PackageManager.GET_PERMISSIONS
                )
                val perms = permInfo.requestedPermissions ?: continue
                val audioPerms = setOf(
                    "android.permission.RECORD_AUDIO",
                    "android.permission.MODIFY_AUDIO_SETTINGS",
                    "android.permission.MEDIA_CONTENT_CONTROL"
                )
                if (perms.any { it in audioPerms }) {
                    packages.add(app.packageName)
                }
            }
        } catch (e: Exception) { /* ignore */ }

        return packages.toList()
    }

    /**
     * Returns true if this is a core system app the user can't interact with.
     * We keep system apps that have a launcher icon (like Samsung Music, YouTube etc.)
     */
    private fun isSystemOnlyApp(pm: PackageManager, packageName: String): Boolean {
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            val isSystem = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (!isSystem) return false
            // Even system apps are OK if they have a launcher icon
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            launchIntent == null // true = system-only (no launcher), false = keep it
        } catch (e: Exception) { true }
    }

    fun isAppPlaying(packageName: String): Boolean {
        try {
            val mediaSessionManager =
                context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = android.content.ComponentName(
                context,
                com.audiomixer.app.service.AudioNotificationListener::class.java
            )
            val controllers = mediaSessionManager?.getActiveSessions(componentName) ?: emptyList()
            for (controller in controllers) {
                if (controller.packageName == packageName) {
                    val state = controller.playbackState
                    if (state?.state == android.media.session.PlaybackState.STATE_PLAYING) return true
                }
            }
        } catch (e: Exception) { /* ignore */ }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val configs = audioManager.activePlaybackConfigurations
                for (config in configs) {
                    val uid = getClientUid(config) ?: continue
                    val pkgs = context.packageManager.getPackagesForUid(uid)
                    if (pkgs?.contains(packageName) == true) return true
                }
            } catch (e: Exception) { /* ignore */ }
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
        try {
            val configs = audioManager.activePlaybackConfigurations
            for (config in configs) {
                val uid = getClientUid(config) ?: continue
                val pkgs = context.packageManager.getPackagesForUid(uid) ?: continue
                if (pkgs.contains(packageName)) {
                    val sessionId = getAudioSessionId(config) ?: continue
                    applyGainToSession(sessionId, volume)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
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
}
