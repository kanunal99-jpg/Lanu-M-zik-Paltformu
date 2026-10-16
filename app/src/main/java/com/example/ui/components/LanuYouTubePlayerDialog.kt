package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LanuDarkSurface
import java.net.URI
import java.util.Locale

/**
 * Official, visible YouTube iframe player, not an extracted audio stream.
 * Search and access through YouTube Data API require a separately authorized
 * project/API client; a pasted public video URL is the safe first integration.
 */
object YouTubeVideoLink {
    private val idPattern = Regex("[A-Za-z0-9_-]{11}")
    private val trustedHosts = setOf(
        "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com",
        "youtube-nocookie.com", "www.youtube-nocookie.com", "youtu.be", "www.youtu.be"
    )

    fun videoId(text: String): String? {
        val input = text.trim()
        if (idPattern.matches(input)) return input
        val uri = try { URI(input) } catch (_: Exception) { return null }
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.userInfo != null || uri.port != -1) return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        if (host !in trustedHosts) return null
        val parts = uri.path.orEmpty().trim('/').split('/').filter { it.isNotBlank() }
        val id = when {
            host == "youtu.be" || host == "www.youtu.be" -> parts.firstOrNull()
            parts.firstOrNull() == "watch" ->
                uri.rawQuery.orEmpty().split('&')
                    .firstOrNull { it.substringBefore('=') == "v" }?.substringAfter('=')
            parts.firstOrNull() in setOf("embed", "shorts", "live", "v") -> parts.getOrNull(1)
            else -> null
        }
        return id?.takeIf { idPattern.matches(it) }
    }
}

@Composable
fun LanuYouTubePlayerDialog(onDismiss: () -> Unit) {
    var linkText by remember { mutableStateOf("") }
    var videoId by remember { mutableStateOf<String?>(null) }
    var invalid by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.96f).padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("YouTube • LANU içinde izle", color = Color.White)
                Text(
                    "Resmî video oynatıcı görünür kalır. Video sesini ayırma, arka planda gizli çalma veya indirme yapılmaz.",
                    color = Color.LightGray
                )
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it.take(2048); invalid = false },
                    modifier = Modifier.fillMaxWidth().testTag("youtube_url_input"),
                    label = { Text("YouTube video bağlantısı") },
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    singleLine = true,
                    isError = invalid
                )
                if (invalid) Text("Geçerli bir HTTPS YouTube video bağlantısı gir.", color = Color(0xFFFF9A9A))
                Button(onClick = {
                    val parsed = YouTubeVideoLink.videoId(linkText)
                    invalid = parsed == null
                    if (parsed != null) videoId = parsed
                }, modifier = Modifier.fillMaxWidth().testTag("youtube_play_embedded")) {
                    Text("LANU içinde videoyu oynat")
                }
                val selectedVideo = videoId
                if (selectedVideo != null) {
                    key(selectedVideo) {
                        VisibleYouTubeEmbed(selectedVideo)
                    }
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text("Bağlantıyı yapıştırıp oynatmayı başlat.", color = Color.LightGray)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Kapat")
                }
            }
        }
    }
}

/** WebView is destroyed on dismissal so playback cannot silently continue. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VisibleYouTubeEmbed(videoId: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val webView = remember(videoId) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = true
            webChromeClient = WebChromeClient()
            loadUrl("https://www.youtube.com/embed/$videoId?playsinline=1")
        }
    }
    DisposableEffect(webView, lifecycleOwner) {
        // YouTube forbids playback when the player is no longer visible.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                webView.onPause()
                webView.stopLoading()
                webView.loadUrl("about:blank")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }
    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxWidth().height(230.dp).testTag("visible_youtube_embed")
    )
}
