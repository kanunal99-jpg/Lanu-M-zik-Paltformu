package com.example.data

import kotlinx.coroutines.flow.first

/**
 * A read-only snapshot of retired Room user data. Complete ALL reads before
 * committing the one-time local-library import; never delete legacy rows.
 * Playlist membership order and original IDs are retained.
 */
internal suspend fun loadLegacyRoomLibrary(dao: MusicDao, userId: String): UserLibrarySnapshot {
    val favorites = dao.getFavoriteSongs().first().map {
        FavoriteRecord(it.songId, it.addedAt)
    }
    val playlists = dao.getAllPlaylists().first().map { playlist ->
        PlaylistRecord(
            id = playlist.id,
            name = playlist.name,
            description = playlist.description,
            createdAtMs = playlist.createdAt,
            updatedAtMs = playlist.updatedAt,
            songIds = dao.getSongsForPlaylist(playlist.id).first().map { it.songId }.distinct()
        )
    }
    val history = dao.getHistory().first().distinctBy { it.songId }.map {
        HistoryRecord(it.songId, it.playedAt)
    }
    return UserLibrarySnapshot(userId, favorites, playlists, history)
}
