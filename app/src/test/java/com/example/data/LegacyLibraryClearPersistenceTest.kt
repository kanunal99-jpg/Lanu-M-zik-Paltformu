package com.example.data

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class LegacyLibraryClearPersistenceTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun clearingLastFavoriteRemainsExplicitlyEmptyAfterBackendRecreation() = runBlocking {
        val user = "migration-test-${UUID.randomUUID()}"
        val backend = LocalUserLibraryBackend(context)
        backend.addFavorite(user, "legacy-track")
        backend.removeFavorite(user, "legacy-track")
        val reopened = LocalUserLibraryBackend(context)
        assertTrue(reopened.hasPersistedCategory(user, "favorites"))
        assertTrue(reopened.snapshot(user).favorites.isEmpty())
    }

    @Test fun clearHistoryAndClearUserPersistEmptyTombstones() = runBlocking {
        val user = "migration-test-${UUID.randomUUID()}"
        val backend = LocalUserLibraryBackend(context)
        backend.recordPlay(user, "old-song")
        backend.clearHistory(user)
        assertTrue(LocalUserLibraryBackend(context).hasPersistedCategory(user, "history"))
        assertTrue(backend.snapshot(user).history.isEmpty())
        backend.addFavorite(user, "old-song")
        backend.createPlaylist(user, "Old playlist")
        backend.clearUser(user)
        val reopened = LocalUserLibraryBackend(context)
        for (category in listOf("favorites", "playlists", "history")) {
            assertTrue(reopened.hasPersistedCategory(user, category))
        }
        val snapshot = reopened.snapshot(user)
        assertTrue(snapshot.favorites.isEmpty())
        assertTrue(snapshot.playlists.isEmpty())
        assertTrue(snapshot.history.isEmpty())
    }

    @Test fun markingEmptyLegacyCategoryIsIdempotentAndDoesNotOverwriteData() = runBlocking {
        val user = "migration-test-${UUID.randomUUID()}"
        val backend = LocalUserLibraryBackend(context)
        assertFalse(backend.hasPersistedCategory(user, "favorites"))
        backend.markLegacyCategoryHandled(user, "favorites")
        backend.markLegacyCategoryHandled(user, "favorites")
        backend.addFavorite(user, "new-track")
        backend.markLegacyCategoryHandled(user, "favorites")
        assertEquals("new-track", LocalUserLibraryBackend(context).snapshot(user).favorites.single().songId)
    }
    @Test fun atomicLegacyImportKeepsPlaylistIdentityOrderAndDoesNotResurrectClears() = runBlocking {
        val user = "migration-test-${UUID.randomUUID()}"
        val backend = LocalUserLibraryBackend(context)
        val playlist = PlaylistRecord("old-playlist-id", "Original", "Legacy", 100L, 120L, listOf("song-b", "song-a"))
        val favorites = listOf(FavoriteRecord("old-song", 200L))
        val history = listOf(HistoryRecord("old-song", 300L))
        backend.importLegacyIfAbsent(user, favorites, listOf(playlist), history)
        val first = LocalUserLibraryBackend(context).snapshot(user)
        assertEquals(playlist, first.playlists.single())
        assertEquals("old-song", first.favorites.single().songId)
        backend.removeFavorite(user, "old-song")
        backend.deletePlaylist(user, playlist.id)
        backend.clearHistory(user)
        backend.importLegacyIfAbsent(user, favorites, listOf(playlist), history)
        val reopened = LocalUserLibraryBackend(context).snapshot(user)
        assertTrue(reopened.favorites.isEmpty())
        assertTrue(reopened.playlists.isEmpty())
        assertTrue(reopened.history.isEmpty())
    }

    @Test fun atomicLegacyImportDoesNotOverwritePreexistingNewLibrary() = runBlocking {
        val user = "migration-test-${UUID.randomUUID()}"
        val backend = LocalUserLibraryBackend(context)
        backend.addFavorite(user, "new-song")
        backend.importLegacyIfAbsent(user, listOf(FavoriteRecord("old-song", 1L)), emptyList(), emptyList())
        val reopened = LocalUserLibraryBackend(context).snapshot(user)
        assertEquals(listOf("new-song"), reopened.favorites.map { it.songId })
    }

}
