package com.example.service

import com.example.data.catalog.CatalogLicensePolicy
import com.example.model.Song
import com.example.model.SongSourceType

/**
 * Media3 library is another playback entry point. Apply the same provenance
 * boundary as the main player before publishing a playable URI to external
 * media browsers, Android Auto, or headset clients.
 */
internal object MediaLibrarySongPolicy {
    fun playableUri(song: Song): String? {
        val raw = song.audioUrl.trim()
        return when (song.sourceType) {
            SongSourceType.LOCAL ->
                raw.takeIf { it.startsWith("content://") || it.startsWith("file://") }
            SongSourceType.VERIFIED_REMOTE ->
                raw.takeIf {
                    it.startsWith("https://") &&
                        CatalogLicensePolicy.isPermittedRemoteLicense(song.license)
                }
            SongSourceType.VERIFIED_PREVIEW -> raw.takeIf { it.startsWith("https://") }
            SongSourceType.UNKNOWN -> null
        }
    }

    fun localSongs(songs: List<Song>): List<Song> =
        songs.filter { it.sourceType == SongSourceType.LOCAL && playableUri(it) != null }

    fun catalogSongs(songs: List<Song>): List<Song> =
        songs.filter { it.sourceType != SongSourceType.LOCAL && playableUri(it) != null }

    fun searchableSongs(songs: List<Song>): List<Song> =
        songs.filter { playableUri(it) != null }
}
