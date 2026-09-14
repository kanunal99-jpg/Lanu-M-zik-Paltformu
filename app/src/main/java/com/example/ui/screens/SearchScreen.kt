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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.ui.components.MusicSearchBar
import com.example.ui.components.SearchFilterScope
import com.example.ui.components.SongSearchBarWithListView
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuDarkSurfaceElevated
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

@Composable
fun SearchScreen(
    searchQuery: String,
    selectedCategory: MusicCategory?,
    allSongs: List<Song>,
    onQueryChange: (String) -> Unit,
    onSelectCategory: (MusicCategory?) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    cachedSongs: List<Song> = allSongs
) {
    var searchMode by remember { mutableStateOf(0) } // 0: Room Önbellek Arama, 1: Türler & Keşfet
    var selectedScope by remember { mutableStateOf(SearchFilterScope.ALL) }
    val q = searchQuery.trim().lowercase()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LanuDarkBg)
            .statusBarsPadding()
            .testTag("search_screen")
    ) {
        // Mode switch tabs
        if (searchQuery.isEmpty() && selectedCategory == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Genel Arama", "Room Önbellek (Başlık & Sanatçı)").forEachIndexed { index, title ->
                    val isSelected = searchMode == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) LanuGreen else LanuDarkSurface)
                            .clickable { searchMode = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.Black else LanuTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (searchMode == 1 && selectedCategory == null) {
            // Dedicated SearchBar component with list view below it using Room local caching
            SongSearchBarWithListView(
                cachedSongs = cachedSongs,
                onSongClick = onPlaySong,
                initialQuery = searchQuery,
                onQueryChange = onQueryChange,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Search Screen Title
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ara & Keşfet",
                    color = LanuTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Şarkı, sanatçı veya albüm arayın",
                    color = LanuTextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // SearchBar component with leading icon, clear button, and scope chips
                MusicSearchBar(
                    query = searchQuery,
                    onQueryChange = onQueryChange,
                    placeholder = "Şarkı, sanatçı veya albüm ara...",
                    selectedScope = selectedScope,
                    onScopeChange = { selectedScope = it },
                    showScopeFilters = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val searchResults = if (searchQuery.isNotEmpty() || selectedCategory != null) {
                    allSongs.filter { song ->
                        val matchesScope = when (selectedScope) {
                            SearchFilterScope.ALL ->
                                q.isEmpty() ||
                                song.title.lowercase().contains(q) ||
                                song.artist.lowercase().contains(q) ||
                                song.album.lowercase().contains(q) ||
                                song.lyrics.any { it.text.lowercase().contains(q) }

                            SearchFilterScope.SONGS ->
                                q.isEmpty() ||
                                song.title.lowercase().contains(q) ||
                                song.lyrics.any { it.text.lowercase().contains(q) }

                            SearchFilterScope.ARTISTS ->
                                q.isEmpty() ||
                                song.artist.lowercase().contains(q)

                            SearchFilterScope.ALBUMS ->
                                q.isEmpty() ||
                                song.album.lowercase().contains(q)
                        }

                        val matchesCategory = selectedCategory == null || song.category == selectedCategory
                        matchesScope && matchesCategory
                    }
                } else emptyList()

                if (searchQuery.isNotEmpty() || selectedCategory != null) {
            // Active Filter Chip
            if (selectedCategory != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(LanuGreen.copy(alpha = 0.2f))
                        .clickable { onSelectCategory(null) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Kategori: ${selectedCategory.titleTr}  ✕",
                        color = LanuGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Search Results List Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${searchResults.size} sonuç bulundu",
                    color = LanuTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (selectedScope != SearchFilterScope.ALL) {
                    Text(
                        text = "Filtre: ${selectedScope.label}",
                        color = LanuGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults, key = { it.id }) { song ->
                    val matchingLyric = if (q.isNotEmpty()) {
                        song.lyrics.firstOrNull { it.text.lowercase().contains(q) }
                    } else null

                    val isAlbumMatch = q.isNotEmpty() && song.album.lowercase().contains(q)
                    val isArtistMatch = q.isNotEmpty() && song.artist.lowercase().contains(q)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LanuDarkSurface)
                            .clickable { onPlaySong(song, searchResults) }
                            .padding(10.dp)
                            .testTag("search_result_${song.id}")
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
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
                                text = "${song.artist} • ${song.category.titleTr}",
                                color = LanuTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )

                            // Album match badge
                            if (isAlbumMatch) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        tint = LanuGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Albüm: ${song.album}",
                                        color = LanuGreen,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            } else if (matchingLyric != null) {
                                Text(
                                    text = "♪ \"${matchingLyric.text}\"",
                                    color = LanuGreen,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            } else {
                                Text(
                                    text = "Albüm: ${song.album}",
                                    color = LanuTextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        if (song.lyrics.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Sözler",
                                tint = LanuGreen,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(16.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = LanuGreen.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Çal",
                                    tint = LanuGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Browse All Categories (Attractive 2-Column Visual Grid)
            Text(
                text = "Hepsine Göz At (Kategoriler)",
                color = LanuTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(MusicCategory.values()) { category ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(category.primaryColor, category.secondaryColor)
                                )
                            )
                            .clickable { onSelectCategory(category) }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = category.titleTr,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                }
            }
        }
    }
}
}
}
