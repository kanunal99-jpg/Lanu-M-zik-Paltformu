package com.example.playback

import com.example.model.MusicCategory
import com.example.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class YouTubeFallbackTest {
    @Test
    fun searchUriUsesOfficialYouTubeSearchAndEncodedTrackQuery() {
        val song = Song(
            id = "test",
            title = "Güzel Şarkı",
            artist = "LANU Sanatçı",
            artistId = "artist",
            album = "Albüm",
            durationMs = 180_000,
            category = MusicCategory.TURKCE_POP,
            coverUrl = "",
            audioUrl = "https://example.invalid/audio.mp3",
            releaseYear = 2026
        )

        assertEquals(
            "https://www.youtube.com/results?search_query=LANU%20Sanat%C3%A7%C4%B1%20G%C3%BCzel%20%C5%9Eark%C4%B1",
            YouTubeFallback.searchUri(song).toString()
        )
    }
}
