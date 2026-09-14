package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.CachedSongEntity
import com.example.data.toCachedEntity
import com.example.data.toSong
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.ui.components.SongSearchBarWithListView
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SongSearchBarWithListViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testSongs = listOf(
        Song(
            id = "test_1",
            title = "Kuzu Kuzu",
            artist = "Tarkan",
            artistId = "tarkan",
            album = "Karma",
            durationMs = 234000L,
            category = MusicCategory.TURKCE_POP,
            coverUrl = "",
            audioUrl = "",
            releaseYear = 2001
        ),
        Song(
            id = "test_2",
            title = "Belalım",
            artist = "Sezen Aksu",
            artistId = "sezen",
            album = "Sezen Aksu Söylüyor",
            durationMs = 310000L,
            category = MusicCategory.TURKCE_POP,
            coverUrl = "",
            audioUrl = "",
            releaseYear = 1989
        ),
        Song(
            id = "test_3",
            title = "Blinding Lights",
            artist = "The Weeknd",
            artistId = "weeknd",
            album = "After Hours",
            durationMs = 200000L,
            category = MusicCategory.GLOBAL_POP,
            coverUrl = "",
            audioUrl = "",
            releaseYear = 2020
        )
    )

    @Test
    fun testSearchBarWithListView_rendersRoomBadgeAndItems() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SongSearchBarWithListView(
                    cachedSongs = testSongs,
                    onSongClick = { _, _ -> }
                )
            }
        }

        // Verify SearchBar and Room Cache Badge are displayed
        composeTestRule.onNodeWithTag("song_search_bar_with_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("song_search_bar_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("room_cache_badge").assertIsDisplayed()

        // Verify items in the list view below the search bar
        composeTestRule.onNodeWithTag("song_search_item_test_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("song_search_item_test_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("song_search_list_view").performScrollToNode(hasTestTag("song_search_item_test_3"))
        composeTestRule.onNodeWithTag("song_search_item_test_3").assertIsDisplayed()
    }

    @Test
    fun testSearchBarWithListView_filtersByTitle() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SongSearchBarWithListView(
                    cachedSongs = testSongs,
                    onSongClick = { _, _ -> }
                )
            }
        }

        // Type title "Kuzu"
        composeTestRule.onNodeWithTag("song_search_bar_input").performTextInput("Kuzu")

        // Only matching song should be displayed in the list view
        composeTestRule.onNodeWithTag("song_search_item_test_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Başlık Eşleşti").assertIsDisplayed()
    }

    @Test
    fun testSearchBarWithListView_filtersByArtist() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SongSearchBarWithListView(
                    cachedSongs = testSongs,
                    onSongClick = { _, _ -> }
                )
            }
        }

        // Type artist "Sezen"
        composeTestRule.onNodeWithTag("song_search_bar_input").performTextInput("Sezen")

        // Only matching song should be displayed in the list view
        composeTestRule.onNodeWithTag("song_search_item_test_2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sanatçı Eşleşti").assertIsDisplayed()
    }

    @Test
    fun testSearchBarWithListView_songClickCallback() {
        var clickedSong: Song? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                SongSearchBarWithListView(
                    cachedSongs = testSongs,
                    onSongClick = { song, _ -> clickedSong = song }
                )
            }
        }

        composeTestRule.onNodeWithTag("song_search_item_test_1").performClick()
        assertEquals("test_1", clickedSong?.id)
        assertEquals("Tarkan", clickedSong?.artist)
    }

    @Test
    fun testCachedSongEntity_toSongConversion() {
        val originalSong = testSongs.first()
        val cachedEntity = originalSong.toCachedEntity()
        assertEquals(originalSong.id, cachedEntity.id)
        assertEquals(originalSong.title, cachedEntity.title)
        assertEquals(originalSong.artist, cachedEntity.artist)

        val restoredSong = cachedEntity.toSong()
        assertEquals(originalSong.id, restoredSong.id)
        assertEquals(originalSong.title, restoredSong.title)
        assertEquals(originalSong.artist, restoredSong.artist)
        assertEquals(originalSong.category, restoredSong.category)
    }
}
