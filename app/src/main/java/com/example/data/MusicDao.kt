package com.example.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.model.MusicCategory
import com.example.model.Song
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val songId: String,
    val quality: String,
    val fileSize: String,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["name"]),
        Index(value = ["isOfflineAvailable"])
    ]
)
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isOfflineAvailable: Boolean = true,
    val coverUri: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val totalSongsCount: Int = 0,
    val totalDurationMs: Long = 0L
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["songId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSongEntity(
    val playlistId: String,
    val songId: String,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_songs")
data class CachedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val durationMs: Long,
    val categoryName: String,
    val language: String = "tr",
    val coverUrl: String,
    val audioUrl: String,
    val releaseYear: Int = 2024,
    val playCount: Long = 0L,
    val isNewRelease: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "local_songs",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["isAvailableOffline"])
    ]
)
data class LocalSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val trackNumber: Int = 1,
    val discNumber: Int = 1,
    val genre: String = "",
    val categoryName: String = "TURKCE_POP",
    val releaseYear: Int = 2024,
    val language: String = "tr",
    val localAudioPath: String = "",
    val localCoverPath: String = "",
    val remoteAudioUrl: String = "",
    val remoteCoverUrl: String = "",
    val mimeType: String = "audio/mpeg",
    val bitrate: String = "320 kbps",
    val fileSizeBytes: Long = 0L,
    val audioQuality: String = "Yüksek",
    val isAvailableOffline: Boolean = true,
    val lyricsText: String = "",
    val downloadedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val playCount: Long = 0L
)

@Entity(tableName = "release_sync_state")
data class ReleaseSyncStateEntity(
    @PrimaryKey val id: String = "last_sync",
    val lastSyncTimeMs: Long,
    val addedCount: Int,
    val status: String
)

data class PlaylistWithOfflineSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongEntity::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val offlineSongs: List<LocalSongEntity> = emptyList()
)

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongEntity::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<CachedSongEntity> = emptyList()
)

@Dao
interface MusicDao {
    @Query("SELECT * FROM local_songs ORDER BY title ASC")
    fun getAllLocalSongs(): Flow<List<LocalSongEntity>>

    @Query("SELECT * FROM local_songs WHERE isAvailableOffline = 1 ORDER BY title ASC")
    fun getOfflineAvailableSongs(): Flow<List<LocalSongEntity>>

    @Query("SELECT * FROM local_songs WHERE id = :songId")
    fun getLocalSongById(songId: String): Flow<LocalSongEntity?>

    @Query("SELECT * FROM local_songs WHERE id = :songId")
    suspend fun getLocalSongByIdSync(songId: String): LocalSongEntity?

    @Query("SELECT * FROM local_songs WHERE isAvailableOffline = 1 AND (LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(artist) LIKE '%' || LOWER(:query) || '%' OR LOWER(album) LIKE '%' || LOWER(:query) || '%') ORDER BY title ASC")
    fun searchOfflineSongs(query: String): Flow<List<LocalSongEntity>>

    @Query("SELECT * FROM local_songs WHERE LOWER(artist) = LOWER(:artist) AND isAvailableOffline = 1 ORDER BY title ASC")
    fun getOfflineSongsByArtist(artist: String): Flow<List<LocalSongEntity>>

    @Query("SELECT * FROM local_songs WHERE isAvailableOffline = 1 AND LOWER(album) = LOWER(:album) ORDER BY title ASC")
    fun getOfflineSongsByAlbum(album: String): Flow<List<LocalSongEntity>>

    @Query("SELECT COUNT(*) FROM local_songs WHERE isAvailableOffline = 1")
    fun getOfflineSongsCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM local_songs WHERE isAvailableOffline = 1")
    fun getTotalOfflineStorageBytes(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalSong(song: LocalSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalSongs(songs: List<LocalSongEntity>)

    @Update
    suspend fun updateLocalSong(song: LocalSongEntity)

    @Query("UPDATE local_songs SET localAudioPath = :path, fileSizeBytes = :fileSize, isAvailableOffline = 1 WHERE id = :songId")
    suspend fun updateLocalAudioPath(songId: String, path: String, fileSize: Long)

    @Query("UPDATE local_songs SET isAvailableOffline = :isAvailable WHERE id = :songId")
    suspend fun setSongOfflineAvailability(songId: String, isAvailable: Boolean)

    @Query("DELETE FROM local_songs WHERE id = :songId")
    suspend fun deleteLocalSongById(songId: String)

    @Query("DELETE FROM local_songs")
    suspend fun clearAllLocalSongs()

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isOfflineAvailable = 1 ORDER BY createdAt DESC")
    fun getOfflinePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistById(playlistId: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistByIdSync(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET isOfflineAvailable = :isOffline, updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun setPlaylistOfflineStatus(playlistId: String, isOffline: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE playlists SET name = :name, description = :description, updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun updatePlaylistDetails(playlistId: String, name: String, description: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: String)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getSongsForPlaylist(playlistId: String): Flow<List<PlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(entity: PlaylistSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongsToPlaylist(entities: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("UPDATE playlist_songs SET orderIndex = :newIndex WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongOrderInPlaylist(playlistId: String, songId: String, newIndex: Int)

    @Query("""
        SELECT ls.* FROM local_songs ls
        INNER JOIN playlist_songs ps ON ls.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.orderIndex ASC
    """)
    fun getLocalSongsForPlaylist(playlistId: String): Flow<List<LocalSongEntity>>

    @Query("""
        SELECT cs.* FROM cached_songs cs
        INNER JOIN playlist_songs ps ON cs.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.orderIndex ASC
    """)
    fun getCachedSongsForPlaylist(playlistId: String): Flow<List<CachedSongEntity>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithOfflineSongs(playlistId: String): Flow<PlaylistWithOfflineSongs?>

    @Transaction
    @Query("SELECT * FROM playlists WHERE isOfflineAvailable = 1 ORDER BY createdAt DESC")
    fun getOfflinePlaylistsWithSongs(): Flow<List<PlaylistWithOfflineSongs>>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>>

    @Query("SELECT * FROM favorite_songs ORDER BY addedAt DESC")
    fun getFavoriteSongs(): Flow<List<FavoriteSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteSongEntity)

    @Query("DELETE FROM favorite_songs WHERE songId = :songId")
    suspend fun removeFavorite(songId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE songId = :songId)")
    fun isFavorite(songId: String): Flow<Boolean>

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun getDownloadedSongs(): Flow<List<DownloadedSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDownload(entity: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE songId = :songId")
    suspend fun removeDownload(songId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE songId = :songId)")
    fun isDownloaded(songId: String): Flow<Boolean>

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 50")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToHistory(entity: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("SELECT * FROM cached_songs ORDER BY title ASC")
    fun getAllCachedSongs(): Flow<List<CachedSongEntity>>

    @Query("SELECT * FROM cached_songs WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(artist) LIKE '%' || LOWER(:query) || '%' ORDER BY title ASC")
    fun searchCachedSongsByTitleOrArtist(query: String): Flow<List<CachedSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedSongs(songs: List<CachedSongEntity>)

    @Query("DELETE FROM cached_songs")
    suspend fun clearCachedSongs()

    @Query("SELECT * FROM release_sync_state WHERE id = :id")
    suspend fun getReleaseSyncState(id: String = "last_sync"): ReleaseSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReleaseSyncState(state: ReleaseSyncStateEntity)
}

fun CachedSongEntity.toSong(): Song {
    val cat = try {
        MusicCategory.valueOf(categoryName)
    } catch (e: Exception) {
        MusicCategory.TURKCE_POP
    }
    return Song(
        id = id,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        durationMs = durationMs,
        category = cat,
        language = language,
        coverUrl = coverUrl,
        audioUrl = audioUrl,
        releaseYear = releaseYear,
        playCount = playCount,
        isNewRelease = isNewRelease
    )
}

fun Song.toCachedEntity(): CachedSongEntity {
    return CachedSongEntity(
        id = id,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        durationMs = durationMs,
        categoryName = category.name,
        language = language,
        coverUrl = coverUrl,
        audioUrl = audioUrl,
        releaseYear = releaseYear,
        playCount = playCount,
        isNewRelease = isNewRelease
    )
}

fun Song.toLocalEntity(
    localAudioPath: String = "",
    localCoverPath: String = "",
    isOffline: Boolean = true,
    audioQuality: String = "Yüksek",
    fileSizeBytes: Long = 0L
): LocalSongEntity {
    return LocalSongEntity(
        id = id,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        durationMs = durationMs,
        categoryName = category.name,
        language = language,
        localAudioPath = localAudioPath,
        localCoverPath = localCoverPath,
        remoteAudioUrl = audioUrl,
        remoteCoverUrl = coverUrl,
        mimeType = "audio/mpeg",
        bitrate = "320 kbps",
        fileSizeBytes = fileSizeBytes,
        audioQuality = audioQuality,
        isAvailableOffline = isOffline,
        releaseYear = releaseYear,
        playCount = playCount,
        lyricsText = lyrics.joinToString("\n") { it.text }
    )
}

fun LocalSongEntity.toSong(): Song {
    val cat = try {
        MusicCategory.valueOf(categoryName)
    } catch (e: Exception) {
        MusicCategory.TURKCE_POP
    }
    return Song(
        id = id,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        durationMs = durationMs,
        category = cat,
        language = language,
        coverUrl = if (localCoverPath.isNotEmpty()) localCoverPath else remoteCoverUrl,
        audioUrl = if (localAudioPath.isNotEmpty()) localAudioPath else remoteAudioUrl,
        releaseYear = releaseYear,
        playCount = playCount,
        isNewRelease = false
    )
}
