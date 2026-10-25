package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupPlaybackQueueTest {
    @Test fun tapBeforeConnectionIsDeliveredOnce() {
        val requests = StartupPlaybackQueue<String>()
        requests.submit("play song A")
        assertTrue(requests.hasPending())
        assertEquals("play song A", requests.drain())
        assertNull(requests.drain())
        assertFalse(requests.hasPending())
    }

    @Test fun latestUserRequestWins() {
        val requests = StartupPlaybackQueue<Int>()
        requests.submit(1)
        requests.submit(2)
        requests.submit(3)
        assertEquals(3, requests.drain())
    }

    @Test fun releaseNeverReplaysQueuedMedia() {
        val requests = StartupPlaybackQueue<String>()
        requests.submit("start")
        requests.close()
        assertNull(requests.drain())
        assertFalse(requests.hasPending())
        requests.submit("ignored after release")
        assertNull(requests.drain())
    }

    @Test fun aNewRequestAfterDrainingCanBeDelivered() {
        val requests = StartupPlaybackQueue<String>()
        requests.submit("first")
        assertEquals("first", requests.drain())
        requests.submit("second")
        assertEquals("second", requests.drain())
    }
}
