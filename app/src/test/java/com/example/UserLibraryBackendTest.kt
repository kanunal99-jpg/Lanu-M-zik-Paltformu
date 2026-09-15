package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocalUserLibraryBackend
import kotlinx.coroutines.runBlocking
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
    fun `playlist can be renamed without changing its song order`() = runBlocking {
        val playlist = backend.createPlaylist("user-a", "Gece")
        backend.addSongToPlaylist("user-a", playlist.id, "song-1")
        backend.addSongToPlaylist("user-a", playlist.id, "song-2")

        backend.renamePlaylist("user-a", playlist.id, "Gece Yolculuğu", "Uzun yol listesi", nowMs = 500L)

        val updated = backend.snapshot("user-a").playlists.single()
        assertEquals("Gece Yolculuğu", updated.name)
        assertEquals("Uzun yol listesi", updated.description)
        assertEquals(listOf("song-1", "song-2"), updated.songIds)
        assertEquals(500L, updated.updatedAtMs)
    }

    @Test
    fun `playlist reordering persists deterministically`() = runBlocking {
        val playlist = backend.createPlaylist("user-a", "Sıra")
        backend.addSongToPlaylist("user-a", playlist.id, "song-1")
        backend.addSongToPlaylist("user-a", playlist.id, "song-2")
        backend.addSongToPlaylist("user-a", playlist.id, "song-3")

        backend.moveSongInPlaylist("user-a", playlist.id, "song-3", targetIndex = 0, nowMs = 600L)
        assertEquals(listOf("song-3", "song-1", "song-2"), backend.snapshot("user-a").playlists.single().songIds)

        backend.moveSongInPlaylist("user-a", playlist.id, "song-1", targetIndex = 2, nowMs = 700L)
        val updated = backend.snapshot("user-a").playlists.single()
        assertEquals(listOf("song-3", "song-2", "song-1"), updated.songIds)
        assertEquals(700L, updated.updatedAtMs)
    }

    @Test
    fun `history keeps latest fifty unique song entries`() = runBlocking {
        repeat(55) { index -> backend.recordPlay("user-a", "song-$index", playedAtMs = index.toLong()) }
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
}
