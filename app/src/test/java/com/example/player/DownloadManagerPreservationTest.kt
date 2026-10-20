package com.example.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AudioQuality
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DownloadManagerPreservationTest {
    private lateinit var context: Context
    private lateinit var directory: File
    private lateinit var existing: File
    private lateinit var manager: DownloadManager

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        directory = File(context.filesDir, "offline_audio")
        assertTrue(directory.isDirectory || directory.mkdirs())
        existing = File(directory, "p0-offline-preserve.bin")
        existing.writeText("existing user audio")
        manager = DownloadManager(context)
    }

    @After fun cleanUp() {
        manager.close()
        existing.delete()
        File(directory, "p0-offline-preserve.part").delete()
        File(context.cacheDir, "p0-offline-source.tmp").delete()
    }

    private fun song(source: File): Song = Song(
        id = "p0-offline-preserve",
        title = "Offline regression",
        artist = "Local Artist",
        artistId = "local-artist",
        album = "Local Collection",
        durationMs = 1000L,
        category = MusicCategory.LOCAL,
        coverUrl = "",
        audioUrl = source.toURI().toString(),
        releaseYear = 2026,
        sourceType = SongSourceType.LOCAL
    )

    private suspend fun download(source: File): DownloadProgress {
        val finished = CompletableDeferred<DownloadProgress>()
        manager.startDownload(song(source), AudioQuality.STANDARD) { state ->
            if (!finished.isCompleted) finished.complete(state)
        }
        return withTimeout(15000) { finished.await() }
    }

    @Test fun missingSourceDoesNotDeletePreviouslyCompletedOfflineTrack() = runBlocking {
        val missing = File(context.cacheDir, "p0-offline-source.tmp")
        missing.delete()
        val result = download(missing)
        assertEquals(DownloadStatus.FAILED, result.status)
        assertTrue(existing.isFile)
        assertEquals("existing user audio", existing.readText())
    }

    @Test fun verifiedTempCanReplaceExistingAudioWithoutDeletingItFirst() = runBlocking {
        val source = File(context.cacheDir, "p0-offline-source.tmp")
        source.writeText("new verified audio")
        val result = download(source)
        assertEquals(DownloadStatus.COMPLETED, result.status)
        assertEquals("new verified audio", existing.readText())
        assertTrue(result.checksum?.length == 64)
    }
}
