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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.PlaylistEntity
import com.example.data.RepositoryRegistry
import com.example.model.Song
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuDarkSurfaceElevated
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuPurple
import com.example.ui.theme.LanuRose
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary
import kotlinx.coroutines.launch

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LanuDarkSurface,
        title = { Text("Yeni Çalma Listesi Oluştur", color = LanuTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Çalma Listesi Adı") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LanuGreen,
                        unfocusedBorderColor = LanuDarkBorder,
                        focusedTextColor = LanuTextPrimary,
                        unfocusedTextColor = LanuTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_playlist_name")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama (İsteğe bağlı)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LanuGreen,
                        unfocusedBorderColor = LanuDarkBorder,
                        focusedTextColor = LanuTextPrimary,
                        unfocusedTextColor = LanuTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name.trim(), description.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = LanuGreen),
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("button_confirm_create_playlist")
            ) { Text("Oluştur", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal", color = LanuTextSecondary) } }
    )
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (playlistId: String) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LanuDarkSurface,
        title = { Text("Çalma Listesine Ekle", color = LanuTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text("'${song.title}' parçasını bir listeye ekleyin:", color = LanuTextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onCreateNewPlaylist,
                    colors = ButtonDefaults.buttonColors(containerColor = LanuDarkSurfaceElevated),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LanuGreen, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = LanuGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("+ Yeni Liste Oluştur", color = LanuGreen, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                if (playlists.isEmpty()) {
                    Text("Henüz bir çalma listeniz yok.", color = LanuTextMuted, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    LazyColumn(Modifier.height(180.dp)) {
                        items(playlists, key = { it.id }) { pl ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { onSelectPlaylist(pl.id) }.padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = LanuTextSecondary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(pl.name, color = LanuTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kapat", color = LanuTextSecondary) } }
    )
}

@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LanuDarkSurface,
        title = { Text("Çalma Listesini Yeniden Adlandır", color = LanuTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Liste adı") },
                modifier = Modifier.fillMaxWidth().testTag("input_rename_playlist"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LanuGreen,
                    unfocusedBorderColor = LanuDarkBorder,
                    focusedTextColor = LanuTextPrimary,
                    unfocusedTextColor = LanuTextPrimary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onRename(name.trim()) },
                enabled = name.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = LanuGreen),
                modifier = Modifier.testTag("button_confirm_rename_playlist")
            ) { Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal", color = LanuTextSecondary) } }
    )
}

@Composable
fun PlaylistDetailSheet(
    playlist: PlaylistEntity,
    songs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onRemoveSong: (String) -> Unit,
    onSharePlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRename by remember(playlist.id, playlist.name) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val repository = RepositoryRegistry.repository

    Column(Modifier.fillMaxSize().background(LanuDarkBg).statusBarsPadding().testTag("playlist_detail_screen")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri", tint = LanuTextPrimary) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showRename = true }) { Icon(Icons.Default.Edit, "Yeniden adlandır", tint = LanuTextPrimary) }
                IconButton(onClick = onSharePlaylist) { Icon(Icons.Default.Share, "Paylaş", tint = LanuTextPrimary) }
                IconButton(onClick = onDeletePlaylist) { Icon(Icons.Default.Delete, "Sil", tint = LanuRose) }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier.size(110.dp).clip(RoundedCornerShape(16.dp)).background(LanuPurple.copy(alpha = 0.3f)).border(1.dp, LanuDarkBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.PlaylistPlay, null, tint = LanuPurple, modifier = Modifier.size(54.dp)) }
            Spacer(Modifier.height(14.dp))
            Text(playlist.name, color = LanuTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (playlist.description.isNotBlank()) Text(playlist.description, color = LanuTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Text("${songs.size} parça", color = LanuTextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            if (songs.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onPlaySong(songs.first(), songs) },
                    colors = ButtonDefaults.buttonColors(containerColor = LanuGreen),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tümünü Çal", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QueueMusic, null, tint = LanuTextMuted, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Bu çalma listesi henüz boş.", color = LanuTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Şarkı eklediğinizde burada görünecek.", color = LanuTextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(LanuDarkSurface).clickable { onPlaySong(song, songs) }.padding(10.dp)
                    ) {
                        AsyncImage(song.coverUrl, song.title, ContentScale.Crop, Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = LanuTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(song.artist, color = LanuTextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(
                            enabled = index > 0,
                            onClick = { repository?.let { repo -> scope.launch { repo.reorderPlaylist(playlist.id, index, index - 1) } } }
                        ) { Icon(Icons.Default.KeyboardArrowUp, "Yukarı taşı", tint = if (index > 0) LanuTextPrimary else LanuTextMuted) }
                        IconButton(
                            enabled = index < songs.lastIndex,
                            onClick = { repository?.let { repo -> scope.launch { repo.reorderPlaylist(playlist.id, index, index + 1) } } }
                        ) { Icon(Icons.Default.KeyboardArrowDown, "Aşağı taşı", tint = if (index < songs.lastIndex) LanuTextPrimary else LanuTextMuted) }
                        IconButton(onClick = { onRemoveSong(song.id) }) { Icon(Icons.Default.Delete, "Listeden Kaldır", tint = LanuTextMuted, modifier = Modifier.size(20.dp)) }
                    }
                }
            }
        }
    }

    if (showRename) {
        RenamePlaylistDialog(
            currentName = playlist.name,
            onDismiss = { showRename = false },
            onRename = { newName ->
                repository?.let { repo -> scope.launch { repo.renamePlaylist(playlist.id, newName) } }
                showRename = false
            }
        )
    }
}
