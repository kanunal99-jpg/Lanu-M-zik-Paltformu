package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Song
import com.example.service.RepeatMode
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurfaceElevated
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuRose
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

/**
 * Persistent BottomAppBar component acting as the primary music player controller.
 *
 * Features:
 * - Persistent across the application above the navigation bar
 * - Real-time animated track progress indicator along top edge
 * - Track duration and elapsed time display
 * - Responsive Play/Pause toggle with prominent M3 touch target
 * - Skip Previous and Skip Next controls
 * - Toggleable Shuffle and Repeat mode controls with active indicators
 * - Favorite quick-toggle button
 * - Track artwork thumbnail and title/artist typography
 * - Tap-to-expand into the full-screen Now Playing and karaoke lyrics sheet
 */
@Composable
fun PlayerBottomAppBar(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isFavorite: Boolean,
    isShuffle: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    onToggleFavorite: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onExpand: () -> Unit,
    onSeekTo: ((Long) -> Unit)? = null,
    onSeekToRatio: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }

    val rawProgress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val displayFraction = if (isSeeking) seekFraction else rawProgress
    val displayPositionMs = if (isSeeking) {
        (seekFraction * durationMs).toLong()
    } else {
        currentPositionMs
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        label = "PlayerProgressAnimation"
    )

    Surface(
        color = LanuDarkSurfaceElevated,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = LanuDarkBorder,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .testTag("persistent_bottom_app_bar_player")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Interactive Progress Bar Slider to scrub and handle playback position
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("player_progress_slider_container")
            ) {
                Slider(
                    value = displayFraction,
                    onValueChange = {
                        isSeeking = true
                        seekFraction = it
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        val targetMs = (seekFraction * durationMs).toLong()
                        onSeekTo?.invoke(targetMs)
                        onSeekToRatio?.invoke(seekFraction)
                    },
                    enabled = song != null && durationMs > 0,
                    colors = SliderDefaults.colors(
                        thumbColor = LanuGreen,
                        activeTrackColor = LanuGreen,
                        inactiveTrackColor = LanuDarkBorder,
                        disabledThumbColor = Color.Transparent,
                        disabledActiveTrackColor = LanuDarkBorder,
                        disabledInactiveTrackColor = LanuDarkBorder.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .testTag("player_progress_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(displayPositionMs),
                        color = if (isSeeking) LanuGreen else LanuTextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (isSeeking) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.testTag("player_current_position_text")
                    )

                    if (isSeeking) {
                        Text(
                            text = "Konuma Bırak",
                            color = LanuGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = formatDuration(durationMs),
                        color = LanuTextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.testTag("player_duration_text")
                    )
                }
            }

            // Material 3 BottomAppBar as primary player controller
            BottomAppBar(
                containerColor = LanuDarkSurfaceElevated,
                contentColor = LanuTextPrimary,
                tonalElevation = 6.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .testTag("m3_player_bottom_app_bar")
            ) {
                if (song != null) {
                    // Active Track Layout
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        // Left: Album Artwork + Title & Artist Info (Clickable to expand)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onExpand() }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                .testTag("player_track_info")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, LanuDarkBorder, RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = song.title,
                                        color = LanuTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (song.lyrics.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Sözler",
                                            tint = LanuGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = song.artist,
                                        color = LanuTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = " • ${formatDuration(currentPositionMs)} / ${formatDuration(durationMs)}",
                                        color = LanuTextMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Right: Primary Player Controls (Shuffle, Prev, Play/Pause, Next, Repeat, Favorite, Expand)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            // Shuffle Toggle
                            IconButton(
                                onClick = onToggleShuffle,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("player_shuffle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = if (isShuffle) "Karıştırma Açık" else "Karıştırma Kapalı",
                                    tint = if (isShuffle) LanuGreen else LanuTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Skip Previous
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("player_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Önceki Şarkı",
                                    tint = LanuTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Primary Play / Pause Toggle Button (High-Contrast M3 Circular Pill)
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LanuGreen)
                                    .testTag("player_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Skip Next
                            IconButton(
                                onClick = onNext,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("player_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Sonraki Şarkı",
                                    tint = LanuTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Repeat Mode Toggle (OFF -> ALL -> ONE)
                            val isRepeatActive = repeatMode != RepeatMode.OFF
                            val repeatIcon = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                            IconButton(
                                onClick = onToggleRepeat,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("player_repeat_button")
                            ) {
                                Icon(
                                    imageVector = repeatIcon,
                                    contentDescription = when (repeatMode) {
                                        RepeatMode.ONE -> "Tekrar: Tek Şarkı"
                                        RepeatMode.ALL -> "Tekrar: Tüm Liste"
                                        RepeatMode.OFF -> "Tekrar: Kapalı"
                                    },
                                    tint = if (isRepeatActive) LanuGreen else LanuTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Favorite Toggle
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("player_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle",
                                    tint = if (isFavorite) LanuRose else LanuTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Expand Sheet
                            IconButton(
                                onClick = onExpand,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("player_expand_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Genişlet",
                                    tint = LanuTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Persistent Idle/Ready State (When app just launched or no song selected)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onExpand() }
                            .padding(horizontal = 8.dp)
                            .testTag("player_idle_state")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = LanuGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = LanuGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "LANU Müzik Çalar",
                                    color = LanuTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Dinlemek için bir şarkı seçin",
                                    color = LanuTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Right: Controls in idle state
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            // Shuffle Toggle in idle state
                            IconButton(
                                onClick = onToggleShuffle,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("player_idle_shuffle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = if (isShuffle) "Karıştırma Açık" else "Karıştırma Kapalı",
                                    tint = if (isShuffle) LanuGreen else LanuTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Repeat Mode Toggle in idle state
                            IconButton(
                                onClick = onToggleRepeat,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("player_idle_repeat_button")
                            ) {
                                val repeatIcon = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                                Icon(
                                    imageVector = repeatIcon,
                                    contentDescription = when (repeatMode) {
                                        RepeatMode.ONE -> "Tekrar: Tek Şarkı"
                                        RepeatMode.ALL -> "Tekrar: Tüm Liste"
                                        RepeatMode.OFF -> "Tekrar: Kapalı"
                                    },
                                    tint = if (repeatMode != RepeatMode.OFF) LanuGreen else LanuTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Play/Pause button ready to play default recommended song
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LanuGreen)
                                    .testTag("player_idle_play_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Oynat",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Persistent BottomAppBar component containing essential playback controls:
 * - Play and Pause controls
 * - Skip controls (Previous & Next)
 * - Progress bar slider to scrub and handle music playback state
 */
@Composable
fun PlaybackBottomAppBar(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onExpand: () -> Unit = {}
) {
    PlayerBottomAppBar(
        song = song,
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onTogglePlayPause = onTogglePlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onExpand = onExpand,
        onSeekTo = onSeekTo,
        modifier = modifier
    )
}

/**
 * Alias for PlaybackBottomAppBar for persistent music playback controls.
 */
@Composable
fun PersistentPlaybackBottomAppBar(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onExpand: () -> Unit = {}
) {
    PlaybackBottomAppBar(
        song = song,
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        onTogglePlayPause = onTogglePlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onSeekTo = onSeekTo,
        modifier = modifier,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onExpand = onExpand
    )
}

