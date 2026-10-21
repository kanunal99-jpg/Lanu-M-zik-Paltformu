package com.example.data

import android.content.Context
import android.database.MatrixCursor
import android.provider.MediaStore
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 29, 35])
class LocalMusicScannerFailureTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun mediaStoreAccessRevokedMustPropagateSecurityErrorInsteadOfEmptySuccess() = runBlocking {
        val scanner = LocalMusicScanner(context) { _, _, _ ->
            throw SecurityException("READ_MEDIA_AUDIO permission revoked")
        }
        val failure = runCatching { scanner.scanLocalMusic() }.exceptionOrNull()
        assertTrue(failure is SecurityException)
    }

    @Test fun brokenOEMCursorMustNotReturnPartiallyScannedSongs() = runBlocking {
        val cols = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE)
        val rows = MatrixCursor(cols)
        rows.addRow(arrayOf<Any>(100L, "First track"))
        rows.addRow(arrayOf<Any>(101L, "Second track"))
        var processed = 0
        val scanner = LocalMusicScanner(
            context,
            mapRow = { _ ->
                processed++
                if (processed == 2) throw IllegalStateException("OEM provider failed halfway")
                // Keep this regression focused on mid-scan failure propagation:
                // Robolectric's Android URI mapper is covered by the separate
                // valid-cursor test, not coupled to this partial-scan fixture.
                Song(
                    id = "local_100",
                    title = "First track",
                    artist = "Test artist",
                    artistId = "test_artist",
                    album = "Test album",
                    durationMs = 0L,
                    category = MusicCategory.LOCAL,
                    coverUrl = "",
                    audioUrl = "content://media/external/audio/media/100",
                    releaseYear = 0,
                    sourceType = SongSourceType.LOCAL
                )
            },
            audioQuery = { _, _, _ -> rows }
        )
        val error = runCatching { scanner.scanLocalMusic() }.exceptionOrNull()
        assertTrue("Expected IOException from incomplete OEM cursor, got: $error", error is IOException)
        // Coroutines can copy IOException while recovering stack traces on a
        // dispatcher boundary, nesting the original IOException as its cause.
        // Verify the root failure without weakening the fail-closed contract.
        val rootCause = error?.let { generateSequence(it) { failure -> failure.cause }.last() }
        assertTrue(
            "Expected original OEM provider failure at the root, got: $rootCause",
            rootCause is IllegalStateException && rootCause.message == "OEM provider failed halfway"
        )
        assertTrue(error?.message.orEmpty().contains("korundu"))
        assertEquals("First song was mapped before the second-row error", 2, processed)
    }

    @Test fun validOEMCursorStillReturnsAllReadableLocalTracks() = runBlocking {
        val rows = MatrixCursor(arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE
        ))
        rows.addRow(arrayOf<Any>(200L, "A"))
        rows.addRow(arrayOf<Any>(201L, "B"))
        val scanner = LocalMusicScanner(context) { _, _, _ -> rows }
        val result = scanner.scanLocalMusic()
        assertEquals(listOf("local_200", "local_201"), result.map { it.id })
    }
}
