package com.example.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes offline deletion against delayed completion callbacks across all
 * repository instances in the Android process (not only one ViewModel instance).
 * This is a process-local safety barrier; it does not claim cross-process locks.
 */
internal class OfflineMetadataMutationGate {
    companion object {
        private val processLock = Mutex()
    }

    suspend fun <T> withExclusiveAccess(action: suspend () -> T): T =
        processLock.withLock { action() }
}
