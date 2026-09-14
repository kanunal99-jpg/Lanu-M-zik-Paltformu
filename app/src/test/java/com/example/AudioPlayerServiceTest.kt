package com.example

import android.content.Intent
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.service.AudioPlayerService
import com.example.service.AudioPlayerState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioPlayerServiceTest {

    private lateinit var controller: ServiceController<AudioPlayerService>
    private lateinit var service: AudioPlayerService

    private val testSongs = listOf(
        Song(
            id = "test_song_1",
            title = "Antidepresan",
            artist = "Mert Demir",
            album = "Single",
            durationMs = 210000L,
            category = MusicCategory.TURKCE_POP,
            audioUrl = "file:///android_asset/audio/test_1.mp3",
            coverUrl = "https://example.com/cover1.jpg",
            artistId = "artist_1",
            releaseYear = 2022
        ),
        Song(
            id = "test_song_2",
            title = "Ateşe Düştüm",
            artist = "Mert Demir",
            album = "Single",
            durationMs = 195000L,
            category = MusicCategory.TURKCE_POP,
            audioUrl = "file:///android_asset/audio/test_2.mp3",
            coverUrl = "https://example.com/cover2.jpg",
            artistId = "artist_1",
            releaseYear = 2023
        ),
        Song(
            id = "test_song_3",
            title = "Dönence",
            artist = "Barış Manço",
            album = "Sözüm Meclisten Dışarı",
            durationMs = 360000L,
            category = MusicCategory.ANADOLU_ROCK,
            audioUrl = "file:///android_asset/audio/test_3.mp3",
            coverUrl = "https://example.com/cover3.jpg",
            artistId = "artist_2",
            releaseYear = 1981
        )
    )

    @Before
    fun setUp() {
        controller = Robolectric.buildService(AudioPlayerService::class.java)
        service = controller.create().get()
    }

    @After
    fun tearDown() {
        controller.destroy()
    }

    @Test
    fun testServiceBindingAndInitialState() {
        val binder = service.onBind(Intent()) as AudioPlayerService.AudioPlayerBinder
        val boundService = binder.getService()
        assertNotNull(boundService)

        // Verify initial state
        assertFalse(service.isPlaying.value)
        assertEquals(AudioPlayerState.IDLE, service.playbackState.value)
        assertEquals(0, service.playlist.value.size)
        assertEquals(0, service.currentIndex.value)
    }

    @Test
    fun testSetPlaylistAndPlayback() {
        service.setPlaylist(testSongs, startIndex = 0, autoPlay = true)

        assertEquals(3, service.playlist.value.size)
        assertEquals(0, service.currentIndex.value)
        assertEquals("test_song_1", service.currentSong.value?.id)
        assertEquals("Antidepresan", service.currentSong.value?.title)
        assertTrue(service.isPlaying.value)
    }

    @Test
    fun testPlayPauseAndToggle() {
        service.setPlaylist(testSongs, startIndex = 0, autoPlay = true)
        assertTrue(service.isPlaying.value)

        // Pause
        service.pause()
        assertFalse(service.isPlaying.value)

        // Play
        service.play()
        assertTrue(service.isPlaying.value)

        // Toggle to pause
        service.togglePlayPause()
        assertFalse(service.isPlaying.value)

        // Toggle to play
        service.togglePlayPause()
        assertTrue(service.isPlaying.value)
    }

    @Test
    fun testSkipToNextAndPrevious() {
        service.setPlaylist(testSongs, startIndex = 0, autoPlay = true)
        assertEquals(0, service.currentIndex.value)
        assertEquals("test_song_1", service.currentSong.value?.id)

        // Skip to next song
        service.skipToNext()
        assertEquals(1, service.currentIndex.value)
        assertEquals("test_song_2", service.currentSong.value?.id)
        assertEquals("Ateşe Düştüm", service.currentSong.value?.title)
        assertTrue(service.isPlaying.value)

        // Skip to next again
        service.skipToNext()
        assertEquals(2, service.currentIndex.value)
        assertEquals("test_song_3", service.currentSong.value?.id)

        // Skip next wraps to start
        service.skipToNext()
        assertEquals(0, service.currentIndex.value)
        assertEquals("test_song_1", service.currentSong.value?.id)

        // Skip previous wraps to end
        service.skipToPrevious()
        assertEquals(2, service.currentIndex.value)
        assertEquals("test_song_3", service.currentSong.value?.id)

        // Skip previous to second song
        service.skipToPrevious()
        assertEquals(1, service.currentIndex.value)
        assertEquals("test_song_2", service.currentSong.value?.id)
    }

    @Test
    fun testPlaySpecificSongDirectly() {
        service.setPlaylist(testSongs, startIndex = 0, autoPlay = false)

        // Play song 3 directly
        service.playSong(testSongs[2], autoPlay = true)
        assertEquals(2, service.currentIndex.value)
        assertEquals("test_song_3", service.currentSong.value?.id)
        assertTrue(service.isPlaying.value)
    }

    @Test
    fun testSeekTo() {
        service.setPlaylist(testSongs, startIndex = 0, autoPlay = true)
        service.seekTo(45000L)
        assertEquals(45000L, service.currentPositionMs.value)
    }

    @Test
    fun testIntentActionsHandling() {
        service.setPlaylist(testSongs, startIndex = 0, autoPlay = false)
        assertFalse(service.isPlaying.value)

        // Intent Action: PLAY
        val playIntent = Intent().apply { action = AudioPlayerService.ACTION_PLAY }
        service.onStartCommand(playIntent, 0, 1)
        assertTrue(service.isPlaying.value)

        // Intent Action: PAUSE
        val pauseIntent = Intent().apply { action = AudioPlayerService.ACTION_PAUSE }
        service.onStartCommand(pauseIntent, 0, 2)
        assertFalse(service.isPlaying.value)

        // Intent Action: SKIP_NEXT
        val nextIntent = Intent().apply { action = AudioPlayerService.ACTION_SKIP_NEXT }
        service.onStartCommand(nextIntent, 0, 3)
        assertEquals(1, service.currentIndex.value)
        assertEquals("test_song_2", service.currentSong.value?.id)

        // Intent Action: SKIP_PREV
        val prevIntent = Intent().apply { action = AudioPlayerService.ACTION_SKIP_PREV }
        service.onStartCommand(prevIntent, 0, 4)
        assertEquals(0, service.currentIndex.value)
        assertEquals("test_song_1", service.currentSong.value?.id)

        // Intent Action: STOP
        val stopIntent = Intent().apply { action = AudioPlayerService.ACTION_STOP }
        service.onStartCommand(stopIntent, 0, 5)
        assertFalse(service.isPlaying.value)
    }
}
