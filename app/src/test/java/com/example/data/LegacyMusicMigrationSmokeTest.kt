package com.example.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Smoke-tests existing SQL migrations against old user rows without touching the
 * app's real database. This does not replace full exported-schema migration tests.
 */
@RunWith(RobolectricTestRunner::class)
class LegacyMusicMigrationSmokeTest {
    private fun withV3Rows(block: (SupportSQLiteDatabase) -> Unit) {
        val context: Context = RuntimeEnvironment.getApplication()
        val dbName = "migration-smoke-" + System.nanoTime() + ".db"
        context.deleteDatabase(dbName)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE favorite_songs (songId TEXT NOT NULL PRIMARY KEY, addedAt INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE playlists (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE playlist_songs (playlistId TEXT NOT NULL, songId TEXT NOT NULL, PRIMARY KEY(playlistId, songId))")
                    db.execSQL("CREATE TABLE cached_songs (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, audioUrl TEXT NOT NULL)")
                    db.execSQL("INSERT INTO favorite_songs (songId, addedAt) VALUES ('song-1', 12345)")
                    db.execSQL("INSERT INTO playlists (id, name) VALUES ('list-1', 'Kaydedilenler')")
                    db.execSQL("INSERT INTO playlist_songs (playlistId, songId) VALUES ('list-1', 'song-1')")
                    db.execSQL("INSERT INTO cached_songs (id, title, audioUrl) VALUES ('song-1', 'Şarkım', 'file:///song.mp3')")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    error("Robolectric fixture should be created at version 3 only")
                }
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        try {
            block(helper.writableDatabase)
        } finally {
            helper.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun oldFavoritesAndPlaylistMembershipRemainAfterV3ToV5() = withV3Rows { db ->
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)

        db.query("SELECT songId, addedAt FROM favorite_songs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("song-1", cursor.getString(0))
            assertEquals(12345L, cursor.getLong(1))
            assertEquals(1, cursor.count)
        }
        db.query("SELECT id, name FROM playlists").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("list-1", cursor.getString(0))
            assertEquals("Kaydedilenler", cursor.getString(1))
        }
        db.query("SELECT playlistId, songId FROM playlist_songs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("list-1", cursor.getString(0))
            assertEquals("song-1", cursor.getString(1))
        }
    }

    @Test
    fun newMetadataDefaultsAndSyncTableDoNotReplaceCachedSong() = withV3Rows { db ->
        AppDatabase.MIGRATION_3_4.migrate(db)
        db.execSQL("INSERT INTO release_sync_state (id, lastSyncTimeMs, addedCount, status) VALUES ('last_sync', 42, 5, 'ok')")
        AppDatabase.MIGRATION_4_5.migrate(db)

        db.query("SELECT id, title, audioUrl, sourceTypeName, license FROM cached_songs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("song-1", cursor.getString(0))
            assertEquals("Şarkım", cursor.getString(1))
            assertEquals("file:///song.mp3", cursor.getString(2))
            assertEquals("UNKNOWN", cursor.getString(3))
            assertEquals("", cursor.getString(4))
            assertEquals(1, cursor.count)
        }
        db.query("SELECT lastSyncTimeMs, addedCount, status FROM release_sync_state WHERE id = 'last_sync'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(42L, cursor.getLong(0))
            assertEquals(5, cursor.getInt(1))
            assertEquals("ok", cursor.getString(2))
        }
    }
}
