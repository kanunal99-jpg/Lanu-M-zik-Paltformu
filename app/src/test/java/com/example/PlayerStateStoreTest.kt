package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.PlayerStateStore
import com.example.service.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayerStateStoreTest {
    private lateinit var store: PlayerStateStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lanu_player_state", Context.MODE_PRIVATE).edit().clear().commit()
        store = PlayerStateStore(context)
    }

    @Test
    fun `snapshot survives a new store instance`() {
        store.save(
            PlayerStateStore.Snapshot(
                songId = "song-42",
                title = "Parça",
                artist = "Sanatçı",
                positionMs = 12345L,
                isPlaying = true,
                shuffle = true,
                repeatMode = RepeatMode.ALL
            )
        )

        val restored = PlayerStateStore(
            ApplicationProvider.getApplicationContext()
        ).read()

        assertEquals("song-42", restored.songId)
        assertEquals("Parça", restored.title)
        assertEquals("Sanatçı", restored.artist)
        assertEquals(12345L, restored.positionMs)
        assertEquals(true, restored.isPlaying)
        assertEquals(true, restored.shuffle)
        assertEquals(RepeatMode.ALL, restored.repeatMode)
    }
}
