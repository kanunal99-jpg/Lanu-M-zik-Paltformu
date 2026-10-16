package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Artist
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.PlaybackKind
import com.example.model.playbackKind
import com.example.model.playbackBadge
import com.example.model.preferFullStreams
import com.example.ui.components.LanuYouTubePlayerDialog
import com.example.ui.components.MusicSearchBar
import com.example.ui.components.SearchFilterScope
import com.example.ui.components.SongSearchBarWithListView
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

@Composable
fun SearchScreen(searchQuery: String, selectedCategory: MusicCategory?, allSongs: List<Song>, onQueryChange: (String) -> Unit, onSelectCategory: (MusicCategory?) -> Unit, onPlaySong: (Song, List<Song>) -> Unit, modifier: Modifier = Modifier, cachedSongs: List<Song> = allSongs, remoteArtists: List<Artist> = emptyList(), onSelectArtist: (Artist) -> Unit = {}, isCatalogExpanding: Boolean = false, onExpandCatalog: () -> Unit = {}, searchIsLoading: Boolean = false, searchHasMore: Boolean = false, searchError: String? = null, onLoadMoreResults: () -> Unit = {}, onOpenYouTube: () -> Unit = {}) {
    var selectedScope by remember { mutableStateOf(SearchFilterScope.ALL) }
    var browseAll by remember { mutableStateOf(false) }
    var fullStreamsOnly by remember { mutableStateOf(false) }
    var showYouTubeDialog by remember { mutableStateOf(false) }
    val q = searchQuery.trim().lowercase()
    if (showYouTubeDialog) LanuYouTubePlayerDialog(onDismiss = { showYouTubeDialog = false })
    Column(modifier = modifier.fillMaxSize().background(LanuDarkBg).statusBarsPadding().testTag("search_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Ara & Keşfet", color = LanuTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = "YouTube", color = LanuGreen, fontSize = 13.sp,
                        modifier = Modifier.clickable { onOpenYouTube(); showYouTubeDialog = true }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .testTag("open_youtube_embedded_player"))
                }
                Text(text = "Şarkı, sanatçı veya albüm arayın • ${allSongs.size} indekslenmiş kayıt", color = LanuTextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(14.dp))
                MusicSearchBar(query = searchQuery, onQueryChange = onQueryChange, placeholder = "Şarkı, sanatçı veya albüm ara...", selectedScope = selectedScope, onScopeChange = { selectedScope = it }, showScopeFilters = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                val searchResults = if (searchQuery.isNotEmpty() || selectedCategory != null) {
                    allSongs.filter { song ->
                        val songTitle = song.title.lowercase(); val songArtist = song.artist.lowercase(); val songAlbum = song.album.lowercase()
                        val matchesScope = when (selectedScope) {
                            SearchFilterScope.ALL -> q.isEmpty() || songTitle.contains(q) || songArtist.contains(q) || songAlbum.contains(q) || song.lyrics.any { it.text.lowercase().contains(q) }
                            SearchFilterScope.SONGS -> q.isEmpty() || songTitle.contains(q) || songArtist.contains(q) || song.lyrics.any { it.text.lowercase().contains(q) }
                            SearchFilterScope.ARTISTS -> q.isEmpty() || songArtist.contains(q)
                            SearchFilterScope.ALBUMS -> q.isEmpty() || songAlbum.contains(q)
                        }
                        matchesScope && (selectedCategory == null || song.category == selectedCategory)
                    }
                } else emptyList()
                val visibleResults = searchResults.filter { !fullStreamsOnly || it.playbackKind() == PlaybackKind.FULL }.preferFullStreams()
                if (searchQuery.isNotEmpty() || selectedCategory != null) {
                    if (selectedCategory != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(LanuGreen.copy(alpha = 0.2f)).clickable { onSelectCategory(null) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(text = "Kategori: ${selectedCategory.titleTr}  ✕", color = LanuGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (selectedScope == SearchFilterScope.ARTISTS && q.isNotBlank()) {
                        Text(text = "${remoteArtists.size} doğrulanmış sanatçı", color = LanuTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 120.dp), modifier = Modifier.fillMaxSize()) {
                            items(remoteArtists, key = { it.id }) { artist -> ArtistSearchRow(artist = artist, onClick = { onSelectArtist(artist) }) }
                        }
                    } else {
                        if (selectedScope == SearchFilterScope.ALL && q.isNotBlank() && remoteArtists.isNotEmpty()) {
                            Text(text = "Sanatçılar", color = LanuTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                                remoteArtists.take(8).forEach { artist -> ArtistSearchRow(artist = artist, onClick = { onSelectArtist(artist) }) }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${visibleResults.size} kayıt • ${searchResults.size} indekslendi",
                                color = LanuTextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (fullStreamsOnly) "✓ Yalnız tam şarkı" else "Yalnız tam şarkı",
                                color = LanuGreen,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { fullStreamsOnly = !fullStreamsOnly }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .testTag("full_stream_filter")
                            )
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 120.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                            if (searchIsLoading) item { Text("Yeni şarkılar getiriliyor…", color = LanuTextSecondary, fontSize = 12.sp) }
                            if (visibleResults.isEmpty() && !searchIsLoading) item { Text(if (fullStreamsOnly) "Bu aramada izinli tam kayıt bulunamadı. 30 sn örnekleri görmek için filtreyi kaldırın." else "Bu aramada veya kategoride henüz kayıt yok. Bu, dünyada böyle bir şarkı olmadığı anlamına gelmez.", color = LanuTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 20.dp)) }
                            if (searchError != null) item { Text(searchError, color = LanuTextSecondary, fontSize = 12.sp) }
                            items(visibleResults, key = { it.id }) { song ->
                                val matchingLyric = if (q.isNotEmpty()) song.lyrics.firstOrNull { it.text.lowercase().contains(q) } else null
                                val isAlbumMatch = q.isNotEmpty() && song.album.lowercase().contains(q)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LanuDarkSurface).clickable { onPlaySong(song, visibleResults) }.padding(10.dp).testTag("search_result_${song.id}")) {
                                    AsyncImage(model = song.coverUrl, contentDescription = song.title, contentScale = ContentScale.Crop, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = song.title, color = LanuTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(text = "${song.artist} • ${song.category.titleTr}", color = LanuTextSecondary, fontSize = 12.sp, maxLines = 1)
                                        if (isAlbumMatch) Text(text = "Albüm: ${song.album}", color = LanuGreen, fontSize = 11.sp, maxLines = 1)
                                        else if (matchingLyric != null) Text(text = "♪ \"${matchingLyric.text}\"", color = LanuGreen, fontSize = 11.sp, maxLines = 1)
                                        else Text(text = "Albüm: ${song.album}", color = LanuTextMuted, fontSize = 11.sp, maxLines = 1)
                                        Text(text = song.playbackBadge(), color = if (song.playbackKind() == PlaybackKind.FULL) LanuGreen else LanuTextSecondary, fontSize = 11.sp)
                                    }
                                    if (song.lyrics.isNotEmpty()) Icon(imageVector = Icons.Default.Mic, contentDescription = "Sözler", tint = LanuGreen, modifier = Modifier.padding(end = 8.dp).size(16.dp))
                                    Surface(shape = CircleShape, color = LanuGreen.copy(alpha = 0.2f), modifier = Modifier.size(34.dp)) { Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Çal", tint = LanuGreen, modifier = Modifier.size(18.dp)) } }
                                }
                            }
                            if (q.isNotBlank() && searchHasMore) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(LanuGreen.copy(alpha = 0.12f))
                                        .clickable(enabled = !searchIsLoading) { onLoadMoreResults() }
                                        .padding(14.dp).testTag("search_load_more"), contentAlignment = Alignment.Center) {
                                        Text("Daha fazla şarkı yükle", color = LanuGreen, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                } else if (browseAll) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Tüm Şarkılar", color = LanuTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${allSongs.size} gerçek katalog sonucu/cihaz parçası indekslendi", color = LanuTextSecondary, fontSize = 12.sp)
                        }
                        Text(text = "Kategoriler", color = LanuGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { browseAll = false })
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LanuGreen.copy(alpha = 0.12f)).clickable(enabled = !isCatalogExpanding) { onExpandCatalog() }.padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            if (isCatalogExpanding) {
                                CircularProgressIndicator(color = LanuGreen, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Gerçek katalog genişletiliyor…", color = LanuGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Daha fazla gerçek katalog yükle", color = LanuGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 120.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(allSongs.preferFullStreams(), key = { it.id }) { song ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LanuDarkSurface).clickable { onPlaySong(song, allSongs) }.padding(10.dp)) {
                                AsyncImage(model = song.coverUrl, contentDescription = song.title, contentScale = ContentScale.Crop, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, color = LanuTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("${song.artist} • ${song.album}", color = LanuTextSecondary, fontSize = 12.sp, maxLines = 1)
                                    Text("${song.category.titleTr} • ${song.playbackBadge()}", color = LanuTextMuted, fontSize = 11.sp, maxLines = 1)
                                }
                                Surface(shape = CircleShape, color = LanuGreen.copy(alpha = 0.2f), modifier = Modifier.size(34.dp)) { Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Çal", tint = LanuGreen, modifier = Modifier.size(18.dp)) } }
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(text = "Türlere Göre Keşfet", color = LanuTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Tüm kayıtlar (${allSongs.size})", color = LanuGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { browseAll = true })
                    }
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(LanuDarkSurface)
                        .clickable(enabled = !isCatalogExpanding) { onExpandCatalog() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("expand_verified_catalog")) {
                        Text(
                            text = if (isCatalogExpanding) "İzinli kaynaklar taranıyor…" else "Kataloğu genişlet  ↗",
                            color = LanuGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 120.dp), modifier = Modifier.fillMaxSize()) {
                        items(MusicCategory.values()) { category ->
                            Box(modifier = Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(colors = listOf(category.primaryColor.copy(alpha = 0.30f), LanuDarkSurface))).clickable { browseAll = false; onSelectCategory(category) }.padding(14.dp).testTag("genre_${category.name.lowercase()}")) {
                                Text(text = category.titleTr, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart))
                                val count = allSongs.count { it.category == category }
                                Text(text = if (count == 0) "Henüz indekslenmedi" else "$count parça", color = LanuTextSecondary, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomStart))
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun ArtistSearchRow(artist: Artist, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LanuDarkSurface).clickable(onClick = onClick).padding(10.dp).testTag("artist_search_result_${artist.id}")) {
        AsyncImage(model = artist.imageUrl, contentDescription = artist.name, contentScale = ContentScale.Crop, modifier = Modifier.size(54.dp).clip(CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = artist.name, color = LanuTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = artist.genre.ifBlank { "Doğrulanmış katalog sanatçısı" }, color = LanuTextSecondary, fontSize = 12.sp, maxLines = 1)
        }
        Icon(imageVector = Icons.Default.Person, contentDescription = "Sanatçı", tint = LanuGreen, modifier = Modifier.size(20.dp))
    }
}
