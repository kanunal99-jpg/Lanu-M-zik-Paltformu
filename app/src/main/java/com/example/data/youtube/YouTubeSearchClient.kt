package com.example.data.youtube

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** YouTube metadata only. Never download, extract, or proxy audiovisual streams. */
data class YouTubeVideo(
    val id: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String
)

data class YouTubeSearchPage(val videos: List<YouTubeVideo>, val nextPageToken: String?)

class YouTubeSearchException(message: String) : IOException(message)

/** Uses the official YouTube Data API v3 search endpoint. */
object YouTubeSearchClient {
    private val videoIdPattern = Regex("[A-Za-z0-9_-]{11}")
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    fun requestUrl(query: String, apiKey: String, pageToken: String? = null): HttpUrl {
        require(query.trim().length >= 2) { "En az iki karakterle arama yapın." }
        require(apiKey.isNotBlank()) { "YouTube Data API anahtarı gerekli." }
        return HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("youtube/v3/search")
            .addQueryParameter("part", "snippet")
            .addQueryParameter("type", "video")
            .addQueryParameter("videoEmbeddable", "true")
            .addQueryParameter("maxResults", "25")
            .addQueryParameter("q", query.trim())
            .addQueryParameter("key", apiKey.trim())
            .apply { if (!pageToken.isNullOrBlank()) addQueryParameter("pageToken", pageToken) }
            .build()
    }

    fun parseResponse(json: String): YouTubeSearchPage {
        val root = JSONObject(json)
        val items = root.optJSONArray("items")
        val found = mutableListOf<YouTubeVideo>()
        if (items != null) {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
                if (!videoIdPattern.matches(id)) continue
                val snippet = item.optJSONObject("snippet") ?: continue
                val title = snippet.optString("title").trim()
                if (title.isBlank()) continue
                val thumbnails = snippet.optJSONObject("thumbnails")
                val thumbnail = listOf("medium", "high", "default")
                    .firstNotNullOfOrNull { size ->
                        thumbnails?.optJSONObject(size)?.optString("url")?.takeIf { it.startsWith("https://") }
                    }.orEmpty()
                found.add(
                    YouTubeVideo(
                        id = id,
                        title = title,
                        channel = snippet.optString("channelTitle").ifBlank { "YouTube" },
                        thumbnailUrl = thumbnail
                    )
                )
            }
        }
        val nextPage = root.optString("nextPageToken").takeIf { it.isNotBlank() }
        return YouTubeSearchPage(found.distinctBy { it.id }, nextPage)
    }

    fun errorMessage(code: Int, json: String): String {
        val reason = runCatching {
            JSONObject(json).optJSONObject("error")
                ?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason").orEmpty()
        }.getOrDefault("")
        return when {
            code == 403 && reason.contains("quota", ignoreCase = true) ->
                "YouTube arama kotası doldu. Kota yenilendiğinde tekrar deneyin."
            code == 400 || (code == 403 && reason.contains("key", ignoreCase = true)) ->
                "YouTube API anahtarı geçersiz veya bu uygulama için yetkili değil."
            code == 401 || code == 403 ->
                "YouTube arama erişimi reddedildi. API yetkisini ve anahtar kısıtlarını kontrol edin."
            code == 429 ->
                "Çok fazla arama yapıldı. Bir süre sonra tekrar deneyin."
            code in 500..599 ->
                "YouTube şu anda yanıt vermiyor. Tekrar deneyin."
            else -> "YouTube arama isteği başarısız oldu (HTTP " + code + ")."
        }
    }

    /** Android-restricted API keys require the installed app signing identity. */
    private fun signingSha1(context: Context): String? = runCatching {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        val bytes = signatures?.firstOrNull()?.toByteArray() ?: return@runCatching null
        MessageDigest.getInstance("SHA-1").digest(bytes)
            .joinToString("") { "%02X".format(it) }
    }.getOrNull()

    suspend fun search(
        context: Context,
        query: String,
        apiKey: String,
        pageToken: String? = null
    ): YouTubeSearchPage = withContext(Dispatchers.IO) {
        val url = requestUrl(query, apiKey, pageToken)
        val builder = Request.Builder().url(url).get()
            .header("X-Android-Package", context.packageName)
        signingSha1(context)?.let { builder.header("X-Android-Cert", it) }
        val request = builder.build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw YouTubeSearchException(errorMessage(response.code, body))
                try {
                    parseResponse(body)
                } catch (_: Exception) {
                    throw YouTubeSearchException("YouTube yanıtı okunamadı. Tekrar deneyin.")
                }
            }
        } catch (e: YouTubeSearchException) {
            throw e
        } catch (e: IOException) {
            throw YouTubeSearchException("İnternet bağlantısı kurulamadı. Ağınızı kontrol edip tekrar deneyin.")
        }
    }
}
