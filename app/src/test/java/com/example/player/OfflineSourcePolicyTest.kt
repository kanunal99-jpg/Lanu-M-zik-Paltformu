package com.example.player

import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSourcePolicyTest {
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

    private fun song(uri: String, source: SongSourceType) = Song(
        id = "safe-id",
        title = "Track", artist = "Artist", artistId = "artist",
        album = "Album", durationMs = 1000L,
        category = MusicCategory.LOCAL, coverUrl = "",
        audioUrl = uri, releaseYear = 2026, sourceType = source
    )

    @Test
    fun allowsLocalContentAndFileWhenSourceTypeIsExplicitlyLocal() {
        assertTrue(OfflineSourcePolicy.isAuthorized(
            song("content://media/external/audio/17", SongSourceType.LOCAL)))
        assertTrue(OfflineSourcePolicy.isAuthorized(
            song("file:///storage/emulated/0/Music/song.mp3", SongSourceType.LOCAL)))
    }

    @Test
    fun rejectsSpoofedContentAndPrivateFilesFromEveryNonLocalSource() {
        listOf(SongSourceType.UNKNOWN, SongSourceType.VERIFIED_REMOTE,
            SongSourceType.VERIFIED_PREVIEW).forEach { untrusted ->
            assertFalse(OfflineSourcePolicy.isAuthorized(
                song("file:///data/user/0/com.aistudio.lanumusic.kpxzqa/shared_prefs/lanu_auth.xml", untrusted)))
            assertFalse(OfflineSourcePolicy.isAuthorized(
                song("content://media/external/audio/17", untrusted)))
        }
    }

    @Test
    fun rejectsNetworkUrisEvenWhenMetadataClaimsLocalSource() {
        assertFalse(OfflineSourcePolicy.isAuthorized(
            song("https://example.com/music.mp3", SongSourceType.LOCAL)))
        assertFalse(OfflineSourcePolicy.isAuthorized(
            song("http://example.com/music.mp3", SongSourceType.LOCAL)))
    }
}
