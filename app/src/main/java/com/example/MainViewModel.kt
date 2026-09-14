package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DownloadedSongEntity
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val titleTr: String) {
    HOME("Ana Sayfa"),
    SEARCH("Keşfet"),
    LIBRARY("Kitaplığım"),
    FRIENDS("Arkadaşlar"),
    EQUALIZER("Ekolayzır")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MusicRepository(application)
    val playerController = AudioPlayerController(application)

    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()
    val isPlayerExpanded: StateFlow<Boolean> get() = _isNowPlayingExpanded

    fun setPlayerExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    private val _nowPlayingTab = MutableStateFlow(0)
    val nowPlayingTab: StateFlow<Int> = _nowPlayingTab.asStateFlow()

    fun setNowPlayingTab(tab: Int) {
        _nowPlayingTab.value = tab
    }

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    private val _recommendationAlert = MutableStateFlow<String?>(null)
    val recommendationAlert: StateFlow<String?> = _recommendationAlert.asStateFlow()

    fun clearRecommendationAlert() {
        _recommendationAlert.value = null
    }

    private val _showLyricsInNowPlaying = MutableStateFlow(false)
    val showLyricsInNowPlaying: StateFlow<Boolean> = _showLyricsInNowPlaying.asStateFlow()

    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    private val _songToAddToPlaylist = MutableStateFlow<Song?>(null)
    val songToAddToPlaylist: StateFlow<Song?> = _songToAddToPlaylist.asStateFlow()

    // Loading is reserved for real asynchronous operations; there is no artificial network delay.
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Compatibility shim for callers that used the old fake-loading API.
     * It deliberately does nothing: UI latency must come from real work, not timers.
     */
    @Deprecated("Artificial network loading was removed; use real operation state instead.")
    fun simulateNetworkLoading(delayMs: Long = 0) = Unit

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
    val artists: List<Artist> = repository.artists
    val friendActivities: StateFlow<List<FriendActivity>> = repository.friendActivities
    val newReleaseNotification: StateFlow<Song?> = repository.newReleaseNotification

    val favoriteSongIds: StateFlow<Set<String>> = repository.favoriteSongIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val downloadedSongs: StateFlow<List<DownloadedSongEntity>> = repository.downloadedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val downloadedSongIds: StateFlow<Set<String>> = repository.downloadedSongIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )
    val offlineSongIds: StateFlow<Set<String>> get() = downloadedSongIds

    val favoriteSongs: StateFlow<List<Song>> = combine(allSongs, favoriteSongIds) { songs, favIds ->
        songs.filter { favIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineSongs: StateFlow<List<Song>> = combine(allSongs, downloadedSongIds) { songs, downIds ->
        songs.filter { downIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<Song>> = combine(allSongs, _searchQuery, _selectedCategoryFilter) { songs, query, cat ->
        songs.filter { song ->
            val matchesQuery = query.isBlank() ||
                    song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    song.album.contains(query, ignoreCase = true)
            val matchesCat = cat == null || song.category == cat
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cachedSongs: StateFlow<List<Song>> = repository.cachedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val roomCachedSearchResults: StateFlow<List<Song>> = _searchQuery
        .flatMapLatest { query -> repository.searchCachedSongs(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun searchSongsFromRoom(query: String): Flow<List<Song>> = repository.searchCachedSongs(query)

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentSong: StateFlow<Song?> = playerController.currentSong
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val currentPositionMs: StateFlow<Long> = playerController.currentPositionMs
    val durationMs: StateFlow<Long> = playerController.durationMs
    val isShuffle: StateFlow<Boolean> = playerController.isShuffle
    val repeatMode: StateFlow<RepeatMode> = playerController.repeatMode
    val isRepeat: StateFlow<Boolean> = repeatMode.map { it != RepeatMode.OFF }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeLyricIndex: StateFlow<Int> = combine(currentSong, currentPositionMs) { song, posMs ->
        val lyrics = song?.lyrics ?: emptyList()
        val idx = lyrics.indexOfLast { it.timeMs <= posMs }
        if (idx >= 0) idx else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val equalizerState: StateFlow<EqualizerState> = playerController.equalizerState
    val equalizerSettings: StateFlow<EqualizerSettings> = equalizerState
        .map { it.toSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerSettings())

    fun setTab(tab: MainTab) { _currentTab.value = tab }
    fun openNowPlaying() { _isNowPlayingExpanded.value = true }
    fun closeNowPlaying() { _isNowPlayingExpanded.value = false }
    fun toggleLyricsInNowPlaying() { _showLyricsInNowPlaying.value = !_showLyricsInNowPlaying.value }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playerController.setQueue(queue, index, autoPlay = true)
        viewModelScope.launch { repository.recordPlayedSong(song.id) }
    }

    fun togglePlayPause() {
        if (currentSong.value == null) {
            val firstSong = allSongs.value.firstOrNull()
            if (firstSong != null) {
                playSong(firstSong, allSongs.value)
                return
            }
        }
        playerController.togglePlayPause()
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
        viewModelScope.launch { repository.scanAndSyncLocalMusic() }
    }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }

    fun toggleDownload(song: Song) {
        viewModelScope.launch { repository.toggleDownload(song, _audioQuality.value) }
    }

    fun openCreatePlaylistDialog() { _showCreatePlaylistDialog.value = true }
    fun closeCreatePlaylistDialog() { _showCreatePlaylistDialog.value = false }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
            _showCreatePlaylistDialog.value = false
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylist.value?.id == playlistId) _selectedPlaylist.value = null
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity?) { _selectedPlaylist.value = playlist }
    fun openAddToPlaylist(song: Song) { _songToAddToPlaylist.value = song }
    fun closeAddToPlaylist() { _songToAddToPlaylist.value = null }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
            _songToAddToPlaylist.value = null
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch { repository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun getSongsForPlaylist(playlistId: String) = repository.getSongsForPlaylist(playlistId)

    fun selectArtist(artist: Artist?) { _selectedArtist.value = artist }

    fun getSongsForArtist(artistId: String): List<Song> = allSongs.value.filter { it.artistId == artistId }

    fun shareSong(context: Context, song: Song) {
        val shareText = "🎵 ${song.title} - ${song.artist}\n\nLANU Müzik'te şimdi dinle:\nhttps://lanumusic.app/track/${song.id}\n\nSöz: \"${song.lyrics.firstOrNull()?.text ?: ""}\""
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Parçayı Paylaş")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    fun sharePlaylist(context: Context, playlist: PlaylistEntity) {
        val shareText = "🎶 '${playlist.name}' Çalma Listesi\n\nLANU Müzik'te harika şarkılar dinliyorum, listeme göz at!\nhttps://lanumusic.app/playlist/${playlist.id}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Çalma Listesini Paylaş")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    fun shareArtist(context: Context, artist: Artist) {
        val shareText = "🌟 ${artist.name} (${artist.genre})\n\nLANU Müzik'te tüm şarkılarını ve albümlerini keşfet!\nhttps://lanumusic.app/artist/${artist.id}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Sanatçıyı Paylaş")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    fun likeFriendActivity(activityId: String) { repository.likeFriendActivity(activityId) }
    fun shareRecommendationToFriends(friendName: String, song: Song, note: String) {
        repository.shareRecommendationToFriends(friendName, song, note)
    }

    fun simulateNewReleasePush() { repository.pushSimulatedNewRelease() }
    fun dismissNewReleaseNotification() { repository.dismissNewReleaseNotification() }
}