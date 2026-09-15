package com.example

import com.example.sync.LocalOnlyMusicSyncBackend
import com.example.sync.SyncSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSyncBackendTest {
    @Test
    fun localOnlyBackendDoesNotInventRemoteLibrary() = runTest {
        val backend = LocalOnlyMusicSyncBackend()

        val snapshot = backend.pullLibrary()
        val result = backend.pushLibrary(
            SyncSnapshot(favoriteSongIds = setOf("local-song"))
        )

        assertTrue(snapshot.favoriteSongIds.isEmpty())
        assertTrue(snapshot.playlists.isEmpty())
        assertFalse(result.accepted)
        assertTrue(result.errorMessage?.contains("not configured") == true)
    }
}
