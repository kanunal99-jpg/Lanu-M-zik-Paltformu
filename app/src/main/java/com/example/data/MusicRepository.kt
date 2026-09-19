package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.auth.AuthResult
import com.example.auth.LocalAuthBackend
import com.example.data.catalog.AlternativeCatalogProvider
import com.example.data.catalog.AudiusCatalogProvider
import com.example.data.catalog.CachedCatalogProvider
import com.example.data.catalog.CatalogProvider
import com.example.data.catalog.DeezerCatalogProvider
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    private val audiusCatalogProvider: CatalogProvider = AudiusCatalogProvider()
    private val deezerCatalogProvider: CatalogProvider = DeezerCatalogProvider()
    private val jamendoCatalogProvider: CatalogProvider = PrimaryCatalogProvider(
        BuildConfig.JAMENDO_CLIENT_ID,
        BuildConfig.JAMENDO_COMMERCIAL_LICENSE_CONFIRMED
    )
    private val catalogProvider: CatalogProvider = AlternativeCatalogProvider(
        listOf(
            audiusCatalogProvider,
            deezerCatalogProvider,
            jamendoCatalogProvider,
            CachedCatalogProvider(dao),
            LocalCatalogProvider(dao)
        )
    )
    private val catalogPreferences = context.getSharedPreferences("catalog_sync", Context.MODE_PRIVATE)
    private val catalogRefreshIntervalMs = 6L * 60L * 60L * 1000L
    private val catalogSyncVersion = 3
    private val catalogPageSize = 40
    private val backgroundPagesPerQuery = 1
    private val manualPagesPerQuery = 3
    private val maxConsecutiveEmptyPages = 2

    // Long-tail discovery plan for Turkish rap. The app progressively advances page cursors,
    // so repeated refresh/expansion operations can keep discovering deeper verified pages.
    private val turkishRapCatalogQueries = listOf(
        "turkish rap", "türkçe rap", "turkish hip hop", "türkçe hip hop",
        "turkish underground rap", "türkçe underground rap", "turkish trap", "türkçe trap",
        "turkish drill", "türkçe drill", "turkish old school rap", "türkçe old school rap"
    )

    private val generalCatalogQueries = listOf(
        "turkish pop", "turkish rock", "anatolian rock", "turkish indie", "turkish alternative",
        "pop", "rock", "hip hop", "rap", "electronic", "jazz", "classical", "latin", "arabic",
        "kpop", "jpop", "afrobeat", "reggae", "country"
    )

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
                    .filter { it.sourceType != com.example.model.SongSourceType.UNKNOWN }
                val local = dao.getAllLocalSongs().first().map { it.toSong() }
                _songs.value = (local + cached).distinctBy { it.id }
                migrateLegacyLibraryIfNeeded()
                syncOfflineStorage()
                refreshRemoteCatalogIfStale()
            }.onFailure { Log.e("MusicRepository", "Stored music initialization failed", it) }
        }
    }

    private fun catalogCursorKey(query: String): String =
        "catalog_next_page_${Math.abs(query.trim().lowercase().hashCode()).toString(16)}"

    private fun catalogExhaustedKey(query: String): String =
        "catalog_exhausted_${Math.abs(query.trim().lowercase().hashCode()).toString(16)}"

    private suspend fun collectVerifiedCatalogPages(
        query: String,
        maxPages: Int,
        fetched: MutableMap<String, Song>
    ) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return
        val exhaustedKey = catalogExhaustedKey(normalized)
        if (catalogPreferences.getBoolean(exhaustedKey, false)) return

        var page = catalogPreferences.getInt(catalogCursorKey(normalized), 0).coerceAtLeast(0)
        var consecutiveEmptyPages = 0
        var pagesRead = 0

        while (pagesRead < maxPages && !catalogPreferences.getBoolean(exhaustedKey, false)) {
            val result = runCatching {
                catalogProvider.searchSongsPage(normalized, page, catalogPageSize)
            }.getOrElse { failure ->
                Log.w("MusicRepository", "Catalog page failed: query=$normalized page=$page", failure)
                return
            }

            if (result.isFailure) {
                Log.w("MusicRepository", "Catalog page provider failure: query=$normalized page=$page", result.exceptionOrNull())
                return
            }

            val songs = result.getOrDefault(emptyList())
                .filter { it.sourceType != com.example.model.SongSourceType.UNKNOWN }

            synchronized(fetched) {
                songs.forEach { fetched[it.id] = it }
            }

            page += 1
            pagesRead += 1
            if (songs.isEmpty()) {
                consecutiveEmptyPages += 1
            } else {
                consecutiveEmptyPages = 0
            }

            catalogPreferences.edit().putInt(catalogCursorKey(normalized), page).apply()

            // A sparse provider can legally return an empty page before its true end.
            // Require two consecutive empty verified pages before marking this query exhausted.
            if (consecutiveEmptyPages >= maxConsecutiveEmptyPages) {
                catalogPreferences.edit().putBoolean(exhaustedKey, true).apply()
                break
            }
        }
    }

    private suspend fun refreshRemoteCatalogIfStale() {
        val now = System.currentTimeMillis()
        val lastSync = catalogPreferences.getLong("last_sync_ms", 0L)
        val storedVersion = catalogPreferences.getInt("catalog_sync_version", 0)
        if (storedVersion == catalogSyncVersion && now - lastSync < catalogRefreshIntervalMs) return

        // Automatic refresh advances a small batch of real provider pages. It deliberately
        // prioritizes Turkish rap long-tail discovery while keeping startup network usage bounded.
        val queries = (turkishRapCatalogQueries.take(6) + generalCatalogQueries.take(4)).distinct()
        val fetched = LinkedHashMap<String, Song>()
        coroutineScope {
            queries.map { query ->
                async { collectVerifiedCatalogPages(query, backgroundPagesPerQuery, fetched) }
            }.awaitAll()
        }
        catalogProvider.getLatestReleases(null)
            .onSuccess { songs ->
                songs.filter { it.sourceType != com.example.model.SongSourceType.UNKNOWN }
                    .forEach { fetched[it.id] = it }
            }
            .onFailure { error -> Log.w("MusicRepository", "Catalog latest refresh failed", error) }

        if (fetched.isNotEmpty()) {
            cacheSongs(fetched.values.toList())
            _songs.value = (_songs.value + fetched.values).distinctBy { it.id }
            catalogPreferences.edit()
                .putLong("last_sync_ms", now)
                .putInt("catalog_sync_version", catalogSyncVersion)
                .apply()
            Log.i("MusicRepository", "Progressive verified catalog sync added ${fetched.size} songs; total=${_songs.value.size}")
        } else {
            catalogPreferences.edit()
                .putLong("last_sync_ms", now)
                .putInt("catalog_sync_version", catalogSyncVersion)
                .apply()
            Log.w("MusicRepository", "Progressive catalog sync returned no new verified songs; existing catalog retained")
        }
    }

    /**
     * User-triggered expansion of the verified remote index.
     * It resumes per-query cursors instead of repeatedly downloading page zero, allowing the
     * indexed catalog to grow deeper across repeated expansions without inventing records.
     */
    suspend fun expandVerifiedCatalog(): Int = coroutineScope {
        val queries = (turkishRapCatalogQueries + generalCatalogQueries.take(6)).distinct()
        val fetched = LinkedHashMap<String, Song>()
        queries.map { query ->
            async { collectVerifiedCatalogPages(query, manualPagesPerQuery, fetched) }
        }.awaitAll()

        if (fetched.isNotEmpty()) {
            val merged = (_songs.value + fetched.values).distinctBy { it.id }
            cacheSongs(fetched.values.toList())
            _songs.value = merged
        }

        catalogPreferences.edit()
            .putLong("last_manual_expand_ms", System.currentTimeMillis())
            .putInt("catalog_sync_version", catalogSyncVersion)
            .apply()

        fetched.size
    }
    suspend fun searchRemoteCatalog(query: String): Result<List<Song>> = catalogProvider.searchSongs(query).map { remoteSongs ->
        remoteSongs.filter { it.sourceType != com.example.model.SongSourceType.UNKNOWN }
    }.onSuccess { remoteSongs ->
        if (remoteSongs.isNotEmpty()) {
            cacheSongs(remoteSongs)
            _songs.value = (_songs.value + remoteSongs).distinctBy { it.id }
        }
    }

    suspend fun searchRemoteArtists(query: String): Result<List<Artist>> = catalogProvider.searchArtists(query)

    /** Resolves an artist's tracks by the provider identity, never by artist-name substring. */
    suspend fun getArtistDiscography(artistId: String): Result<List<Song>> =
        catalogProvider.getArtistDiscography(artistId).map { songs ->
            songs.filter { it.artistId == artistId && it.sourceType != com.example.model.SongSourceType.UNKNOWN }
                .distinctBy { it.id }.also { verifiedSongs ->
                if (verifiedSongs.isNotEmpty()) cacheSongs(verifiedSongs)
                if (verifiedSongs.isNotEmpty()) {
                    _songs.value = (_songs.value + verifiedSongs).distinctBy { it.id }
                }
            }
        }

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
        id = song.id, title = song.title, artist = song.artist, artistId = song.artistId, album = song.album,
        durationMs = song.durationMs, categoryName = song.category.name, language = song.language,
        localAudioPath = song.audioUrl, localCoverPath = song.coverUrl, remoteAudioUrl = song.audioUrl,
        remoteCoverUrl = song.coverUrl, mimeType = "audio/*", bitrate = "", fileSizeBytes = 0L,
        audioQuality = "Yerel", isAvailableOffline = true, lyricsText = song.lyrics.joinToString("\n") { it.text },
        downloadedAt = System.currentTimeMillis(), lastPlayedAt = null, playCount = song.playCount
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
        snapshot?.playlists.orEmpty().map { playlist -> PlaylistEntity(id = playlist.id, name = playlist.name, description = playlist.description, createdAt = playlist.createdAtMs, updatedAt = playlist.updatedAtMs, totalSongsCount = playlist.songIds.size) }
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

    suspend fun renamePlaylist(playlistId: String, name: String) { val result = userLibraryService.renamePlaylist(playlistId, name); if (result is AuthResult.Failure) error(result.message) }
    suspend fun reorderPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) { val result = userLibraryService.reorderPlaylist(playlistId, fromIndex, toIndex); if (result is AuthResult.Failure) error(result.message) }
    suspend fun deletePlaylist(playlistId: String) { val result = userLibraryService.deletePlaylist(playlistId); if (result is AuthResult.Failure) error(result.message); dao.deletePlaylistSongs(playlistId); dao.deletePlaylist(playlistId) }
    suspend fun addSongToPlaylist(playlistId: String, songId: String) { val result = userLibraryService.addSongToPlaylist(playlistId, songId); if (result is AuthResult.Failure) error(result.message) }
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) { val result = userLibraryService.removeSongFromPlaylist(playlistId, songId); if (result is AuthResult.Failure) error(result.message) }
    suspend fun recordPlayedSong(songId: String) { val result = userLibraryService.recordPlay(songId); if (result is AuthResult.Failure) Log.e("MusicRepository", "History operation failed: ${result.message}") }
    suspend fun clearHistory(): AuthResult = userLibraryService.clearHistory()
    val history: Flow<List<HistoryRecord>> = userLibrarySnapshot.map { it?.history.orEmpty() }
    suspend fun signOut(): AuthResult = userLibraryService.signOut()
    val cachedSongs: Flow<List<Song>> = dao.getAllCachedSongs().map { items -> items.map { it.toSong() }.filter { it.sourceType != com.example.model.SongSourceType.UNKNOWN } }
    fun searchCachedSongs(query: String): Flow<List<Song>> { val q = normalizeSearch(query); return dao.getAllCachedSongs().map { items -> val songs = items.map { it.toSong() }.filter { it.sourceType != com.example.model.SongSourceType.UNKNOWN }; if (q.isEmpty()) songs else songs.filter { matchesQuery(it, q) } } }
    suspend fun cacheSongs(songsToCache: List<Song>) = dao.insertCachedSongs(songsToCache.map { it.toCachedEntity() })
    fun searchSongs(query: String, categoryFilter: MusicCategory? = null, languageFilter: String? = null): List<Song> { val q = normalizeSearch(query); return _songs.value.filter { song -> val matches = q.isEmpty() || matchesQuery(song, q); matches && (categoryFilter == null || song.category == categoryFilter) && (languageFilter == null || song.language == languageFilter) } }
    private fun matchesQuery(song: Song, normalizedQuery: String): Boolean = listOf(song.title, song.artist, song.album).any { normalizeSearch(it).contains(normalizedQuery) } || song.lyrics.any { normalizeSearch(it.text).contains(normalizedQuery) }
    private fun normalizeSearch(value: String): String = value.trim().lowercase().replace('ı','i').replace('İ','i').replace('ş','s').replace('Ş','s').replace('ğ','g').replace('Ğ','g').replace('ü','u').replace('Ü','u').replace('ö','o').replace('Ö','o').replace('ç','c').replace('Ç','c')
    fun likeFriendActivity(activityId: String) { _friendActivities.value = _friendActivities.value.map { if (it.id == activityId) it.copy(recommendationLikes = it.recommendationLikes + 1) else it } }
    fun shareRecommendationToFriends(friendName: String, song: Song, note: String) { val newActivity = FriendActivity("act_${UUID.randomUUID().toString().take(6)}", friendName, "", song, "Şimdi paylaştı", false, note, 1); _friendActivities.value = listOf(newActivity) + _friendActivities.value }
    fun pushSimulatedNewRelease() {}
    fun dismissNewReleaseNotification() { _newReleaseNotification.value = null }
    fun shutdown() { scope.cancel() }
}
