package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicGenreResolverTest {
    private fun track(artist: String, category: MusicCategory = MusicCategory.GLOBAL_POP): Song =
        Song(id = "test_1", title = "Deneme", artist = artist, artistId = "a",
            album = "Single", durationMs = 1000L, category = category,
            coverUrl = "", audioUrl = "", releaseYear = 2026,
            sourceType = SongSourceType.VERIFIED_PREVIEW)

    @Test fun cezaLegacyGlobalPopBecomesTurkishRap() {
        assertEquals(MusicCategory.TURKCE_RAP, MusicGenreResolver.resolve(track("Ceza")).category)
    }
    @Test fun unknownArtistIsNotMislabelledGlobalPop() {
        assertEquals(MusicCategory.OTHER, MusicGenreResolver.resolve(track("SevJazz Bulut")).category)
    }
    @Test fun providerTagsOverrideUnknownGenre() {
        assertEquals(MusicCategory.TURKCE_ROCK, MusicGenreResolver.fromMetadata("Bilinmeyen", "rock", listOf("turkish"), "tr"))
        assertEquals(MusicCategory.TURKCE_POP, MusicGenreResolver.fromMetadata("Yeni Şarkıcı", "pop", emptyList(), "tr"))
        assertEquals(MusicCategory.OTHER, MusicGenreResolver.fromMetadata("Bilinmeyen", "", emptyList(), "und"))
    }
    @Test fun explicitGenreIsNeverOverwritten() {
        assertEquals(MusicCategory.TURKCE_ROCK, MusicGenreResolver.resolve(track("Ceza", MusicCategory.TURKCE_ROCK)).category)
    }
}
