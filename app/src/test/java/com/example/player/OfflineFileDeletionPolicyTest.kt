package com.example.player

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFileDeletionPolicyTest {
    @Test fun alreadyAbsentPrivateAudioIsASuccessfulExplicitDeletion() {
        val directory = Files.createTempDirectory("lanu-empty-delete").toFile()
        try {
            assertTrue(OfflineFileDeletionPolicy.deleteKnownFiles(
                listOf(File(directory, "song.bin"), File(directory, "song.part"))
            ))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun completedAudioAndPartialTransferAreBothRemoved() {
        val directory = Files.createTempDirectory("lanu-files-delete").toFile()
        try {
            val finalFile = File(directory, "song.bin").apply { writeText("offline music") }
            val partial = File(directory, "song.part").apply { writeText("unfinished") }
            assertTrue(OfflineFileDeletionPolicy.deleteKnownFiles(listOf(finalFile, partial)))
            assertFalse(finalFile.exists())
            assertFalse(partial.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun failedRemovalNeverReportsSuccessOrClearsMetadata() {
        val directory = Files.createTempDirectory("lanu-failed-delete").toFile()
        try {
            val notAnAudioFile = File(directory, "song.bin").apply { mkdirs() }
            File(notAnAudioFile, "do-not-delete").writeText("retained")
            assertFalse(OfflineFileDeletionPolicy.deleteKnownFiles(listOf(notAnAudioFile)))
            assertTrue(notAnAudioFile.exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
