package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AudioQuality
import com.example.model.Song
import com.example.model.SongSourceType
import com.example.data.catalog.CatalogLicensePolicy
import com.example.service.RepeatMode
import com.example.ui.theme.LanuCyan
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurfaceElevated
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuPurple
import com.example.ui.theme.LanuRose
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

@Composable
fun NowPlayingSheet(
    song: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    audioQuality: AudioQuality,
    showLyrics: Boolean,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDownload: () -> Unit,
    onToggleLyrics: () -> Unit,
    onSelectAudioQuality: (AudioQuality) -> Unit,
    onOpenEqualizer: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSourceInfo by remember { mutableStateOf(false) }

    val currentFraction = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayPositionMs = if (isSeeking) {
        (seekFraction * durationMs).toLong()
    } else currentPositionMs

    if (showSourceInfo) {
        AlertDialog(
            onDismissRequest = { showSourceInfo = false },
            title = { Text("Kaynak ve lisans") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sanatçı / lisans sağlayıcı: ${song.artist}")
                    Text("Kaynak türü: ${song.sourceType.name}")
                    Text("Lisans: ${song.license.ifBlank { "Bilinmiyor" }}")
                    Text("Kaynak URI: ${song.audioUrl.ifBlank { "Yok" }}")
                    if (song.license.contains("Audius Open Music License", ignoreCase = true)) {
                        Text("OML referansı: ${CatalogLicensePolicy.AUDIUS_OML_URI}", color = LanuTextSecondary)
                        Text("Atıf kimliği: ${song.artist}", color = LanuTextSecondary)
                        Text(
                            "Ticari kullanımda Audius OML'nin öngördüğü lisans sağlayıcı kimliği, telif bildirimi, OML bildirimi ve kaynak bağlantısı mümkün olduğunca korunmalıdır.",
                            color = LanuTextSecondary
                        )
                    }
                    if (song.sourceType == SongSourceType.VERIFIED_PREVIEW) {
                        Text(
                            "Bu içerik tam parça değil, doğrulanmış resmi önizleme kaynağıdır.",
                            color = LanuTextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSourceInfo = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        song.category.primaryColor.copy(alpha = 0.35f),
                        LanuDarkBg,
                        LanuDarkBg
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("now_playing_screen")
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("button_collapse_now_playing")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Küçült",
                    tint = LanuTextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ŞU AN ÇALIYOR",
                    color = LanuTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = song.category.titleTr,
                    color = LanuTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Audio Quality Selector Chip & Share
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(
                        color = LanuDarkSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showQualityMenu = true }
                            .border(1.dp, LanuGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = audioQuality.title,
                            color = LanuGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DropdownMenu(
                        expanded = showQualityMenu,
                        onDismissRequest = { showQualityMenu = false },
                        modifier = Modifier.background(LanuDarkSurfaceElevated)
                    ) {
                        AudioQuality.values().forEach { quality ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(quality.title, color = LanuTextPrimary, fontWeight = FontWeight.Bold)
                                        Text(quality.bitRate, color = LanuTextSecondary, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    onSelectAudioQuality(quality)
                                    showQualityMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = { showSourceInfo = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Kaynak ve lisans bilgisi",
                        tint = LanuTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Paylaş",
                        tint = LanuTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center: Toggle between Album Art & Synchronized Karaoke Lyrics
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (showLyrics) {
                SynchronizedLyricsView(
                    song = song,
                    currentPositionMs = currentPositionMs,
                    onSeekToLyric = { targetMs -> onSeekTo(targetMs) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Large Glowing Album Artwork
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1f)
                        .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = LanuGreen, spotColor = LanuPurple)
                        .border(1.5.dp, LanuDarkBorder, RoundedCornerShape(24.dp))
                        .clickable { onToggleLyrics() }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // "Sözleri Göster" floating badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = LanuGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Şarkı Sözlerini Senkronize Aç",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Song Title, Artist & Quick Action Buttons (Favorite, Download)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = LanuTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    color = LanuTextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Download / Offline Button
                IconButton(onClick = onToggleDownload) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                        contentDescription = "Çevrimdışı İndir",
                        tint = if (isDownloaded) LanuGreen else LanuTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Favorite Toggle
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorilere Ekle",
                        tint = if (isFavorite) LanuRose else LanuTextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrubber / Seek Slider with timestamps
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = if (isSeeking) seekFraction else currentFraction,
                onValueChange = {
                    isSeeking = true
                    seekFraction = it
                },
                onValueChangeFinished = {
                    isSeeking = false
                    val targetMs = (seekFraction * durationMs).toLong()
                    onSeekTo(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = LanuGreen,
                    activeTrackColor = LanuGreen,
                    inactiveTrackColor = LanuDarkBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("now_playing_seek_bar")
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatMs(displayPositionMs),
                    color = LanuTextMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = formatMs(durationMs),
                    color = LanuTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Playback Controls: Shuffle, Prev, Play/Pause, Next, Repeat
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Karıştır",
                    tint = if (isShuffle) LanuGreen else LanuTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Önceki",
                    tint = LanuTextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Central Play/Pause button
            Surface(
                shape = CircleShape,
                color = LanuGreen,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onTogglePlayPause() }
                    .testTag("now_playing_play_pause")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Sonraki",
                    tint = LanuTextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            IconButton(onClick = onToggleRepeat) {
                val icon = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Tekrarla",
                    tint = if (repeatMode != RepeatMode.OFF) LanuGreen else LanuTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Tools Bar: Lyrics mode switch, Equalizer shortcut, Add to Playlist
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LanuDarkSurfaceElevated.copy(alpha = 0.6f))
                .padding(vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onToggleLyrics() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (showLyrics) LanuGreen else LanuTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showLyrics) "Kapağa Dön" else "Sözler",
                    color = if (showLyrics) LanuGreen else LanuTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onOpenEqualizer() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = LanuPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ekolayzır",
                    color = LanuTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onAddToPlaylist() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    tint = LanuCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Listeye Ekle",
                    color = LanuTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
