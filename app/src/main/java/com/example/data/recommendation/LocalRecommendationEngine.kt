package com.example.data.recommendation

import com.example.data.FavoriteRecord
import com.example.data.HistoryRecord
import com.example.data.PlaylistRecord
import com.example.model.Song

/**
 * Local-only ranking over an already verified catalog.
 * It never creates songs or metadata and has no network dependency.
 */
class LocalRecommendationEngine {
    fun rank(
        catalog: List<Song>,
        history: List<HistoryRecord>,
        favorites: List<FavoriteRecord>,
        playlists: List<PlaylistRecord>,
        limit: Int = 20
    ): List<Song> {
        if (catalog.isEmpty() || limit <= 0) return emptyList()

        val historyBySong = history.withIndex().associate { it.value.songId to it.index }
        val favoriteIds = favorites.mapTo(mutableSetOf()) { it.songId }
        val playlistIds = playlists.flatMapTo(mutableSetOf()) { it.songIds }
        val recentHistory = history.take(10).mapNotNull { record ->
            catalog.firstOrNull { it.id == record.songId }
        }
        val recentArtists = recentHistory.mapTo(mutableSetOf()) { it.artistId }
        val recentCategories = recentHistory.mapTo(mutableSetOf()) { it.category.id }

        return catalog
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<Song> { song ->
                    var score = 0
                    if (favoriteIds.contains(song.id)) score += 40
                    if (playlistIds.contains(song.id)) score += 25
                    if (recentArtists.contains(song.artistId)) score += 15
                    if (recentCategories.contains(song.category.id)) score += 10
                    historyBySong[song.id]?.let { index -> score += (10 - index).coerceAtLeast(0) }
                    score
                }.thenBy { it.title.lowercase() }
            )
            .take(limit)
    }
}
