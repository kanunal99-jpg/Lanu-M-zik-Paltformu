package com.example.data

import com.example.model.MusicCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomGenreFallbackTest {
    private fun cached(category: String) = CachedSongEntity(
        id = "cache-1", title = "Test", artist = "Artist", artistId = "artist-1",
        album = "Album", durationMs = 1000L, categoryName = category,
        coverUrl = "", audioUrl = ""
    )

    private fun local(category: String) = LocalSongEntity(
        id = "local-1", title = "Test", artist = "Artist", categoryName = category
    )

    @Test fun malformedCachedCategoryDoesNotPretendToBeTurkishPop() {
        for (category in listOf("", "MISSING_CATEGORY", " turkce_pop ", "GLOBAL POP")) {
            assertEquals(MusicCategory.OTHER, cached(category).toSong().category)
        }
    }

    @Test fun malformedLocalCategoryDoesNotPretendToBeTurkishPop() {
        for (category in listOf("", "MISSING_CATEGORY", "OTHER_INVALID")) {
            assertEquals(MusicCategory.OTHER, local(category).toSong().category)
        }
    }

    @Test fun actualVerifiedCategoriesRemainUnchanged() {
        for (category in MusicCategory.entries) {
            assertEquals(category, cached(category.name).toSong().category)
            assertEquals(category, local(category.name).toSong().category)
        }
    }
}
