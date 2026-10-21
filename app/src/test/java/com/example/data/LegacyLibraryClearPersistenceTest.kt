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
}
