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
        assertTrue(OfflineSourcePolicy.isAuthorized("file:///data/user/0/com.example/files/audio.bin"))
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
