package com.audiomixer.app.ui

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.audiomixer.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        setupVolumeSliders()
        setupButtons()
    }

    private fun setupVolumeSliders() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Music stream
        setupStreamSlider(
            audioManager,
            AudioManager.STREAM_MUSIC,
            binding.sliderMusic,
            binding.tvMusicPercent
        )

        // Ring stream
        setupStreamSlider(
            audioManager,
            AudioManager.STREAM_RING,
            binding.sliderRing,
            binding.tvRingPercent
        )

        // Notification stream
        setupStreamSlider(
            audioManager,
            AudioManager.STREAM_NOTIFICATION,
            binding.sliderNotification,
            binding.tvNotificationPercent
        )

        // Alarm stream
        setupStreamSlider(
            audioManager,
            AudioManager.STREAM_ALARM,
            binding.sliderAlarm,
            binding.tvAlarmPercent
        )

        // Voice call stream
        setupStreamSlider(
            audioManager,
            AudioManager.STREAM_VOICE_CALL,
            binding.sliderVoiceCall,
            binding.tvVoiceCallPercent
        )
    }

    private fun setupStreamSlider(
        audioManager: AudioManager,
        streamType: Int,
        slider: com.google.android.material.slider.Slider,
        label: android.widget.TextView
    ) {
        val max = audioManager.getStreamMaxVolume(streamType)
        val current = audioManager.getStreamVolume(streamType)

        slider.valueTo = max.toFloat()
        slider.value = current.toFloat()
        label.text = "${(current * 100 / max)}%"

        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                audioManager.setStreamVolume(streamType, value.toInt(), 0)
                label.text = "${(value.toInt() * 100 / max)}%"
            }
        }
    }

    private fun setupButtons() {
        binding.btnNotificationListener.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnAudioSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
