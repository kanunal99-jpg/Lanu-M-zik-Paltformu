package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeVideoLinkTest {
    private val id = "M7lc1UVf-VE"

    @Test fun acceptsOfficialWatchAndShortUrls() {
        assertEquals(id, YouTubeVideoLink.videoId("https://www.youtube.com/watch?v=$id&list=PL123"))
        assertEquals(id, YouTubeVideoLink.videoId("https://youtu.be/$id?t=14"))
        assertEquals(id, YouTubeVideoLink.videoId("https://music.youtube.com/watch?v=$id"))
        assertEquals(id, YouTubeVideoLink.videoId("https://www.youtube.com/shorts/$id"))
        assertEquals(id, YouTubeVideoLink.videoId("https://www.youtube-nocookie.com/embed/$id"))
        assertEquals(id, YouTubeVideoLink.videoId(id))
    }

    @Test fun rejectsUntrustedHostsAndNonVideoUrls() {
        assertNull(YouTubeVideoLink.videoId("https://www.youtube.com.evil.example/watch?v=$id"))
        assertNull(YouTubeVideoLink.videoId("http://www.youtube.com/watch?v=$id"))
        assertNull(YouTubeVideoLink.videoId("javascript:alert(1)"))
        assertNull(YouTubeVideoLink.videoId("https://youtube.com/playlist?list=PL123"))
        assertNull(YouTubeVideoLink.videoId("https://user:password@youtube.com/watch?v=$id"))
        assertNull(YouTubeVideoLink.videoId("https://youtube.com:8080/watch?v=$id"))
        assertNull(YouTubeVideoLink.videoId("https://notyoutube.com/watch?v=$id"))
    }
}
