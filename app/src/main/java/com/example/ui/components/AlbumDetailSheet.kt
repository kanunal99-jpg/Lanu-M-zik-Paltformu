package com.example.ui.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Album
import com.example.model.Song
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

@Composable
fun AlbumDetailSheet(
    album: Album,
    onPlaySong: (Song, List<Song>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs = album.songs.distinctBy { it.id }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LanuDarkBg)
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
            }
            Text(album.title, color = LanuTextPrimary, fontSize = 20.sp)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            AsyncImage(
                model = album.coverUrl,
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(190.dp).clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text(album.artist, color = LanuTextPrimary, fontSize = 18.sp)
            Text(
                text = listOf(album.releaseYear.takeIf { it > 0 }?.toString(), album.genre.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" • "),
                color = LanuTextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))
            if (songs.isNotEmpty()) {
                Button(
                    onClick = { onPlaySong(songs.first(), songs) },
                    colors = ButtonDefaults.buttonColors(containerColor = LanuGreen),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Albümü Çal", color = Color.Black)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(songs, key = { it.id }) { song ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LanuDarkSurface)
                        .clickable { onPlaySong(song, songs) }
                        .padding(10.dp)
                ) {
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, color = LanuTextPrimary, fontSize = 14.sp, maxLines = 1)
                        Text(song.artist, color = LanuTextSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Çal", tint = LanuGreen)
                }
            }
        }
    }
}
