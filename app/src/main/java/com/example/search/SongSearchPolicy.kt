package com.example.search

import com.example.model.Song

/**
 * One offline, provider-agnostic search policy shared by UI, ViewModel and Room cache.
 * Only title/artist/album are fuzzy matched: arbitrary long lyric bodies use exact
 * normalized substrings to avoid quadratic UI work and misleading lyric hits.
 *
 * This is a *filter of existing verified records*, never a catalog generator.
 */
object SongSearchPolicy {
    fun matchesAll(song: Song, query: String): Boolean =
        matchesTitle(song, query) || matchesArtist(song, query) ||
            matchesAlbum(song, query) || matchesLyrics(song, query)

    fun matchesSongs(song: Song, query: String): Boolean =
        matchesTitle(song, query) || matchesArtist(song, query) || matchesLyrics(song, query)

    fun matchesTitle(song: Song, query: String): Boolean =
        TypoTolerantSearch.matches(query, song.title)

    fun matchesArtist(song: Song, query: String): Boolean =
        TypoTolerantSearch.matches(query, song.artist)

    fun matchesAlbum(song: Song, query: String): Boolean =
        TypoTolerantSearch.matches(query, song.album)

    fun matchesLyrics(song: Song, query: String): Boolean {
        val normalized = TypoTolerantSearch.normalize(query)
        return normalized.isEmpty() || song.lyrics.any {
            TypoTolerantSearch.normalize(it.text).contains(normalized)
        }
    }
}
