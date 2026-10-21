package com.example.update

import android.content.SharedPreferences

/** Automatic update-check cooldown is optional metadata, never trusted APK state. */
internal object OtaCheckCooldownPolicy {
    private const val CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L

    fun canSkipCheck(preferences: SharedPreferences, nowMs: Long): Boolean {
        val last = try {
            preferences.getLong("last_successful_check", 0L)
        } catch (_: ClassCastException) {
            // OEM restore can assign a String/Boolean to the long field.
            // Do not crash on app launch or clear the staged verified APK.
            return false
        }
        return last > 0L && nowMs >= last && nowMs - last < CHECK_INTERVAL_MS
    }
}
