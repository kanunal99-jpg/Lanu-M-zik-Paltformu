package com.example

import com.example.auth.AuthBackend
import com.example.auth.AuthFailure
import com.example.auth.AuthProvider
import com.example.auth.AuthResult
import com.example.auth.AuthSession
import com.example.data.UserLibraryService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserLibraryServiceTest {
    private class FakeAuthBackend : AuthBackend {
        private var session: AuthSession? = null

        override suspend fun currentSession(): AuthSession? = session

        override suspend fun signIn(identifier: String, secret: String): AuthResult {
            if (identifier.isBlank() || secret.isBlank()) {
                return AuthResult.Failure(AuthFailure.INVALID_INPUT, "invalid")
            }
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

        val login = service.signIn("user@example.com", "secret")
        assertTrue(login is AuthResult.Success)
        service.addFavorite("song-1")
        service.recordPlay("song-1")
        val playlist = service.createPlaylist("Benim Listem").getOrThrow()
        service.addSongToPlaylist(playlist.id, "song-1")

        assertEquals(listOf("song-1"), service.snapshot.value?.favorites?.map { it.songId })
        assertEquals(listOf("song-1"), service.snapshot.value?.history?.map { it.songId })
        assertEquals(listOf("song-1"), service.snapshot.value?.playlists?.single()?.songIds)
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

    private open class LocalUserLibraryBackendForTest : com.example.data.UserLibraryBackend {
        private val delegate = InMemoryLibraryBackend()
        override suspend fun snapshot(userId: String) = delegate.snapshot(userId)
        override suspend fun addFavorite(userId: String, songId: String, nowMs: Long) = delegate.addFavorite(userId, songId, nowMs)
        override suspend fun removeFavorite(userId: String, songId: String) = delegate.removeFavorite(userId, songId)
        override suspend fun createPlaylist(userId: String, name: String, description: String, nowMs: Long) = delegate.createPlaylist(userId, name, description, nowMs)
        override suspend fun deletePlaylist(userId: String, playlistId: String) = delegate.deletePlaylist(userId, playlistId)
        override suspend fun addSongToPlaylist(userId: String, playlistId: String, songId: String, nowMs: Long) = delegate.addSongToPlaylist(userId, playlistId, songId, nowMs)
        override suspend fun removeSongFromPlaylist(userId: String, playlistId: String, songId: String) = delegate.removeSongFromPlaylist(userId, playlistId, songId)
        override suspend fun recordPlay(userId: String, songId: String, playedAtMs: Long) = delegate.recordPlay(userId, songId, playedAtMs)
        override suspend fun clearUser(userId: String) = delegate.clearUser(userId)
    }

    private class InMemoryLibraryBackend : com.example.data.UserLibraryBackend {
        private val data = mutableMapOf<String, MutableList<String>>()
        private val playlists = mutableMapOf<String, MutableList<com.example.data.PlaylistRecord>>()
        private val history = mutableMapOf<String, MutableList<com.example.data.HistoryRecord>>()

        override suspend fun snapshot(userId: String) = com.example.data.UserLibrarySnapshot(
            userId,
            data[userId].orEmpty().mapIndexed { i, id -> com.example.data.FavoriteRecord(id, i.toLong()) },
            playlists[userId].orEmpty(),
            history[userId].orEmpty()
        )
        override suspend fun addFavorite(userId: String, songId: String, nowMs: Long) { data.getOrPut(userId) { mutableListOf() }.apply { remove(songId); add(songId) } }
        override suspend fun removeFavorite(userId: String, songId: String) { data[userId]?.remove(songId) }
        override suspend fun createPlaylist(userId: String, name: String, description: String, nowMs: Long): com.example.data.PlaylistRecord {
            val item = com.example.data.PlaylistRecord("pl-1", name, description, nowMs, nowMs, emptyList())
            playlists.getOrPut(userId) { mutableListOf() }.add(item)
            return item
        }
        override suspend fun deletePlaylist(userId: String, playlistId: String) {
            playlists[userId]?.removeAll { it.id == playlistId }
        }
        override suspend fun addSongToPlaylist(userId: String, playlistId: String, songId: String, nowMs: Long) {
            playlists[userId] = playlists[userId].orEmpty().map { if (it.id == playlistId) it.copy(songIds = (it.songIds + songId).distinct()) else it }.toMutableList()
        }
        override suspend fun removeSongFromPlaylist(userId: String, playlistId: String, songId: String) { playlists[userId] = playlists[userId].orEmpty().map { if (it.id == playlistId) it.copy(songIds = it.songIds.filterNot { id -> id == songId }) else it }.toMutableList() }
        override suspend fun recordPlay(userId: String, songId: String, playedAtMs: Long) { history.getOrPut(userId) { mutableListOf() }.apply { removeAll { it.songId == songId }; add(0, com.example.data.HistoryRecord(songId, playedAtMs)) } }
        override suspend fun clearUser(userId: String) { data.remove(userId); playlists.remove(userId); history.remove(userId) }
    }
}
