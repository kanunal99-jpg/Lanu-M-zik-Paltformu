package com.example.service

import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaLibraryArtworkPolicyTest {
    private fun song(
        cover: String,
        source: SongSourceType = SongSourceType.VERIFIED_PREVIEW
    ) = Song(
        id = "test", title = "Track", artist = "Artist", artistId = "artist",
        album = "Album", durationMs = 120_000, category = MusicCategory.OTHER,
        coverUrl = cover, audioUrl = "https://example.com/preview.mp3",
        releaseYear = 2026, sourceType = source
    )

    @Test fun providerHttpsArtworkAndMediaStoreLocalAlbumArtRemainAvailable() {
        val remote = "https://cdn.example.com/artwork%20test.jpg?width=480"
        assertEquals(remote, MediaLibraryArtworkPolicy.authorizedUri(song(remote)))
        val local = "content://media/external/audio/albumart/71"
        assertEquals(local, MediaLibraryArtworkPolicy.authorizedUri(song(local, SongSourceType.LOCAL)))
        assertEquals("https://example.org/cover.jpg",
            MediaLibraryArtworkPolicy.authorizedUri(song("https://example.org/cover.jpg", SongSourceType.LOCAL)))
    }

    @Test fun externalCatalogsCannotPassPrivateOrUnverifiedContentProvidersToMedia3() {
        val forbidden = listOf(
            "content://media/external/audio/albumart/71",
            "content://com.example.private.provider/secrets",
            "file:///data/user/0/com.example/databases/private.db",
            "android.resource://com.example/private",
            "http://example.com/art.jpg",
            "javascript:alert(1)",
            "/sdcard/Pictures/private.jpg",
            "https://user:secret@cdn.example.com/art.jpg",
            "https://",
            ""
        )
        for (cover in forbidden) {
            assertNull("Unexpected external artwork: $cover",
                MediaLibraryArtworkPolicy.authorizedUri(song(cover)))
        }
    }

    @Test fun localSongsCanOnlyExportTheirMediaStoreAlbumArtContentUri() {
        val rejected = listOf(
            "content://com.example.private.provider/auth",
            "content://media/external/audio/media/1",
            "content://media/external/audio/albumart/0",
            "content://media/external/audio/albumart/11/../../private",
            "content://media/external/audio/albumart/11?token=secret",
            "content://media/external/audio/albumart/11#fragment",
            "file:///storage/emulated/0/Pictures/cover.png"
        )
        for (cover in rejected) {
            assertNull("Unexpected local artwork: $cover",
                MediaLibraryArtworkPolicy.authorizedUri(song(cover, SongSourceType.LOCAL)))
        }
    }

    @Test fun malformedControlsAndOversizedInputsCannotEnterExternalMetadata() {
        for (cover in listOf(
            "https://example.com/a b.jpg",
            "https://example.com/a\nsecret",
            "https://example.com/a\u0000secret",
            "https://example.com/\tprivate",
            "https://example.com/" + "x".repeat(2050),
            "https://example.com/%broken",
            " "
        )) {
            assertNull("Rejected artwork: " + cover.take(80),
                MediaLibraryArtworkPolicy.authorizedUri(song(cover)))
        }
    }
}
