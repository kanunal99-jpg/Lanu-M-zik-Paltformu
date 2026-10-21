package com.example.data

import com.example.auth.AuthBackend
import com.example.auth.AuthFailure
import com.example.auth.AuthResult
import com.example.auth.AuthSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Coordinates authentication and user-owned library data without coupling the app to a cloud SDK. */
class UserLibraryService(
    private val authBackend: AuthBackend,
    private val libraryBackend: UserLibraryBackend
) {
    private val sessionTransitionMutex = Mutex()
    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private val _snapshot = MutableStateFlow<UserLibrarySnapshot?>(null)
    val snapshot: StateFlow<UserLibrarySnapshot?> = _snapshot.asStateFlow()

    suspend fun restoreSession(): AuthSession? = sessionTransitionMutex.withLock {
        val current = authBackend.currentSession()
        // Read data first. Publishing the new identity before snapshot loading
        // could briefly expose the previous account's favorites under a new ID.
        val restoredSnapshot = try {
            current?.let { libraryBackend.snapshot(it.userId) }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            // Corrupt local JSON is NOT an empty library: don't overwrite it,
            // don't publish an authenticated identity with stale user content.
            _session.value = null
            _snapshot.value = null
            return@withLock null
        }
        _snapshot.value = restoredSnapshot
        _session.value = current
        current
    }

    suspend fun signIn(identifier: String, secret: String): AuthResult = sessionTransitionMutex.withLock {
        val result = authBackend.signIn(identifier, secret)
        if (result is AuthResult.Success) {
            // Avoid publishing another user's identity with the old favorites.
            val nextSnapshot = try {
                libraryBackend.snapshot(result.session.userId)
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _session.value = null
                _snapshot.value = null
                return@withLock AuthResult.Failure(
                    AuthFailure.UNKNOWN, "Yerel arşiv okunamadı; kayıtlar korunuyor."
                )
            }
            _snapshot.value = nextSnapshot
            _session.value = result.session
        }
        result
    }

    suspend fun signOut(): AuthResult = sessionTransitionMutex.withLock {
        val result = authBackend.signOut()
        if (result is AuthResult.Success) {
            _session.value = null
            _snapshot.value = null
        }
        result
    }


    /**
     * Import only the current session's legacy data. Keep session publication
     * synchronized with user edits; read back after atomic legacy persistence.
     */
    suspend fun importLegacyIfNeeded(legacy: UserLibrarySnapshot): Boolean =
        sessionTransitionMutex.withLock {
            val session = _session.value ?: return@withLock false
            if (legacy.userId != session.userId) return@withLock false
            val localBackend = libraryBackend as? LocalUserLibraryBackend
                ?: return@withLock false
            val imported = localBackend.importLegacyIfUninitialized(legacy)
            if (imported) refreshSnapshot(session.userId)
            imported
        }

    suspend fun addFavorite(songId: String): AuthResult = withSession { userId ->
        libraryBackend.addFavorite(userId, songId)
        refreshSnapshot(userId)
    }

    suspend fun removeFavorite(songId: String): AuthResult = withSession { userId ->
        libraryBackend.removeFavorite(userId, songId)
        refreshSnapshot(userId)
    }

    suspend fun createPlaylist(name: String, description: String = ""): Result<PlaylistRecord> = sessionTransitionMutex.withLock {
        val userId = _session.value?.userId
            ?: return@withLock Result.failure(IllegalStateException("Aktif LANU oturumu yok"))
        try {
            Result.success(libraryBackend.createPlaylist(userId, name, description).also {
                refreshSnapshot(userId)
            })
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun renamePlaylist(playlistId: String, name: String): AuthResult = withSession { userId ->
        libraryBackend.renamePlaylist(userId, playlistId, name)
        refreshSnapshot(userId)
    }

    suspend fun reorderPlaylist(playlistId: String, fromIndex: Int, toIndex: Int): AuthResult = withSession { userId ->
        libraryBackend.reorderPlaylist(userId, playlistId, fromIndex, toIndex)
        refreshSnapshot(userId)
    }

    suspend fun deletePlaylist(playlistId: String): AuthResult = withSession { userId ->
        libraryBackend.deletePlaylist(userId, playlistId)
        refreshSnapshot(userId)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String): AuthResult = withSession { userId ->
        libraryBackend.addSongToPlaylist(userId, playlistId, songId)
        refreshSnapshot(userId)
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): AuthResult = withSession { userId ->
        libraryBackend.removeSongFromPlaylist(userId, playlistId, songId)
        refreshSnapshot(userId)
    }

    suspend fun recordPlay(songId: String): AuthResult = withSession { userId ->
        libraryBackend.recordPlay(userId, songId)
        refreshSnapshot(userId)
    }

    suspend fun clearHistory(): AuthResult = withSession { userId ->
        libraryBackend.clearHistory(userId)
        refreshSnapshot(userId)
    }

    suspend fun clearCurrentUser(): AuthResult = withSession { userId ->
        libraryBackend.clearUser(userId)
        refreshSnapshot(userId)
    }

    private suspend fun refreshSnapshot(userId: String) {
        _snapshot.value = libraryBackend.snapshot(userId)
    }

    private suspend fun withSession(action: suspend (String) -> Unit): AuthResult = sessionTransitionMutex.withLock {
        val session = _session.value
            ?: return@withLock AuthResult.Failure(AuthFailure.NOT_CONFIGURED, "Aktif LANU oturumu yok")
        try {
            action(session.userId)
            AuthResult.Success(session)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            // Cancellation belongs to structured concurrency, not a user-facing
            // write failure. Never swallow it and continue stale account work.
            throw cancelled
        } catch (error: Exception) {
            AuthResult.Failure(AuthFailure.UNKNOWN, error.message ?: "Kullanıcı kitaplığı işlemi başarısız")
        }
    }
}
