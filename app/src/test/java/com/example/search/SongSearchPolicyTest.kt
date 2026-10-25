package com.example.search

import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.TimedLyric
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongSearchPolicyTest {
    private fun track(
        title: String = "Şarkılar",
        artist: String = "Tarkan",
        album: String = "Dudu",
        lyrics: List<TimedLyric> = emptyList()
    ) = Song(
        id = "local:test",
        title = title,
        artist = artist,
        artistId = "artist:test",
        album = album,
        durationMs = 180_000L,
        category = MusicCategory.TURKCE_POP,
        coverUrl = "",
        audioUrl = "",
        releaseYear = 2000,
        lyrics = lyrics
    )

    @Test fun turkishDiacriticsMatchInAllMetadataFields() {
        val song = track(title = "Şarkı", artist = "Işık", album = "Görüş")
        assertTrue(SongSearchPolicy.matchesTitle(song, "sarki"))
        assertTrue(SongSearchPolicy.matchesArtist(song, "isik"))
        assertTrue(SongSearchPolicy.matchesAlbum(song, "gorus"))
    }

    @Test fun boundedTyposWorkForKnownArtistAndSong() {
        val song = track(title = "Kuzu Kuzu", artist = "Tarkan")
        assertTrue(SongSearchPolicy.matchesAll(song, "tarkn"))
        assertTrue(SongSearchPolicy.matchesSongs(song, "kuzz"))
        assertFalse(SongSearchPolicy.matchesArtist(song, "ceza"))
    }

    @Test fun scopeLimitsMatchingFields() {
        val song = track(title = "Kuzu Kuzu", artist = "Tarkan", album = "Dudu")
        assertTrue(SongSearchPolicy.matchesAll(song, "dudu"))
        assertFalse(SongSearchPolicy.matchesSongs(song, "dudu"))
        assertFalse(SongSearchPolicy.matchesAlbum(song, "tarkan"))
    }

    @Test fun lyricsHaveAccentInsensitiveExactMatchButNeverFuzzy() {
        val song = track(lyrics = listOf(TimedLyric(0L, "Güneş doğuyor")))
        assertTrue(SongSearchPolicy.matchesLyrics(song, "gunes"))
        assertFalse(SongSearchPolicy.matchesLyrics(song, "gunss"))
        assertTrue(SongSearchPolicy.matchesAll(song, "gunes"))
    }

    @Test fun queryBlankRemainsCompatibleWithBrowseAndCategoryFilters() {
        val song = track()
        assertTrue(SongSearchPolicy.matchesAll(song, " "))
        assertTrue(SongSearchPolicy.matchesSongs(song, " "))
        assertTrue(SongSearchPolicy.matchesArtist(song, " "))
        assertTrue(SongSearchPolicy.matchesAlbum(song, " "))
    }

    @Test fun extremelyLongUnrelatedInputDoesNotUseUnboundedFuzzyMatching() {
        val song = track()
        assertFalse(SongSearchPolicy.matchesAll(song, "tark" + "n".repeat(10_000)))
        assertFalse(TypoTolerantSearch.matches("tarkn", "A".repeat(100_000)))
    }
}
