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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DownloadedSongEntity
import com.example.data.PlaylistEntity
import com.example.model.Song
import com.example.ui.components.MusicSearchBar
import com.example.ui.components.SearchFilterScope
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuDarkSurfaceElevated
import com.example.ui.theme.LanuEmerald
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuPurple
import com.example.ui.theme.LanuRose
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

enum class LibraryTab {
    PLAYLISTS,
    DOWNLOADS,
    FAVORITES
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
    var selectedTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    var searchQuery by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf(SearchFilterScope.ALL) }

    val downloadedSongs = remember(downloadedEntities, allSongs) {
        val dlIds = downloadedEntities.map { it.songId }.toSet()
        allSongs.filter { dlIds.contains(it.id) }
    }

    val favoriteSongs = remember(favoriteIds, allSongs) {
        allSongs.filter { favoriteIds.contains(it.id) }
    }

    val q = searchQuery.trim().lowercase()

    val filteredPlaylists = remember(playlists, q) {
        if (q.isEmpty()) playlists
        else playlists.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }

    val filteredDownloadedSongs = remember(downloadedSongs, q, searchScope) {
        if (q.isEmpty()) downloadedSongs
        else downloadedSongs.filter { song ->
            when (searchScope) {
                SearchFilterScope.ALL ->
                    song.title.lowercase().contains(q) ||
                    song.artist.lowercase().contains(q) ||
                    song.album.lowercase().contains(q)
                SearchFilterScope.SONGS ->
                    song.title.lowercase().contains(q)
                SearchFilterScope.ARTISTS ->
                    song.artist.lowercase().contains(q)
                SearchFilterScope.ALBUMS ->
                    song.album.lowercase().contains(q)
            }
        }
    }

    val filteredFavoriteSongs = remember(favoriteSongs, q, searchScope) {
        if (q.isEmpty()) favoriteSongs
        else favoriteSongs.filter { song ->
            when (searchScope) {
                SearchFilterScope.ALL ->
                    song.title.lowercase().contains(q) ||
                    song.artist.lowercase().contains(q) ||
                    song.album.lowercase().contains(q)
                SearchFilterScope.SONGS ->
                    song.title.lowercase().contains(q)
                SearchFilterScope.ARTISTS ->
                    song.artist.lowercase().contains(q)
                SearchFilterScope.ALBUMS ->
                    song.album.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LanuDarkBg)
            .statusBarsPadding()
            .testTag("library_screen")
    ) {
        // Library Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = "Arşivim",
                    color = LanuTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Çalma Listeleri & Çevrimdışı İndirilenler",
                    color = LanuTextMuted,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onCreatePlaylist,
                colors = ButtonDefaults.buttonColors(containerColor = LanuGreen),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("button_new_playlist")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Yeni Liste", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Sub-tabs row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            val tabs = listOf(
                LibraryTab.PLAYLISTS to "Çalma Listeleri (${playlists.size})",
                LibraryTab.DOWNLOADS to "Çevrimdışı (${downloadedSongs.size})",
                LibraryTab.FAVORITES to "Beğenilenler (${favoriteSongs.size})"
            )

            tabs.forEach { (tab, title) ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) LanuGreen else LanuDarkSurfaceElevated)
                        .border(
                            1.dp,
                            if (isSelected) LanuGreen else LanuDarkBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.Black else LanuTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // SearchBar within the library
        MusicSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = when (selectedTab) {
                LibraryTab.PLAYLISTS -> "Listelerde ara..."
                LibraryTab.DOWNLOADS -> "İndirilenlerde şarkı, sanatçı veya albüm ara..."
                LibraryTab.FAVORITES -> "Beğenilenlerde şarkı, sanatçı veya albüm ara..."
            },
            selectedScope = searchScope,
            onScopeChange = { searchScope = it },
            showScopeFilters = selectedTab != LibraryTab.PLAYLISTS,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        when (selectedTab) {
            LibraryTab.PLAYLISTS -> {
                if (filteredPlaylists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (q.isNotEmpty()) "\"$searchQuery\" için çalma listesi bulunamadı."
                            else "Henüz çalma listesi oluşturulmadı.",
                            color = LanuTextMuted,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp)
                    ) {
                        items(filteredPlaylists, key = { it.id }) { pl ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LanuDarkSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, LanuDarkBorder, RoundedCornerShape(14.dp))
                                    .clickable { onSelectPlaylist(pl) }
                                    .testTag("playlist_item_${pl.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(LanuPurple.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = LanuPurple,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            color = LanuTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = pl.description.ifBlank { "Özel Çalma Listesi" },
                                            color = LanuTextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.PlaylistPlay,
                                        contentDescription = null,
                                        tint = LanuGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LibraryTab.DOWNLOADS -> {
                // Offline status badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = LanuDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .border(1.dp, LanuDarkBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflinePin,
                            contentDescription = null,
                            tint = LanuGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Çevrimdışı Dinleme Modu Hazır",
                                color = LanuGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "İnternet bağlantınız olmasa dahi kesintisiz dinleyebilirsiniz.",
                                color = LanuTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (filteredDownloadedSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (q.isNotEmpty()) "\"$searchQuery\" için indirilen şarkı bulunamadı."
                            else "Henüz çevrimdışı parça indirmediniz.\nŞarkı detayından indirme simgesine dokunarak kaydedebilirsiniz.",
                            color = LanuTextMuted,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp)
                    ) {
                        items(filteredDownloadedSongs, key = { it.id }) { song ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LanuDarkSurface)
                                    .clickable { onPlaySong(song, filteredDownloadedSongs) }
                                    .padding(10.dp)
                            ) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = LanuTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${song.artist} • ${song.album}",
                                        color = LanuTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = "İndirildi",
                                    tint = LanuGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            LibraryTab.FAVORITES -> {
                if (filteredFavoriteSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (q.isNotEmpty()) "\"$searchQuery\" için beğenilen şarkı bulunamadı."
                            else "Henüz beğendiğiniz şarkı yok.\nŞarkıların yanındaki kalp simgesine dokunarak buraya ekleyebilirsiniz.",
                            color = LanuTextMuted,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp)
                    ) {
                        items(filteredFavoriteSongs, key = { it.id }) { song ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LanuDarkSurface)
                                    .clickable { onPlaySong(song, filteredFavoriteSongs) }
                                    .padding(10.dp)
                            ) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = LanuTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${song.artist} • ${song.album}",
                                        color = LanuTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Beğenildi",
                                    tint = LanuRose,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
