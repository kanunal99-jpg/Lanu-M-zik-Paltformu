package com.example.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeEmbedNavigationPolicyTest {
    @Test fun acceptsOfficialSecureYouTubePagesAndCleanupNavigation() {
        for (url in listOf(
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube-nocookie.com/embed/dQw4w9WgXcQ",
            "https://www.youtube-nocookie.com/",
            "about:blank"
        )) assertTrue(url, YouTubeEmbedNavigationPolicy.isAllowedMainFrame(url))
    }

    @Test fun blocksCleartextFilesOtherHostsAndDomainConfusion() {
        for (url in listOf(
            "http://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://youtube.com.evil.example/",
            "https://evil-youtube.com/",
            "https://user:password@youtube.com/",
            "https://www.youtube.com:444/",
            "file:///data/data/com.example/secret",
            "content://com.example.provider/",
            "javascript:alert(1)",
            "data:text/html,payload",
            "https://google.com/",
            "",
            "not a URL"
        )) assertFalse(url, YouTubeEmbedNavigationPolicy.isAllowedMainFrame(url))
    }
}
