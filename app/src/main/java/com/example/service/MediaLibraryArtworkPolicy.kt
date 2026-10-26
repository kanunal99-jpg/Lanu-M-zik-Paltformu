package com.example.service

import com.example.model.Song
import com.example.model.SongSourceType
import java.net.URI

/**
 * Artwork supplied by remote catalogs or restored local metadata is untrusted.
 * Do not publish private file URIs, arbitrary content providers or non-HTTPS
 * links through Media3 to external Android Auto / media browser clients.
 * A missing/rejected image is a safe no-artwork fallback, not a playback error.
 */
internal object MediaLibraryArtworkPolicy {
    private const val MAX_ARTWORK_URI_LENGTH = 2048
    private val mediaStoreAlbumArt = Regex("""/external/audio/albumart/[1-9][0-9]*""")

    fun authorizedUri(song: Song): String? {
        val raw = song.coverUrl.trim()
        if (raw.isEmpty() || raw.length > MAX_ARTWORK_URI_LENGTH ||
            raw.any { it.isWhitespace() || Character.isISOControl(it) }
        ) return null
        val uri = try {
            URI(raw)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (uri.isOpaque) return null
        return when (uri.scheme) {
            "https" -> raw.takeIf { !uri.host.isNullOrBlank() && uri.rawUserInfo == null }
            "content" -> raw.takeIf {
                song.sourceType == SongSourceType.LOCAL &&
                    uri.host == "media" &&
                    uri.rawQuery == null && uri.rawFragment == null &&
                    mediaStoreAlbumArt.matches(uri.path.orEmpty())
            }
            else -> null
        }
    }
}
