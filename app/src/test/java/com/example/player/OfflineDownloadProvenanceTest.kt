package com.example.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AudioQuality
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineDownloadProvenanceTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun remoteRecordSpoofingExistingPrivateFileCannotCopyUserDataOrReplaceCachedSong() = runBlocking {
        val manager = DownloadManager(context)
        val privateFile = File(context.filesDir, "sensitive-private-record.txt")
        privateFile.writeText("private must not be copied")
        val directory = File(context.filesDir, "offline_audio").apply { mkdirs() }
        val cached = File(directory, "catalog-spoof-test.bin")
        cached.writeText("keep existing offline audio")
        try {
            val forged = Song(
                id = "catalog-spoof-test", title = "Forged",
                artist = "Remote", artistId = "remote", album = "Remote",
                durationMs = 1000, category = MusicCategory.OTHER,
                coverUrl = "", audioUrl = privateFile.toURI().toString(),
                releaseYear = 2026, sourceType = SongSourceType.VERIFIED_REMOTE,
                license = "CC BY 4.0"
            )
            val result = CompletableDeferred<DownloadProgress>()
            manager.startDownload(forged, AudioQuality.STANDARD) {
                if (!result.isCompleted) result.complete(it)
            }
            val completed = withTimeout(15000L) { result.await() }
            assertEquals(DownloadStatus.FAILED, completed.status)
            assertEquals("REMOTE_DOWNLOAD_NOT_AUTHORIZED", completed.error)
            assertEquals("keep existing offline audio", cached.readText())
            assertEquals("private must not be copied", privateFile.readText())
            assertFalse(File(directory, "catalog-spoof-test.part").exists())
        } finally {
            manager.close()
            cached.delete()
            privateFile.delete()
        }
    }
}
