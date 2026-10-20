package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineStorageAuditPolicyTest {
    @Test fun missingMediaAndOrphanedFilesAreReportedButNotRemoved() {
        val files = mutableSetOf("saved.bin", "extra.bin", "unfinished.part")
        val downloaded = mutableSetOf("saved", "disconnected")
        val audit = OfflineStorageAuditPolicy.inspect(downloaded, files)
        assertEquals(setOf("disconnected"), audit.missingDownloadRecords)
        assertEquals(setOf("extra"), audit.unindexedFiles)
        assertEquals(setOf("saved", "disconnected"), downloaded)
        assertEquals(setOf("saved.bin", "extra.bin", "unfinished.part"), files)
    }

    @Test fun exactMatchingMediaDoesNotGenerateFalseAlarms() {
        val audit = OfflineStorageAuditPolicy.inspect(
            listOf("local_5", "aud:1"), listOf("local_5.bin", "aud:1.bin")
        )
        assertTrue(audit.missingDownloadRecords.isEmpty())
        assertTrue(audit.unindexedFiles.isEmpty())
    }

    @Test fun missingOrUnmountedDirectoryDoesNotEraseDatabaseIdentity() {
        val audit = OfflineStorageAuditPolicy.inspect(listOf("known-user-music"), emptyList())
        assertEquals(setOf("known-user-music"), audit.missingDownloadRecords)
        assertTrue(audit.unindexedFiles.isEmpty())
    }
}
