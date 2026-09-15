package com.example

import com.example.data.FavoriteRecord
import com.example.data.HistoryRecord
import com.example.data.PlaylistRecord
import com.example.data.recommendation.LocalRecommendationEngine
import com.example.model.MusicCategory
import com.example.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRecommendationEngineTest {
    private val songs = listOf(
        song("a", "Alpha", "artist-a", MusicCategory.TURKCE_POP),
        song("b", "Beta", "artist-a", MusicCategory.TURKCE_POP),
        song("c", "Gamma", "artist-c", MusicCategory.ROCK_CLASSICS)
    )

    @Test
    fun `ranking only returns supplied catalog`() {
        val result = LocalRecommendationEngine().rank(
            catalog = songs,
            history = listOf(HistoryRecord("a", 100L)),
            favorites = emptyList(),
            playlists = emptyList()
        )

        assertEquals(3, result.size)
        assertTrue(result.all { candidate -> songs.any { it.id == candidate.id } })
    }

    @Test
    fun `favorite and recent artist signals rank matching song first`() {
        val result = LocalRecommendationEngine().rank(
            catalog = songs,
            history = listOf(HistoryRecord("a", 100L)),
            favorites = listOf(FavoriteRecord("b", 90L)),
            playlists = emptyList()
        )

        assertEquals("b", result.first().id)
    }

    @Test
    fun `empty catalog and nonpositive limit are safe`() {
        val engine = LocalRecommendationEngine()
        assertTrue(engine.rank(emptyList(), emptyList(), emptyList(), emptyList()).isEmpty())
        assertTrue(engine.rank(songs, emptyList(), emptyList(), emptyList(), limit = 0).isEmpty())
    }

    private fun song(id: String, title: String, artistId: String, category: MusicCategory) = Song(
        id = id,
        title = title,
        artist = artistId,
        artistId = artistId,
        album = "Album $id",
        durationMs = 180_000L,
        category = category,
        coverUrl = "content://cover/$id",
        audioUrl = "content://audio/$id",
        releaseYear = 2026
    )
}
