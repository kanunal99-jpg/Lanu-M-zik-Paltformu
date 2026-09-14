package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.ui.components.MusicSearchBar
import com.example.ui.components.SearchFilterScope
import com.example.ui.screens.SearchScreen
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
class MusicSearchBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleSongs = listOf(
        Song(
            id = "song_1",
            title = "Antidepresan",
            artist = "Mert Demir",
            artistId = "artist_mert",
            album = "Antidepresan Single",
            durationMs = 210000L,
            category = MusicCategory.TURKCE_POP,
            coverUrl = "",
            audioUrl = "",
            releaseYear = 2022
        ),
        Song(
            id = "song_2",
            title = "Ateşe Düştüm",
            artist = "Mert Demir",
            artistId = "artist_mert",
            album = "Ateşe Düştüm EP",
            durationMs = 200000L,
            category = MusicCategory.TURKCE_POP,
            coverUrl = "",
            audioUrl = "",
            releaseYear = 2023
        ),
        Song(
            id = "song_3",
            title = "Aşkın Olayım",
            artist = "Simge",
            artistId = "artist_simge",
            album = "Ben Bazen",
            durationMs = 245000L,
            category = MusicCategory.TURKCE_POP,
            coverUrl = "",
            audioUrl = "",
            releaseYear = 2018
        )
    )

    @Test
    fun testMusicSearchBar_rendersAndAcceptsInput() {
        var query by mutableStateOf("")
        var scope by mutableStateOf(SearchFilterScope.ALL)

        composeTestRule.setContent {
            MyApplicationTheme {
                MusicSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    selectedScope = scope,
                    onScopeChange = { scope = it }
                )
            }
        }

        // Verify search input field exists
        composeTestRule.onNodeWithTag("search_input_field").assertIsDisplayed()

        // Type query
        composeTestRule.onNodeWithTag("search_input_field").performTextInput("Simge")
        assertEquals("Simge", query)

        // Clear button should now be visible and clickable
        composeTestRule.onNodeWithTag("search_clear_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_clear_button").performClick()
        assertEquals("", query)
    }

    @Test
    fun testMusicSearchBar_scopeSelection() {
        var query by mutableStateOf("Ben Bazen")
        var scope by mutableStateOf(SearchFilterScope.ALL)

        composeTestRule.setContent {
            MyApplicationTheme {
                MusicSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    selectedScope = scope,
                    onScopeChange = { scope = it }
                )
            }
        }

        // Verify filter chips
        composeTestRule.onNodeWithTag("search_filter_chip_ALL").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_filter_chip_SONGS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_filter_chip_ARTISTS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_filter_chip_ALBUMS").assertIsDisplayed()

        // Click Albums chip
        composeTestRule.onNodeWithTag("search_filter_chip_ALBUMS").performClick()
        assertEquals(SearchFilterScope.ALBUMS, scope)
    }

    @Test
    fun testSearchScreen_findsSongsByAlbum() {
        var query by mutableStateOf("Ben Bazen")

        composeTestRule.setContent {
            MyApplicationTheme {
                SearchScreen(
                    searchQuery = query,
                    selectedCategory = null,
                    allSongs = sampleSongs,
                    onQueryChange = { query = it },
                    onSelectCategory = {},
                    onPlaySong = { _, _ -> }
                )
            }
        }

        // Ben Bazen is the album of song_3 (Aşkın Olayım)
        composeTestRule.onNodeWithTag("search_result_song_3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aşkın Olayım").assertIsDisplayed()
        composeTestRule.onNodeWithText("Albüm: Ben Bazen").assertIsDisplayed()
    }

    @Test
    fun testSearchScreen_findsSongsByArtist() {
        var query by mutableStateOf("Mert Demir")

        composeTestRule.setContent {
            MyApplicationTheme {
                SearchScreen(
                    searchQuery = query,
                    selectedCategory = null,
                    allSongs = sampleSongs,
                    onQueryChange = { query = it },
                    onSelectCategory = {},
                    onPlaySong = { _, _ -> }
                )
            }
        }

        // Both song_1 and song_2 are by Mert Demir
        composeTestRule.onNodeWithTag("search_result_song_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_result_song_2").assertIsDisplayed()
    }
}
