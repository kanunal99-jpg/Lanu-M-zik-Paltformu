package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthResult
import com.example.auth.AuthSession
import com.example.data.DownloadedSongEntity
import com.example.data.HistoryRecord
import com.example.data.MusicRepository
import com.example.data.PlaylistEntity
import com.example.model.Artist
import com.example.model.AudioQuality
import com.example.model.EqualizerPreset
import com.example.model.EqualizerSettings
import com.example.model.EqualizerState
import com.example.model.toSettings
import com.example.model.FriendActivity
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.service.AudioPlayerController
import com.example.service.RepeatMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val titleTr: String) { HOME("Ana Sayfa"), SEARCH("Keşfet"), LIBRARY("Kitaplığım"), FRIENDS("Arkadaşlar"), EQUALIZER("Ekolayzır"), ACCOUNT("Hesabım") }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MusicRepository(application)
    val playerController = AudioPlayerController(application)

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
    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()
    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()
    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()
    private val _showRenamePlaylistDialog = MutableStateFlow(false)
    val showRenamePlaylistDialog: StateFlow<Boolean> = _showRenamePlaylistDialog.asStateFlow()
    private val _songToAddToPlaylist = MutableStateFlow<Song?>(null)
    val songToAddToPlaylist: StateFlow<Song?> = _songToAddToPlaylist.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    @Deprecated("Artificial network loading was removed; use real operation state instead.") fun simulateNetworkLoading(delayMs: Long = 0) = Unit
    private val _audioQuality = MutableStateFlow(AudioQuality.HIGH)
    val audioQuality: StateFlow<AudioQuality> = _audioQuality.asStateFlow()
    private val _appLanguage = MutableStateFlow("tr")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
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
    val favoriteSongs: StateFlow<List<Song>> = combine(allSongs, favoriteSongIds) { songs, favIds -> songs.filter { it.id in favIds } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val offlineSongs: StateFlow<List<Song>> = combine(allSongs, downloadedSongIds) { songs, downIds -> songs.filter { it.id in downIds } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val searchResults: StateFlow<List<Song>> = combine(allSongs, _searchQuery, _selectedCategoryFilter) { songs, query, cat ->
        val q = normalizeSearch(query)
        songs.filter { song ->
            val matchesQuery = q.isBlank() || listOf(song.title, song.artist, song.album).any { normalizeSearch(it).contains(q) } || song.lyrics.any { normalizeSearch(it.text).contains(q) }
            matchesQuery && (cat == null || song.category == cat)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun normalizeSearch(value: String): String = value.trim().lowercase()
        .replace('ı','i').replace('İ','i').replace('ş','s').replace('Ş','s').replace('ğ','g').replace('Ğ','g')
        .replace('ü','u').replace('Ü','u').replace('ö','o').replace('Ö','o').replace('ç','c').replace('Ç','c')
    val cachedSongs: StateFlow<List<Song>> = repository.cachedSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    @OptIn(ExperimentalCoroutinesApi::class)
    val roomCachedSearchResults: StateFlow<List<Song>> = _searchQuery.flatMapLatest { repository.searchCachedSongs(it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun searchSongsFromRoom(query: String): Flow<List<Song>> = repository.searchCachedSongs(query)
    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentSong: StateFlow<Song?> = playerController.currentSong
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val currentPositionMs: StateFlow<Long> = playerController.currentPositionMs
    val durationMs: StateFlow<Long> = playerController.durationMs
    val isShuffle: StateFlow<Boolean> = playerController.isShuffle
    val repeatMode: StateFlow<RepeatMode> = playerController.repeatMode
    val isRepeat: StateFlow<Boolean> = repeatMode.map { it != RepeatMode.OFF }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val activeLyricIndex: StateFlow<Int> = combine(currentSong, currentPositionMs) { song, posMs ->
        val idx = song?.lyrics?.indexOfLast { it.timeMs <= posMs } ?: -1
        if (idx >= 0) idx else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val equalizerState: StateFlow<EqualizerState> = playerController.equalizerState
    val equalizerSettings: StateFlow<EqualizerSettings> = equalizerState.map { it.toSettings() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerSettings())

    init {
        viewModelScope.launch {
            playerController.currentSong.drop(1).filterNotNull().distinctUntilChangedBy { it.id }.collect { song ->
                repository.recordPlayedSong(song.id)
            }
        }
    }

    fun setTab(tab: MainTab) { _currentTab.value = tab }
    fun openNowPlaying() { _isNowPlayingExpanded.value = true }
    fun closeNowPlaying() { _isNowPlayingExpanded.value = false }
    fun toggleLyricsInNowPlaying() { _showLyricsInNowPlaying.value = !_showLyricsInNowPlaying.value }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        val wasSameSong = currentSong.value?.id == song.id
        playerController.setQueue(queue, index, autoPlay = true)
        if (wasSameSong) viewModelScope.launch { repository.recordPlayedSong(song.id) }
    }

    fun togglePlayPause() {
        if (currentSong.value == null) { allSongs.value.firstOrNull()?.let { playSong(it, allSongs.value) } ?: return } else playerController.togglePlayPause()
    }
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
    fun filterByCategory(category: MusicCategory?) { _selectedCategoryFilter.value = category }

    fun scanLocalMusic() {
        viewModelScope.launch {
            _isLoading.value = true
            try { repository.scanAndSyncLocalMusic() } catch (e: Exception) { Log.e("MainViewModel", "Local music scan failed", e) } finally { _isLoading.value = false }
        }
    }

    fun toggleFavorite(songId: String) { viewModelScope.launch { repository.toggleFavorite(songId) } }
    fun toggleDownload(song: Song) { viewModelScope.launch { repository.toggleDownload(song, _audioQuality.value) } }

    fun signOut() {
        viewModelScope.launch {
            when (val result = repository.signOut()) {
                is AuthResult.Failure -> Log.e("MainViewModel", "Sign out failed: ${result.message}")
                is AuthResult.Success -> _currentTab.value = MainTab.HOME
            }
        }
    }

    fun openCreatePlaylistDialog() { _showCreatePlaylistDialog.value = true }
    fun closeCreatePlaylistDialog() { _showCreatePlaylistDialog.value = false }
    fun openRenamePlaylistDialog() { if (_selectedPlaylist.value != null) _showRenamePlaylistDialog.value = true }
    fun closeRenamePlaylistDialog() { _showRenamePlaylistDialog.value = false }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            runCatching { repository.createPlaylist(name, description) }.onFailure { Log.e("MainViewModel", "Create playlist failed", it) }
            _showCreatePlaylistDialog.value = false
        }
    }

    fun renameSelectedPlaylist(name: String, description: String = "") {
        val playlistId = _selectedPlaylist.value?.id ?: return
        viewModelScope.launch {
            runCatching { repository.renamePlaylist(playlistId, name, description) }.onFailure { Log.e("MainViewModel", "Rename playlist failed", it) }
            _showRenamePlaylistDialog.value = false
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            runCatching { repository.deletePlaylist(playlistId) }.onFailure { Log.e("MainViewModel", "Delete playlist failed", it) }
            if (_selectedPlaylist.value?.id == playlistId) _selectedPlaylist.value = null
        }
    }

    fun moveSongInSelectedPlaylist(songId: String, targetIndex: Int) {
        val playlistId = _selectedPlaylist.value?.id ?: return
        viewModelScope.launch { runCatching { repository.moveSongInPlaylist(playlistId, songId, targetIndex) }.onFailure { Log.e("MainViewModel", "Move playlist song failed", it) } }
    }

    fun selectPlaylist(playlist: PlaylistEntity?) { _selectedPlaylist.value = playlist }
    fun openAddToPlaylist(song: Song) { _songToAddToPlaylist.value = song }
    fun closeAddToPlaylist() { _songToAddToPlaylist.value = null }
    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            runCatching { repository.addSongToPlaylist(playlistId, songId) }.onFailure { Log.e("MainViewModel", "Add to playlist failed", it) }
            _songToAddToPlaylist.value = null
        }
    }
    fun removeSongFromPlaylist(playlistId: String, songId: String) { viewModelScope.launch { runCatching { repository.removeSongFromPlaylist(playlistId, songId) }.onFailure { Log.e("MainViewModel", "Remove from playlist failed", it) } } }
    fun getSongsForPlaylist(playlistId: String) = repository.getSongsForPlaylist(playlistId)
    fun selectArtist(artist: Artist?) { _selectedArtist.value = artist }
    fun getSongsForArtist(artistId: String): List<Song> = allSongs.value.filter { it.artistId == artistId }

    private fun share(context: Context, chooserTitle: String, text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_TEXT, text); type = "text/plain" }
        context.startActivity(Intent.createChooser(sendIntent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareSong(context: Context, song: Song) {
        val line = song.lyrics.firstOrNull()?.text?.takeIf { it.isNotBlank() }
        val lyrics = line?.let { "\n\nSöz: \"$it\"" }.orEmpty()
        share(context, "Parçayı Paylaş", "🎵 ${song.title} — ${song.artist}\nLANU Müzik yerel/izinli içerik paylaşımı.$lyrics")
    }

    fun sharePlaylist(context: Context, playlist: PlaylistEntity) = share(context, "Çalma Listesini Paylaş", "🎶 ${playlist.name}\nLANU Müzik'te oluşturuldu. Liste ID: ${playlist.id}")
    fun shareArtist(context: Context, artist: Artist) = share(context, "Sanatçıyı Paylaş", "🌟 ${artist.name} — ${artist.genre}\nLANU Müzik sanatçı kaydı: ${artist.id}")
    fun likeFriendActivity(activityId: String) { repository.likeFriendActivity(activityId) }
    fun shareRecommendationToFriends(friendName: String, song: Song, note: String) { repository.shareRecommendationToFriends(friendName, song, note) }
    fun simulateNewReleasePush() { repository.pushSimulatedNewRelease() }
    fun dismissNewReleaseNotification() { repository.dismissNewReleaseNotification() }

    override fun onCleared() { playerController.release(); repository.close(); super.onCleared() }
}
