package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.res.stringResource
import com.example.R
import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
fun LanuYouTubePlayerDialog(onDismiss: () -> Unit, initialQuery: String = "") {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var searchText by remember(initialQuery) { mutableStateOf(initialQuery) }
    var sessionApiKey by remember { mutableStateOf("") }
    val configuredKey = com.example.BuildConfig.YOUTUBE_DATA_API_KEY
    val apiKey = configuredKey.ifBlank { sessionApiKey.trim() }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchedQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<com.example.data.youtube.YouTubeVideo>>(emptyList()) }
    var nextPageToken by remember { mutableStateOf<String?>(null) }
    var linkText by remember { mutableStateOf("") }
    var videoId by remember { mutableStateOf<String?>(null) }
    var invalid by remember { mutableStateOf(false) }

    fun loadResults(pageToken: String? = null) {
        if (searchLoading) return
        val query = if (pageToken == null) searchText.trim() else searchedQuery
        if (query.length < 2) {
            searchError = "En az iki karakter girin."
            return
        }
        if (apiKey.isBlank()) {
            searchError = "YouTube Data API anahtarı gerekli. Aşağıya anahtar girin veya doğrudan video bağlantısı yapıştırın."
            return
        }
        searchLoading = true
        searchError = null
        if (pageToken == null) {
            searchedQuery = query
            results = emptyList()
            nextPageToken = null
        }
        scope.launch {
            try {
                val page = com.example.data.youtube.YouTubeSearchClient.search(context, query, apiKey, pageToken)
                results = if (pageToken == null) page.videos else (results + page.videos).distinctBy { it.id }
                nextPageToken = page.nextPageToken
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                searchError = e.message ?: "Arama başarısız oldu. Tekrar deneyin."
            } finally {
                searchLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f).padding(8.dp)) {
            Column(
                modifier = Modifier.verticalScroll(scrollState).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("YouTube • LANU içinde ara ve oynat", color = Color.White)
                Text(
                    "YouTube videoları resmî, görünür oynatıcıda açılır. Ses ayırma, YouTube indirme veya ekran kapalıyken gizli çalma yoktur.",
                    color = Color.LightGray
                )
                val selected = videoId
                if (selected != null) {
                    Text("Seçili YouTube videosu", color = Color.White)
                    key(selected) { VisibleYouTubeEmbed(selected) }
                    Text(
                        "Video gömme izni, coğrafi bölge veya yaş kısıtı nedeniyle bazı içerikler oynatılamayabilir.",
                        color = Color.LightGray
                    )
                }
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it.take(200) },
                    modifier = Modifier.fillMaxWidth().testTag("youtube_search_input"),
                    label = { Text("Şarkı veya sanatçı ara") },
                    singleLine = true
                )
                if (configuredKey.isBlank()) {
                    OutlinedTextField(
                        value = sessionApiKey,
                        onValueChange = { sessionApiKey = it.trim().take(256) },
                        modifier = Modifier.fillMaxWidth().testTag("youtube_api_key_input"),
                        label = { Text("YouTube Data API v3 anahtarı (bu oturum için)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    Text(
                        "Anahtar cihazda kaydedilmez. Yayımlanan sürüme anahtar tanımlamak için GitHub Actions YOUTUBE_DATA_API_KEY secret kullanılabilir.",
                        color = Color.LightGray
                    )
                }
                Button(
                    onClick = { loadResults() },
                    enabled = !searchLoading,
                    modifier = Modifier.fillMaxWidth().testTag("youtube_search_button")
                ) { Text(if (searchLoading) "YouTube aranıyor…" else "YouTube'da şarkı ara") }
                if (searchError != null) {
                    Text(searchError.orEmpty(), color = Color(0xFFFF9A9A), modifier = Modifier.testTag("youtube_search_error"))
                }
                if (results.isNotEmpty()) {
                    Text("YouTube arama sonuçları • " + results.size + " video", color = Color.White)
                    results.forEach { result ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { videoId = result.id; scope.launch { scrollState.animateScrollTo(0) } }
                                .padding(vertical = 5.dp)
                                .testTag("youtube_result_" + result.id),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            coil.compose.AsyncImage(
                                model = result.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.size(width = 100.dp, height = 62.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    android.text.Html.fromHtml(result.title, android.text.Html.FROM_HTML_MODE_LEGACY).toString(),
                                    color = Color.White, maxLines = 2
                                )
                                Text(result.channel, color = Color.LightGray, maxLines = 1)
                            }
                        }
                    }
                } else if (searchedQuery.isNotEmpty() && !searchLoading && searchError == null) {
                    Text("Gömülebilir sonuç bulunamadı. Farklı bir arama deneyin.", color = Color.LightGray)
                }
                if (nextPageToken != null) {
                    Button(
                        onClick = { loadResults(nextPageToken) },
                        enabled = !searchLoading,
                        modifier = Modifier.fillMaxWidth().testTag("youtube_load_more")
                    ) { Text("Daha fazla YouTube sonucu") }
                }
                Text("Veya video bağlantısı yapıştır", color = Color.White)
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it.take(2048); invalid = false },
                    modifier = Modifier.fillMaxWidth().testTag("youtube_url_input"),
                    label = { Text("YouTube bağlantısı veya video ID") },
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    singleLine = true,
                    isError = invalid
                )
                if (invalid) Text("Geçerli bir HTTPS YouTube bağlantısı gir.", color = Color(0xFFFF9A9A))
                Button(onClick = {
                    val parsed = YouTubeVideoLink.videoId(linkText)
                    invalid = parsed == null
                    if (parsed != null) { videoId = parsed; scope.launch { scrollState.animateScrollTo(0) } }
                }, modifier = Modifier.fillMaxWidth().testTag("youtube_play_embedded")) {
                    Text("LANU içinde videoyu oynat")
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
    // WebView can be absent/disabled on some de-Googled Android and OEM builds.
    // A missing provider must not crash the entire music application.
    val webView = remember(videoId) {
        runCatching {
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                // No mixed-content subresources on older Android releases.
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.setSupportMultipleWindows(false)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?, request: WebResourceRequest?
                    ): Boolean {
                        if (request == null || !request.isForMainFrame) return false
                        return !YouTubeEmbedNavigationPolicy.isAllowedMainFrame(request.url.toString())
                    }
                }
                settings.mediaPlaybackRequiresUserGesture = true
                webChromeClient = WebChromeClient()
                loadUrl(
                    "https://www.youtube.com/embed/$videoId?playsinline=1",
                    mapOf("Referer" to "https://${context.packageName}")
                )
            }
        }.getOrNull()
    }
    if (webView == null) {
        Column {
            Text(stringResource(R.string.youtube_webview_unavailable))
            TextButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(YouTubeBrowserFallback.url(videoId)))
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                    )
                }
            }) { Text(stringResource(R.string.youtube_browser_fallback)) }
        }
        return
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

/** Explicit browser fallback, only for an already verified public YouTube video ID. */
internal object YouTubeBrowserFallback {
    private val videoIdPattern = Regex("[A-Za-z0-9_-]{11}")

    fun url(videoId: String): String {
        require(videoIdPattern.matches(videoId)) { "Invalid YouTube video ID" }
        return "https://www.youtube.com/watch?v=$videoId"
    }
}

/**
 * Prevent an embedded YouTube iframe from navigating the main WebView to an
 * unrelated web origin, file/content URI or cleartext URL.
 * Subresources loaded inside YouTube use the platform's HTTPS-only policy.
 */
internal object YouTubeEmbedNavigationPolicy {
    fun isAllowedMainFrame(url: String): Boolean {
        if (url == "about:blank") return true // Explicit lifecycle cleanup
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        if (uri.scheme != "https" || uri.userInfo != null || uri.port != -1) return false
        val host = uri.host?.lowercase(java.util.Locale.ROOT) ?: return false
        return host == "youtube.com" || host.endsWith(".youtube.com") ||
            host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com")
    }
}
