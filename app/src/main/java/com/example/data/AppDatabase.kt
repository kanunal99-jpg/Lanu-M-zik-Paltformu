package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteSongEntity::class,
        DownloadedSongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        HistoryEntity::class,
        CachedSongEntity::class,
        LocalSongEntity::class,
        ReleaseSyncStateEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `release_sync_state` (`id` TEXT NOT NULL, `lastSyncTimeMs` INTEGER NOT NULL, `addedCount` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `cached_songs` ADD COLUMN `sourceTypeName` TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE `cached_songs` ADD COLUMN `license` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lanu_music_db"
                )
                 .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
