package com.example.player

import kotlinx.coroutines.CompletableDeferred
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

    @Test fun cancelKeepsGlobalOwnershipUntilCoroutineFinishesCleanup() {
        val registry = InFlightDownloadRegistry()
        val first = Job()
        val second = Job()
        assertTrue(registry.register("one", first))
        assertTrue(registry.register("two", second))
        registry.cancelAll()
        assertTrue(first.isCancelled)
        assertTrue(second.isCancelled)
        // A cancelled coroutine can still be finishing its disk cleanup.
        assertFalse(registry.register("one", Job()))
        assertTrue(registry.release("one", first))
        assertTrue(registry.release("two", second))
        val retryOne = Job()
        val retryTwo = Job()
        assertTrue(registry.register("one", retryOne))
        assertTrue(registry.register("two", retryTwo))
        assertTrue(registry.release("one", retryOne))
        assertTrue(registry.release("two", retryTwo))
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

    @Test fun deleteBarrierBlocksNewDownloadsUntilFileDeletionCompletes() = runBlocking {
        val first = InFlightDownloadRegistry()
        val anotherManager = InFlightDownloadRegistry()
        val insideDelete = CompletableDeferred<Unit>()
        val unblock = CompletableDeferred<Unit>()
        val removing = async(Dispatchers.Default) {
            first.deleteExclusively("one-song") {
                insideDelete.complete(Unit)
                unblock.await()
                true
            }
        }
        insideDelete.await()
        assertFalse(anotherManager.register("one-song", Job()))
        assertFalse(anotherManager.deleteExclusively("one-song") { true })
        unblock.complete(Unit)
        assertTrue(removing.await())
        val retry = Job()
        assertTrue(anotherManager.register("one-song", retry))
        assertTrue(anotherManager.release("one-song", retry))
    }
}
