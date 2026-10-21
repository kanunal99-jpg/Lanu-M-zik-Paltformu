package com.example.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes offline file-removal metadata with asynchronous completion callbacks. */
internal class OfflineMetadataMutationGate {
    private val lock = Mutex()

    suspend fun <T> withExclusiveAccess(action: suspend () -> T): T =
        lock.withLock { action() }
}
