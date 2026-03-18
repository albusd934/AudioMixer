package com.audiomixer.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.audiomixer.app.R
import com.audiomixer.app.databinding.ItemAppVolumeBinding
import com.audiomixer.app.model.AudioApp

class AppVolumeAdapter(
    private val onVolumeChanged: (packageName: String, volume: Float) -> Unit,
    private val onMuteToggled: (packageName: String, muted: Boolean) -> Unit
) : ListAdapter<AudioApp, AppVolumeAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppVolumeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAppVolumeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AudioApp) {
            binding.ivAppIcon.setImageDrawable(app.icon)
            binding.tvAppName.text = app.appName
            binding.tvPackageName.text = app.packageName

            // Playing indicator
            binding.ivPlayingIndicator.visibility = if (app.isPlaying) View.VISIBLE else View.GONE
            binding.tvStatus.text = if (app.isPlaying) "Playing" else "Inactive"

            // Volume slider (0–100)
            val sliderValue = (app.volume * 100).toInt()
            binding.sliderVolume.value = if (app.isMuted) 0f else sliderValue.toFloat()
            binding.tvVolumePercent.text = "${if (app.isMuted) 0 else sliderValue}%"

            // Mute button state
            binding.btnMute.isSelected = app.isMuted
            binding.btnMute.setImageResource(
                if (app.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
            )

            // Slider change
            binding.sliderVolume.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val vol = value / 100f
                    binding.tvVolumePercent.text = "${value.toInt()}%"
                    onVolumeChanged(app.packageName, vol)
                }
            }

            // Mute toggle
            binding.btnMute.setOnClickListener {
                val newMuted = !app.isMuted
                onMuteToggled(app.packageName, newMuted)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AudioApp>() {
        override fun areItemsTheSame(oldItem: AudioApp, newItem: AudioApp) =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AudioApp, newItem: AudioApp) =
            oldItem == newItem
    }
}
