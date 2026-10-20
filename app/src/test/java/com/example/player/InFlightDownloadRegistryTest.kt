package com.example.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InFlightDownloadRegistryTest {
    @Test fun parallelCallsRegisterOnlyOneOwnerForTheSameSong() = runBlocking {
        val registry = InFlightDownloadRegistry()
        val results = coroutineScope {
            (0 until 48).map {
                async(Dispatchers.Default) { registry.register("shared-song", Job()) }
            }.awaitAll()
        }
        assertEquals("Exactly one transfer is allowed to own a .part file", 1, results.count { it })
    }

    @Test fun obsoleteCompletionMustNotRemoveANewDownload() {
        val registry = InFlightDownloadRegistry()
        val old = Job()
        val newer = Job()
        assertTrue(registry.register("song", old))
        assertFalse(registry.register("song", newer))
        assertFalse(registry.release("song", newer))
        assertFalse(registry.register("song", newer))
        assertTrue(registry.release("song", old))
        assertTrue(registry.register("song", newer))
        assertTrue(registry.release("song", newer))
    }

    @Test fun differentSongsCanDownloadConcurrently() {
        val registry = InFlightDownloadRegistry()
        assertTrue(registry.register("one", Job()))
        assertTrue(registry.register("two", Job()))
        registry.cancelAll()
        assertTrue(registry.register("one", Job()))
        assertTrue(registry.register("two", Job()))
    }
}
