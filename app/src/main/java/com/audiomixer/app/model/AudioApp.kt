package com.audiomixer.app.model

import android.graphics.drawable.Drawable

data class AudioApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val volume: Float = 1.0f,      // 0.0 to 1.0
    val isMuted: Boolean = false,
    val isPlaying: Boolean = false
)
