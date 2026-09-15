package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import android.graphics.Color

class LanuQuickWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            LanuQuickWidgetContent()
        }
    }
}

@Composable
private fun LanuQuickWidgetContent() {
    val openApp = Intent("android.intent.action.VIEW").apply {
        data = android.net.Uri.parse("lanumusic://home")
    }
    val openLibrary = Intent("android.intent.action.VIEW").apply {
        data = android.net.Uri.parse("lanumusic://library")
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.rgb(18, 18, 22)))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "LANU Müzik",
            style = TextStyle(
                color = ColorProvider(Color.WHITE),
                fontSize = 16.sp
            )
        )
        Row(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.padding(top = 10.dp)
        ) {
            androidx.glance.Button(
                text = "Aç",
                onClick = actionStartActivity(openApp),
                modifier = GlanceModifier.padding(end = 8.dp)
            )
            androidx.glance.Button(
                text = "Arşivim",
                onClick = actionStartActivity(openLibrary)
            )
        }
    }
}

class LanuQuickWidgetReceiver : androidx.glance.appwidget.GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LanuQuickWidget()
}
