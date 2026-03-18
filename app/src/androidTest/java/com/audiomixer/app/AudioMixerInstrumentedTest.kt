package com.audiomixer.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests that run on a real Android device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class AudioMixerInstrumentedTest {

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.audiomixer.app", appContext.packageName)
    }

    @Test
    fun audioManagerIsAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE)
            as android.media.AudioManager
        assertNotNull(audioManager)
    }

    @Test
    fun maxMusicVolumeIsPositive() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE)
            as android.media.AudioManager
        val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        assertTrue("Max volume should be > 0", maxVol > 0)
    }
}
