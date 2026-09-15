package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DownloadedSongEntity
import com.example.data.HistoryRecord
import com.example.data.PlaylistEntity
import com.example.data.RepositoryRegistry
import com.example.model.Song
import com.example.ui.components.MusicSearchBar
import com.example.ui.components.SearchFilterScope
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuDarkSurfaceElevated
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuPurple
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary
import kotlinx.coroutines.launch

enum class LibraryTab {
    PLAYLISTS,
    DOWNLOADS,
    FAVORITES,
    HISTORY
}

@Composable
fun LibraryScreen(
    playlists: List<PlaylistEntity>,
    downloadedEntities: List<DownloadedSongEntity>,
    favoriteIds: Set<String>,
    allSongs: List<Song>,
    onSelectPlaylist: (PlaylistEntity) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = RepositoryRegistry.repository
    val history by (repository?.history ?: kotlinx.coroutines.flow.flowOf(emptyList<HistoryRecord>()))
        .collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    var searchQuery by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf(SearchFilterScope.ALL) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val downloadedSongs = remember(downloadedEntities, allSongs) {
        val ids = downloadedEntities.map { it.songId }.toSet()
        allSongs.filter { it.id in ids }
    }
    val favoriteSongs = remember(favoriteIds, allSongs) { allSongs.filter { it.id in favoriteIds } }
    val historySongs = remember(history, allSongs) {
        val songMap = allSongs.associateBy { it.id }
        history.mapNotNull { record -> songMap[record.songId] }
    }
    val q = searchQuery.trim().lowercase()

    fun filterSongs(songs: List<Song>): List<Song> = if (q.isEmpty()) songs else songs.filter { song ->
        when (searchScope) {
            SearchFilterScope.ALL -> listOf(song.title, song.artist, song.album).any { it.lowercase().contains(q) }
            SearchFilterScope.SONGS -> song.title.lowercase().contains(q)
            SearchFilterScope.ARTISTS -> song.artist.lowercase().contains(q)
            SearchFilterScope.ALBUMS -> song.album.lowercase().contains(q)
        }
    }

    val filteredPlaylists = remember(playlists, q) {
        if (q.isEmpty()) playlists else playlists.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }
    val filteredDownloadedSongs = remember(downloadedSongs, q, searchScope) { filterSongs(downloadedSongs) }
    val filteredFavoriteSongs = remember(favoriteSongs, q, searchScope) { filterSongs(favoriteSongs) }
    val filteredHistorySongs = remember(historySongs, q, searchScope) { filterSongs(historySongs) }

    Column(
        modifier = modifier.fillMaxSize().background(LanuDarkBg).statusBarsPadding().testTag("library_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column {
                Text("Arşivim", color = LanuTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Çalma listeleri, çevrimdışı, favoriler ve geçmiş", color = LanuTextMuted, fontSize = 12.sp)
            }
            Button(
                onClick = onCreatePlaylist,
                colors = ButtonDefaults.buttonColors(containerColor = LanuGreen),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("button_new_playlist")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Yeni Liste", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            val tabs = listOf(
                LibraryTab.PLAYLISTS to "Listeler (${playlists.size})",
                LibraryTab.DOWNLOADS to "Çevrimdışı (${downloadedSongs.size})",
                LibraryTab.FAVORITES to "Favoriler (${favoriteSongs.size})",
                LibraryTab.HISTORY to "Geçmiş (${history.size})"
            )
            tabs.forEach { (tab, title) ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(18.dp))
                        .background(if (selected) LanuGreen else LanuDarkSurfaceElevated)
                        .border(1.dp, if (selected) LanuGreen else LanuDarkBorder, RoundedCornerShape(18.dp))
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(title, color = if (selected) Color.Black else LanuTextPrimary, fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        MusicSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = when (selectedTab) {
                LibraryTab.PLAYLISTS -> "Listelerde ara..."
                LibraryTab.DOWNLOADS -> "İndirilenlerde ara..."
                LibraryTab.FAVORITES -> "Favorilerde ara..."
                LibraryTab.HISTORY -> "Geçmişte ara..."
            },
            selectedScope = searchScope,
            onScopeChange = { searchScope = it },
            showScopeFilters = selectedTab != LibraryTab.PLAYLISTS,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
        )

        when (selectedTab) {
            LibraryTab.PLAYLISTS -> {
                if (filteredPlaylists.isEmpty()) {
                    EmptyArchive("Henüz çalma listesi bulunmuyor.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().padding(bottom = 120.dp)) {
                        items(filteredPlaylists, key = { it.id }) { pl ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LanuDarkSurface)
                                    .border(1.dp, LanuDarkBorder, RoundedCornerShape(14.dp)).clickable { onSelectPlaylist(pl) }.padding(12.dp)
                            ) {
                                Box(Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(LanuPurple.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = LanuPurple, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(pl.name, color = LanuTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(pl.description.ifBlank { "Özel çalma listesi" }, color = LanuTextSecondary, fontSize = 12.sp, maxLines = 1)
                                    Text("${pl.totalSongsCount} parça", color = LanuTextMuted, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = LanuGreen, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
            LibraryTab.DOWNLOADS -> {
                ArchiveSongs(
                    songs = filteredDownloadedSongs,
                    emptyMessage = "Henüz çevrimdışı parça yok.",
                    icon = Icons.Default.OfflinePin,
                    onPlaySong = { onPlaySong(it, filteredDownloadedSongs) }
                )
            }
            LibraryTab.FAVORITES -> {
                ArchiveSongs(
                    songs = filteredFavoriteSongs,
                    emptyMessage = "Henüz beğenilen parça yok.",
                    icon = Icons.Default.LibraryMusic,
                    onPlaySong = { onPlaySong(it, filteredFavoriteSongs) }
                )
            }
            LibraryTab.HISTORY -> {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = LanuGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Gerçek oynatma geçmişi", color = LanuTextSecondary, fontSize = 12.sp)
                        }
                        if (history.isNotEmpty()) {
                            TextButton(onClick = { showClearHistoryDialog = true }, modifier = Modifier.testTag("button_clear_history")) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = LanuTextMuted, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Temizle", color = LanuTextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                    ArchiveSongs(
                        songs = filteredHistorySongs,
                        emptyMessage = if (q.isBlank()) "Henüz gerçekten oynatılmış parça yok." else "Aramanızla eşleşen geçmiş kaydı yok.",
                        icon = Icons.Default.History,
                        onPlaySong = { onPlaySong(it, filteredHistorySongs) }
                    )
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Dinleme geçmişi temizlensin mi?", color = LanuTextPrimary) },
            text = { Text("Bu işlem yalnızca aktif yerel LANU hesabının gerçek geçmişini siler.", color = LanuTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showClearHistoryDialog = false
                    scope.launch { repository?.clearHistory() }
                }) { Text("Temizle", color = LanuGreen, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showClearHistoryDialog = false }) { Text("İptal", color = LanuTextSecondary) } },
            containerColor = LanuDarkSurface
        )
    }
}

@Composable
private fun ArchiveSongs(
    songs: List<Song>,
    emptyMessage: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onPlaySong: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyArchive(emptyMessage)
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().padding(bottom = 120.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LanuDarkSurface)
                    .clickable { onPlaySong(song) }.padding(10.dp)
            ) {
                AsyncImage(song.coverUrl, contentDescription = song.title, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, color = LanuTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${song.artist} • ${song.album}", color = LanuTextSecondary, fontSize = 12.sp, maxLines = 1)
                }
                IconButton(onClick = { onPlaySong(song) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Çal", tint = LanuGreen)
                }
            }
        }
    }
}

@Composable
private fun EmptyArchive(message: String) {
    Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = LanuTextMuted, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, color = LanuTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}
