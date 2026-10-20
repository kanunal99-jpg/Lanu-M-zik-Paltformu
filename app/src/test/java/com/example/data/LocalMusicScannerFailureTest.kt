package com.example.data

import android.content.Context
import android.database.CursorWrapper
import android.database.MatrixCursor
import android.provider.MediaStore
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
        rows.addRow(arrayOf(100L, "First track"))
        rows.addRow(arrayOf(101L, "Second track"))
        val scanner = LocalMusicScanner(context) { _, _, _ ->
            object : CursorWrapper(rows) {
                private var rowsRead = 0
                override fun moveToNext(): Boolean {
                    if (rowsRead++ >= 1) throw IllegalStateException("OEM provider failed halfway")
                    return super.moveToNext()
                }
            }
        }
        val error = runCatching { scanner.scanLocalMusic() }.exceptionOrNull()
        assertTrue(error is IOException)
        assertTrue(error?.cause is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("korundu"))
    }

    @Test fun validOEMCursorStillReturnsAllReadableLocalTracks() = runBlocking {
        val rows = MatrixCursor(arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE
        ))
        rows.addRow(arrayOf(200L, "A"))
        rows.addRow(arrayOf(201L, "B"))
        val scanner = LocalMusicScanner(context) { _, _, _ -> rows }
        val result = scanner.scanLocalMusic()
        assertEquals(listOf("local_200", "local_201"), result.map { it.id })
    }
}
