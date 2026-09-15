package com.example

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.ArtistDetailSheet
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerView
import com.example.ui.components.FriendActivityView
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.PlayerBottomAppBar
import com.example.ui.components.PlaylistDetailSheet
import com.example.ui.components.RenamePlaylistDialog
import com.example.ui.screens.AccountHistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { MainAppScreen() } }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { viewModel.scanLocalMusic() }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    val currentTab by viewModel.currentTab.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val showLyricsInNowPlaying by viewModel.showLyricsInNowPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val equalizerState by viewModel.equalizerState.collectAsState()
    val audioQuality by viewModel.audioQuality.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val cachedSongs by viewModel.cachedSongs.collectAsState()
    val favoriteIds by viewModel.favoriteSongIds.collectAsState()
    val downloadedEntities by viewModel.downloadedSongs.collectAsState()
    val downloadedSongIds by viewModel.downloadedSongIds.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val friendActivities by viewModel.friendActivities.collectAsState()
    val newReleaseAlert by viewModel.newReleaseNotification.collectAsState()
    val session by viewModel.session.collectAsState()
    val history by viewModel.history.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsState()
    val showRenamePlaylistDialog by viewModel.showRenamePlaylistDialog.collectAsState()
    val songToAddToPlaylist by viewModel.songToAddToPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(LanuDarkBg)) {
        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    if (!isNowPlayingExpanded) {
                        PlayerBottomAppBar(
                            song = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            isFavorite = currentSong?.let { favoriteIds.contains(it.id) } ?: false,
                            isShuffle = isShuffle,
                            repeatMode = repeatMode,
                            onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it.id) } },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onPrevious = { viewModel.prevSong() },
                            onNext = { viewModel.nextSong() },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onToggleRepeat = { viewModel.toggleRepeat() },
                            onExpand = { viewModel.openNowPlaying() },
                            onSeekTo = { viewModel.seekTo(it) }
                        )
                    }
                    NavigationBar(containerColor = LanuDarkSurface, tonalElevation = 8.dp, modifier = Modifier.testTag("bottom_navigation_bar")) {
                        val tabs = listOf(
                            Triple(MainTab.HOME, "Ana Sayfa", Icons.Default.Home),
                            Triple(MainTab.SEARCH, "Keşfet", Icons.Default.Search),
                            Triple(MainTab.LIBRARY, "Arşivim", Icons.Default.LibraryMusic),
                            Triple(MainTab.FRIENDS, "Sosyal", Icons.Default.Group),
                            Triple(MainTab.EQUALIZER, "Ekolayzır", Icons.Default.Equalizer),
                            Triple(MainTab.ACCOUNT, "Hesabım", Icons.Default.Person)
                        )
                        tabs.forEach { (tab, label, icon) ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { if (currentTab != tab) viewModel.setTab(tab) },
                                icon = { Icon(icon, contentDescription = label, tint = if (isSelected) LanuGreen else LanuTextMuted, modifier = Modifier.size(24.dp)) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) LanuGreen else LanuTextMuted) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = LanuGreen.copy(alpha = 0.15f)),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (currentTab) {
                    MainTab.HOME -> HomeScreen(
                        songs = allSongs,
                        artists = viewModel.artists,
                        newReleaseAlert = newReleaseAlert,
                        onPlaySong = { song, queue -> viewModel.playSong(song, queue) },
                        onSelectArtist = { viewModel.selectArtist(it) },
                        onCategoryClick = { category -> viewModel.filterByCategory(category); viewModel.setTab(MainTab.SEARCH) },
                        onSimulateNewRelease = { viewModel.simulateNewReleasePush() },
                        onDismissNewReleaseAlert = { viewModel.dismissNewReleaseNotification() }
                    )
                    MainTab.SEARCH -> SearchScreen(
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategoryFilter,
                        allSongs = allSongs,
                        cachedSongs = cachedSongs,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onSelectCategory = { viewModel.filterByCategory(it) },
                        onPlaySong = { song, queue -> viewModel.playSong(song, queue) }
                    )
                    MainTab.LIBRARY -> LibraryScreen(
                        playlists = playlists,
                        downloadedEntities = downloadedEntities,
                        favoriteIds = favoriteIds,
                        allSongs = allSongs,
                        onSelectPlaylist = { viewModel.selectPlaylist(it) },
                        onCreatePlaylist = { viewModel.openCreatePlaylistDialog() },
                        onPlaySong = { song, queue -> viewModel.playSong(song, queue) }
                    )
                    MainTab.FRIENDS -> FriendActivityView(
                        activities = friendActivities,
                        availableSongs = allSongs,
                        onPlaySong = { viewModel.playSong(it, listOf(it)) },
                        onShareSong = { viewModel.shareSong(context, it) },
                        onLikeActivity = { viewModel.likeFriendActivity(it) },
                        onSendRecommendation = { friendName, song, note -> viewModel.shareRecommendationToFriends(friendName, song, note) }
                    )
                    MainTab.EQUALIZER -> EqualizerView(
                        equalizerState = equalizerState,
                        isPlaying = isPlaying,
                        onPresetSelected = { viewModel.setEqualizerPreset(it) },
                        onBandLevelChanged = { band, level -> viewModel.setBandLevel(band, level) },
                        onBassBoostChanged = { viewModel.setBassBoost(it) },
                        onVirtualizerChanged = { viewModel.setVirtualizer(it) },
                        onToggleEqualizer = { viewModel.toggleEqualizer() }
                    )
                    MainTab.ACCOUNT -> AccountHistoryScreen(
                        session = session,
                        history = history,
                        allSongs = allSongs,
                        onPlaySong = { song, _ -> viewModel.playSong(song, listOf(song)) },
                        onSignOut = { viewModel.signOut() }
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(LanuDarkBg.copy(alpha = 0.5f)).clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {},
                        contentAlignment = Alignment.Center
                    ) { androidx.compose.material3.CircularProgressIndicator(color = LanuGreen, strokeWidth = 4.dp) }
                }
            }
        }

        AnimatedVisibility(
            visible = isNowPlayingExpanded && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            currentSong?.let { song ->
                NowPlayingSheet(
                    song = song,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    isFavorite = favoriteIds.contains(song.id),
                    isDownloaded = downloadedSongIds.contains(song.id),
                    audioQuality = audioQuality,
                    showLyrics = showLyricsInNowPlaying,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextSong() },
                    onPrevious = { viewModel.prevSong() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                    onToggleDownload = { viewModel.toggleDownload(song) },
                    onToggleLyrics = { viewModel.toggleLyricsInNowPlaying() },
                    onSelectAudioQuality = { viewModel.setAudioQuality(it) },
                    onOpenEqualizer = { viewModel.closeNowPlaying(); viewModel.setTab(MainTab.EQUALIZER) },
                    onAddToPlaylist = { viewModel.openAddToPlaylist(song) },
                    onShare = { viewModel.shareSong(context, song) },
                    onClose = { viewModel.closeNowPlaying() }
                )
            }
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(onDismiss = { viewModel.closeCreatePlaylistDialog() }, onCreate = { name, desc -> viewModel.createPlaylist(name, desc) })
        }

        songToAddToPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                playlists = playlists,
                onDismiss = { viewModel.closeAddToPlaylist() },
                onSelectPlaylist = { plId -> viewModel.addSongToPlaylist(plId, song.id) },
                onCreateNewPlaylist = { viewModel.closeAddToPlaylist(); viewModel.openCreatePlaylistDialog() }
            )
        }

        selectedArtist?.let { artist ->
            val artistSongs = viewModel.getSongsForArtist(artist.id)
            ArtistDetailSheet(
                artist = artist,
                songs = artistSongs,
                onPlaySong = { song, queue -> viewModel.playSong(song, queue) },
                onShareArtist = { viewModel.shareArtist(context, artist) },
                onBack = { viewModel.selectArtist(null) }
            )
        }

        selectedPlaylist?.let { playlist ->
            val playlistSongsFlow = viewModel.getSongsForPlaylist(playlist.id).collectAsState(initial = emptyList())
            PlaylistDetailSheet(
                playlist = playlist,
                songs = playlistSongsFlow.value,
                onPlaySong = { song, queue -> viewModel.playSong(song, queue) },
                onRemoveSong = { songId -> viewModel.removeSongFromPlaylist(playlist.id, songId) },
                onMoveSong = { songId, targetIndex -> viewModel.moveSongInSelectedPlaylist(songId, targetIndex) },
                onSharePlaylist = { viewModel.sharePlaylist(context, playlist) },
                onRenamePlaylist = { viewModel.openRenamePlaylistDialog() },
                onDeletePlaylist = { viewModel.deletePlaylist(playlist.id) },
                onBack = { viewModel.selectPlaylist(null) }
            )
        }

        if (showRenamePlaylistDialog) {
            selectedPlaylist?.let { playlist ->
                RenamePlaylistDialog(
                    playlist = playlist,
                    onDismiss = { viewModel.closeRenamePlaylistDialog() },
                    onRename = { name, description -> viewModel.renameSelectedPlaylist(name, description) }
                )
            }
        }
    }
}
