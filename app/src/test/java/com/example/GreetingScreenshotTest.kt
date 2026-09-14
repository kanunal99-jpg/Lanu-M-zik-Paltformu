package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.Song
import com.example.service.RepeatMode
import com.example.ui.components.PlayerBottomAppBar
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun player_bottom_app_bar_screenshot() {
    val sampleSong = Song(
      id = "song_test",
      title = "Aşkın Olayım",
      artist = "Simge",
      artistId = "artist_simge",
      album = "Ben Bazen",
      durationMs = 245000L,
      category = com.example.model.MusicCategory.TURKCE_POP,
      coverUrl = "",
      audioUrl = "",
      releaseYear = 2018
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        PlayerBottomAppBar(
          song = sampleSong,
          isPlaying = true,
          currentPositionMs = 60000L,
          durationMs = 245000L,
          isFavorite = true,
          isShuffle = true,
          repeatMode = RepeatMode.ALL,
          onToggleFavorite = {},
          onTogglePlayPause = {},
          onPrevious = {},
          onNext = {},
          onToggleShuffle = {},
          onToggleRepeat = {},
          onExpand = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/player_bottom_app_bar.png")
  }
}
