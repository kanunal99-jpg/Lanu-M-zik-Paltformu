package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SongPlaybackPolicyTest {
    private fun track(id: String = "deezer:1", source: SongSourceType): Song =
        Song(id = id, title = "Parça", artist = "Ceza", artistId = "a",
            album = "Albüm", durationMs = 190_000L, category = MusicCategory.TURKCE_RAP,
            coverUrl = "", audioUrl = "https://example.com/test.mp3", releaseYear = 2026,
            sourceType = source)

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

    @Test fun discoveryPrefersFullStreamsWithoutReplacingDifferentRecordings() {
        val preview = track("deezer:1", SongSourceType.VERIFIED_PREVIEW)
        val full = track("audius:2", SongSourceType.VERIFIED_REMOTE)
        val ordered = listOf(preview, full).preferFullStreams()
        assertEquals(listOf("audius:2", "deezer:1"), ordered.map { it.id })
    }
}
