package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.Artist
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

@Composable
fun HomeScreen(
    songs: List<Song>,
    artists: List<Artist>,
    newReleaseAlert: Song?,
    onPlaySong: (Song, List<Song>) -> Unit,
    onSelectArtist: (Artist) -> Unit,
    onCategoryClick: (MusicCategory) -> Unit,
    onSimulateNewRelease: () -> Unit,
    onDismissNewReleaseAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actualArtists = songs
        .filter { it.artistId.isNotBlank() && it.artist.isNotBlank() }
        .distinctBy { it.artistId }
        .map {
            Artist(
                id = it.artistId,
                name = it.artist,
                genre = it.category.titleTr,
                bio = "Yerel veya doğrulanmış katalog verisi",
                imageUrl = it.coverUrl,
                monthlyListeners = ""
            )
        }
    val categories = songs.map { it.category }.distinct()

    Column(
        modifier = modifier.fillMaxSize().background(LanuDarkBg).statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 120.dp).testTag("home_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Surface(color = LanuGreen, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Headphones, contentDescription = null, tint = Color.Black, modifier = Modifier.size(23.dp)) }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("LANU MÜZİK", color = LanuTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text("Yerel ve doğrulanmış içerik", color = LanuTextMuted, fontSize = 11.sp)
            }
        }

        if (songs.isEmpty()) {
            EmptyHomeState()
        } else {
            HomeSectionTitle("Kitaplığındaki Müzikler", "Cihazında veya doğrulanmış yerel katalogda bulunan parçalar")
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(songs.take(20), key = { it.id }) { song -> LocalSongCard(song, { onPlaySong(song, songs) }) }
            }

            if (categories.isNotEmpty()) {
                HomeSectionTitle("Kategoriler", "Mevcut parçalarından oluşturulan filtreler")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                ) {
                    categories.forEach { category ->
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, LanuDarkBorder, RoundedCornerShape(20.dp)).clickable { onCategoryClick(category) }.padding(horizontal = 14.dp, vertical = 9.dp)
                        ) { Text(category.titleTr, color = LanuTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            if (actualArtists.isNotEmpty()) {
                HomeSectionTitle("Sanatçılar", "Mevcut müziklerinden çıkarılan sanatçı listesi")
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(actualArtists, key = { it.id }) { artist ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp).clickable { onSelectArtist(artist) }) {
                            if (artist.imageUrl.isBlank()) {
                                Surface(color = LanuDarkSurface, shape = RoundedCornerShape(48.dp), modifier = Modifier.size(86.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = LanuGreen, modifier = Modifier.size(32.dp)) }
                                }
                            } else {
                                AsyncImage(artist.imageUrl, contentDescription = artist.name, contentScale = ContentScale.Crop, modifier = Modifier.size(86.dp).clip(RoundedCornerShape(48.dp)))
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(artist.name, color = LanuTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHomeState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 72.dp)) {
        Surface(color = LanuDarkSurface, shape = RoundedCornerShape(24.dp), modifier = Modifier.size(92.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LibraryMusic, null, tint = LanuGreen, modifier = Modifier.size(44.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        Text("Henüz müzik bulunamadı", color = LanuTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Cihazınızdaki müzikleri taradığınızda veya doğrulanmış bir katalog kaynağı hazır olduğunda parçalar burada görünecek.", color = LanuTextSecondary, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun HomeSectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(title, color = LanuTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = LanuTextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun LocalSongCard(song: Song, onClick: () -> Unit) {
    Column(modifier = Modifier.width(140.dp).clip(RoundedCornerShape(14.dp)).clickable { onClick() }) {
        Box(modifier = Modifier.size(140.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, LanuDarkBorder, RoundedCornerShape(14.dp))) {
            if (song.coverUrl.isBlank()) {
                Box(Modifier.fillMaxSize().background(LanuDarkSurface), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = LanuGreen, modifier = Modifier.size(42.dp)) }
            } else {
                AsyncImage(song.coverUrl, contentDescription = song.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Surface(color = LanuGreen, shape = RoundedCornerShape(50.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp).size(34.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, "Oynat", tint = Color.Black, modifier = Modifier.size(20.dp)) }
            }
        }
        Text(song.title, color = LanuTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
        Text(song.artist, color = LanuTextSecondary, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}
