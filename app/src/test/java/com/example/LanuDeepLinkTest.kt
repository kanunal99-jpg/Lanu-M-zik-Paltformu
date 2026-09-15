package com.example

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanuDeepLinkTest {
    @Test
    fun `track link parses to track target`() {
        assertEquals(LanuDeepLink.Track("song-42"), LanuDeepLink.parse(Uri.parse("lanumusic://track/song-42")))
    }

    @Test
    fun `playlist link parses to playlist target`() {
        assertEquals(LanuDeepLink.Playlist("pl-42"), LanuDeepLink.parse(Uri.parse("lanumusic://playlist/pl-42")))
    }

    @Test
    fun `search link preserves query`() {
        assertEquals(LanuDeepLink.Search("mor ve otesi"), LanuDeepLink.parse(Uri.parse("lanumusic://search?q=mor%20ve%20otesi")))
    }

    @Test
    fun `unknown scheme is ignored`() {
        assertNull(LanuDeepLink.parse(Uri.parse("https://example.com/track/song-42")))
    }
}
