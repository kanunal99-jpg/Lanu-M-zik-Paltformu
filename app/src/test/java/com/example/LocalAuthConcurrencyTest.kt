package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthResult
import com.example.auth.LocalAuthBackend
import com.example.data.LocalUserLibraryBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalAuthConcurrencyTest {
    private lateinit var context: Context
    private lateinit var backend: LocalAuthBackend

    @Before fun resetIdentity() {
        context = ApplicationProvider.getApplicationContext()
        assertTrue(context.getSharedPreferences("lanu_auth", Context.MODE_PRIVATE)
            .edit().clear().commit())
        backend = LocalAuthBackend(context)
    }

    @Test fun parallelFirstLaunchAcrossBackendInstancesRetainsOneUserId() = runBlocking {
        val instances = List(4) { LocalAuthBackend(context) }
        val sessions = coroutineScope {
            (0 until 32).map { index ->
                async(Dispatchers.Default) { instances[index % 4].ensureLocalSession() }
            }.awaitAll()
        }
        val ids = sessions.map { it.userId }.toSet()
        assertEquals(1, ids.size)
        assertTrue(ids.single().startsWith("local-"))
        assertEquals(ids.single(), LocalAuthBackend(context).currentSession()?.userId)
        assertEquals(1, sessions.map { it.createdAtMs }.toSet().size)
    }

    @Test fun logoutAndReopenKeepsOriginalLibraryIdentity() = runBlocking {
        val initial = backend.ensureLocalSession()
        assertTrue(backend.signOut() is AuthResult.Success)
        assertNull(backend.currentSession())
        val again = LocalAuthBackend(context).ensureLocalSession()
        assertEquals(initial.userId, again.userId)
        assertNotNull(again.createdAtMs)
    }

    @Test fun simultaneousLocalSignInAndSessionCreationNeverForksIdentity() = runBlocking {
        val a = LocalAuthBackend(context)
        val b = LocalAuthBackend(context)
        val results = coroutineScope {
            (0 until 24).map { index ->
                async(Dispatchers.Default) {
                    if (index % 2 == 0) {
                        a.ensureLocalSession().userId
                    } else {
                        (b.signIn("local-user", "local-entry") as AuthResult.Success).session.userId
                    }
                }
            }.awaitAll()
        }
        assertEquals(1, results.toSet().size)
        assertEquals(results.first(), LocalAuthBackend(context).currentSession()?.userId)
    }

    @Test fun logoutAndReopenPreservesUserLibraryUnderSameIdentity() = runBlocking {
        val firstId = backend.ensureLocalSession().userId
        val library = LocalUserLibraryBackend(context)
        library.addFavorite(firstId, "first-song", nowMs = 5L)
        val playlist = library.createPlaylist(firstId, "Korunan Liste", nowMs = 6L)
        library.addSongToPlaylist(firstId, playlist.id, "first-song", nowMs = 7L)

        assertTrue(backend.signOut() is AuthResult.Success)
        val restoredId = LocalAuthBackend(context).ensureLocalSession().userId
        assertEquals(firstId, restoredId)
        val restored = LocalUserLibraryBackend(context).snapshot(restoredId)
        assertEquals(listOf("first-song"), restored.favorites.map { it.songId })
        assertEquals(listOf("first-song"), restored.playlists.single().songIds)
    }
}
