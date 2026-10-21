package com.example

import com.example.auth.AuthBackend
import com.example.auth.AuthFailure
import com.example.auth.AuthProvider
import com.example.auth.AuthResult
import com.example.auth.AuthSession
import com.example.data.UserLibraryService
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class UserLibraryServiceTest {
    private class FakeAuthBackend : AuthBackend {
        private var session: AuthSession? = null
        override suspend fun currentSession(): AuthSession? = session
        override suspend fun signIn(identifier: String, secret: String): AuthResult {
            if (identifier.isBlank() || secret.isBlank()) return AuthResult.Failure(AuthFailure.INVALID_INPUT, "invalid")
            session = AuthSession("user-1", identifier, null, AuthProvider.LOCAL, 1L)
            return AuthResult.Success(session!!)
        }
        override suspend fun signOut(): AuthResult {
            val old = session ?: return AuthResult.Failure(AuthFailure.UNKNOWN, "none")
            session = null
            return AuthResult.Success(old)
        }
    }

    @Test
    fun operations_require_active_session() = runTest {
        val service = UserLibraryService(FakeAuthBackend(), object : LocalUserLibraryBackendForTest() {})
        val result = service.addFavorite("song-1")
        assertTrue(result is AuthResult.Failure)
        assertEquals(AuthFailure.NOT_CONFIGURED, (result as AuthResult.Failure).reason)
    }

    @Test
    fun successful_session_scopes_library_to_user() = runTest {
        val auth = FakeAuthBackend()
        val backend = InMemoryLibraryBackend()
        val service = UserLibraryService(auth, backend)
        assertTrue(service.signIn("user@example.com", "secret") is AuthResult.Success)
        service.addFavorite("song-1")
        service.recordPlay("song-1")
        val playlist = service.createPlaylist("Benim Listem").getOrThrow()
        service.addSongToPlaylist(playlist.id, "song-1")
        assertEquals(listOf("song-1"), service.snapshot.value?.favorites?.map { it.songId })
        assertEquals(listOf("song-1"), service.snapshot.value?.history?.map { it.songId })
        assertEquals(listOf("song-1"), service.snapshot.value?.playlists?.single()?.songIds)
    }

    @Test
    fun history_can_be_cleared_without_clearing_other_library_data() = runTest {
        val auth = FakeAuthBackend()
        val backend = InMemoryLibraryBackend()
        val service = UserLibraryService(auth, backend)
        service.signIn("user@example.com", "secret")
        service.addFavorite("song-1")
        service.recordPlay("song-1")
        val result = service.clearHistory()
        assertTrue(result is AuthResult.Success)
        assertTrue(service.snapshot.value?.history.orEmpty().isEmpty())
        assertEquals(listOf("song-1"), service.snapshot.value?.favorites?.map { it.songId })
    }

    @Test
    fun sign_out_clears_active_snapshot_and_blocks_writes() = runTest {
        val auth = FakeAuthBackend()
        val backend = InMemoryLibraryBackend()
        val service = UserLibraryService(auth, backend)
        service.signIn("user@example.com", "secret")
        service.addFavorite("song-1")
        service.signOut()
        assertEquals(null, service.session.value)
        assertEquals(null, service.snapshot.value)
        assertTrue(service.addFavorite("song-2") is AuthResult.Failure)
    }


    @Test
    fun concurrentLogoutWaitsForInFlightFavoriteAndNeverRepopulatesSignedOutSnapshot() = runTest {
        val auth = FakeAuthBackend()
        val delegate = InMemoryLibraryBackend()
        val started = CompletableDeferred<Unit>()
        val continueWrite = CompletableDeferred<Unit>()
        val blocking = object : com.example.data.UserLibraryBackend by delegate {
            override suspend fun addFavorite(userId: String, songId: String, nowMs: Long) {
                started.complete(Unit)
                continueWrite.await()
                delegate.addFavorite(userId, songId, nowMs)
            }
        }
        val service = UserLibraryService(auth, blocking)
        assertTrue(service.signIn("user@example.com", "secret") is AuthResult.Success)
        val favorite = async { service.addFavorite("song-during-logout") }
        started.await()
        val logout = async { service.signOut() }
        assertFalse("Logout must wait for the in-flight authorized mutation", logout.isCompleted)
        continueWrite.complete(Unit)
        assertTrue(favorite.await() is AuthResult.Success)
        assertTrue(logout.await() is AuthResult.Success)
        assertNull(service.session.value)
        assertNull(service.snapshot.value)
        assertTrue(service.addFavorite("unauthorized") is AuthResult.Failure)
    }


    @Test
    fun accountSwitchDoesNotPublishNewIdentityWithPreviousUsersFavorites() = runTest {
        val auth = object : AuthBackend {
            var current: AuthSession? = null
            var index = 0
            override suspend fun currentSession(): AuthSession? = current
            override suspend fun signIn(identifier: String, secret: String): AuthResult {
                val identity = AuthSession("user-${++index}", identifier, null, AuthProvider.LOCAL, 1L)
                current = identity
                return AuthResult.Success(identity)
            }
            override suspend fun signOut(): AuthResult {
                val old = current ?: return AuthResult.Failure(AuthFailure.UNKNOWN, "none")
                current = null
                return AuthResult.Success(old)
            }
        }
        val delegate = InMemoryLibraryBackend()
        val started = CompletableDeferred<Unit>()
        val unblock = CompletableDeferred<Unit>()
        val blocked = object : com.example.data.UserLibraryBackend by delegate {
            override suspend fun snapshot(userId: String): com.example.data.UserLibrarySnapshot {
                if (userId == "user-2") {
                    started.complete(Unit)
                    unblock.await()
                }
                return delegate.snapshot(userId)
            }
        }
        val service = UserLibraryService(auth, blocked)
        assertTrue(service.signIn("first", "secret") is AuthResult.Success)
        assertTrue(service.addFavorite("private-first") is AuthResult.Success)
        val switching = async { service.signIn("second", "secret") }
        started.await()
        assertEquals("user-1", service.session.value?.userId)
        assertEquals(listOf("private-first"), service.snapshot.value?.favorites?.map { it.songId })
        unblock.complete(Unit)
        assertTrue(switching.await() is AuthResult.Success)
        assertEquals("user-2", service.session.value?.userId)
        assertTrue(service.snapshot.value?.favorites.orEmpty().isEmpty())
    }


    @Test
    fun corruptLibraryFailsLoginClosedWithoutPublishingPartialIdentity() = runTest {
        val auth = FakeAuthBackend()
        val stable = InMemoryLibraryBackend()
        val failing = object : com.example.data.UserLibraryBackend by stable {
            override suspend fun snapshot(userId: String): com.example.data.UserLibrarySnapshot {
                throw java.io.IOException("corrupt playlist JSON")
            }
        }
        val service = UserLibraryService(auth, failing)
        val login = service.signIn("local@example.com", "secret")
        assertTrue(login is AuthResult.Failure)
        assertEquals(AuthFailure.UNKNOWN, (login as AuthResult.Failure).reason)
        assertNull(service.session.value)
        assertNull(service.snapshot.value)
        assertTrue(service.addFavorite("new-song") is AuthResult.Failure)
    }

    @Test
    fun corruptStoredLibraryDoesNotExposePreviousSessionOrEraseBytes() = runTest {
        val auth = FakeAuthBackend()
        val backend = InMemoryLibraryBackend()
        val normal = UserLibraryService(auth, backend)
        assertTrue(normal.signIn("local@example.com", "secret") is AuthResult.Success)
        assertTrue(normal.addFavorite("retained-song") is AuthResult.Success)

        val failing = object : com.example.data.UserLibraryBackend by backend {
            override suspend fun snapshot(userId: String): com.example.data.UserLibrarySnapshot {
                throw java.io.IOException("malformed backup")
            }
        }
        val restored = UserLibraryService(auth, failing)
        assertNull(restored.restoreSession())
        assertNull(restored.session.value)
        assertNull(restored.snapshot.value)
        // We did not write, clear, or replace the real stored user records.
        assertEquals(listOf("retained-song"), backend.snapshot("user-1").favorites.map { it.songId })
    }

    private open class LocalUserLibraryBackendForTest : com.example.data.UserLibraryBackend {
        private val delegate = InMemoryLibraryBackend()
        override suspend fun snapshot(userId: String) = delegate.snapshot(userId)
        override suspend fun addFavorite(userId: String, songId: String, nowMs: Long) = delegate.addFavorite(userId, songId, nowMs)
        override suspend fun removeFavorite(userId: String, songId: String) = delegate.removeFavorite(userId, songId)
        override suspend fun createPlaylist(userId: String, name: String, description: String, nowMs: Long) = delegate.createPlaylist(userId, name, description, nowMs)
        override suspend fun renamePlaylist(userId: String, playlistId: String, name: String, nowMs: Long) = delegate.renamePlaylist(userId, playlistId, name, nowMs)
        override suspend fun reorderPlaylist(userId: String, playlistId: String, fromIndex: Int, toIndex: Int, nowMs: Long) = delegate.reorderPlaylist(userId, playlistId, fromIndex, toIndex, nowMs)
        override suspend fun deletePlaylist(userId: String, playlistId: String) = delegate.deletePlaylist(userId, playlistId)
        override suspend fun addSongToPlaylist(userId: String, playlistId: String, songId: String, nowMs: Long) = delegate.addSongToPlaylist(userId, playlistId, songId, nowMs)
        override suspend fun removeSongFromPlaylist(userId: String, playlistId: String, songId: String) = delegate.removeSongFromPlaylist(userId, playlistId, songId)
        override suspend fun recordPlay(userId: String, songId: String, playedAtMs: Long) = delegate.recordPlay(userId, songId, playedAtMs)
        override suspend fun clearHistory(userId: String) = delegate.clearHistory(userId)
        override suspend fun clearUser(userId: String) = delegate.clearUser(userId)
    }

    private class InMemoryLibraryBackend : com.example.data.UserLibraryBackend {
        private val data = mutableMapOf<String, MutableList<String>>()
        private val playlists = mutableMapOf<String, MutableList<com.example.data.PlaylistRecord>>()
        private val history = mutableMapOf<String, MutableList<com.example.data.HistoryRecord>>()
        override suspend fun snapshot(userId: String) = com.example.data.UserLibrarySnapshot(userId, data[userId].orEmpty().mapIndexed { i, id -> com.example.data.FavoriteRecord(id, i.toLong()) }, playlists[userId].orEmpty(), history[userId].orEmpty())
        override suspend fun addFavorite(userId: String, songId: String, nowMs: Long) { data.getOrPut(userId) { mutableListOf() }.apply { remove(songId); add(songId) } }
        override suspend fun removeFavorite(userId: String, songId: String) { data[userId]?.remove(songId) }
        override suspend fun createPlaylist(userId: String, name: String, description: String, nowMs: Long): com.example.data.PlaylistRecord {
            val item = com.example.data.PlaylistRecord("pl-1", name, description, nowMs, nowMs, emptyList())
            playlists.getOrPut(userId) { mutableListOf() }.add(item)
            return item
        }
        override suspend fun renamePlaylist(userId: String, playlistId: String, name: String, nowMs: Long) { playlists[userId] = playlists[userId].orEmpty().map { if (it.id == playlistId) it.copy(name = name, updatedAtMs = nowMs) else it }.toMutableList() }
        override suspend fun reorderPlaylist(userId: String, playlistId: String, fromIndex: Int, toIndex: Int, nowMs: Long) {
            playlists[userId] = playlists[userId].orEmpty().map { playlist ->
                if (playlist.id != playlistId) playlist else {
                    val ids = playlist.songIds.toMutableList()
                    if (fromIndex in ids.indices && toIndex in ids.indices) ids.add(toIndex, ids.removeAt(fromIndex))
                    playlist.copy(songIds = ids, updatedAtMs = nowMs)
                }
            }.toMutableList()
        }
        override suspend fun deletePlaylist(userId: String, playlistId: String) { playlists[userId]?.removeAll { it.id == playlistId } }
        override suspend fun addSongToPlaylist(userId: String, playlistId: String, songId: String, nowMs: Long) { playlists[userId] = playlists[userId].orEmpty().map { if (it.id == playlistId) it.copy(songIds = (it.songIds + songId).distinct(), updatedAtMs = nowMs) else it }.toMutableList() }
        override suspend fun removeSongFromPlaylist(userId: String, playlistId: String, songId: String) { playlists[userId] = playlists[userId].orEmpty().map { if (it.id == playlistId) it.copy(songIds = it.songIds.filterNot { id -> id == songId }, updatedAtMs = System.currentTimeMillis()) else it }.toMutableList() }
        override suspend fun recordPlay(userId: String, songId: String, playedAtMs: Long) { history.getOrPut(userId) { mutableListOf() }.apply { removeAll { it.songId == songId }; add(0, com.example.data.HistoryRecord(songId, playedAtMs)) } }
        override suspend fun clearHistory(userId: String) { history.remove(userId) }
        override suspend fun clearUser(userId: String) { data.remove(userId); playlists.remove(userId); history.remove(userId) }
    }
}
