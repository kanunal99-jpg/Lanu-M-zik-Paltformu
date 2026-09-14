package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.Artist
import com.example.model.AudioQuality
import com.example.model.FriendActivity
import com.example.model.MusicCategory
import com.example.model.Song
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
import java.io.File
import java.util.UUID

class MusicRepository(context: Context) {
    private val context = context.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.musicDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val localMusicScanner = LocalMusicScanner(context)
    val downloadManager = com.example.player.DownloadManager(context)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    val artists: List<Artist>
        get() = _songs.value.distinctBy { it.artistId }.map { song ->
            Artist(id = song.artistId, name = song.artist, genre = song.category.titleTr, bio = "Yerel veya doğrulanmış katalog verisi", imageUrl = song.coverUrl, monthlyListeners = "")
        }

    private val _friendActivities = MutableStateFlow<List<FriendActivity>>(emptyList())
    val friendActivities: StateFlow<List<FriendActivity>> = _friendActivities.asStateFlow()
    private val _newReleaseNotification = MutableStateFlow<Song?>(null)
    val newReleaseNotification: StateFlow<Song?> = _newReleaseNotification.asStateFlow()

    init {
        scope.launch {
            runCatching {
                val cached = dao.getAllCachedSongs().first().map { it.toSong() }
                val local = dao.getAllLocalSongs().first().map { it.toSong() }
                _songs.value = (local + cached).distinctBy { it.id }
                syncOfflineStorage()
            }.onFailure { Log.e("MusicRepository", "Stored music initialization failed", it) }
        }
    }

    suspend fun syncOfflineStorage() {
        val downloaded = dao.getDownloadedSongs().first()
        val dir = File(context.filesDir, "offline_audio")
        val files = if (dir.exists()) dir.listFiles()?.filter { it.isFile && it.name.endsWith(".bin") }.orEmpty() else emptyList()
        val names = files.map { it.name }.toSet()
        downloaded.forEach { entity ->
            val fileName = "${entity.songId}.bin"
            if (fileName !in names) {
                dao.removeDownload(entity.songId)
                dao.setSongOfflineAvailability(entity.songId, false)
            }
        }
        val dbIds = downloaded.map { it.songId }.toSet()
        files.filter { it.nameWithoutExtension !in dbIds }.forEach { it.delete() }
    }

    val favoriteSongIds: Flow<Set<String>> = dao.getFavoriteSongs().map { it.map { fav -> fav.songId }.toSet() }

    suspend fun scanAndSyncLocalMusic() {
        val localSongs = localMusicScanner.scanLocalMusic()
        if (localSongs.isEmpty()) return
        val byId = _songs.value.associateBy { it.id }.toMutableMap()
        localSongs.forEach { byId[it.id] = it }
        _songs.value = byId.values.toList()
        dao.insertLocalSongs(localSongs.map { it.toLocalEntity(isOffline = true) })
    }

    fun toggleFavorite(songId: String) {
        scope.launch {
            val favs = favoriteSongIds.first()
            if (songId in favs) dao.removeFavorite(songId) else dao.addFavorite(FavoriteSongEntity(songId))
        }
    }

    val downloadedSongs: Flow<List<DownloadedSongEntity>> = dao.getDownloadedSongs()
    val downloadedSongIds: Flow<Set<String>> = dao.getDownloadedSongs().map { it.map { item -> item.songId }.toSet() }
    val offlineSongs: Flow<List<LocalSongEntity>> = dao.getOfflineAvailableSongs()
    val offlinePlaylists: Flow<List<PlaylistEntity>> = dao.getOfflinePlaylists()
    val offlineSongsCount: Flow<Int> = dao.getOfflineSongsCount()
    val totalOfflineStorageBytes: Flow<Long> = dao.getTotalOfflineStorageBytes()

    suspend fun toggleDownload(song: Song, quality: AudioQuality) {
        if (downloadedSongIds.first().contains(song.id)) {
            downloadManager.deleteDownloadedFile(song.id)
            dao.removeDownload(song.id)
            dao.setSongOfflineAvailability(song.id, false)
            return
        }
        downloadManager.startDownload(song, quality) { state ->
            scope.launch {
                if (state.status == com.example.player.DownloadStatus.COMPLETED && state.localFilePath != null) {
                    val bytes = File(state.localFilePath).length()
                    val sizeLabel = String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576f)
                    dao.addDownload(DownloadedSongEntity(song.id, quality.title, sizeLabel))
                    dao.insertLocalSong(song.toLocalEntity(localAudioPath = state.localFilePath, localCoverPath = "", isOffline = true, audioQuality = quality.title, fileSizeBytes = bytes))
                } else {
                    dao.removeDownload(song.id)
                    dao.setSongOfflineAvailability(song.id, false)
                }
            }
        }
    }

    fun getOfflineSongsForPlaylist(playlistId: String): Flow<List<LocalSongEntity>> = dao.getLocalSongsForPlaylist(playlistId)
    suspend fun setPlaylistOfflineStatus(playlistId: String, isOffline: Boolean) = dao.setPlaylistOfflineStatus(playlistId, isOffline)
    val playlists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> = dao.getSongsForPlaylist(playlistId).combine(songs) { playlistSongs, allSongs ->
        val songMap = allSongs.associateBy { it.id }
        playlistSongs.mapNotNull { songMap[it.songId] }
    }

    suspend fun createPlaylist(name: String, description: String = ""): String {
        val id = "pl_${UUID.randomUUID().toString().take(8)}"
        dao.insertPlaylist(PlaylistEntity(id, name.ifBlank { "Yeni Çalma Listem" }, description, System.currentTimeMillis()))
        return id
    }

    suspend fun deletePlaylist(playlistId: String) {
        dao.deletePlaylist(playlistId)
        dao.deletePlaylistSongs(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        val currentCount = dao.getSongsForPlaylist(playlistId).first().size
        dao.addSongToPlaylist(PlaylistSongEntity(playlistId, songId, currentCount))
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) = dao.removeSongFromPlaylist(playlistId, songId)
    suspend fun recordPlayedSong(songId: String) = dao.addToHistory(HistoryEntity(songId = songId))

    val cachedSongs: Flow<List<Song>> = dao.getAllCachedSongs().map { it.map { item -> item.toSong() } }

    fun searchCachedSongs(query: String): Flow<List<Song>> {
        val q = normalizeSearch(query)
        return dao.getAllCachedSongs().map { items ->
            val songs = items.map { it.toSong() }
            if (q.isEmpty()) songs else songs.filter { matchesQuery(it, q) }
        }
    }

    suspend fun cacheSongs(songsToCache: List<Song>) = dao.insertCachedSongs(songsToCache.map { it.toCachedEntity() })

    fun searchSongs(query: String, categoryFilter: MusicCategory? = null, languageFilter: String? = null): List<Song> {
        val q = normalizeSearch(query)
        return _songs.value.filter { song ->
            val matches = q.isEmpty() || matchesQuery(song, q)
            matches && (categoryFilter == null || song.category == categoryFilter) && (languageFilter == null || song.language == languageFilter)
        }
    }

    private fun matchesQuery(song: Song, normalizedQuery: String): Boolean =
        listOf(song.title, song.artist, song.album).any { normalizeSearch(it).contains(normalizedQuery) } ||
            song.lyrics.any { normalizeSearch(it.text).contains(normalizedQuery) }

    private fun normalizeSearch(value: String): String = value.trim().lowercase()
        .replace('ı', 'i').replace('İ', 'i').replace('ş', 's').replace('Ş', 's')
        .replace('ğ', 'g').replace('Ğ', 'g').replace('ü', 'u').replace('Ü', 'u')
        .replace('ö', 'o').replace('Ö', 'o').replace('ç', 'c').replace('Ç', 'c')

    fun likeFriendActivity(activityId: String) {
        _friendActivities.value = _friendActivities.value.map { if (it.id == activityId) it.copy(recommendationLikes = it.recommendationLikes + 1) else it }
    }

    fun shareRecommendationToFriends(friendName: String, song: Song, note: String) {
        val newActivity = FriendActivity("act_${UUID.randomUUID().toString().take(6)}", friendName, "", song, "Şimdi paylaştı", false, note, 1)
        _friendActivities.value = listOf(newActivity) + _friendActivities.value
    }

    fun pushSimulatedNewRelease() { }
    fun dismissNewReleaseNotification() { _newReleaseNotification.value = null }
}
