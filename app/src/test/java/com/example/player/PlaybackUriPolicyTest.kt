package com.example.player

import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackUriPolicyTest {
    private val base = File("/data/user/0/com.example/files")

    private fun song(id: String = "audius:1", uri: String, type: SongSourceType,
                     license: String = ""): Song = Song(
        id = id,
        title = "Test", artist = "Independent", artistId = "artist",
        album = "Album", durationMs = 120000L, category = MusicCategory.OTHER,
        coverUrl = "", audioUrl = uri, releaseYear = 2026, sourceType = type,
        license = license
    )

    @Test fun maliciousAndOversizedIdentifiersCannotReachFilesOutsideOfflineDirectory() {
        val malicious = listOf(
            "../important-user-record", "../../cache/secret", "a/b",
            "a\\b", "..", ".hidden..", "", "a".repeat(161),
            "☠️", "album\nfile"
        )
        malicious.forEach { id ->
            assertNull("Must reject untrusted id: $id",
                PlaybackUriPolicy.managedOfflineFile(base, id))
        }
    }

    @Test fun legitimateHistoricalIdsKeepTheirExactOfflineFileNames() {
        for (id in listOf("audius:12345", "deezer:123", "local_track-1", "music.2026")) {
            val file = PlaybackUriPolicy.managedOfflineFile(base, id)
            assertNotNull(file)
            assertEquals(File(base, "offline_audio/$id.bin").path, file?.path)
            assertTrue(file!!.path.startsWith(File(base, "offline_audio").path))
        }
    }

    @Test fun metadataAndSpoofedLocalUriNeverBecomePlayable() {
        assertNull(PlaybackUriPolicy.verifiedSource(song(
            uri = "https://example.com/song.mp3", type = SongSourceType.UNKNOWN)))
        assertNull(PlaybackUriPolicy.verifiedSource(song(
            uri = "file:///data/local/tmp/other.mp3", type = SongSourceType.VERIFIED_REMOTE,
            license = "CC BY 4.0")))
        assertNull(PlaybackUriPolicy.verifiedSource(song(
            uri = "content://media/audio/1", type = SongSourceType.UNKNOWN)))
    }

    @Test fun remoteHttpIsNeverPlayableEvenWhenLicensed() {
        assertNull(PlaybackUriPolicy.verifiedSource(song(
            uri = "http://example.com/song.mp3", type = SongSourceType.VERIFIED_REMOTE,
            license = "CC BY 4.0")))
        assertNull(PlaybackUriPolicy.verifiedSource(song(
            uri = "http://example.com/preview.mp3", type = SongSourceType.VERIFIED_PREVIEW)))
    }

    @Test fun remoteWithNoLicenseOrNonCommercialLicenseIsDenied() {
        listOf("", "all rights reserved", "CC BY-NC 4.0").forEach { license ->
            assertNull(PlaybackUriPolicy.verifiedSource(song(
                uri = "https://example.com/song.mp3",
                type = SongSourceType.VERIFIED_REMOTE, license = license)))
        }
    }

    @Test fun malformedOrCredentialedRemoteUrlsNeverReachMedia3OrPlayer() {
        val rejected = listOf(
            "https://",
            "https:///track.mp3",
            "https://?track=1",
            "https://user:secret@cdn.example.com/full.mp3",
            "https://cdn.example.com/a b.mp3",
            "https://cdn.example.com/line\nfeed.mp3",
            "https://cdn.example.com/a\u0000.mp3",
            "https://cdn.example.com/%broken",
            "https://cdn.example.com/" + "a".repeat(9000),
            "https://%31invalid.example.com/track.mp3"
        )
        for (url in rejected) {
            assertNull("remote accepted malformed URL: ${url.take(80)}",
                PlaybackUriPolicy.verifiedSource(song(
                    uri = url, type = SongSourceType.VERIFIED_REMOTE, license = "CC BY 4.0")))
            assertNull("preview accepted malformed URL: ${url.take(80)}",
                PlaybackUriPolicy.verifiedSource(song(
                    uri = url, type = SongSourceType.VERIFIED_PREVIEW)))
        }
    }

    @Test fun officialSignedHttpsUrlsRemainUnchanged() {
        val urls = listOf(
            "https://cdn.example.com/music.mp3?Expires=1860000000&Signature=abc%2Fdef%3D",
            "https://api.audius.co/v1/tracks/123/stream",
            "https://cdn.example.com:443/sample.mp3?token=abc-123"
        )
        for (url in urls) {
            assertEquals(url, PlaybackUriPolicy.verifiedSource(song(
                uri = url, type = SongSourceType.VERIFIED_REMOTE, license = "CC BY 4.0")))
            assertEquals(url, PlaybackUriPolicy.verifiedSource(song(
                uri = url, type = SongSourceType.VERIFIED_PREVIEW)))
        }
    }

    @Test fun licensedHttpsPreviewAndLocalContentStillWork() {
        val full = "https://example.com/full.mp3"
        assertEquals(full, PlaybackUriPolicy.verifiedSource(song(
            uri = full, type = SongSourceType.VERIFIED_REMOTE,
            license = "CC BY 4.0")))
        val preview = "https://example.com/preview.mp3"
        assertEquals(preview, PlaybackUriPolicy.verifiedSource(song(
            uri = preview, type = SongSourceType.VERIFIED_PREVIEW)))
        val local = "content://media/external/audio/17"
        assertEquals(local, PlaybackUriPolicy.verifiedSource(song(
            uri = local, type = SongSourceType.LOCAL)))
        assertEquals("file:///storage/emulated/0/Music/test.mp3",
            PlaybackUriPolicy.verifiedSource(song(
                uri = "file:///storage/emulated/0/Music/test.mp3",
                type = SongSourceType.LOCAL)))
        assertNull(PlaybackUriPolicy.verifiedSource(song(
            uri = "https://example.com/remote.mp3", type = SongSourceType.LOCAL)))
    }
}
