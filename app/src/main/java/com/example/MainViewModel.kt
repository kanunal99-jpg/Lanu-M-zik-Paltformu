package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.auth.AuthResult
import com.example.auth.AuthSession
import com.example.data.DownloadedSongEntity
import com.example.data.HistoryRecord
import com.example.data.MusicRepository
import com.example.data.SearchCatalogSource
import com.example.data.PlaylistEntity
import com.example.model.Artist
import com.example.model.AudioQuality
import com.example.model.EqualizerPreset
import com.example.model.EqualizerSettings
import com.example.model.EqualizerState
import com.example.model.FriendActivity
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.search.SongSearchPolicy
import com.example.model.toSettings
import com.example.service.AudioPlayerController
import com.example.service.RepeatMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val titleTr: String) { HOME("Ana Sayfa"), SEARCH("Keşfet"), LIBRARY("Kitaplığım"), FRIENDS("Arkadaşlar"), EQUALIZER("Ekolayzır"), ACCOUNT("Hesabım"), UPDATE("Güncelleme Kontrol") }

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    val repository: MusicRepository,
    val playerController: AudioPlayerController
) : AndroidViewModel(application) {
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()
    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()
    val isPlayerExpanded: StateFlow<Boolean> get() = _isNowPlayingExpanded
    fun setPlayerExpanded(expanded: Boolean) { _isNowPlayingExpanded.value = expanded }
    private val _nowPlayingTab = MutableStateFlow(0)
    val nowPlayingTab: StateFlow<Int> = _nowPlayingTab.asStateFlow()
    fun setNowPlayingTab(tab: Int) { _nowPlayingTab.value = tab }
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()
    fun setShowSettingsDialog(show: Boolean) { _showSettingsDialog.value = show }
    private val _recommendationAlert = MutableStateFlow<String?>(null)
    val recommendationAlert: StateFlow<String?> = _recommendationAlert.asStateFlow()
    fun clearRecommendationAlert() { _recommendationAlert.value = null }
    private val _showLyricsInNowPlaying = MutableStateFlow(false)
    val showLyricsInNowPlaying: StateFlow<Boolean> = _showLyricsInNowPlaying.asStateFlow()
    fun toggleLyricsInNowPlaying() { _showLyricsInNowPlaying.value = !_showLyricsInNowPlaying.value }
    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()
    private val _remoteArtistResults = MutableStateFlow<List<Artist>>(emptyList())
    val remoteArtistResults: StateFlow<List<Artist>> = _remoteArtistResults.asStateFlow()
    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()
    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()
    private val _songToAddToPlaylist = MutableStateFlow<Song?>(null)
    val songToAddToPlaylist: StateFlow<Song?> = _songToAddToPlaylist.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _isCatalogExpanding = MutableStateFlow(false)
    val isCatalogExpanding: StateFlow<Boolean> = _isCatalogExpanding.asStateFlow()
    @Deprecated("Artificial network loading was removed; use real operation state instead.") fun simulateNetworkLoading(delayMs: Long = 0) = Unit
    private val _audioQuality = MutableStateFlow(AudioQuality.HIGH)
    val audioQuality: StateFlow<AudioQuality> = _audioQuality.asStateFlow()
    private val _appLanguage = MutableStateFlow("tr")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _searchIsLoading = MutableStateFlow(false)
    val searchIsLoading: StateFlow<Boolean> = _searchIsLoading.asStateFlow()
    private val _searchHasMore = MutableStateFlow(false)
    val searchHasMore: StateFlow<Boolean> = _searchHasMore.asStateFlow()
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()
    private val searchSources = SearchCatalogSource.entries.toList()
    private val sourcePages = mutableMapOf<SearchCatalogSource, Int>()
    private val sourceEmptyPages = mutableMapOf<SearchCatalogSource, Int>()
    private val exhaustedSources = mutableSetOf<SearchCatalogSource>()
    private var artistDiscoveryJob: Job? = null

    private fun recordSearchPage(source: SearchCatalogSource, page: Int, result: Result<List<Song>>?) {
        if (result == null || result.isFailure) {
            _searchError.value = "Bazı müzik kaynaklarına ulaşılamadı; bulunan şarkılar korunuyor."
            return
        }
        sourcePages[source] = page + 1
        val emptyCount = if (result.getOrThrow().isEmpty()) (sourceEmptyPages[source] ?: 0) + 1 else 0
        sourceEmptyPages[source] = emptyCount
        if (emptyCount >= 2) exhaustedSources.add(source)
        _searchHasMore.value = searchSources.any { it !in exhaustedSources }
    }

    private val _selectedCategoryFilter = MutableStateFlow<MusicCategory?>(null)
    val selectedCategoryFilter: StateFlow<MusicCategory?> = _selectedCategoryFilter.asStateFlow()
    val selectedCategory: StateFlow<MusicCategory?> get() = selectedCategoryFilter
    val allSongs: StateFlow<List<Song>> = repository.songs
    val artists: List<Artist> get() = repository.artists
    val friendActivities: StateFlow<List<FriendActivity>> = repository.friendActivities
    val newReleaseNotification: StateFlow<Song?> = repository.newReleaseNotification
    val session: StateFlow<AuthSession?> = repository.userSession
    val history: StateFlow<List<HistoryRecord>> = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteSongIds: StateFlow<Set<String>> = repository.favoriteSongIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val downloadedSongs: StateFlow<List<DownloadedSongEntity>> = repository.downloadedSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val downloadedSongIds: StateFlow<Set<String>> = repository.downloadedSongIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val offlineSongIds: StateFlow<Set<String>> get() = downloadedSongIds
    val favoriteSongs: StateFlow<List<Song>> = combine(allSongs, favoriteSongIds) { songs, favIds -> songs.filter { favIds.contains(it.id) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val offlineSongs: StateFlow<List<Song>> = combine(allSongs, downloadedSongIds) { songs, downIds -> songs.filter { downIds.contains(it.id) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val searchResults: StateFlow<List<Song>> = combine(allSongs, _searchQuery, _selectedCategoryFilter) { songs, query, cat ->
        val normalizedQuery = normalizeSearch(query)
        songs.filter { song ->
            val matchesQuery = SongSearchPolicy.matchesAll(song, normalizedQuery)
            val matchesCat = cat == null || song.category == cat
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private fun normalizeSearch(value: String): String = value.trim().lowercase().replace('ı', 'i').replace('İ', 'i').replace('ş', 's').replace('Ş', 's').replace('ğ', 'g').replace('Ğ', 'g').replace('ü', 'u').replace('Ü', 'u').replace('ö', 'o').replace('Ö', 'o').replace('ç', 'c').replace('Ç', 'c')
    val cachedSongs: StateFlow<List<Song>> = repository.cachedSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    @OptIn(ExperimentalCoroutinesApi::class) val roomCachedSearchResults: StateFlow<List<Song>> = _searchQuery.flatMapLatest { query -> repository.searchCachedSongs(query) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun searchSongsFromRoom(query: String): Flow<List<Song>> = repository.searchCachedSongs(query)
    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentSong: StateFlow<Song?> = playerController.currentSong
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val currentPositionMs: StateFlow<Long> = playerController.currentPositionMs
    val durationMs: StateFlow<Long> = playerController.durationMs
    val isShuffle: StateFlow<Boolean> = playerController.isShuffle
    val repeatMode: StateFlow<RepeatMode> = playerController.repeatMode
    val isRepeat: StateFlow<Boolean> = repeatMode.map { it != RepeatMode.OFF }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val activeLyricIndex: StateFlow<Int> = combine(currentSong, currentPositionMs) { song, posMs -> val lyrics = song?.lyrics ?: emptyList(); val idx = lyrics.indexOfLast { it.timeMs <= posMs }; if (idx >= 0) idx else 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val equalizerState: StateFlow<EqualizerState> = playerController.equalizerState
    val equalizerSettings: StateFlow<EqualizerSettings> = equalizerState.map { it.toSettings() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerSettings())

    init {
        viewModelScope.launch { allSongs.collect { songs -> playerController.restoreStateFromCatalog(songs) } }
        viewModelScope.launch {
            _searchQuery.map(::normalizeSearch).distinctUntilChanged().debounce(350).collectLatest { query ->
                artistDiscoveryJob?.cancel()
                _remoteArtistResults.value = emptyList()
                _searchError.value = null
                _searchHasMore.value = false
                sourcePages.clear()
                sourceEmptyPages.clear()
                exhaustedSources.clear()
                if (query.length < 2) {
                    _searchIsLoading.value = false
                    return@collectLatest
                }

                // Artist discovery and potentially huge discographies are not allowed
                // to block initial song results. Cancellation prevents stale queries.
                artistDiscoveryJob = viewModelScope.launch {
                    val artistsResult = withTimeoutOrNull(8_000L) {
                        repository.searchRemoteArtists(query)
                    }
                    if (query != normalizeSearch(_searchQuery.value)) return@launch
                    val artists = artistsResult?.getOrNull().orEmpty().distinctBy { it.id }
                    _remoteArtistResults.value = artists
                    // The detailed discography is optional enrichment, not a search gate.
                    artists.filter { normalizeSearch(it.name) == query }.take(2).forEach { artist ->
                        withTimeoutOrNull(12_000L) {
                            repository.getArtistDiscography(artist.id)
                        }?.onFailure { error ->
                            Log.w("MainViewModel", "Artist discography unavailable: ${artist.id}", error)
                        }
                    }
                }

                _searchIsLoading.value = true
                try {
                    // Concurrent, independently bounded source fetches publish songs
                    // progressively. A slow or unavailable provider no longer hides
                    // results already returned by faster sources.
                    coroutineScope {
                        searchSources.forEach { source ->
                            launch {
                                val page = sourcePages[source] ?: 0
                                val result = withTimeoutOrNull(10_000L) {
                                    runCatching {
                                        repository.searchRemoteCatalogPageFromSource(query, page, source)
                                    }.getOrElse { Result.failure(it) }
                                }
                                if (query != normalizeSearch(_searchQuery.value)) return@launch
                                recordSearchPage(source, page, result)
                            }
                        }
                    }
                } finally {
                    if (query == normalizeSearch(_searchQuery.value))
                        _searchIsLoading.value = false
                }
            }
        }
    }

    fun setTab(tab: MainTab) { _currentTab.value = tab }
    fun openNowPlaying() { _isNowPlayingExpanded.value = true }
    fun closeNowPlaying() { _isNowPlayingExpanded.value = false }
    fun playSong(song: Song, queue: List<Song> = listOf(song)) { val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0); playerController.setQueue(queue, index, autoPlay = true); viewModelScope.launch { repository.recordPlayedSong(song.id) } }
    fun togglePlayPause() { if (currentSong.value == null) { val firstSong = allSongs.value.firstOrNull(); if (firstSong != null) { playSong(firstSong, allSongs.value); return } }; playerController.togglePlayPause() }
    fun nextSong() { playerController.next() }
    fun prevSong() { playerController.previous() }
    fun seekTo(positionMs: Long) { playerController.seekTo(positionMs) }
    fun toggleShuffle() { playerController.toggleShuffle() }
    fun toggleRepeat() { playerController.toggleRepeat() }
    fun setEqualizerPreset(preset: EqualizerPreset) { playerController.setEqualizerPreset(preset) }
    fun setBandLevel(bandIndex: Int, levelDb: Float) { playerController.setBandLevel(bandIndex, levelDb) }
    fun setBassBoost(percent: Float) { playerController.setBassBoost(percent) }
    fun setVirtualizer(percent: Float) { playerController.setVirtualizer(percent) }
    fun toggleEqualizer() { playerController.toggleEqualizerEnabled() }
    fun setAudioQuality(quality: AudioQuality) { _audioQuality.value = quality }
    fun setAppLanguage(lang: String) { _appLanguage.value = lang }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun loadMoreSearchResults() {
        val query = normalizeSearch(_searchQuery.value)
        if (query.length < 2 || _searchIsLoading.value || !_searchHasMore.value) return
        viewModelScope.launch {
            _searchIsLoading.value = true
            try {
                coroutineScope {
                    searchSources.filter { it !in exhaustedSources }.forEach { source ->
                        launch {
                            val page = sourcePages[source] ?: 0
                            val result = withTimeoutOrNull(10_000L) {
                                runCatching {
                                    repository.searchRemoteCatalogPageFromSource(query, page, source)
                                }.getOrElse { Result.failure(it) }
                            }
                            if (query == normalizeSearch(_searchQuery.value))
                                recordSearchPage(source, page, result)
                        }
                    }
                }
            } finally {
                if (query == normalizeSearch(_searchQuery.value))
                    _searchIsLoading.value = false
            }
        }
    }
    fun filterByCategory(category: MusicCategory?) { _selectedCategoryFilter.value = category }
    fun scanLocalMusic() { viewModelScope.launch { _isLoading.value = true; try { repository.scanAndSyncLocalMusic() } catch (e: Exception) { Log.e("MainViewModel", "Local music scan failed", e) } finally { _isLoading.value = false } } }
    fun expandVerifiedCatalog() {
        if (_isCatalogExpanding.value) return
        viewModelScope.launch {
            _isCatalogExpanding.value = true
            try {
                repository.expandVerifiedCatalog()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Verified catalog expansion failed", e)
            } finally {
                _isCatalogExpanding.value = false
            }
        }
    }
    fun toggleFavorite(songId: String) { viewModelScope.launch { repository.toggleFavorite(songId) } }
    fun toggleDownload(song: Song) { viewModelScope.launch { repository.toggleDownload(song, _audioQuality.value) } }
    fun signOut() { viewModelScope.launch { when (val result = repository.signOut()) { is AuthResult.Failure -> Log.e("MainViewModel", "Sign out failed: ${result.message}"); is AuthResult.Success -> _currentTab.value = MainTab.HOME } } }
    fun openCreatePlaylistDialog() { _showCreatePlaylistDialog.value = true }
    fun closeCreatePlaylistDialog() { _showCreatePlaylistDialog.value = false }
    fun createPlaylist(name: String, description: String = "") { viewModelScope.launch { repository.createPlaylist(name, description); _showCreatePlaylistDialog.value = false } }
    fun renamePlaylist(playlistId: String, name: String) { viewModelScope.launch { runCatching { repository.renamePlaylist(playlistId, name) }.onFailure { Log.e("MainViewModel", "Playlist rename failed", it) } } }
    fun reorderPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) { viewModelScope.launch { runCatching { repository.reorderPlaylist(playlistId, fromIndex, toIndex) }.onFailure { Log.e("MainViewModel", "Playlist reorder failed", it) } } }
    fun deletePlaylist(playlistId: String) { viewModelScope.launch { repository.deletePlaylist(playlistId); if (_selectedPlaylist.value?.id == playlistId) _selectedPlaylist.value = null } }
    fun selectPlaylist(playlist: PlaylistEntity?) { _selectedPlaylist.value = playlist }
    fun openAddToPlaylist(song: Song) { _songToAddToPlaylist.value = song }
    fun closeAddToPlaylist() { _songToAddToPlaylist.value = null }
    fun addSongToPlaylist(playlistId: String, songId: String) { viewModelScope.launch { repository.addSongToPlaylist(playlistId, songId); _songToAddToPlaylist.value = null } }
    fun removeSongFromPlaylist(playlistId: String, songId: String) { viewModelScope.launch { repository.removeSongFromPlaylist(playlistId, songId) } }
    fun getSongsForPlaylist(playlistId: String) = repository.getSongsForPlaylist(playlistId)
    fun selectArtist(artist: Artist?) {
        _selectedArtist.value = artist
        if (artist != null) {
            viewModelScope.launch {
                val existing = allSongs.value.filter { it.artistId == artist.id }
                repository.getArtistDiscography(artist.id).onSuccess { verifiedSongs ->
                    if (verifiedSongs.isEmpty() && existing.isEmpty()) {
                        repository.searchRemoteCatalog(artist.name).onFailure { error ->
                            Log.w("MainViewModel", "Artist search fallback failed", error)
                        }
                    }
                }.onFailure { error -> Log.w("MainViewModel", "Verified artist discography lookup failed", error) }
            }
        }
    }
    fun getSongsForArtist(artistId: String): List<Song> = allSongs.value.filter { it.artistId == artistId }.distinctBy { it.id }
    fun shareSong(context: Context, song: Song) { val shareText = "🎵 ${song.title} - ${song.artist}\n\nLANU Müzik'te şimdi dinle:\nlanumusic://track/${song.id}"; context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_TEXT, shareText); type = "text/plain" }, "Parçayı Paylaş").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }
    fun sharePlaylist(context: Context, playlist: PlaylistEntity) { val shareText = "🎶 '${playlist.name}' Çalma Listesi\n\nLANU Müzik'te aç:\nlanumusic://playlist/${playlist.id}"; context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_TEXT, shareText); type = "text/plain" }, "Çalma Listesini Paylaş").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }
    fun shareArtist(context: Context, artist: Artist) { val shareText = "🌟 ${artist.name}\n\nLANU Müzik'te aç:\nlanumusic://artist/${artist.id}"; context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_TEXT, shareText); type = "text/plain" }, "Sanatçıyı Paylaş").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }
    fun likeFriendActivity(activityId: String) { repository.likeFriendActivity(activityId) }
    fun shareRecommendationToFriends(friendName: String, song: Song, note: String) { repository.shareRecommendationToFriends(friendName, song, note) }
    fun simulateNewReleasePush() { repository.pushSimulatedNewRelease() }
    fun dismissNewReleaseNotification() { repository.dismissNewReleaseNotification() }
    override fun onCleared() { playerController.release(); repository.close(); super.onCleared() }
}
