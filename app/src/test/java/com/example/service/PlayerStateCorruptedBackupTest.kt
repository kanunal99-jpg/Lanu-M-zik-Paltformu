package com.example.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 29, 35])
class PlayerStateCorruptedBackupTest {
    @Test fun wrongPreferenceTypesDoNotCrashMediaServiceOrEraseOtherValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lanu_player_state", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear()
                .putInt("song_id", 123)
                .putString("title", "My song")
                .putString("position_ms", "not a number")
                .putString("is_playing", "not boolean")
                .putString("repeat_mode", "unknown future enum")
                .commit()
            val restored = PlayerStateStore(context).read()
            assertNull(restored.songId)
            assertEquals("My song", restored.title)
            assertEquals(0L, restored.positionMs)
            assertFalse(restored.isPlaying)
            assertEquals(RepeatMode.OFF, restored.repeatMode)
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun corruptedPositionIsClampedAndValidTrackIdentitySurvives() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lanu_player_state", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear()
                .putString("song_id", "local_42")
                .putLong("position_ms", Long.MAX_VALUE)
                .putBoolean("is_playing", true)
                .putBoolean("shuffle", true)
                .commit()
            val restored = PlayerStateStore(context).read()
            assertEquals("local_42", restored.songId)
            assertEquals(24L * 60L * 60L * 1000L, restored.positionMs)
            assertTrue(restored.isPlaying)
            assertTrue(restored.shuffle)
        } finally { prefs.edit().clear().commit() }
    }
}
