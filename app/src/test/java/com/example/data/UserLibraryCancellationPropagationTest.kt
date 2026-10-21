package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthBackend
import com.example.auth.AuthResult
import com.example.auth.AuthSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserLibraryCancellationPropagationTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun newService(): UserLibraryService {
        val session = AuthSession(
            userId = "cancel-regression-user",
            displayName = "Offline user",
            createdAtMs = 1L
        )
        val auth = object : AuthBackend {
            override suspend fun currentSession(): AuthSession = session
            override suspend fun signIn(identifier: String, secret: String): AuthResult =
                AuthResult.Success(session)
            override suspend fun signOut(): AuthResult = AuthResult.Success(session)
        }
        val backing = LocalUserLibraryBackend(context)
        val cancelledWrites = object : UserLibraryBackend by backing {
            override suspend fun addFavorite(userId: String, songId: String, nowMs: Long) {
                throw CancellationException("favorite task cancelled")
            }
            override suspend fun createPlaylist(
                userId: String, name: String, description: String, nowMs: Long
            ): PlaylistRecord {
                throw CancellationException("playlist task cancelled")
            }
        }
        return UserLibraryService(auth, cancelledWrites)
    }

    @Test fun cancelledFavoriteWriteDoesNotBecomeNormalAuthFailure() = runBlocking {
        val service = newService()
        service.restoreSession()
        val before = service.snapshot.value
        val error = runCatching { service.addFavorite("cancel-test-track") }.exceptionOrNull()
        assertTrue("Cancellation must escape the library boundary", error is CancellationException)
        assertEquals("Cancellation must not rewrite the published snapshot", before, service.snapshot.value)
        assertEquals("cancel-regression-user", service.session.value?.userId)
    }

    @Test fun cancelledPlaylistWriteDoesNotBecomeResultFailure() = runBlocking {
        val service = newService()
        service.restoreSession()
        val before = service.snapshot.value
        val error = runCatching { service.createPlaylist("Cancelled playlist") }.exceptionOrNull()
        assertTrue("Cancelled playlist must propagate structured cancellation", error is CancellationException)
        assertEquals(before, service.snapshot.value)
        assertEquals("cancel-regression-user", service.session.value?.userId)
    }
}
