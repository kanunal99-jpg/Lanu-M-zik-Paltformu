package com.example.data

import android.content.SharedPreferences

/**
 * Catalog sync metadata is disposable, never authority for users' music/Room.
 * OEM backup/restore can store a wrong type in an individual preference field.
 * Recover with a safe default without clearing any user library, catalog cache,
 * other valid cursor fields or APKs. Only ClassCastException is recoverable.
 */
internal class SafeCatalogSyncPreferences(private val preferences: SharedPreferences) {
    fun boolean(key: String, default: Boolean): Boolean =
        try { preferences.getBoolean(key, default) } catch (_: ClassCastException) { default }

    fun int(key: String, default: Int): Int =
        try { preferences.getInt(key, default) } catch (_: ClassCastException) { default }

    fun long(key: String, default: Long): Long =
        try { preferences.getLong(key, default) } catch (_: ClassCastException) { default }
}
