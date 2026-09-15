package com.example

import android.net.Uri
import com.example.player.OfflineSourcePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSourcePolicyTest {
    @Test
    fun `content uri is authorized`() {
        assertTrue(OfflineSourcePolicy.isAuthorized(Uri.parse("content://media/external/audio/media/42")))
    }

    @Test
    fun `file uri is authorized`() {
        assertTrue(OfflineSourcePolicy.isAuthorized(Uri.parse("file:///data/user/0/com.example/audio.mp3")))
    }

    @Test
    fun `http uri is rejected`() {
        assertFalse(OfflineSourcePolicy.isAuthorized(Uri.parse("https://example.com/song.mp3")))
    }

    @Test
    fun `blank uri is rejected`() {
        assertFalse(OfflineSourcePolicy.isAuthorized(Uri.parse("")))
    }
}
