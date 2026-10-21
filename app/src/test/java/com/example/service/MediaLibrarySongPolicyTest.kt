package com.example.service

import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaLibrarySongPolicyTest {
    private fun song(
        id: String,
        source: SongSourceType,
        url: String,
        license: String = ""
    ) = Song(
        id = id, title = "Verified test music", artist = "Artist",
        artistId = "artist", album = "Album", durationMs = 120_000,
        category = MusicCategory.OTHER, coverUrl = "", audioUrl = url,
        releaseYear = 2026, sourceType = source, license = license
    )

    @Test fun localAndRemoteLibrarySectionsAreNeverIdentical() {
        val local = song("local", SongSourceType.LOCAL, "content://media/external/audio/1")
        val remote = song("remote", SongSourceType.VERIFIED_REMOTE, "https://example.com/track.mp3",
            "https://creativecommons.org/licenses/by/4.0/")
        val preview = song("preview", SongSourceType.VERIFIED_PREVIEW, "https://example.com/preview.mp3")
        val unknown = song("unknown", SongSourceType.UNKNOWN, "https://example.com/unknown.mp3")
        val all = listOf(local, remote, preview, unknown)
        assertEquals(listOf("local"), MediaLibrarySongPolicy.localSongs(all).map { it.id })
        assertEquals(listOf("remote", "preview"), MediaLibrarySongPolicy.catalogSongs(all).map { it.id })
        assertEquals(listOf("local", "remote", "preview"),
            MediaLibrarySongPolicy.searchableSongs(all).map { it.id })
    }

    @Test fun unknownSourceNeverPublishesAudioUriEvenWithFullLookingHttpsUrl() {
        assertNull(MediaLibrarySongPolicy.playableUri(
            song("metadata", SongSourceType.UNKNOWN, "https://example.com/full-track.mp3")))
        assertNull(MediaLibrarySongPolicy.playableUri(
            song("fake-local", SongSourceType.UNKNOWN, "content://media/external/audio/99")))
    }

    @Test fun unlicensedAndNonCommercialRemoteSongsAreNotPublishedAsPlayable() {
        listOf("", "all rights reserved", "CC BY-NC 4.0").forEach { license ->
            assertNull(MediaLibrarySongPolicy.playableUri(
                song("unlicensed", SongSourceType.VERIFIED_REMOTE, "https://example.com/music.mp3", license)))
        }
    }

    @Test fun licensedHttpsRemoteAndVerifiedPreviewsArePlayable() {
        val remote = song("permitted", SongSourceType.VERIFIED_REMOTE,
            "https://example.com/music.mp3", "CC BY 4.0")
        assertEquals(remote.audioUrl, MediaLibrarySongPolicy.playableUri(remote))
        val preview = song("preview", SongSourceType.VERIFIED_PREVIEW,
            "https://example.com/preview.mp3")
        assertEquals(preview.audioUrl, MediaLibrarySongPolicy.playableUri(preview))
    }

    @Test fun plaintextHttpAndForeignLocalSourceAreBlocked() {
        assertNull(MediaLibrarySongPolicy.playableUri(
            song("http", SongSourceType.VERIFIED_REMOTE, "http://example.com/music.mp3", "CC BY 4.0")))
        assertNull(MediaLibrarySongPolicy.playableUri(
            song("preview-http", SongSourceType.VERIFIED_PREVIEW, "http://example.com/preview.mp3")))
        assertNull(MediaLibrarySongPolicy.playableUri(
            song("remote-in-local", SongSourceType.LOCAL, "https://example.com/remote.mp3")))
    }

    @Test fun localContentAndFileUrisAreAllowedAndEmptyUriNeverExposed() {
        assertEquals("content://media/external/audio/1", MediaLibrarySongPolicy.playableUri(
            song("l1", SongSourceType.LOCAL, "content://media/external/audio/1")))
        assertEquals("file:///storage/emulated/0/Music/test.mp3", MediaLibrarySongPolicy.playableUri(
            song("l2", SongSourceType.LOCAL, "file:///storage/emulated/0/Music/test.mp3")))
        assertNull(MediaLibrarySongPolicy.playableUri(
            song("empty", SongSourceType.LOCAL, "")))
    }
}
