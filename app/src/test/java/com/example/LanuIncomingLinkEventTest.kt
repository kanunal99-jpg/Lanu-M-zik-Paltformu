package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanuIncomingLinkEventTest {
    @Test fun sameLinkOpenedTwiceIsTwoSeparateActions() {
        val initial = LanuIncomingLinkEvent(null, 0L)
        val first = LanuIncomingLinkEvent.next(initial, "lanumusic://action/next")
        val second = LanuIncomingLinkEvent.next(first, "lanumusic://action/next")
        assertNotEquals(first, second)
        assertEquals(first.rawUri, second.rawUri)
        assertEquals(2L, second.sequence)
    }

    @Test fun changingTargetAdvancesWithoutChangingOldEvent() {
        val first = LanuIncomingLinkEvent("lanumusic://library", 4L)
        val next = LanuIncomingLinkEvent.next(first, "lanumusic://search?q=ceza")
        assertEquals("lanumusic://library", first.rawUri)
        assertEquals("lanumusic://search?q=ceza", next.rawUri)
        assertEquals(5L, next.sequence)
    }

    @Test fun intentWithoutDataClearsPreviouslyPendingRoute() {
        val before = LanuIncomingLinkEvent("lanumusic://track/audius:3", 9L)
        val after = LanuIncomingLinkEvent.next(before, null)
        assertNull(after.rawUri)
        assertEquals(10L, after.sequence)
    }

    @Test fun invalidExternalLinksRemainRejectedByParser() {
        val event = LanuIncomingLinkEvent.next(LanuIncomingLinkEvent(null, 0), "lanumusic://track/%ZZ")
        assertNull(LanuDeepLink.parse(event.rawUri.orEmpty()))
    }
}
