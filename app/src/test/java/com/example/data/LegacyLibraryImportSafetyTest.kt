package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthBackend
import com.example.auth.AuthResult
import com.example.auth.AuthSession
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression for #89: user-deleted favorites/playlists/history MUST NOT resurrect. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 29, 35])
class LegacyLibraryImportSafetyTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun reset() {
        assertTrue(
            context.getSharedPreferences("lanu_user_library", Context.MODE_PRIVATE)
                .edit().clear().commit()
        )
    }

    private fun legacy(id: String) = UserLibrarySnapshot(
        userId = id,
        favorites = listOf(FavoriteRecord("old-favorite", 30L)),
        playlists = listOf(
            PlaylistRecord("old-list", "Previous Playlist", "safe",
                10L, 20L, listOf("song-one", "song-two"))
        ),
        history = listOf(HistoryRecord("played-song", 40L))
    )

    @Test fun firstImportCommitsAllLegacySectionsExactlyOnceAndSurvivesReopen() = runBlocking {
        reset()
        val user = "local-first-import"
        val original = legacy(user)
        val first = LocalUserLibraryBackend(context)
        assertTrue(first.needsLegacyImport(user))
        assertTrue(first.importLegacyIfUninitialized(original))
        assertEquals(original, first.snapshot(user))
        val reopened = LocalUserLibraryBackend(context)
        assertFalse(reopened.needsLegacyImport(user))
        assertFalse(reopened.importLegacyIfUninitialized(original))
        assertEquals(original, reopened.snapshot(user))
    }

    @Test fun clearingLastItemsAndHistoryCannotResurrectRetiredRoomRecords() = runBlocking {
        reset()
        val user = "local-explicit-clear"
        val backend = LocalUserLibraryBackend(context)
        val old = legacy(user)
        assertTrue(backend.importLegacyIfUninitialized(old))
        backend.removeFavorite(user, "old-favorite")
        backend.deletePlaylist(user, "old-list")
        backend.clearHistory(user)
        val reopened = LocalUserLibraryBackend(context)
        assertFalse(reopened.needsLegacyImport(user))
        assertFalse(reopened.importLegacyIfUninitialized(old))
        assertEquals(UserLibrarySnapshot(user), reopened.snapshot(user))
    }

    @Test fun clearingEntireCurrentUserRetainsDurableEmptyTombstones() = runBlocking {
        reset()
        val user = "local-account-erased"
        val backend = LocalUserLibraryBackend(context)
        val original = legacy(user)
        assertTrue(backend.importLegacyIfUninitialized(original))
        backend.clearUser(user)
        val reopened = LocalUserLibraryBackend(context)
        assertFalse(reopened.needsLegacyImport(user))
        assertFalse(reopened.importLegacyIfUninitialized(original))
        assertEquals(UserLibrarySnapshot(user), reopened.snapshot(user))
    }

    @Test fun existingModernFavoritePreventsAllRetiredRoomRowsBeingReimported() = runBlocking {
        reset()
        val user = "local-modern-owned"
        val backend = LocalUserLibraryBackend(context)
        backend.addFavorite(user, "my-modern-favorite")
        assertFalse(backend.needsLegacyImport(user))
        assertFalse(backend.importLegacyIfUninitialized(legacy(user)))
        val snapshot = LocalUserLibraryBackend(context).snapshot(user)
        assertEquals(listOf("my-modern-favorite"), snapshot.favorites.map { it.songId })
        assertTrue(snapshot.playlists.isEmpty())
        assertTrue(snapshot.history.isEmpty())
    }

    @Test fun intentionalClearBeforeFirstStartupAlsoBlocksLegacyResurrection() = runBlocking {
        reset()
        val user = "local-clear-before-import"
        val backend = LocalUserLibraryBackend(context)
        backend.clearUser(user)
        assertFalse(backend.needsLegacyImport(user))
        assertFalse(backend.importLegacyIfUninitialized(legacy(user)))
        assertEquals(UserLibrarySnapshot(user), backend.snapshot(user))
    }

    @Test fun emptyInitialImportIsRecordedAndDoesNotLaterImportStaleRows() = runBlocking {
        reset()
        val user = "local-empty-first"
        val backend = LocalUserLibraryBackend(context)
        assertTrue(backend.importLegacyIfUninitialized(UserLibrarySnapshot(user)))
        assertFalse(backend.importLegacyIfUninitialized(legacy(user)))
        assertEquals(UserLibrarySnapshot(user), backend.snapshot(user))
    }

    @Test fun simultaneousBackendImportsAreSerializedAcrossRepositoryInstances() = runBlocking {
        reset()
        val user = "local-racing-imports"
        val first = LocalUserLibraryBackend(context)
        val second = LocalUserLibraryBackend(context)
        val old = legacy(user)
        val outcomes = coroutineScope {
            listOf(
                async { first.importLegacyIfUninitialized(old) },
                async { second.importLegacyIfUninitialized(old) }
            ).map { it.await() }
        }
        assertEquals(1, outcomes.count { it })
        assertEquals(old, first.snapshot(user))
        assertEquals(old, second.snapshot(user))
    }

    @Test fun legacyRoomReaderPreservesActualPlaylistAndFavoriteRows() = runBlocking {
        reset()
        val user = "local-room-fixture"
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.musicDao()
            dao.addFavorite(FavoriteSongEntity("old-favorite", 30L))
            dao.insertPlaylist(PlaylistEntity(
                id = "old-list", name = "Previous Playlist",
                description = "safe", createdAt = 10L, updatedAt = 20L
            ))
            dao.addSongToPlaylist(PlaylistSongEntity("old-list", "song-two", orderIndex = 1))
            dao.addSongToPlaylist(PlaylistSongEntity("old-list", "song-one", orderIndex = 0))
            dao.addToHistory(HistoryEntity(songId = "played-song", playedAt = 40L))

            val readOnlyLegacy = loadLegacyRoomLibrary(dao, user)
            assertEquals(legacy(user), readOnlyLegacy)
            val target = LocalUserLibraryBackend(context)
            assertTrue(target.importLegacyIfUninitialized(readOnlyLegacy))
            assertEquals(readOnlyLegacy, target.snapshot(user))
            // This migration must not delete the old Room source.
            assertEquals(1, dao.getFavoriteSongs().first().size)
        } finally {
            db.close()
        }
    }

    @Test fun accountSwitchCannotImportAnotherAccountsLegacySnapshot() = runBlocking {
        reset()
        val activeUser = "local-current-user"
        val session = AuthSession(userId = activeUser, displayName = "Current", createdAtMs = 1L)
        val auth = object : AuthBackend {
            override suspend fun currentSession(): AuthSession = session
            override suspend fun signIn(identifier: String, secret: String): AuthResult =
                AuthResult.Success(session)
            override suspend fun signOut(): AuthResult = AuthResult.Success(session)
        }
        val service = UserLibraryService(auth, LocalUserLibraryBackend(context))
        service.restoreSession()
        assertFalse(service.importLegacyIfNeeded(legacy("local-previous-user")))
        assertEquals(UserLibrarySnapshot(activeUser), service.snapshot.value)
        assertTrue(service.importLegacyIfNeeded(legacy(activeUser)))
        assertEquals(legacy(activeUser), service.snapshot.value)
    }
}
