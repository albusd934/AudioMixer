package com.audiomixer.app.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiomixer.app.model.AudioApp
import com.audiomixer.app.utils.AppVolumeManager
import com.audiomixer.app.utils.VolumePreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    private val _audioApps = MutableLiveData<List<AudioApp>>()
    val audioApps: LiveData<List<AudioApp>> = _audioApps

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val volumeMap = mutableMapOf<String, Float>()
    private val muteMap   = mutableMapOf<String, Boolean>()

    fun loadAudioApps(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true

            if (volumeMap.isEmpty()) {
                val prefs = VolumePreferencesManager(context)
                volumeMap.putAll(prefs.getAllSavedVolumes())
                muteMap.putAll(prefs.getAllSavedMutes())
            }

            val apps = withContext(Dispatchers.IO) {
                fetchAudioApps(context)
            }
            _audioApps.value = apps
            _isLoading.value = false
        }
    }

    private fun fetchAudioApps(context: Context): List<AudioApp> {
        val pm = context.packageManager
        val appVolumeManager = AppVolumeManager(context)
        val audioApps = mutableListOf<AudioApp>()
        val activePackages = appVolumeManager.getActiveAudioPackages()

        for (packageName in activePackages) {
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val currentVolume = volumeMap[packageName] ?: appVolumeManager.getAppVolume(packageName)
                val isMuted = muteMap[packageName] ?: false

                audioApps.add(
                    AudioApp(
                        packageName = packageName,
                        appName = appName,
                        icon = icon,
                        volume = currentVolume,
                        isMuted = isMuted,
                        isPlaying = appVolumeManager.isAppPlaying(packageName)
                    )
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // skip uninstalled apps
            }
        }

        return audioApps.sortedByDescending { it.isPlaying }
    }

    fun updateAppVolume(packageName: String, volume: Float) {
        volumeMap[packageName] = volume
        val currentList = _audioApps.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.packageName == packageName }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(volume = volume)
            _audioApps.value = currentList
        }
    }

    fun updateAppMute(packageName: String, muted: Boolean) {
        muteMap[packageName] = muted
        val currentList = _audioApps.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.packageName == packageName }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(isMuted = muted)
            _audioApps.value = currentList
        }
    }
}
