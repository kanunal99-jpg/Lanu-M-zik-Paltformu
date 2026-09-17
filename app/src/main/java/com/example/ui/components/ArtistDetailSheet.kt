package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.Album
import com.example.model.Artist
import com.example.model.Song
import com.example.ui.theme.LanuCyan
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

@Composable
fun ArtistDetailSheet(
    artist: Artist,
    songs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onSelectAlbum: (Album) -> Unit,
    onShareArtist: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val verifiedSongs = songs.filter { it.artistId == artist.id }.distinctBy { it.id }
    val albums = verifiedSongs
        .groupBy { it.album.trim().ifBlank { "Single" } }
        .map { (title, tracks) ->
            val first = tracks.first()
            Album(
                id = "derived_album:${artist.id}:$title",
                title = title,
                artist = artist.name,
                artistId = artist.id,
                coverUrl = first.coverUrl,
                releaseYear = tracks.map { it.releaseYear }.firstOrNull { it > 0 } ?: 0,
                genre = first.category.titleTr,
                songs = tracks
            )
        }
        .sortedWith(compareByDescending<Album> { it.releaseYear }.thenBy { it.title.lowercase() })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LanuDarkBg)
            .statusBarsPadding()
            .testTag("artist_detail_screen")
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            AsyncImage(model = artist.imageUrl, contentDescription = artist.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, LanuDarkBg))))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).align(Alignment.TopCenter)
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White) }
                IconButton(onClick = onShareArtist) { Icon(Icons.Default.Share, contentDescription = "Sanatçıyı Paylaş", tint = Color.White) }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Doğrulanmış", tint = LanuCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Doğrulanmış Sanatçı", color = LanuCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(artist.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("${artist.monthlyListeners} aylık dinleyici • ${artist.genre}", color = LanuTextSecondary, fontSize = 13.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            if (verifiedSongs.isNotEmpty()) {
                Button(
                    onClick = { onPlaySong(verifiedSongs.first(), verifiedSongs) },
                    colors = ButtonDefaults.buttonColors(containerColor = LanuGreen),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tümünü Çal", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (artist.bio.isNotBlank()) {
            Text(artist.bio, color = LanuTextSecondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)
        ) {
            if (albums.isNotEmpty()) {
                item {
                    Text("Albümler / Yayınlar", color = LanuTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
                }
                items(albums, key = { it.id }) { album ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LanuDarkSurface)
                            .clickable { onSelectAlbum(album) }
                            .padding(10.dp)
                    ) {
                        AsyncImage(model = album.coverUrl, contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.size(58.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(album.title, color = LanuTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            val meta = listOf(album.releaseYear.takeIf { it > 0 }?.toString(), "${album.songs.size} parça").filterNotNull().joinToString(" • ")
                            Text(meta, color = LanuTextSecondary, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "Albümü aç", tint = LanuGreen)
                    }
                }
            }

            item {
                Text("Parçalar", color = LanuTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
            }
            items(verifiedSongs, key = { it.id }) { song ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(LanuDarkSurface).clickable { onPlaySong(song, verifiedSongs) }.padding(10.dp)
                ) {
                    AsyncImage(model = song.coverUrl, contentDescription = song.title, contentScale = ContentScale.Crop, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = LanuTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${song.album}${song.releaseYear.takeIf { it > 0 }?.let { " ($it)" } ?: ""}", color = LanuTextSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                    Surface(shape = CircleShape, color = LanuGreen.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, contentDescription = "Çal", tint = LanuGreen, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }
}
