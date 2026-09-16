package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.auth.AuthResult
import com.example.auth.LocalAuthBackend
import com.example.data.catalog.AlternativeCatalogProvider
import com.example.data.catalog.CachedCatalogProvider
import com.example.data.catalog.CatalogProvider
import com.example.data.catalog.LocalCatalogProvider
import com.example.data.catalog.PrimaryCatalogProvider
import com.example.model.Artist
import com.example.model.AudioQuality
import com.example.model.FriendActivity
import com.example.model.MusicCategory
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val localMusicScanner = LocalMusicScanner(context)
    val downloadManager = com.example.player.DownloadManager(context)

    private val localAuthBackend = LocalAuthBackend(context)
    private val userLibraryService = UserLibraryService(
        authBackend = localAuthBackend,
        libraryBackend = LocalUserLibraryBackend(context)
    )

    private val primaryCatalogProvider: CatalogProvider = PrimaryCatalogProvider(BuildConfig.JAMENDO_CLIENT_ID)
    private val catalogProvider: CatalogProvider = AlternativeCatalogProvider(
        listOf(
            primaryCatalogProvider,
            CachedCatalogProvider(dao),
            LocalCatalogProvider(dao)
        )
    )
    private val catalogPreferences = context.getSharedPreferences("catalog_sync", Context.MODE_PRIVATE)
    private val catalogRefreshIntervalMs = 6L * 60L * 60L * 1000L

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

    val userLibrarySnapshot: StateFlow<UserLibrarySnapshot?> = userLibraryService.snapshot
    val userSession = userLibraryService.session

    init {
        RepositoryRegistry.repository = this
        scope.launch {
            runCatching {
                localAuthBackend.ensureLocalSession()
                userLibraryService.restoreSession()
                val cached = dao.getAllCachedSongs().first().map { it.toSong() }
                val local = dao.getAllLocalSongs().first().map { it.toSong() }
                _songs.value = (local + cached).distinctBy { it.id }
                migrateLegacyLibraryIfNeeded()
                syncOfflineStorage()
                refreshRemoteCatalogIfStale()
            }.onFailure { Log.e("MusicRepository", "Stored music initialization failed", it) }
        }
    }

    private suspend fun refreshRemoteCatalogIfStale() {
        if (BuildConfig.JAMENDO_CLIENT_ID.isBlank()) {
            Log.w("MusicRepository", "Verified remote catalog is disabled: JAMENDO_CLIENT_ID is not configured")
            return
        }
        val now = System.currentTimeMillis()
        val lastSync = catalogPreferences.getLong("last_sync_ms", 0L)
        if (now - lastSync < catalogRefreshIntervalMs) return

        val queries = listOf(
            "turkish pop", "turkish rap", "turkish rock", "anatolian rock", "turkish classical",
            "global pop", "hip hop", "rock", "electronic dance", "acoustic chill"
        )
        val fetched = LinkedHashMap<String, Song>()
        queries.forEach { query ->
            primaryCatalogProvider.searchSongs(query)
                .onSuccess { songs -> songs.forEach { fetched[it.id] = it } }
                .onFailure { error -> Log.w("MusicRepository", "Catalog query failed: $query", error) }
        }
        primaryCatalogProvider.getLatestReleases(null)
            .onSuccess { songs -> songs.forEach { fetched[it.id] = it } }
            .onFailure { error -> Log.w("MusicRepository", "Latest catalog refresh failed", error) }

        if (fetched.isNotEmpty()) {
            cacheSongs(fetched.values.toList())
            _songs.value = (_songs.value + fetched.values).distinctBy { it.id }
            catalogPreferences.edit().putLong("last_sync_ms", now).apply()
            Log.i("MusicRepository", "Verified catalog refresh added ${fetched.size} songs")
        }
    }

    suspend fun searchRemoteCatalog(query: String): Result<List<Song>> = catalogProvider.searchSongs(query)

    private suspend fun migrateLegacyLibraryIfNeeded() {
        val session = userLibraryService.session.value ?: return
        val snapshot = userLibraryService.snapshot.value ?: return
        if (snapshot.favorites.isEmpty()) {
            dao.getFavoriteSongs().first().forEach { favorite -> userLibraryService.addFavorite(favorite.songId) }
        }
        if (snapshot.playlists.isEmpty()) {
            dao.getAllPlaylists().first().forEach { playlist ->
                val created = userLibraryService.createPlaylist(playlist.name, playlist.description).getOrNull() ?: return@forEach
                dao.getSongsForPlaylist(playlist.id).first().forEach { relation -> userLibraryService.addSongToPlaylist(created.id, relation.songId) }
            }
        }
        if (snapshot.history.isEmpty()) {
            dao.getHistory().first().reversed().forEach { history -> userLibraryService.recordPlay(history.songId) }
        }
        Log.d("MusicRepository", "User library ready for ${session.userId}")
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

    val favoriteSongIds: Flow<Set<String>> = userLibrarySnapshot.map { snapshot -> snapshot?.favorites?.map { it.songId }?.toSet().orEmpty() }

    suspend fun scanAndSyncLocalMusic() {
        val localSongs = localMusicScanner.scanLocalMusic()
        if (localSongs.isEmpty()) return
        val byId = _songs.value.associateBy { it.id }.toMutableMap()
        localSongs.forEach { byId[it.id] = it }
        _songs.value = byId.values.toList()
        dao.insertLocalSongs(localSongs.map(::toVerifiedLocalEntity))
    }

    private fun toVerifiedLocalEntity(song: Song): LocalSongEntity = LocalSongEntity(
        id = song.id,
        title = song.title,
        artist = song.artist,
        artistId = song.artistId,
        album = song.album,
        durationMs = song.durationMs,
        categoryName = song.category.name,
        language = song.language,
        localAudioPath = song.audioUrl,
        localCoverPath = song.coverUrl,
        remoteAudioUrl = song.audioUrl,
        remoteCoverUrl = song.coverUrl,
        mimeType = "audio/*",
        bitrate = "",
        fileSizeBytes = 0L,
        audioQuality = "Yerel",
        isAvailableOffline = true,
        lyricsText = song.lyrics.joinToString("\n") { it.text },
        downloadedAt = System.currentTimeMillis(),
        lastPlayedAt = null,
        playCount = song.playCount
    )

    suspend fun toggleFavorite(songId: String) {
        val current = userLibrarySnapshot.value?.favorites?.any { it.songId == songId } == true
        val result = if (current) userLibraryService.removeFavorite(songId) else userLibraryService.addFavorite(songId)
        if (result is AuthResult.Failure) Log.e("MusicRepository", "User favorite operation failed: ${result.message}")
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

    val playlists: Flow<List<PlaylistEntity>> = userLibrarySnapshot.map { snapshot ->
        snapshot?.playlists.orEmpty().map { playlist ->
            PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                description = playlist.description,
                createdAt = playlist.createdAtMs,
                updatedAt = playlist.updatedAtMs,
                totalSongsCount = playlist.songIds.size
            )
        }
    }

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> = userLibrarySnapshot.combine(songs) { snapshot, allSongs ->
        val ids = snapshot?.playlists?.firstOrNull { it.id == playlistId }?.songIds.orEmpty()
        val songMap = allSongs.associateBy { it.id }
        ids.mapNotNull { songMap[it] }
    }

    suspend fun createPlaylist(name: String, description: String = ""): String {
        val result = userLibraryService.createPlaylist(name, description)
        return result.getOrElse { error("Çalma listesi oluşturulamadı: ${it.message}") }.id
    }

    suspend fun renamePlaylist(playlistId: String, name: String) {
        val result = userLibraryService.renamePlaylist(playlistId, name)
        if (result is AuthResult.Failure) error(result.message)
    }

    suspend fun reorderPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) {
        val result = userLibraryService.reorderPlaylist(playlistId, fromIndex, toIndex)
        if (result is AuthResult.Failure) error(result.message)
    }

    suspend fun deletePlaylist(playlistId: String) {
        val result = userLibraryService.deletePlaylist(playlistId)
        if (result is AuthResult.Failure) error(result.message)
        dao.deletePlaylistSongs(playlistId)
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        val result = userLibraryService.addSongToPlaylist(playlistId, songId)
        if (result is AuthResult.Failure) error(result.message)
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val result = userLibraryService.removeSongFromPlaylist(playlistId, songId)
        if (result is AuthResult.Failure) error(result.message)
    }

    suspend fun recordPlayedSong(songId: String) {
        val result = userLibraryService.recordPlay(songId)
        if (result is AuthResult.Failure) Log.e("MusicRepository", "History operation failed: ${result.message}")
    }

    suspend fun clearHistory(): AuthResult = userLibraryService.clearHistory()
    val history: Flow<List<HistoryRecord>> = userLibrarySnapshot.map { it?.history.orEmpty() }
    suspend fun signOut(): AuthResult = userLibraryService.signOut()

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

    fun pushSimulatedNewRelease() {}
    fun dismissNewReleaseNotification() { _newReleaseNotification.value = null }

    fun close() {
        if (RepositoryRegistry.repository === this) RepositoryRegistry.repository = null
        scope.cancel()
        downloadManager.close()
    }
}
