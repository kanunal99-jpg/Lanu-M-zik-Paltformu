package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.FriendActivity
import com.example.model.Song
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

@Composable
fun FriendActivityView(
    activities: List<FriendActivity>,
    availableSongs: List<Song>,
    onPlaySong: (Song) -> Unit,
    onShareSong: (Song) -> Unit,
    onLikeActivity: (String) -> Unit,
    onSendRecommendation: (friendName: String, song: Song, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRecommendDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LanuDarkBg)
            .testTag("friend_activity_screen")
    ) {
        // Feed Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LanuGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = LanuGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Arkadaşlarım Ne Dinliyor?",
                        color = LanuTextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Canlı Müzik Akışı ve Paylaşımlar",
                        color = LanuTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = { showRecommendDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = LanuGreen),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("button_open_recommend_dialog")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Öneri Gönder",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Activities List
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp)
        ) {
            items(activities, key = { it.id }) { activity ->
                FriendActivityCard(
                    activity = activity,
                    onPlaySong = { onPlaySong(activity.song) },
                    onShareSong = { onShareSong(activity.song) },
                    onLike = { onLikeActivity(activity.id) }
                )
            }
        }
    }

    if (showRecommendDialog) {
        SendRecommendationDialog(
            songs = availableSongs,
            onDismiss = { showRecommendDialog = false },
            onSend = { friendName, song, note ->
                onSendRecommendation(friendName, song, note)
                showRecommendDialog = false
            }
        )
    }
}

@Composable
fun FriendActivityCard(
    activity: FriendActivity,
    onPlaySong: () -> Unit,
    onShareSong: () -> Unit,
    onLike: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LanuDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LanuDarkBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Friend Info Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        AsyncImage(
                            model = activity.avatarUrl,
                            contentDescription = activity.friendName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        if (activity.isCurrentlyPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(LanuGreen)
                                    .border(2.dp, LanuDarkSurface, CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = activity.friendName,
                            color = LanuTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (activity.isCurrentlyPlaying) {
                                Text(
                                    text = "CANLI DİNLİYOR",
                                    color = LanuGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = activity.statusText,
                                    color = LanuTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Share Button
                IconButton(
                    onClick = onShareSong,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Paylaş",
                        tint = LanuTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Mutual note if present
            activity.mutualNote?.let { note ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = LanuDarkSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note,
                        color = LanuTextSecondary,
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Song Snippet Card
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LanuDarkSurfaceElevated)
                    .clickable { onPlaySong() }
                    .padding(10.dp)
            ) {
                AsyncImage(
                    model = activity.song.coverUrl,
                    contentDescription = activity.song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.song.title,
                        color = LanuTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${activity.song.artist} • ${activity.song.category.titleTr}",
                        color = LanuTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                // Play / Listen together button
                Button(
                    onClick = onPlaySong,
                    colors = ButtonDefaults.buttonColors(containerColor = LanuGreen),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Dinle",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Reactions Row
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onLike() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Beğen",
                        tint = if (activity.recommendationLikes > 0) LanuRose else LanuTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${activity.recommendationLikes} Beğeni",
                        color = LanuTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "LANU Sosyal",
                    color = LanuTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SendRecommendationDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onSend: (friendName: String, song: Song, note: String) -> Unit
) {
    var friendName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedSong by remember { mutableStateOf(songs.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LanuDarkSurface,
        title = {
            Text(
                text = "Arkadaşına Müzik Öner",
                color = LanuTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = friendName,
                    onValueChange = { friendName = it },
                    label = { Text("Arkadaşının Adı (Örn: Burak)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LanuGreen,
                        unfocusedBorderColor = LanuDarkBorder,
                        focusedTextColor = LanuTextPrimary,
                        unfocusedTextColor = LanuTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Önerilecek Parça: ${selectedSong?.title ?: "Parça seç"} - ${selectedSong?.artist ?: ""}",
                    color = LanuGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Kişisel Mesajın / Neden sevecek?") },
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
                onClick = {
                    selectedSong?.let {
                        val name = friendName.ifBlank { "Müziksever Arkadaş" }
                        val n = note.ifBlank { "Harika bir parça, kesinlikle dinlemelisin!" }
                        onSend(name, it, n)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LanuGreen),
                enabled = selectedSong != null
            ) {
                Text("Akışta Paylaş", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = LanuTextSecondary)
            }
        }
    )
}
