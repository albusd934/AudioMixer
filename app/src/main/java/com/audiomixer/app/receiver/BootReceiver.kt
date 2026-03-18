package com.audiomixer.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.audiomixer.app.service.AudioMixerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, AudioMixerService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
