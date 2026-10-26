package com.example.model

import com.example.player.PlaybackUriPolicy

/** Differentiate full permitted streams, official samples, and unavailable audio. */
enum class PlaybackKind { FULL, PREVIEW, UNAVAILABLE }

/**
 * An upstream source-type label alone is not proof that a URL is playable:
 * an unlicensed or cleartext "VERIFIED_REMOTE" must never appear as "Tam parça".
 * Keep the UI badge, full-only search filter and sorting in sync with the exact
 * provenance/license/URI gate used by both in-app and external Media3 playback.
 *
 * This is source authorization, not a filesystem-existence or network check.
 */
fun Song.playbackKind(): PlaybackKind {
    if (PlaybackUriPolicy.verifiedSource(this) == null) return PlaybackKind.UNAVAILABLE
    return when (sourceType) {
        SongSourceType.LOCAL, SongSourceType.VERIFIED_REMOTE -> PlaybackKind.FULL
        SongSourceType.VERIFIED_PREVIEW -> PlaybackKind.PREVIEW
        SongSourceType.UNKNOWN -> PlaybackKind.UNAVAILABLE
    }
}

/** Describes what the *source actually permits*, not the original recording length. */
fun Song.playbackBadge(): String = when (playbackKind()) {
    PlaybackKind.FULL -> "Tam parça"
    PlaybackKind.PREVIEW -> if (id.startsWith("deezer:")) "30 sn önizleme" else "Önizleme"
    PlaybackKind.UNAVAILABLE -> "Dinleme kaynağı yok"
}

/** Full streams take precedence in discovery, without assuming two similarly named recordings match. */
fun List<Song>.preferFullStreams(): List<Song> =
    withIndex().sortedWith(compareBy<IndexedValue<Song>> {
        when (it.value.playbackKind()) {
            PlaybackKind.FULL -> 0
            PlaybackKind.PREVIEW -> 1
            PlaybackKind.UNAVAILABLE -> 2
        }
    }.thenBy { it.index }).map { it.value }
