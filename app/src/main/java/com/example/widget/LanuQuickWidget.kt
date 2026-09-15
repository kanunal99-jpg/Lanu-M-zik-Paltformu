package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.background
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.service.PlayerStateStore

class LanuQuickWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = PlayerStateStore(context).read()
        provideContent { LanuQuickWidgetContent(snapshot) }
    }
}

@Composable
private fun LanuQuickWidgetContent(snapshot: PlayerStateStore.Snapshot) {
    val title = snapshot.title?.takeIf { it.isNotBlank() } ?: "Henüz çalmıyor"
    val artist = snapshot.artist?.takeIf { it.isNotBlank() } ?: "LANU Müzik"
    val playerAction: (String) -> androidx.glance.action.Action = { action ->
        actionStartActivity(Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse("lanumusic://action/$action") })
    }
    val openTrack = snapshot.songId?.takeIf { it.isNotBlank() }?.let { id ->
        actionStartActivity(Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse("lanumusic://track/$id") })
    }
    val openHome = actionStartActivity(Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse("lanumusic://home") })
    val openLibrary = actionStartActivity(Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse("lanumusic://library") })

    Column(
        modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color.rgb(18, 18, 22))).padding(14.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text("LANU Müzik", style = TextStyle(color = ColorProvider(Color.WHITE), fontSize = 16.sp))
        Spacer(GlanceModifier.padding(top = 6.dp))
        Text(title, style = TextStyle(color = ColorProvider(Color.WHITE), fontSize = 14.sp))
        Text(artist, style = TextStyle(color = ColorProvider(Color.LTGRAY), fontSize = 11.sp))
        Row(horizontalAlignment = Alignment.Horizontal.CenterHorizontally, modifier = GlanceModifier.padding(top = 10.dp)) {
            androidx.glance.Button(text = "Önceki", onClick = playerAction("previous"))
            Spacer(modifier = GlanceModifier.width(6.dp))
            androidx.glance.Button(text = if (snapshot.isPlaying) "Duraklat" else "Oynat", onClick = playerAction("playpause"))
            Spacer(modifier = GlanceModifier.width(6.dp))
            androidx.glance.Button(text = "Sonraki", onClick = playerAction("next"))
        }
        Row(horizontalAlignment = Alignment.Horizontal.CenterHorizontally, modifier = GlanceModifier.padding(top = 6.dp)) {
            androidx.glance.Button(text = "Aç", onClick = if (openTrack != null) openTrack else openHome)
            Spacer(modifier = GlanceModifier.width(6.dp))
            androidx.glance.Button(text = "Arşivim", onClick = openLibrary)
        }
    }
}

class LanuQuickWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LanuQuickWidget()
}
