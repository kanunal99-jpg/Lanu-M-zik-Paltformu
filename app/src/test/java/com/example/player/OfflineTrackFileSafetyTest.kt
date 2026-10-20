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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineTrackFileSafetyTest {
    private lateinit var context: Context
    private lateinit var manager: DownloadManager
    private lateinit var protectedFile: File
    private lateinit var safeSource: File

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = DownloadManager(context)
        protectedFile = File(context.filesDir, "lanu-protected-user-file.bin")
        protectedFile.writeText("must survive invalid song IDs")
        safeSource = File(context.cacheDir, "lanu-path-test-source")
        safeSource.writeText("authorized source")
    }

    @After fun cleanup() {
        manager.close()
        safeSource.delete()
        protectedFile.delete()
    }

    @Test fun filenamePolicyAllowsExistingSafeCatalogIdsButRejectsTraversal() {
        listOf("local_12", "audius:123-7", "jamendo-123", "abc.def").forEach {
            assertTrue(it, OfflineFileNamePolicy.accepts(it))
        }
        listOf("", ".", "..", "../lanu-protected-user-file", "x/y",
            "x\\y", "x%2fy", "bad\u0000id", ".hidden",
            "a".repeat(161), "abc..def").forEach {
            assertFalse(it, OfflineFileNamePolicy.accepts(it))
        }
    }

    @Test fun maliciousCatalogIdCannotDeleteOtherOfflineFiles() = runBlocking {
        val id = "../lanu-protected-user-file"
        assertFalse(manager.deleteDownloadedFile(id))
        val song = Song(
            id = id, title = "Unsafe", artist = "Local", artistId = "local",
            album = "Local", durationMs = 500L, category = MusicCategory.LOCAL,
            coverUrl = "", audioUrl = safeSource.toURI().toString(),
            releaseYear = 2026, sourceType = SongSourceType.LOCAL
        )
        val completion = CompletableDeferred<DownloadProgress>()
        manager.startDownload(song, AudioQuality.STANDARD) {
            if (!completion.isCompleted) completion.complete(it)
        }
        val result = withTimeout(15000) { completion.await() }
        assertEquals(DownloadStatus.FAILED, result.status)
        assertTrue(protectedFile.isFile)
        assertEquals("must survive invalid song IDs", protectedFile.readText())
    }
}
