package com.example.player

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.AudioQuality
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.ByteArrayInputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
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
    }

    private fun song(source: String): Song = Song(
        id = "p0-offline-preserve",
        title = "Offline regression",
        artist = "Local Artist",
        artistId = "local-artist",
        album = "Local Collection",
        durationMs = 1000L,
        category = MusicCategory.LOCAL,
        coverUrl = "",
        audioUrl = source,
        releaseYear = 2026,
        sourceType = SongSourceType.LOCAL
    )

    private suspend fun download(source: String): DownloadProgress {
        val finished = CompletableDeferred<DownloadProgress>()
        manager.startDownload(song(source), AudioQuality.STANDARD) { state ->
            if (!finished.isCompleted) finished.complete(state)
        }
        return withTimeout(15000) { finished.await() }
    }

    @Test fun missingSourceDoesNotDeletePreviouslyCompletedOfflineTrack() = runBlocking {
        // Missing *shared* storage remains authorized by path policy but must
        // fail to read, without destroying an earlier completed offline track.
        val result = download("file:///storage/emulated/0/p0-offline-source-missing-2026.tmp")
        assertEquals(DownloadStatus.FAILED, result.status)
        assertTrue(existing.isFile)
        assertEquals("existing user audio", existing.readText())
    }

    @Test fun verifiedTempCanReplaceExistingAudioWithoutDeletingItFirst() = runBlocking {
        // Use a real local-media content URI with a Robolectric resolver stream,
        // NOT an app-private cache file (which must be blocked in production).
        val source = Uri.parse("content://media/external/audio/media/123456")
        Shadows.shadowOf(context.contentResolver).registerInputStream(
            source, ByteArrayInputStream("new verified audio".toByteArray())
        )
        val result = download(source.toString())
        assertEquals(DownloadStatus.COMPLETED, result.status)
        assertEquals("new verified audio", existing.readText())
        assertTrue(result.checksum?.length == 64)
    }
    @Test fun privateAppCacheSourceCannotOverwriteExistingOfflineTrack() = runBlocking {
        val source = File(context.cacheDir, "p0-offline-source.tmp")
        source.writeText("private data must not be copied")
        try {
            val result = download(source.toURI().toString())
            assertEquals(DownloadStatus.FAILED, result.status)
            assertEquals("existing user audio", existing.readText())
        } finally {
            source.delete()
        }
    }

}
