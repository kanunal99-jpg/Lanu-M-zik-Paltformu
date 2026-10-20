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
                async(Dispatchers.Default) {
                    val job = Job()
                    job to registry.register("shared-song", job)
                }
            }.awaitAll()
        }
        assertEquals("Exactly one transfer is allowed to own a .part file", 1, results.count { it.second })
        results.single { it.second }.first.let { assertTrue(registry.release("shared-song", it)) }
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

    @Test fun separateDownloadManagersStillCannotOverwriteTheSameOfflineTrack() {
        val first = InFlightDownloadRegistry()
        val second = InFlightDownloadRegistry()
        val firstJob = Job()
        val secondJob = Job()
        assertTrue(first.register("multi-instance", firstJob))
        assertFalse(second.register("multi-instance", secondJob))
        second.cancelAll()
        assertTrue(firstJob.isActive)
        assertFalse(second.register("multi-instance", secondJob))
        assertTrue(first.release("multi-instance", firstJob))
        assertTrue(second.register("multi-instance", secondJob))
        second.cancelAll()
        assertTrue(secondJob.isCancelled)
        assertTrue(second.release("multi-instance", secondJob))
    }
}
