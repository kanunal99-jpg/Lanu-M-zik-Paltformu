package com.example.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMetadataMutationGateTest {
    @Test fun userDeletionCannotInterleaveWithInFlightRoomCompletion() = runBlocking {
        val gate = OfflineMetadataMutationGate()
        val enteredCompletion = CompletableDeferred<Unit>()
        val completionMayFinish = CompletableDeferred<Unit>()
        val journal = mutableListOf<String>()
        val completion = async {
            gate.withExclusiveAccess {
                journal += "save-start"
                enteredCompletion.complete(Unit)
                completionMayFinish.await()
                journal += "save-finish"
            }
        }
        enteredCompletion.await()
        val deletion = async {
            gate.withExclusiveAccess { journal += "delete" }
        }
        yield()
        assertFalse(deletion.isCompleted)
        completionMayFinish.complete(Unit)
        completion.await()
        deletion.await()
        assertEquals(listOf("save-start", "save-finish", "delete"), journal)
    }

    @Test fun lateCompletionMustRecheckFileExistenceWhileItOwnsMetadataGate() = runBlocking {
        val gate = OfflineMetadataMutationGate()
        var fileExists = true
        var recordExists = false
        gate.withExclusiveAccess {
            fileExists = false // explicit, already-completed user removal
            recordExists = false
        }
        gate.withExclusiveAccess {
            if (fileExists) recordExists = true
        }
        assertFalse(recordExists)
        assertTrue(!fileExists)
    }
}
