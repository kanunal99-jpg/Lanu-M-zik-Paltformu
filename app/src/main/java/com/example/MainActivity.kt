package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.MusicPlaybackService
import com.example.ui.components.ArtistDetailSheet
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerView
import com.example.ui.components.FriendActivityView
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.PlayerBottomAppBar
import com.example.ui.components.PlaylistDetailSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var playbackService: MusicPlaybackService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicPlaybackService.LocalBinder
            playbackService = binder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Bind playback service
        val serviceIntent = Intent(this, MusicPlaybackService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    playbackService = playbackService
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

@Composable
fun MainAppScreen(
    playbackService: MusicPlaybackService?,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // State collections
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
    val artists = viewModel.artists
    val favoriteIds by viewModel.favoriteSongIds.collectAsState()
    val downloadedEntities by viewModel.downloadedSongs.collectAsState()
    val downloadedSongIds by viewModel.downloadedSongIds.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val friendActivities by viewModel.friendActivities.collectAsState()
    val newReleaseAlert by viewModel.newReleaseNotification.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()

    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsState()
    val songToAddToPlaylist by viewModel.songToAddToPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Sync playback state with foreground service notification
    LaunchedEffect(currentSong, isPlaying) {
        currentSong?.let { song ->
            try {
                playbackService?.startForegroundWithNotification(song.title, song.artist, isPlaying)
            } catch (e: Exception) {
                // Ignore service sync errors
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(LanuDarkBg)) {
        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    // Persistent BottomAppBar component as the primary music player controller
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

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = LanuDarkSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        val tabs = listOf(
                            Triple(MainTab.HOME, "Ana Sayfa", Icons.Default.Home),
                            Triple(MainTab.SEARCH, "Keşfet", Icons.Default.Search),
                            Triple(MainTab.LIBRARY, "Arşivim", Icons.Default.LibraryMusic),
                            Triple(MainTab.FRIENDS, "Sosyal", Icons.Default.Group),
                            Triple(MainTab.EQUALIZER, "Ekolayzır", Icons.Default.Equalizer)
                        )

                        tabs.forEach { (tab, label, icon) ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentTab != tab) {
                                        viewModel.simulateNetworkLoading()
                                        viewModel.setTab(tab)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) LanuGreen else LanuTextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) LanuGreen else LanuTextMuted
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = LanuGreen.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    MainTab.HOME -> {
                        HomeScreen(
                            songs = allSongs,
                            artists = artists,
                            newReleaseAlert = newReleaseAlert,
                            onPlaySong = { song, queue -> viewModel.playSong(song, queue) },
                            onSelectArtist = { artist -> viewModel.selectArtist(artist) },
                            onCategoryClick = { category ->
                                viewModel.filterByCategory(category)
                                viewModel.setTab(MainTab.SEARCH)
                            },
                            onSimulateNewRelease = { viewModel.simulateNewReleasePush() },
                            onDismissNewReleaseAlert = { viewModel.dismissNewReleaseNotification() }
                        )
                    }

                    MainTab.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategoryFilter,
                            allSongs = allSongs,
                            cachedSongs = cachedSongs,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onSelectCategory = { viewModel.filterByCategory(it) },
                            onPlaySong = { song, queue -> viewModel.playSong(song, queue) }
                        )
                    }

                    MainTab.LIBRARY -> {
                        LibraryScreen(
                            playlists = playlists,
                            downloadedEntities = downloadedEntities,
                            favoriteIds = favoriteIds,
                            allSongs = allSongs,
                            onSelectPlaylist = { pl -> viewModel.selectPlaylist(pl) },
                            onCreatePlaylist = { viewModel.openCreatePlaylistDialog() },
                            onPlaySong = { song, queue -> viewModel.playSong(song, queue) }
                        )
                    }

                    MainTab.FRIENDS -> {
                        FriendActivityView(
                            activities = friendActivities,
                            availableSongs = allSongs,
                            onPlaySong = { song -> viewModel.playSong(song, listOf(song)) },
                            onShareSong = { song -> viewModel.shareSong(context, song) },
                            onLikeActivity = { id -> viewModel.likeFriendActivity(id) },
                            onSendRecommendation = { friendName, song, note ->
                                viewModel.shareRecommendationToFriends(friendName, song, note)
                            }
                        )
                    }

                    MainTab.EQUALIZER -> {
                        EqualizerView(
                            equalizerState = equalizerState,
                            isPlaying = isPlaying,
                            onPresetSelected = { preset -> viewModel.setEqualizerPreset(preset) },
                            onBandLevelChanged = { band, level -> viewModel.setBandLevel(band, level) },
                            onBassBoostChanged = { viewModel.setBassBoost(it) },
                            onVirtualizerChanged = { viewModel.setVirtualizer(it) },
                            onToggleEqualizer = { viewModel.toggleEqualizer() }
                        )
                    }
                }
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LanuDarkBg.copy(alpha = 0.5f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {},
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = LanuGreen,
                            strokeWidth = 4.dp
                        )
                    }
                }
            }
        }

        // Full-Screen Now Playing Sheet with Slide Animation
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
                    onOpenEqualizer = {
                        viewModel.closeNowPlaying()
                        viewModel.setTab(MainTab.EQUALIZER)
                    },
                    onAddToPlaylist = { viewModel.openAddToPlaylist(song) },
                    onShare = { viewModel.shareSong(context, song) },
                    onClose = { viewModel.closeNowPlaying() }
                )
            }
        }

        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { viewModel.closeCreatePlaylistDialog() },
                onCreate = { name, desc -> viewModel.createPlaylist(name, desc) }
            )
        }

        // Add to Playlist Dialog
        songToAddToPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                playlists = playlists,
                onDismiss = { viewModel.closeAddToPlaylist() },
                onSelectPlaylist = { plId -> viewModel.addSongToPlaylist(plId, song.id) },
                onCreateNewPlaylist = {
                    viewModel.closeAddToPlaylist()
                    viewModel.openCreatePlaylistDialog()
                }
            )
        }

        // Selected Artist Detail Modal
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

        // Selected Playlist Detail Modal
        selectedPlaylist?.let { playlist ->
            val playlistSongsFlow = viewModel.getSongsForPlaylist(playlist.id).collectAsState(initial = emptyList())
            PlaylistDetailSheet(
                playlist = playlist,
                songs = playlistSongsFlow.value,
                onPlaySong = { song, queue -> viewModel.playSong(song, queue) },
                onRemoveSong = { songId -> viewModel.removeSongFromPlaylist(playlist.id, songId) },
                onSharePlaylist = { viewModel.sharePlaylist(context, playlist) },
                onDeletePlaylist = { viewModel.deletePlaylist(playlist.id) },
                onBack = { viewModel.selectPlaylist(null) }
            )
        }
    }
}
