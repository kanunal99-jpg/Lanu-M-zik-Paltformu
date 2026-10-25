package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Album
import com.example.model.MusicGenreResolver
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AlbumDetailSheet
import com.example.ui.components.ArtistDetailSheet
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerView
import com.example.ui.components.FriendActivityView
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.PlayerBottomAppBar
import com.example.ui.components.PlaylistDetailSheet
import com.example.ui.screens.AccountHistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.UpdateScreen
import com.example.update.UpdateScreenState
import com.example.update.UpdateViewModel
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.MyApplicationTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Compose-observed event. Never recreate the Activity just to handle a link:
    // recreation drops current UI state and releases the scoped media controller.
    internal var incomingLinkEvent by mutableStateOf(LanuIncomingLinkEvent(null, 0L))
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingLinkEvent = LanuIncomingLinkEvent(intent?.dataString, 0L)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { MainAppScreen() } }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingLinkEvent = LanuIncomingLinkEvent.next(incomingLinkEvent, intent.dataString)
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel = viewModel(), updates: UpdateViewModel = viewModel()) {
    val updateState by updates.screen.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { updates.checkOnAppLaunch() }
    val context = LocalContext.current
    val incomingEvent = (context as? MainActivity)?.incomingLinkEvent
    val deepLink = remember(incomingEvent) { LanuDeepLink.parse(incomingEvent?.rawUri.orEmpty()) }
    var handledResourceRoute by remember(incomingEvent) { mutableStateOf<LanuDeepLink?>(null) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Notification access is optional; a denied audio permission must never
        // trigger an unauthorized MediaStore scan on any Android OEM.
        val audioPermission = LocalMediaPermissionPolicy.mediaPermission(Build.VERSION.SDK_INT)
        if (results[audioPermission] == true ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, audioPermission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) viewModel.scanLocalMusic()
    }
    LaunchedEffect(Unit) {
        val audioPermission = LocalMediaPermissionPolicy.mediaPermission(Build.VERSION.SDK_INT)
        val hasAudioAccess = androidx.core.content.ContextCompat.checkSelfPermission(
            context, audioPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasAudioAccess) viewModel.scanLocalMusic()
        // Initial launch only asks to read the user's music, not to send notifications.
        // On API 33+ POST_NOTIFICATIONS is a separate, feature-specific consent.
        val pendingPermissions = LocalMediaPermissionPolicy.startupPermissions(
            Build.VERSION.SDK_INT, hasAudioAccess
        )
        if (pendingPermissions.isNotEmpty()) {
            permissionsLauncher.launch(pendingPermissions.toTypedArray())
        }
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    val showLyricsInNowPlaying by viewModel.showLyricsInNowPlaying.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val audioQuality by viewModel.audioQuality.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    // Reclassify only the displayed catalog; never delete or migrate user libraries.
    val categorizedSongs = remember(allSongs) { allSongs.map(MusicGenreResolver::resolve) }
    val cachedSongs by viewModel.cachedSongs.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val downloadedEntities by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val downloadedSongIds by viewModel.downloadedSongIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val friendActivities by viewModel.friendActivities.collectAsStateWithLifecycle()
    val newReleaseAlert by viewModel.newReleaseNotification.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val remoteArtistResults by viewModel.remoteArtistResults.collectAsStateWithLifecycle()
    val searchIsLoading by viewModel.searchIsLoading.collectAsStateWithLifecycle()
    val searchHasMore by viewModel.searchHasMore.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val selectedArtist by viewModel.selectedArtist.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsStateWithLifecycle()
    val songToAddToPlaylist by viewModel.songToAddToPlaylist.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isCatalogExpanding by viewModel.isCatalogExpanding.collectAsStateWithLifecycle()

    LaunchedEffect(incomingEvent) {
        when (val target = deepLink) {
            LanuDeepLink.Home -> viewModel.setTab(MainTab.HOME)
            LanuDeepLink.Library -> viewModel.setTab(MainTab.LIBRARY)
            is LanuDeepLink.Search -> { viewModel.updateSearchQuery(target.query); viewModel.setTab(MainTab.SEARCH) }
            is LanuDeepLink.PlayerAction -> when (target.action) {
                LanuDeepLink.Action.PLAY_PAUSE -> viewModel.togglePlayPause()
                LanuDeepLink.Action.NEXT -> viewModel.nextSong()
                LanuDeepLink.Action.PREVIOUS -> viewModel.prevSong()
                LanuDeepLink.Action.OPEN_PLAYER -> viewModel.openNowPlaying()
            }
            else -> Unit
        }
    }

    LaunchedEffect(incomingEvent, allSongs, playlists) {
        val target = deepLink ?: return@LaunchedEffect
        if (handledResourceRoute == target) return@LaunchedEffect
        when (target) {
            is LanuDeepLink.Track -> allSongs.firstOrNull { it.id == target.id }?.let { viewModel.playSong(it, listOf(it)); handledResourceRoute = target }
            is LanuDeepLink.Playlist -> playlists.firstOrNull { it.id == target.id }?.let { viewModel.selectPlaylist(it); handledResourceRoute = target }
            is LanuDeepLink.Artist -> allSongs.firstOrNull { it.artistId == target.id }?.let { viewModel.selectArtist(com.example.model.Artist(id = it.artistId, name = it.artist, genre = it.category.titleTr, bio = "Yerel veya doğrulanmış katalog verisi", imageUrl = it.coverUrl, monthlyListeners = "")); handledResourceRoute = target }
            else -> Unit
        }
    }

    Box(Modifier.fillMaxSize().background(LanuDarkBg)) {
        Scaffold(bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                if (!isNowPlayingExpanded) PlayerBottomAppBar(song = currentSong, isPlaying = isPlaying, currentPositionMs = currentPositionMs, durationMs = durationMs, isFavorite = currentSong?.let { favoriteIds.contains(it.id) } ?: false, isShuffle = isShuffle, repeatMode = repeatMode, onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it.id) } }, onTogglePlayPause = { viewModel.togglePlayPause() }, onPrevious = { viewModel.prevSong() }, onNext = { viewModel.nextSong() }, onToggleShuffle = { viewModel.toggleShuffle() }, onToggleRepeat = { viewModel.toggleRepeat() }, onExpand = { viewModel.openNowPlaying() }, onSeekTo = { viewModel.seekTo(it) })
                NavigationBar(containerColor = LanuDarkSurface, tonalElevation = 8.dp, modifier = Modifier.testTag("bottom_navigation_bar")) {
                    val tabs = listOf(
                        Triple(MainTab.HOME, stringResource(R.string.nav_home), Icons.Default.Home),
                        Triple(MainTab.SEARCH, stringResource(R.string.nav_search), Icons.Default.Search),
                        Triple(MainTab.LIBRARY, stringResource(R.string.nav_library), Icons.Default.LibraryMusic),
                        Triple(MainTab.ACCOUNT, stringResource(R.string.nav_account), Icons.Default.Person),
                        Triple(MainTab.UPDATE, stringResource(R.string.nav_updates), Icons.Default.SystemUpdate)
                    )
                    tabs.forEach { (tab, label, icon) ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(selected = isSelected, onClick = { if (currentTab != tab) viewModel.setTab(tab) }, icon = {
                            BadgedBox(badge = {
                                if (tab == MainTab.UPDATE && updateState is UpdateScreenState.Available) {
                                    Badge(containerColor = LanuGreen, modifier = Modifier.testTag("new_update_badge"))
                                }
                            }) {
                                Icon(icon, contentDescription = label, tint = if (isSelected) LanuGreen else LanuTextMuted, modifier = Modifier.size(24.dp))
                            }
                        }, label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) LanuGreen else LanuTextMuted) }, colors = NavigationBarItemDefaults.colors(indicatorColor = LanuGreen.copy(alpha = 0.15f)), modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"))
                    }
                }
            }
        }) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                when (currentTab) {
                    MainTab.HOME -> HomeScreen(songs = categorizedSongs, artists = viewModel.artists, newReleaseAlert = newReleaseAlert, onPlaySong = { song, queue -> viewModel.playSong(song, queue) }, onSelectArtist = { viewModel.selectArtist(it) }, onCategoryClick = { category -> viewModel.updateSearchQuery(""); viewModel.filterByCategory(category); viewModel.setTab(MainTab.SEARCH) }, onSimulateNewRelease = { viewModel.simulateNewReleasePush() }, onDismissNewReleaseAlert = { viewModel.dismissNewReleaseNotification() })
                    MainTab.SEARCH -> SearchScreen(searchQuery = searchQuery, selectedCategory = selectedCategoryFilter, allSongs = categorizedSongs, cachedSongs = cachedSongs.map(MusicGenreResolver::resolve), remoteArtists = remoteArtistResults, onSelectArtist = { viewModel.selectArtist(it) }, onQueryChange = { viewModel.updateSearchQuery(it) }, onSelectCategory = { viewModel.filterByCategory(it) }, onPlaySong = { song, queue -> viewModel.playSong(song, queue) }, isCatalogExpanding = isCatalogExpanding, onExpandCatalog = { viewModel.expandVerifiedCatalog() }, searchIsLoading = searchIsLoading, searchHasMore = searchHasMore, searchError = searchError, onLoadMoreResults = { viewModel.loadMoreSearchResults() }, onOpenYouTube = { if (isPlaying) viewModel.togglePlayPause() })
                    MainTab.LIBRARY -> LibraryScreen(
                        playlists = playlists, downloadedEntities = downloadedEntities,
                        favoriteIds = favoriteIds, allSongs = categorizedSongs,
                        onSelectPlaylist = { viewModel.selectPlaylist(it) },
                        onCreatePlaylist = { viewModel.openCreatePlaylistDialog() },
                        onPlaySong = { song, queue -> viewModel.playSong(song, queue) }
                    )
                    MainTab.FRIENDS -> Column {
                        androidx.compose.material3.TextButton(onClick = { viewModel.setTab(MainTab.ACCOUNT) }) {
                            Text(androidx.compose.ui.res.stringResource(R.string.lanu_friends_back))
                        }
                        FriendActivityView(
                            activities = friendActivities, availableSongs = categorizedSongs,
                            onPlaySong = { song -> viewModel.playSong(song, listOf(song)) },
                            onShareSong = { song -> viewModel.shareSong(context, song) },
                            onLikeActivity = viewModel::likeFriendActivity,
                            onSendRecommendation = viewModel::shareRecommendationToFriends
                        )
                    }
                    MainTab.EQUALIZER -> EqualizerView(equalizerState = equalizerState, isPlaying = isPlaying, onPresetSelected = { viewModel.setEqualizerPreset(it) }, onBandLevelChanged = { band, level -> viewModel.setBandLevel(band, level) }, onBassBoostChanged = { viewModel.setBassBoost(it) }, onVirtualizerChanged = { viewModel.setVirtualizer(it) }, onToggleEqualizer = { viewModel.toggleEqualizer() })
                    MainTab.ACCOUNT -> AccountHistoryScreen(session = session, history = history, allSongs = allSongs, onPlaySong = { song, _ -> viewModel.playSong(song, listOf(song)) }, onSignOut = { viewModel.signOut() }, onOpenFriends = { viewModel.setTab(MainTab.FRIENDS) })
                    MainTab.UPDATE -> UpdateScreen(updates = updates)
                }
                if (isLoading) Box(modifier = Modifier.fillMaxSize().background(LanuDarkBg.copy(alpha = 0.5f)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}, contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LanuGreen, strokeWidth = 4.dp) }
            }
        }

        AnimatedVisibility(visible = isNowPlayingExpanded && currentSong != null, enter = slideInVertically(initialOffsetY = { it }), exit = slideOutVertically(targetOffsetY = { it })) {
            currentSong?.let { song -> NowPlayingSheet(song = song, isPlaying = isPlaying, currentPositionMs = currentPositionMs, durationMs = durationMs, isShuffle = isShuffle, repeatMode = repeatMode, isFavorite = favoriteIds.contains(song.id), isDownloaded = downloadedSongIds.contains(song.id), audioQuality = audioQuality, showLyrics = showLyricsInNowPlaying, onTogglePlayPause = { viewModel.togglePlayPause() }, onNext = { viewModel.nextSong() }, onPrevious = { viewModel.prevSong() }, onSeekTo = { viewModel.seekTo(it) }, onToggleShuffle = { viewModel.toggleShuffle() }, onToggleRepeat = { viewModel.toggleRepeat() }, onToggleFavorite = { viewModel.toggleFavorite(song.id) }, onToggleDownload = { viewModel.toggleDownload(song) }, onToggleLyrics = { viewModel.toggleLyricsInNowPlaying() }, onSelectAudioQuality = { viewModel.setAudioQuality(it) }, onOpenEqualizer = { viewModel.closeNowPlaying(); viewModel.setTab(MainTab.EQUALIZER) }, onAddToPlaylist = { viewModel.openAddToPlaylist(song) }, onShare = { viewModel.shareSong(context, song) }, onClose = { viewModel.closeNowPlaying() }) }
        }

        if (showCreatePlaylistDialog) CreatePlaylistDialog(onDismiss = { viewModel.closeCreatePlaylistDialog() }, onCreate = { name, desc -> viewModel.createPlaylist(name, desc) })
        songToAddToPlaylist?.let { song -> AddToPlaylistDialog(song = song, playlists = playlists, onDismiss = { viewModel.closeAddToPlaylist() }, onSelectPlaylist = { plId -> viewModel.addSongToPlaylist(plId, song.id) }, onCreateNewPlaylist = { viewModel.closeAddToPlaylist(); viewModel.openCreatePlaylistDialog() }) }
        selectedArtist?.let { artist ->
            ArtistDetailSheet(
                artist = artist,
                songs = viewModel.getSongsForArtist(artist.id),
                onPlaySong = { song, queue -> viewModel.playSong(song, queue) },
                onSelectAlbum = { selectedAlbum = it },
                onShareArtist = { viewModel.shareArtist(context, artist) },
                onBack = { viewModel.selectArtist(null) }
            )
        }
        selectedAlbum?.let { album ->
            AlbumDetailSheet(
                album = album,
                onPlaySong = { song, queue -> viewModel.playSong(song, queue) },
                onBack = { selectedAlbum = null }
            )
        }
        selectedPlaylist?.let { playlist ->
            val playlistSongsFlow = viewModel.getSongsForPlaylist(playlist.id).collectAsStateWithLifecycle(initialValue = emptyList())
            PlaylistDetailSheet(playlist = playlist, songs = playlistSongsFlow.value, onPlaySong = { song, queue -> viewModel.playSong(song, queue) }, onRemoveSong = { songId -> viewModel.removeSongFromPlaylist(playlist.id, songId) }, onSharePlaylist = { viewModel.sharePlaylist(context, playlist) }, onDeletePlaylist = { viewModel.deletePlaylist(playlist.id) }, onBack = { viewModel.selectPlaylist(null) })
        }
    }
}
