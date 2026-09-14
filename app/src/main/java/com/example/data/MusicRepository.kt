package com.example.data

import android.content.Context
import com.example.model.Artist
import com.example.model.AudioQuality
import com.example.model.FriendActivity
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.TimedLyric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

import java.io.File
import android.util.Log

class MusicRepository(context: Context) {
    private val context = context.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.musicDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val localMusicScanner = LocalMusicScanner(context)
    
    private val catalogProvider = com.example.data.catalog.PrimaryCatalogProvider()
    val downloadManager = com.example.player.DownloadManager(context)

    // Dynamic songs list (can receive new releases)
    private val _songs = MutableStateFlow<List<Song>>(catalogProvider.getStaticSongs())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    // Artists
    val artists: List<Artist> = catalogProvider.getStaticArtists()

    // Friends activity feed
    private val _friendActivities = MutableStateFlow<List<FriendActivity>>(catalogProvider.getInitialFriendActivities())
    val friendActivities: StateFlow<List<FriendActivity>> = _friendActivities.asStateFlow()

    // New release announcement banner state
    private val _newReleaseNotification = MutableStateFlow<Song?>(null)
    val newReleaseNotification: StateFlow<Song?> = _newReleaseNotification.asStateFlow()

    init {
        // Cache songs into Room Database
        scope.launch {
            dao.insertCachedSongs(catalogProvider.getStaticSongs().map { it.toCachedEntity() })
            dao.insertLocalSongs(catalogProvider.getStaticSongs().map { it.toLocalEntity(isOffline = false) })
            
            // Sync offline storage state (P0-07)
            try {
                syncOfflineStorage()
            } catch (e: Exception) {
                Log.e("MusicRepository", "Failed to sync offline storage on initialization", e)
            }
        }

        // Seed a default sample playlist if empty
        scope.launch {
            val currentPlaylists = dao.getAllPlaylists().first()
            if (currentPlaylists.isEmpty()) {
                val pId1 = "pl_turkce_nostalji"
                dao.insertPlaylist(
                    PlaylistEntity(
                        id = pId1,
                        name = "90'lar & 2000'ler Türkçe",
                        description = "En sevilen Türkçe pop ve rock klasikleri",
                        createdAt = System.currentTimeMillis()
                    )
                )
                dao.addSongToPlaylist(PlaylistSongEntity(pId1, "tarkan_1", 0))
                dao.addSongToPlaylist(PlaylistSongEntity(pId1, "sezen_1", 1))
                dao.addSongToPlaylist(PlaylistSongEntity(pId1, "duman_1", 2))

                val pId2 = "pl_gece_surusu"
                dao.insertPlaylist(
                    PlaylistEntity(
                        id = pId2,
                        name = "Gece Sürüşü & Synth",
                        description = "Gece yolculukları ve lofi melodiler",
                        createdAt = System.currentTimeMillis() + 1000
                    )
                )
                dao.addSongToPlaylist(PlaylistSongEntity(pId2, "weeknd_1", 0))
                dao.addSongToPlaylist(PlaylistSongEntity(pId2, "daft_1", 1))
            }
        }
    }

    suspend fun syncOfflineStorage() {
        val downloadedEntities = dao.getDownloadedSongs().first()
        val offlineSongsOnDiskDir = File(context.filesDir, "offline_audio")
        
        val physicalFiles = if (offlineSongsOnDiskDir.exists()) {
            offlineSongsOnDiskDir.listFiles()?.filter { it.isFile && it.name.endsWith(".mp3") } ?: emptyList()
        } else {
            emptyList()
        }
        val physicalFileNames = physicalFiles.map { it.name }.toSet()

        for (entity in downloadedEntities) {
            val fileName = "${entity.songId}.mp3"
            if (!physicalFileNames.contains(fileName)) {
                Log.w("MusicRepository", "Offline track missing on disk: ${entity.songId}. Syncing DB.")
                dao.removeDownload(entity.songId)
                dao.setSongOfflineAvailability(entity.songId, false)
            }
        }

        val dbDownloadedIds = downloadedEntities.map { it.songId }.toSet()
        for (file in physicalFiles) {
            val songId = file.nameWithoutExtension
            if (!dbDownloadedIds.contains(songId)) {
                Log.i("MusicRepository", "Cleaning orphaned offline file: ${file.name}")
                file.delete()
            }
        }
    }

    // Room Favorites
    val favoriteSongIds: Flow<Set<String>> = dao.getFavoriteSongs().map { list ->
        list.map { it.songId }.toSet()
    }

    suspend 
    fun scanAndSyncLocalMusic() {
        scope.launch {
            val localSongs = localMusicScanner.scanLocalMusic()
            if (localSongs.isNotEmpty()) {
                // Combine with existing songs
                val current = _songs.value.toMutableList()
                val existingIds = current.map { it.id }.toSet()
                val newSongs = localSongs.filter { !existingIds.contains(it.id) }
                
                if (newSongs.isNotEmpty()) {
                    current.addAll(newSongs)
                    _songs.value = current
                    
                    // Save to Room
                    dao.insertLocalSongs(newSongs.map { it.toLocalEntity(isOffline = true) })
                }
            }
        }
    }


    fun toggleFavorite(songId: String) {
        scope.launch {
            val favs = favoriteSongIds.first()
            if (favs.contains(songId)) {
                dao.removeFavorite(songId)
            } else {
                dao.addFavorite(FavoriteSongEntity(songId))
            }
        }
    }

    // Room Downloads / Offline
    val downloadedSongs: Flow<List<DownloadedSongEntity>> = dao.getDownloadedSongs()

    val downloadedSongIds: Flow<Set<String>> = dao.getDownloadedSongs().map { list ->
        list.map { it.songId }.toSet()
    }

    // Local Song Metadata (Offline Listening)
    val offlineSongs: Flow<List<LocalSongEntity>> = dao.getOfflineAvailableSongs()
    val offlinePlaylists: Flow<List<PlaylistEntity>> = dao.getOfflinePlaylists()
    val offlineSongsCount: Flow<Int> = dao.getOfflineSongsCount()
    val totalOfflineStorageBytes: Flow<Long> = dao.getTotalOfflineStorageBytes()

    suspend fun toggleDownload(song: Song, quality: AudioQuality) {
        val downloaded = downloadedSongIds.first()
        if (downloaded.contains(song.id)) {
            // Cancel and delete physically from disk
            downloadManager.deleteDownloadedFile(song.id)
            
            dao.removeDownload(song.id)
            dao.setSongOfflineAvailability(song.id, false)
        } else {
            // Trigger physical download task in the background
            downloadManager.startDownload(song, quality)

            val estimatedSize = when (quality) {
                AudioQuality.STANDARD -> "4.2 MB"
                AudioQuality.HIGH -> "10.8 MB"
                AudioQuality.HIFI -> "34.5 MB"
            }
            val estimatedSizeBytes = when (quality) {
                AudioQuality.STANDARD -> 4_404_019L
                AudioQuality.HIGH -> 11_324_620L
                AudioQuality.HIFI -> 36_175_872L
            }
            dao.addDownload(
                DownloadedSongEntity(
                    songId = song.id,
                    quality = quality.title,
                    fileSize = estimatedSize
                )
            )
            dao.insertLocalSong(
                song.toLocalEntity(
                    localAudioPath = "${context.filesDir.absolutePath}/offline_audio/${song.id}.mp3",
                    localCoverPath = "",
                    isOffline = true,
                    audioQuality = quality.title,
                    fileSizeBytes = estimatedSizeBytes
                )
            )
        }
    }

    fun getOfflineSongsForPlaylist(playlistId: String): Flow<List<LocalSongEntity>> {
        return dao.getLocalSongsForPlaylist(playlistId)
    }

    suspend fun setPlaylistOfflineStatus(playlistId: String, isOffline: Boolean) {
        dao.setPlaylistOfflineStatus(playlistId, isOffline)
    }

    // Playlists
    val playlists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return dao.getSongsForPlaylist(playlistId).combine(songs) { playlistSongs, allSongs ->
            val songMap = allSongs.associateBy { it.id }
            playlistSongs.mapNotNull { ps -> songMap[ps.songId] }
        }
    }

    suspend fun createPlaylist(name: String, description: String = ""): String {
        val id = "pl_${UUID.randomUUID().toString().take(8)}"
        dao.insertPlaylist(
            PlaylistEntity(
                id = id,
                name = name.ifBlank { "Yeni Çalma Listem" },
                description = description,
                createdAt = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun deletePlaylist(playlistId: String) {
        dao.deletePlaylist(playlistId)
        dao.deletePlaylistSongs(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        dao.addSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                orderIndex = System.currentTimeMillis().toInt()
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        dao.removeSongFromPlaylist(playlistId, songId)
    }

    // History
    suspend fun recordPlayedSong(songId: String) {
        dao.addToHistory(HistoryEntity(songId = songId))
    }

    // Search and Filters
    // Room-backed cached songs Flow
    val cachedSongs: Flow<List<Song>> = dao.getAllCachedSongs().map { list ->
        list.map { it.toSong() }
    }

    fun searchCachedSongs(query: String): Flow<List<Song>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            dao.getAllCachedSongs().map { list -> list.map { it.toSong() } }
        } else {
            dao.searchCachedSongsByTitleOrArtist(trimmed).map { list -> list.map { it.toSong() } }
        }
    }

    suspend fun cacheSongs(songsToCache: List<Song>) {
        dao.insertCachedSongs(songsToCache.map { it.toCachedEntity() })
    }

    fun searchSongs(
        query: String,
        categoryFilter: MusicCategory? = null,
        languageFilter: String? = null
    ): List<Song> {
        val q = query.trim().lowercase()
        return _songs.value.filter { song ->
            val matchesQuery = q.isEmpty() ||
                    song.title.lowercase().contains(q) ||
                    song.artist.lowercase().contains(q) ||
                    song.album.lowercase().contains(q) ||
                    song.lyrics.any { it.text.lowercase().contains(q) }

            val matchesCategory = categoryFilter == null || song.category == categoryFilter
            val matchesLanguage = languageFilter == null || song.language == languageFilter

            matchesQuery && matchesCategory && matchesLanguage
        }
    }

    // Friends Social Interactivity
    fun likeFriendActivity(activityId: String) {
        _friendActivities.value = _friendActivities.value.map { item ->
            if (item.id == activityId) {
                item.copy(recommendationLikes = item.recommendationLikes + 1)
            } else item
        }
    }

    fun shareRecommendationToFriends(friendName: String, song: Song, note: String) {
        val newActivity = FriendActivity(
            id = "act_${UUID.randomUUID().toString().take(6)}",
            friendName = friendName,
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
            song = song,
            statusText = "Şimdi paylaştı",
            isCurrentlyPlaying = true,
            mutualNote = note,
            recommendationLikes = 1
        )
        _friendActivities.value = listOf(newActivity) + _friendActivities.value
    }

    // Dynamic New Release simulation ("Yeni şarkı çıktığında uygulamama yüklensin")
    fun pushSimulatedNewRelease() {
        // Disabled in production to prevent fake catalog entries.
    }

    fun dismissNewReleaseNotification() {
        _newReleaseNotification.value = null
    }
}
