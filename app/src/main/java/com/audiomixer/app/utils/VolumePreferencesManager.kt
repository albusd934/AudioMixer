package com.audiomixer.app.utils

import android.content.Context

/**
 * Persists per-app volume and mute preferences using SharedPreferences.
 * Survives app restarts and device reboots.
 */
class VolumePreferencesManager(context: Context) {

    private val volumePrefs = context.getSharedPreferences("app_volumes", Context.MODE_PRIVATE)
    private val mutePrefs   = context.getSharedPreferences("app_mutes",   Context.MODE_PRIVATE)

    // ── Volume ────────────────────────────────────────────────────────────────

    fun saveVolume(packageName: String, volume: Float) {
        volumePrefs.edit().putFloat(packageName, volume).apply()
    }

    fun getVolume(packageName: String, default: Float = 1.0f): Float =
        volumePrefs.getFloat(packageName, default)

    fun getAllSavedVolumes(): Map<String, Float> {
        @Suppress("UNCHECKED_CAST")
        return (volumePrefs.all as Map<String, Float>).toMap()
    }

    // ── Mute ──────────────────────────────────────────────────────────────────

    fun saveMute(packageName: String, muted: Boolean) {
        mutePrefs.edit().putBoolean(packageName, muted).apply()
    }

    fun getMute(packageName: String): Boolean =
        mutePrefs.getBoolean(packageName, false)

    fun getAllSavedMutes(): Map<String, Boolean> {
        @Suppress("UNCHECKED_CAST")
        return (mutePrefs.all as Map<String, Boolean>).toMap()
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    fun clearAll() {
        volumePrefs.edit().clear().apply()
        mutePrefs.edit().clear().apply()
    }

    fun removeApp(packageName: String) {
        volumePrefs.edit().remove(packageName).apply()
        mutePrefs.edit().remove(packageName).apply()
    }
}
