package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanuDeepLinkInputSafetyTest {
    @Test fun malformedEscapesAreNeverAllowedToCrashOrNavigate() {
        assertNull(LanuDeepLink.parse("lanumusic://search?q=%ZZ"))
        assertNull(LanuDeepLink.parse("lanumusic://track/%GG"))
        assertNull(LanuDeepLink.parse("lanumusic://search?q=%"))
    }

    @Test fun oversizedUrlAndQueriesAreRejected() {
        assertNull(LanuDeepLink.parse("lanumusic://search?q=" + "a".repeat(5000)))
        assertNull(LanuDeepLink.parse("lanumusic://search?q=" + "a".repeat(201)))
        assertNull(LanuDeepLink.parse("lanumusic://track/" + "a".repeat(1024)))
    }

    @Test fun rejectInjectedCredentialsAndPorts() {
        assertNull(LanuDeepLink.parse("lanumusic://guest@track/one"))
        assertNull(LanuDeepLink.parse("lanumusic://track:8888/one"))
    }

    @Test fun validTurkishQueriesAndShareLinksStillWork() {
        assertEquals(LanuDeepLink.Search("Şarkı Çağrı"),
            LanuDeepLink.parse("lanumusic://search?q=%C5%9Eark%C4%B1%20%C3%87a%C4%9Fr%C4%B1"))
        assertEquals(LanuDeepLink.Track("audius:123"),
            LanuDeepLink.parse("lanumusic://track/audius:123"))
    }

    @Test fun queryWithExcessivePairsIsRejected() {
        val query = (0..20).joinToString("&") { "q=${it}" }
        assertNull(LanuDeepLink.parse("lanumusic://search?" + query))
    }

    @Test fun controlCharactersDoNotReachNavigation() {
        assertNull(LanuDeepLink.parse("lanumusic://search?q=hello%00world"))
        assertNull(LanuDeepLink.parse("lanumusic://track/%00"))
    }
}
