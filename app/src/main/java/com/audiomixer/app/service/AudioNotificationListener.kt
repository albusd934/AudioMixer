package com.audiomixer.app.service

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.audiomixer.app.utils.AudioAppRegistry

class AudioNotificationListener : NotificationListenerService() {

    companion object {
        const val ACTION_APPS_UPDATED = "com.audiomixer.APPS_UPDATED"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notification = sbn.notification
        val extras = notification.extras

        // Check for media-style notifications
        val isMediaStyle = extras.containsKey(android.app.Notification.EXTRA_MEDIA_SESSION) ||
            notification.category == android.app.Notification.CATEGORY_TRANSPORT

        if (isMediaStyle) {
            AudioAppRegistry.addActiveApp(sbn.packageName)
            broadcastUpdate()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Don't remove immediately - app may still be playing
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val active = activeNotifications ?: return
            for (sbn in active) {
                val extras = sbn.notification.extras
                if (extras.containsKey(android.app.Notification.EXTRA_MEDIA_SESSION)) {
                    AudioAppRegistry.addActiveApp(sbn.packageName)
                }
            }
            broadcastUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun broadcastUpdate() {
        val intent = Intent(ACTION_APPS_UPDATED)
        sendBroadcast(intent)
    }
}
