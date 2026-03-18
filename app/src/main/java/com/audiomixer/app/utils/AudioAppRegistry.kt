package com.audiomixer.app.utils

/**
 * Simple in-memory registry of apps detected as active audio players.
 * Populated by AudioNotificationListener and AudioManager queries.
 */
object AudioAppRegistry {
    private val activeApps = mutableSetOf<String>()

    fun addActiveApp(packageName: String) {
        activeApps.add(packageName)
    }

    fun removeActiveApp(packageName: String) {
        activeApps.remove(packageName)
    }

    fun getActiveApps(): Set<String> = activeApps.toSet()

    fun clear() = activeApps.clear()
}
