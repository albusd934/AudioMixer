# Keep AudioEffect and VolumeShaper classes
-keep class android.media.audiofx.** { *; }
-keep class android.media.VolumeShaper { *; }
-keep class android.media.AudioPlaybackConfiguration { *; }

# Keep service and receiver classes
-keep class com.audiomixer.app.service.** { *; }
-keep class com.audiomixer.app.receiver.** { *; }

# Keep model classes
-keep class com.audiomixer.app.model.** { *; }
