package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.ui.components.PlaybackBottomAppBar
import com.example.ui.components.PlayerBottomAppBar
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
class PlayerBottomAppBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testSong = Song(
        id = "test_song_1",
        title = "Ateşe Düştüm",
        artist = "Mert Demir",
        artistId = "artist_mert",
        album = "Ateşe Düştüm EP",
        durationMs = 200000L,
        category = MusicCategory.TURKCE_POP,
        coverUrl = "",
        audioUrl = "",
        releaseYear = 2023
    )

    @Test
    fun testPlayerBottomAppBar_essentialPlaybackControls() {
        var isPlaying by mutableStateOf(false)
        var prevClicked = false
        var nextClicked = false
        var expandClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                PlayerBottomAppBar(
                    song = testSong,
                    isPlaying = isPlaying,
                    currentPositionMs = 50000L,
                    durationMs = testSong.durationMs,
                    isFavorite = false,
                    onToggleFavorite = {},
                    onTogglePlayPause = { isPlaying = !isPlaying },
                    onPrevious = { prevClicked = true },
                    onNext = { nextClicked = true },
                    onExpand = { expandClicked = true },
                    onSeekTo = {}
                )
            }
        }

        // Verify track information is displayed
        composeTestRule.onNodeWithText("Ateşe Düştüm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mert Demir").assertIsDisplayed()

        // Verify essential controls: Play/Pause, Previous, Next, Slider
        composeTestRule.onNodeWithTag("player_play_pause_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_prev_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_next_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_progress_slider").assertIsDisplayed()

        // Test play/pause toggle
        composeTestRule.onNodeWithContentDescription("Oynat").performClick()
        assertTrue(isPlaying)
        composeTestRule.onNodeWithContentDescription("Duraklat").assertIsDisplayed()

        // Test previous click
        composeTestRule.onNodeWithTag("player_prev_button").performClick()
        assertTrue(prevClicked)

        // Test next click
        composeTestRule.onNodeWithTag("player_next_button").performClick()
        assertTrue(nextClicked)
    }

    @Test
    fun testPlayerBottomAppBar_progressBarSliderAndTimestamps() {
        var seekedPositionMs by mutableLongStateOf(0L)

        composeTestRule.setContent {
            MyApplicationTheme {
                PlaybackBottomAppBar(
                    song = testSong,
                    isPlaying = true,
                    currentPositionMs = 60000L,
                    durationMs = 180000L,
                    onTogglePlayPause = {},
                    onPrevious = {},
                    onNext = {},
                    onSeekTo = { seekedPositionMs = it }
                )
            }
        }

        // Progress bar slider and timestamps
        composeTestRule.onNodeWithTag("player_progress_slider").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_current_position_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_duration_text").assertIsDisplayed()

        // Verify formatted time: 60000ms is 01:00, 180000ms is 03:00
        composeTestRule.onNodeWithText("01:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("03:00").assertIsDisplayed()
    }

    @Test
    fun testPlayerBottomAppBar_idleState() {
        var playStarted = false

        composeTestRule.setContent {
            MyApplicationTheme {
                PlayerBottomAppBar(
                    song = null,
                    isPlaying = false,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    isFavorite = false,
                    onToggleFavorite = {},
                    onTogglePlayPause = { playStarted = true },
                    onPrevious = {},
                    onNext = {},
                    onExpand = {},
                    onSeekTo = {}
                )
            }
        }

        // Verify idle state UI is shown
        composeTestRule.onNodeWithTag("player_idle_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_idle_play_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_idle_play_button").performClick()
        assertTrue(playStarted)
    }
}
