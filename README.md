# 🎚️ Audio Mixer — Android App

An Android app that lets you run audio from multiple apps simultaneously and control individual volume levels for each app from a single mixer interface.

---

## Features

- **Per-app volume sliders** — independently adjust volume for Spotify, YouTube, Discord, Zoom, and any other audio app
- **Mute toggle per app** — instantly silence any app without affecting others
- **Master volume control** — global media stream volume at the top
- **System stream controls** — Music, Ring, Notification, Alarm, Voice Call sliders in Settings
- **Active playback detection** — automatically discovers apps currently playing audio (Android 10+)
- **Persistent foreground service** — keeps running in background, restores volumes on boot
- **Media notification detection** — uses NotificationListenerService to catch apps playing audio via media-style notifications

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Android SDK | 29+ (Android 10+) |
| Kotlin | 1.9.10 |
| Target SDK | 34 (Android 14) |
| Android Studio | Hedgehog (2023.1.1) or newer |

> **Why Android 10+?**  
> `AudioManager.getActivePlaybackConfigurations()` — the API that exposes per-app audio sessions and UIDs — was introduced in Android 10. Per-app volume control via `AudioEffect` also requires session IDs obtained from this API.

---

## Project Structure

```
AudioMixer/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/audiomixer/app/
│       │   ├── ui/
│       │   │   ├── MainActivity.kt         # Main mixer screen
│       │   │   └── SettingsActivity.kt     # System stream volume controls
│       │   ├── service/
│       │   │   ├── AudioMixerService.kt    # Foreground service (volume state)
│       │   │   └── AudioNotificationListener.kt  # Detects media apps via notifications
│       │   ├── viewmodel/
│       │   │   └── MainViewModel.kt        # LiveData + coroutines for app list
│       │   ├── adapter/
│       │   │   └── AppVolumeAdapter.kt     # RecyclerView adapter for app cards
│       │   ├── model/
│       │   │   └── AudioApp.kt             # Data class for each audio app entry
│       │   ├── utils/
│       │   │   ├── AppVolumeManager.kt     # Core per-app volume control logic
│       │   │   └── AudioAppRegistry.kt     # In-memory registry of active audio apps
│       │   └── receiver/
│       │       └── BootReceiver.kt         # Restarts service on device boot
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_settings.xml
│           │   ├── item_app_volume.xml     # Per-app volume card
│           │   └── item_stream_volume.xml
│           ├── drawable/                   # All vector icons
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── mipmap-hdpi/               # Launcher icons
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## Setup Instructions

### 1. Open in Android Studio
```
File → Open → Select the AudioMixer/ folder
```

### 2. Sync Gradle
Android Studio will prompt you to sync. Click **Sync Now**.

### 3. Build & Run
- Connect an Android 10+ device or create an emulator (API 29+)
- Click **Run ▶** or use `Shift+F10`

### 4. Grant Permissions (on device)
When the app launches:

1. Tap **"Grant Permissions"** banner if shown
2. In **Notification Listener Settings**, find "Audio Mixer – Notification Listener" and enable it
3. Return to the app — the banner will disappear

---

## How Per-App Volume Works

### Detection
Audio apps are discovered via two complementary mechanisms:

| Method | API | What it detects |
|--------|-----|-----------------|
| `AudioManager.getActivePlaybackConfigurations()` | API 29+ | Apps actively playing audio right now |
| `NotificationListenerService` | API 18+ | Apps showing media-style notifications |

### Volume Control
```
AudioPlaybackConfiguration
    └── clientUid → resolve to packageName
    └── audioSessionId → AudioEffect target

AudioEffect (LoudnessEnhancer)
    └── applied to session → per-app gain adjustment
```

On Android 8+ we also attempt `VolumeShaper` for smooth ramp transitions.

> **Note:** Android does not expose a fully public per-app volume API.  
> This app uses the combination of `AudioPlaybackConfiguration` (public API 29+),  
> `AudioEffect` on session IDs (public but session-scoped), and  
> `MODIFY_AUDIO_SETTINGS` permission to achieve per-app attenuation.  
> Boosting above 100% is possible via `LoudnessEnhancer.setTargetGain()`.

---

## Permissions Explained

| Permission | Why it's needed |
|------------|----------------|
| `MODIFY_AUDIO_SETTINGS` | Required to adjust stream volumes and AudioEffect parameters |
| `FOREGROUND_SERVICE` | Keep the mixer service alive in the background |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Android 14 requirement for media-type foreground services |
| `RECEIVE_BOOT_COMPLETED` | Auto-start service after device reboot |
| `POST_NOTIFICATIONS` | Show the persistent mixer notification (Android 13+) |
| `QUERY_ALL_PACKAGES` | Resolve app names/icons for all installed audio apps |
| Notification Listener | Detect apps playing audio via media notifications (user must grant manually) |

---

## Known Limitations

1. **No fully public per-app volume API** — Android restricts this; our approach uses AudioEffect on session IDs which requires apps to be actively playing.
2. **Some apps use AudioTrack directly** — these may not appear until audio is actively playing.
3. **Volume changes are session-scoped** — if an app creates new sessions (e.g. skipping tracks), the gain must be re-applied.
4. **Root not required** — but root would allow access to `AudioPolicyService` for more reliable per-app control.

---

## Extending the App

### Add a custom app to the always-shown list
In `AppVolumeManager.kt`, add the package name to `wellKnownMediaApps`:
```kotlin
"com.example.mycustomapp",
```

### Persist volume settings across reboots
Add SharedPreferences storage in `AppVolumeManager`:
```kotlin
val prefs = context.getSharedPreferences("volumes", Context.MODE_PRIVATE)
prefs.edit().putFloat(packageName, volume).apply()
```

### Add equalizer support
Use `android.media.audiofx.Equalizer` on the session ID alongside `LoudnessEnhancer`.

---

## License
MIT License — free to use, modify, and distribute.
