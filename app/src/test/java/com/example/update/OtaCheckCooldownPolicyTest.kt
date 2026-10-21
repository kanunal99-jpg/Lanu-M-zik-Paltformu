package com.example.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 29, 35])
class OtaCheckCooldownPolicyTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun corruptTimestampDoesNotCrashOrClearOtherUpdatePreferences() {
        val prefs = context.getSharedPreferences("ota-check-wrong-type-test", Context.MODE_PRIVATE)
        try {
            assertTrue(prefs.edit().clear()
                .putString("last_successful_check", "corrupt-oem-backup")
                .putString("unrelated_staged_apk_hint", "preserve-existing-hint")
                .commit())
            assertFalse(OtaCheckCooldownPolicy.canSkipCheck(prefs, 1_000_000L))
            assertTrue(prefs.contains("last_successful_check"))
            assertTrue(prefs.getString("unrelated_staged_apk_hint", null) == "preserve-existing-hint")
            assertTrue(prefs.edit().putBoolean("last_successful_check", true).commit())
            assertFalse(OtaCheckCooldownPolicy.canSkipCheck(prefs, 1_000_000L))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun validCooldownSkipsOnlyWithinSixHours() {
        val prefs = context.getSharedPreferences("ota-check-wrong-type-test", Context.MODE_PRIVATE)
        try {
            val thenMs = 500_000_000L
            assertTrue(prefs.edit().clear().putLong("last_successful_check", thenMs).commit())
            assertTrue(OtaCheckCooldownPolicy.canSkipCheck(prefs, thenMs))
            assertTrue(OtaCheckCooldownPolicy.canSkipCheck(prefs, thenMs + 6L * 60L * 60L * 1000L - 1L))
            assertFalse(OtaCheckCooldownPolicy.canSkipCheck(prefs, thenMs + 6L * 60L * 60L * 1000L))
            assertFalse(OtaCheckCooldownPolicy.canSkipCheck(prefs, thenMs - 1L))
            assertTrue(prefs.edit().putLong("last_successful_check", 0L).commit())
            assertFalse(OtaCheckCooldownPolicy.canSkipCheck(prefs, thenMs))
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
