package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SongPlaybackPolicyTest {
    private fun track(
        id: String = "deezer:1",
        source: SongSourceType,
        uri: String = if (source == SongSourceType.LOCAL) {
            "content://media/external/audio/4"
        } else {
            "https://example.com/test.mp3"
        },
        license: String = if (source == SongSourceType.VERIFIED_REMOTE) "CC BY 4.0" else ""
    ): Song = Song(
        id = id, title = "Parça", artist = "Ceza", artistId = "a",
        album = "Albüm", durationMs = 190_000L, category = MusicCategory.TURKCE_RAP,
        coverUrl = "", audioUrl = uri, releaseYear = 2026,
        sourceType = source, license = license
    )

    @Test fun deezerPreviewIsNotFullTrackEvenIfOriginalDurationIsLong() {
        val preview = track(source = SongSourceType.VERIFIED_PREVIEW)
        assertEquals(PlaybackKind.PREVIEW, preview.playbackKind())
        assertEquals("30 sn önizleme", preview.playbackBadge())
    }

    @Test fun verifiedStreamAndLocalMusicAreFullTracks() {
        assertEquals(PlaybackKind.FULL, track("audius:2", SongSourceType.VERIFIED_REMOTE).playbackKind())
        assertEquals(PlaybackKind.FULL, track("local:3", SongSourceType.LOCAL).playbackKind())
        assertEquals(PlaybackKind.UNAVAILABLE, track("metadata:4", SongSourceType.UNKNOWN).playbackKind())
    }

    @Test fun invalidSourcesNeverGetFullTrackOrPreviewBadges() {
        val invalid = listOf(
            track("unlicensed", SongSourceType.VERIFIED_REMOTE, license = ""),
            track("non-commercial", SongSourceType.VERIFIED_REMOTE, license = "CC BY-NC 4.0"),
            track("rights-reserved", SongSourceType.VERIFIED_REMOTE, license = "all rights reserved"),
            track("plain-http", SongSourceType.VERIFIED_REMOTE, uri = "http://example.com/full.mp3"),
            track("wrong-local", SongSourceType.LOCAL, uri = "https://example.com/local.mp3"),
            track("missing-local", SongSourceType.LOCAL, uri = ""),
            track("spoofed-preview", SongSourceType.VERIFIED_PREVIEW, uri = "file:///private/track.mp3"),
            track("cleartext-preview", SongSourceType.VERIFIED_PREVIEW, uri = "http://example.com/clip.mp3"),
            track("unknown", SongSourceType.UNKNOWN)
        )
        invalid.forEach { candidate ->
            assertEquals(candidate.id, PlaybackKind.UNAVAILABLE, candidate.playbackKind())
            assertEquals(candidate.id, "Dinleme kaynağı yok", candidate.playbackBadge())
        }
    }

    @Test fun fullOnlyDiscoveryFilterRejectsUnlicensedAndMalformedSources() {
        val rejected = track("unlicensed", SongSourceType.VERIFIED_REMOTE, license = "")
        val preview = track("deezer:1", SongSourceType.VERIFIED_PREVIEW)
        val validLocal = track("local:7", SongSourceType.LOCAL)
        val validRemote = track("audius:9", SongSourceType.VERIFIED_REMOTE)
        val ordered = listOf(rejected, preview, validLocal, validRemote).preferFullStreams()
        assertEquals(listOf("local:7", "audius:9", "deezer:1", "unlicensed"), ordered.map { it.id })
        assertEquals(listOf("local:7", "audius:9"),
            ordered.filter { it.playbackKind() == PlaybackKind.FULL }.map { it.id })
    }

    @Test fun discoveryPrefersFullStreamsWithoutReplacingDifferentRecordings() {
        val preview = track("deezer:1", SongSourceType.VERIFIED_PREVIEW)
        val full = track("audius:2", SongSourceType.VERIFIED_REMOTE)
        val ordered = listOf(preview, full).preferFullStreams()
        assertEquals(listOf("audius:2", "deezer:1"), ordered.map { it.id })
    }
}
