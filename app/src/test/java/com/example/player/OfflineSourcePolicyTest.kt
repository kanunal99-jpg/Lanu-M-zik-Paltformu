package com.example.player

import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSourcePolicyTest {
    private fun song(url: String, origin: SongSourceType): Song = Song(
        id = "trusted-id", title = "Title", artist = "Artist", artistId = "a",
        album = "Album", durationMs = 1000L, category = MusicCategory.LOCAL,
        coverUrl = "", audioUrl = url, releaseYear = 2026, sourceType = origin
    )

    @Test
    fun trustedLocalMediaStoreSourceIsAccepted() {
        assertTrue(OfflineSourcePolicy.isAuthorized(song(
            "content://media/external/audio/media/12", SongSourceType.LOCAL)))
        assertTrue(OfflineSourcePolicy.isAuthorized(song(
            "file:///storage/emulated/0/Music/recording.mp3", SongSourceType.LOCAL)))
    }

    @Test
    fun remoteRecordCannotSpoofAndroidPrivateFilesOrMediaStore() {
        listOf(SongSourceType.UNKNOWN, SongSourceType.VERIFIED_REMOTE,
            SongSourceType.VERIFIED_PREVIEW).forEach { source ->
            assertFalse(OfflineSourcePolicy.isAuthorized(song(
                "file:///data/user/0/com.aistudio.lanumusic.kpxzqa/shared_prefs/lanu_auth.xml",
                source)))
            assertFalse(OfflineSourcePolicy.isAuthorized(song(
                "content://media/external/audio/media/12", source)))
        }
    }

    @Test
    fun localMetadataCannotAuthorizeNetworkDownloads() {
        assertFalse(OfflineSourcePolicy.isAuthorized(song(
            "https://example.org/music.mp3", SongSourceType.LOCAL)))
    }

    @Test
    fun allowsAuthorizedLocalSchemes() {
        assertTrue(OfflineSourcePolicy.isAuthorized("content://media/external/audio/1"))
        assertTrue(OfflineSourcePolicy.isAuthorized("file:///storage/emulated/0/Music/my song.mp3"))
    }

    @Test
    fun privateAppAndPlatformFilesAreNeverOfflineCopySources() {
        val blocked = listOf(
            "file:///data/user/0/com.aistudio.lanumusic.kpxzqa/shared_prefs/lanu_auth.xml",
            "file:///data/data/com.example/databases/music.db",
            "file:///data/system/users/0/accounts.xml",
            "file:///proc/self/environ",
            "file:///etc/hosts",
            "file:///sdcard/../../../data/user/0/com.example/private.db",
            "file:///storage/emulated/0/../../../../data/user/0/com.example/secret.xml",
            "file://evil.example.com/storage/emulated/0/song.mp3",
            "file:///storage/emulated/0/song.mp3?token=secret",
            "file:///storage/emulated/0/song.mp3#fragment"
        )
        blocked.forEach { source ->
            assertFalse("Private file source accepted: $source", OfflineSourcePolicy.isAuthorized(source))
            assertFalse("Private LOCAL song accepted: $source",
                OfflineSourcePolicy.isAuthorized(song(source, SongSourceType.LOCAL)))
        }
    }

    @Test
    fun existingSharedAndEncodedFileSourcesStayUsable() {
        val allowed = listOf(
            "file:///storage/emulated/0/Music/recording.mp3",
            "file:///storage/emulated/0/Music/My Song.mp3",
            "file:///storage/emulated/0/Music/My%20Song.mp3",
            "file:///storage/ABCD-1234/Music/recording.mp3",
            "file:///sdcard/Music/recording.mp3",
            "content://media/external/audio/media/12",
            "content://com.android.providers.media.documents/document/audio%3A123"
        )
        allowed.forEach { source ->
            assertTrue("Normal device source rejected: $source",
                OfflineSourcePolicy.isAuthorized(source))
        }
    }

    @Test
    fun malformedLocalUrisAreSafelyRejected() {
        val invalid = listOf(
            "content:",
            "content://",
            "file:",
            "file://",
            "file:///storage/emulated/0/%broken",
            "file:///storage/emulated/0/line\nsecret",
            "file:///storage/emulated/0/stray\u0000byte",
            "content://media/external/audio/media/12#fragment",
            "file:///storage/emulated/0/" + "a".repeat(9000)
        )
        invalid.forEach { source ->
            assertFalse("Malformed local source accepted: ${source.take(60)}",
                OfflineSourcePolicy.isAuthorized(source))
        }
    }

    @Test
    fun rejectsRemoteAndUnsupportedSchemes() {
        assertFalse(OfflineSourcePolicy.isAuthorized("https://example.test/audio.mp3"))
        assertFalse(OfflineSourcePolicy.isAuthorized("http://example.test/audio.mp3"))
        assertFalse(OfflineSourcePolicy.isAuthorized("rtmp://example.test/audio"))
        assertFalse(OfflineSourcePolicy.isAuthorized("ftp://example.test/audio.mp3"))
        assertFalse(OfflineSourcePolicy.isAuthorized("javascript:alert(1)"))
        assertFalse(OfflineSourcePolicy.isAuthorized(""))
    }
}
