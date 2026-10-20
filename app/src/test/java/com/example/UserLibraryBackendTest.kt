package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocalUserLibraryBackend
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UserLibraryBackendTest {
    private lateinit var backend: LocalUserLibraryBackend

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lanu_user_library", Context.MODE_PRIVATE)
            .edit().clear().commit()
        backend = LocalUserLibraryBackend(context)
    }

    @Test
    fun `library data is isolated by user id`() = runBlocking {
        backend.addFavorite("user-a", "song-1", nowMs = 10L)
        backend.addFavorite("user-b", "song-2", nowMs = 20L)

        assertEquals(listOf("song-1"), backend.snapshot("user-a").favorites.map { it.songId })
        assertEquals(listOf("song-2"), backend.snapshot("user-b").favorites.map { it.songId })
    }

    @Test
    fun `playlist and history persist locally`() = runBlocking {
        val playlist = backend.createPlaylist("user-a", "Gece", "Favoriler", nowMs = 100L)
        backend.addSongToPlaylist("user-a", playlist.id, "song-1", nowMs = 120L)
        backend.addSongToPlaylist("user-a", playlist.id, "song-2", nowMs = 130L)
        backend.recordPlay("user-a", "song-2", playedAtMs = 200L)

        val snapshot = backend.snapshot("user-a")
        assertEquals(listOf("song-1", "song-2"), snapshot.playlists.single().songIds)
        assertEquals("song-2", snapshot.history.single().songId)
        assertEquals(200L, snapshot.history.single().playedAtMs)
    }

    @Test
    fun `removing favorite only affects selected user`() = runBlocking {
        backend.addFavorite("user-a", "song-1", nowMs = 10L)
        backend.addFavorite("user-b", "song-1", nowMs = 20L)

        backend.removeFavorite("user-a", "song-1")

        assertTrue(backend.snapshot("user-a").favorites.isEmpty())
        assertEquals(listOf("song-1"), backend.snapshot("user-b").favorites.map { it.songId })
    }

    @Test
    fun `playlist ids are unique`() = runBlocking {
        val first = backend.createPlaylist("user-a", "Bir")
        val second = backend.createPlaylist("user-a", "İki")
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `deleted playlist is removed without touching another user`() = runBlocking {
        val userAPlaylist = backend.createPlaylist("user-a", "A")
        val userBPlaylist = backend.createPlaylist("user-b", "B")

        backend.deletePlaylist("user-a", userAPlaylist.id)

        assertTrue(backend.snapshot("user-a").playlists.isEmpty())
        assertEquals(listOf(userBPlaylist.id), backend.snapshot("user-b").playlists.map { it.id })
    }

    @Test
    fun `renaming playlist persists and is isolated by user`() = runBlocking {
        val userA = backend.createPlaylist("user-a", "Gece")
        val userB = backend.createPlaylist("user-b", "Sabah")

        backend.renamePlaylist("user-a", userA.id, "Gece Sürüşü", nowMs = 500L)

        val a = backend.snapshot("user-a").playlists.single()
        val b = backend.snapshot("user-b").playlists.single()
        assertEquals("Gece Sürüşü", a.name)
        assertEquals(500L, a.updatedAtMs)
        assertEquals("Sabah", b.name)
        assertNotEquals(a.name, b.name)
    }

    @Test
    fun `reordering playlist preserves exact song order`() = runBlocking {
        val playlist = backend.createPlaylist("user-a", "Sıra")
        backend.addSongToPlaylist("user-a", playlist.id, "song-1")
        backend.addSongToPlaylist("user-a", playlist.id, "song-2")
        backend.addSongToPlaylist("user-a", playlist.id, "song-3")

        backend.reorderPlaylist("user-a", playlist.id, fromIndex = 0, toIndex = 2, nowMs = 900L)

        val updated = backend.snapshot("user-a").playlists.single()
        assertEquals(listOf("song-2", "song-3", "song-1"), updated.songIds)
        assertEquals(900L, updated.updatedAtMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reordering rejects invalid source index`() = runBlocking {
        val playlist = backend.createPlaylist("user-a", "Sıra")
        backend.addSongToPlaylist("user-a", playlist.id, "song-1")
        backend.reorderPlaylist("user-a", playlist.id, fromIndex = 2, toIndex = 0)
    }

    @Test
    fun `history keeps latest fifty unique song entries`() = runBlocking {
        repeat(55) { index ->
            backend.recordPlay("user-a", "song-$index", playedAtMs = index.toLong())
        }
        val history = backend.snapshot("user-a").history
        assertEquals(50, history.size)
        assertEquals("song-54", history.first().songId)
        assertEquals("song-5", history.last().songId)
    }

    @Test
    fun `clearing history only affects selected user`() = runBlocking {
        backend.recordPlay("user-a", "song-a", playedAtMs = 10L)
        backend.recordPlay("user-b", "song-b", playedAtMs = 20L)

        backend.clearHistory("user-a")

        assertTrue(backend.snapshot("user-a").history.isEmpty())
        assertEquals(listOf("song-b"), backend.snapshot("user-b").history.map { it.songId })
    }

    @Test
    fun `concurrent favorites from multiple backend instances retain every item`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instances = List(3) { LocalUserLibraryBackend(context) }
        coroutineScope {
            (0 until 32).map { index ->
                async(Dispatchers.Default) {
                    instances[index % instances.size].addFavorite("user-a", "song-$index", nowMs = index.toLong())
                }
            }.awaitAll()
        }
        val observed = LocalUserLibraryBackend(context).snapshot("user-a")
        assertEquals(32, observed.favorites.size)
        assertEquals((0 until 32).map { "song-$it" }.toSet(), observed.favorites.map { it.songId }.toSet())
    }

    @Test
    fun `simultaneous playlist insertions do not lose each other`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = LocalUserLibraryBackend(context)
        val second = LocalUserLibraryBackend(context)
        val playlist = first.createPlaylist("user-a", "Kayıpsız Liste", nowMs = 4L)
        coroutineScope {
            (0 until 18).map { index ->
                async(Dispatchers.Default) {
                    (if (index % 2 == 0) first else second).addSongToPlaylist(
                        "user-a", playlist.id, "song-$index", nowMs = (index + 10).toLong()
                    )
                }
            }.awaitAll()
        }
        val saved = LocalUserLibraryBackend(context).snapshot("user-a").playlists.single()
        assertEquals(18, saved.songIds.size)
        assertEquals((0 until 18).map { "song-$it" }.toSet(), saved.songIds.toSet())
    }

    @Test
    fun `corrupt favorites fail closed and never overwrite original preference`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lanu_user_library", Context.MODE_PRIVATE)
        val original = "{broken favorites JSON"
        assertTrue(prefs.edit().putString("favorites::user-a", original).commit())
        val error = runCatching { backend.addFavorite("user-a", "new-song") }.exceptionOrNull()
        assertTrue(error is IOException)
        assertEquals(original, prefs.getString("favorites::user-a", null))
        // A broken account's data cannot silently erase another user's records.
        backend.addFavorite("user-b", "safe-song")
        assertEquals(listOf("safe-song"), backend.snapshot("user-b").favorites.map { it.songId })
    }

    @Test
    fun `corrupt playlists or history never become an empty saved library`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lanu_user_library", Context.MODE_PRIVATE)
        val invalid = "invalid JSON"
        assertTrue(prefs.edit().putString("playlists::user-a", invalid).commit())
        assertTrue(runCatching { backend.createPlaylist("user-a", "Yeni") }.exceptionOrNull() is IOException)
        assertEquals(invalid, prefs.getString("playlists::user-a", null))
        assertTrue(prefs.edit().remove("playlists::user-a").putString("history::user-a", invalid).commit())
        assertTrue(runCatching { backend.recordPlay("user-a", "song-1") }.exceptionOrNull() is IOException)
        assertEquals(invalid, prefs.getString("history::user-a", null))
    }
}
