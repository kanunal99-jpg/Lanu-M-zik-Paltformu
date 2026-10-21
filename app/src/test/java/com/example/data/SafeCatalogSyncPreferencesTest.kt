package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 29, 35])
class SafeCatalogSyncPreferencesTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun corruptedBackupTypesFallbackWithoutRewritingHealthyCatalogCursors() {
        val prefs = context.getSharedPreferences("catalog_type_regression", Context.MODE_PRIVATE)
        try {
            assertTrue(prefs.edit().clear()
                .putString("exhausted", "not-a-boolean")
                .putBoolean("page", true)
                .putInt("last_sync", 99)
                .putInt("intact_cursor", 42)
                .putString("intact_name", "cached-music")
                .commit())
            val state = SafeCatalogSyncPreferences(prefs)
            assertFalse(state.boolean("exhausted", false))
            assertEquals(0, state.int("page", 0))
            assertEquals(0L, state.long("last_sync", 0L))
            // Recovery is read-only and never clears cached tracks or valid metadata.
            assertTrue(prefs.contains("exhausted"))
            assertEquals(42, prefs.getInt("intact_cursor", -1))
            assertEquals("cached-music", prefs.getString("intact_name", null))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun healthyFieldsAndAbsentDefaultsStillRoundTrip() {
        val prefs = context.getSharedPreferences("catalog_type_regression", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().putBoolean("exhausted", true).putInt("page", 21)
                .putLong("last_sync", 123456L).commit()
            val state = SafeCatalogSyncPreferences(prefs)
            assertTrue(state.boolean("exhausted", false))
            assertEquals(21, state.int("page", 0))
            assertEquals(123456L, state.long("last_sync", 0L))
            assertEquals(7, state.int("missing", 7))
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
