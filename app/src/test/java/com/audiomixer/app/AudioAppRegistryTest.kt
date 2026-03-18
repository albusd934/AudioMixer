package com.audiomixer.app

import com.audiomixer.app.utils.AudioAppRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AudioAppRegistryTest {

    @Before
    fun setUp() {
        AudioAppRegistry.clear()
    }

    @After
    fun tearDown() {
        AudioAppRegistry.clear()
    }

    @Test
    fun `registry starts empty`() {
        assertTrue(AudioAppRegistry.getActiveApps().isEmpty())
    }

    @Test
    fun `add app appears in registry`() {
        AudioAppRegistry.addActiveApp("com.spotify.music")
        assertTrue(AudioAppRegistry.getActiveApps().contains("com.spotify.music"))
    }

    @Test
    fun `remove app disappears from registry`() {
        AudioAppRegistry.addActiveApp("com.spotify.music")
        AudioAppRegistry.removeActiveApp("com.spotify.music")
        assertFalse(AudioAppRegistry.getActiveApps().contains("com.spotify.music"))
    }

    @Test
    fun `duplicate adds are deduplicated`() {
        AudioAppRegistry.addActiveApp("com.spotify.music")
        AudioAppRegistry.addActiveApp("com.spotify.music")
        assertEquals(1, AudioAppRegistry.getActiveApps().size)
    }

    @Test
    fun `multiple apps tracked independently`() {
        AudioAppRegistry.addActiveApp("com.spotify.music")
        AudioAppRegistry.addActiveApp("com.google.android.youtube")
        AudioAppRegistry.addActiveApp("com.discord")
        assertEquals(3, AudioAppRegistry.getActiveApps().size)
    }

    @Test
    fun `clear removes all apps`() {
        AudioAppRegistry.addActiveApp("com.spotify.music")
        AudioAppRegistry.addActiveApp("com.google.android.youtube")
        AudioAppRegistry.clear()
        assertTrue(AudioAppRegistry.getActiveApps().isEmpty())
    }

    @Test
    fun `getActiveApps returns immutable snapshot`() {
        AudioAppRegistry.addActiveApp("com.spotify.music")
        val snapshot = AudioAppRegistry.getActiveApps()
        AudioAppRegistry.addActiveApp("com.google.android.youtube")
        // snapshot should not have changed
        assertEquals(1, snapshot.size)
    }
}
