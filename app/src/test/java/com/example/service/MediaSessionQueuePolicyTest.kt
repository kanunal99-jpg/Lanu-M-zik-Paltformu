package com.example.service

import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSessionQueuePolicyTest {
    private fun song(id: String, uri: String, source: SongSourceType,
                     license: String = "") = Song(
        id = id, title = "Test", artist = "Artist", artistId = "a",
        album = "Record", durationMs = 1000L, category = MusicCategory.OTHER,
        coverUrl = "", audioUrl = uri, releaseYear = 2026,
        sourceType = source, license = license
    )

    private val local = song("local:one", "content://media/external/audio/media/1", SongSourceType.LOCAL)
    private val licensed = song("audius:two", "https://music.example/allowed.mp3",
        SongSourceType.VERIFIED_REMOTE, "CC BY 4.0")
    private val unknown = song("unverified", "file:///data/user/0/private.xml",
        SongSourceType.UNKNOWN)
    private val unlicensed = song("unlicensed", "https://music.example/full.mp3",
        SongSourceType.VERIFIED_REMOTE, "All Rights Reserved")

    @Test fun validCatalogIdsAreResolvedToTrustedSongs() {
        assertEquals(listOf(local, licensed),
            MediaSessionQueuePolicy.resolve(listOf("local:one", "audius:two"),
                listOf(local, licensed)))
    }

    @Test fun unknownOrMissingIdsRejectEntireQueue() {
        assertTrue(MediaSessionQueuePolicy.resolve(
            listOf("local:one", "file:///data/user/0/private.xml"),
            listOf(local, licensed)).isEmpty())
    }

    @Test fun unknownAndUnlicensedCatalogEntriesCannotBeQueued() {
        assertTrue(MediaSessionQueuePolicy.resolve(
            listOf("unverified"), listOf(local, unknown)).isEmpty())
        assertTrue(MediaSessionQueuePolicy.resolve(
            listOf("unlicensed"), listOf(unlicensed)).isEmpty())
    }

    @Test fun oversizedAndMalformedQueuesFailClosed() {
        assertTrue(MediaSessionQueuePolicy.resolve(emptyList(), listOf(local)).isEmpty())
        assertTrue(MediaSessionQueuePolicy.resolve(listOf("x".repeat(513)), listOf(local)).isEmpty())
        assertTrue(MediaSessionQueuePolicy.resolve(List(501) { "local:one" },
            listOf(local)).isEmpty())
        assertTrue(MediaSessionQueuePolicy.resolve(listOf(""), listOf(local)).isEmpty())
    }

    @Test fun repeatedLegitimateSongsRemainLegalForUserPlaylists() {
        assertEquals(listOf(local, local),
            MediaSessionQueuePolicy.resolve(listOf("local:one", "local:one"), listOf(local)))
    }
}
