package com.audiomixer.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.audiomixer.app.R
import com.audiomixer.app.adapter.AppVolumeAdapter
import com.audiomixer.app.databinding.ActivityMainBinding
import com.audiomixer.app.service.AudioMixerService
import com.audiomixer.app.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: AppVolumeAdapter
    private var audioMixerService: AudioMixerService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as AudioMixerService.LocalBinder
            audioMixerService = localBinder.getService()
            serviceBound = true
            refreshApps()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioMixerService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setupRecyclerView()
        setupUI()
        observeViewModel()
        checkNotificationListenerPermission()
        startAndBindService()
    }

    private fun setupRecyclerView() {
        adapter = AppVolumeAdapter(
            onVolumeChanged = { packageName, volume ->
                audioMixerService?.setAppVolume(packageName, volume)
                viewModel.updateAppVolume(packageName, volume)
            },
            onMuteToggled = { packageName, muted ->
                audioMixerService?.setAppMute(packageName, muted)
                viewModel.updateAppMute(packageName, muted)
            }
        )
        binding.recyclerViewApps.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewApps.adapter = adapter
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Audio Mixer"

        // Master volume slider
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        binding.sliderMasterVolume.valueTo = maxVol.toFloat()
        binding.sliderMasterVolume.value = curVol.toFloat()
        binding.tvMasterVolumeValue.text = "${(curVol * 100 / maxVol)}%"

        binding.sliderMasterVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val vol = value.toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                binding.tvMasterVolumeValue.text = "${(vol * 100 / maxVol)}%"
            }
        }

        binding.btnRefresh.setOnClickListener { refreshApps() }

        binding.btnPermissions.setOnClickListener {
            showPermissionsDialog()
        }

        binding.fabSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.audioApps.observe(this) { apps ->
            if (apps.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerViewApps.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerViewApps.visibility = View.VISIBLE
                adapter.submitList(apps)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun checkNotificationListenerPermission(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val hasPermission = enabledListeners?.contains(packageName) == true
        binding.btnPermissions.visibility = if (hasPermission) View.GONE else View.VISIBLE
        return hasPermission
    }

    private fun showPermissionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(
                "Audio Mixer needs the following permissions to control app volumes:\n\n" +
                "• Notification Listener – to detect active audio apps\n" +
                "• Modify Audio Settings – to adjust per-app volume\n\n" +
                "Tap 'Open Settings' to grant these permissions."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startAndBindService() {
        val intent = Intent(this, AudioMixerService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun refreshApps() {
        viewModel.loadAudioApps(this)
    }

    override fun onResume() {
        super.onResume()
        checkNotificationListenerPermission()
        refreshApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}
