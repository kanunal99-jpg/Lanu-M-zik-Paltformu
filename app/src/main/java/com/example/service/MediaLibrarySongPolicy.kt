package com.example.service

import com.example.model.Song
import com.example.model.SongSourceType
import com.example.player.PlaybackUriPolicy

/**
 * Media3 is a separate playback entry point, not a separate trust authority.
 * Use the SAME provenance and license gate as the in-app player to prevent
 * future divergence that could expose blocked URIs to external controllers.
 */
internal object MediaLibrarySongPolicy {
    fun playableUri(song: Song): String? = PlaybackUriPolicy.verifiedSource(song)

    fun localSongs(songs: List<Song>): List<Song> =
        songs.filter { it.sourceType == SongSourceType.LOCAL && playableUri(it) != null }

    fun catalogSongs(songs: List<Song>): List<Song> =
        songs.filter { it.sourceType != SongSourceType.LOCAL && playableUri(it) != null }

    fun searchableSongs(songs: List<Song>): List<Song> =
        songs.filter { playableUri(it) != null }
}
