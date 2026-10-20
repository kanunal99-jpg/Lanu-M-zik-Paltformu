package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeBrowserFallbackTest {
    @Test fun onlyOfficialYouTubeLinkIsGeneratedFromValidatedId() {
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            YouTubeBrowserFallback.url("dQw4w9WgXcQ"))
    }

    @Test fun malformedAndInjectedIdsAreRejected() {
        for (invalid in listOf("", "a", "video/next", "abc.def/123",
            "dQw4w9WgXcQ?evil", "..\\foo", "https://evil")) {
            assertTrue(invalid, runCatching { YouTubeBrowserFallback.url(invalid) }.isFailure)
        }
    }
}
