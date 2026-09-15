package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AuthProvider
import com.example.auth.AuthSession
import com.example.data.HistoryRecord
import com.example.model.Song
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary
import java.text.DateFormat
import java.util.Date

/**
 * Provider-agnostic account surface. Local sessions are explicitly identified as local;
 * this screen never presents the device identity as a cloud account.
 */
@Composable
fun AccountHistoryScreen(
    session: AuthSession?,
    history: List<HistoryRecord>,
    allSongs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songsById = allSongs.associateBy { it.id }

    Column(
        modifier = modifier.fillMaxSize().background(LanuDarkBg).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Hesap ve Geçmiş", color = LanuTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)

        Card(
            colors = CardDefaults.cardColors(containerColor = LanuDarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, null, tint = LanuGreen, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(session?.displayName ?: "LANU Kullanıcısı", color = LanuTextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        when (session?.provider) {
                            AuthProvider.LOCAL -> "Yerel / çevrimdışı hesap"
                            AuthProvider.REMOTE -> "Doğrulanmış uzak hesap"
                            null -> "Oturum hazırlanıyor"
                        },
                        color = LanuTextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (session != null) {
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = LanuDarkBg),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Logout, null, tint = LanuTextMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Çıkış", color = LanuTextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, tint = LanuGreen, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Dinleme Geçmişi", color = LanuTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (history.isEmpty()) {
            Text("Henüz dinleme geçmişi yok. Bir parçayı gerçekten başlattığınızda burada görünür.", color = LanuTextMuted, fontSize = 13.sp)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history, key = { "${it.songId}:${it.playedAtMs}" }) { record ->
                    val song = songsById[record.songId]
                    if (song != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onPlaySong(song, listOf(song)) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = LanuGreen, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = LanuTextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${song.artist} • ${song.album}", color = LanuTextSecondary, fontSize = 12.sp, maxLines = 1)
                            }
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(record.playedAtMs)),
                                color = LanuTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
