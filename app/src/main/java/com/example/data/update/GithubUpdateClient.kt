package com.example.data.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class VerifiedRelease(
    val versionCode: Int,
    val tag: String,
    val pageUrl: String,
    val apkUrl: String,
    val sha256Url: String,
    val sizeBytes: Long
)

sealed interface UpdateAvailability {
    data class Available(val release: VerifiedRelease) : UpdateAvailability
    data object UpToDate : UpdateAvailability
    data object AwaitingSignedRelease : UpdateAvailability
}

/** Only accept signed APKs from this repository's official GitHub Releases. */
object UpdateReleasePolicy {
    private const val repo = "kanunal99-jpg/Lanu-M-zik-Paltformu"
    const val releasesPage = "https://github.com/" + repo + "/releases"
    private const val downloadPrefix = "https://github.com/" + repo + "/releases/download/"
    private val tagPattern = Regex("""lanu-music-(\d+)-[0-9a-f]{40}""")

    fun choose(json: String, installedCode: Int): UpdateAvailability {
        val releases = JSONArray(json)
        var hasNewer = false
        val signed = mutableListOf<VerifiedRelease>()
        for (i in 0 until releases.length()) {
            val release = releases.optJSONObject(i) ?: continue
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue
            val tag = release.optString("tag_name")
            val version = tagPattern.matchEntire(tag)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            if (version <= installedCode) continue
            hasNewer = true
            val page = release.optString("html_url")
            if (page != releasesPage + "/tag/" + tag) continue
            val assets = release.optJSONArray("assets") ?: continue
            fun assetUrl(name: String): Pair<String, Long>? {
                for (j in 0 until assets.length()) {
                    val asset = assets.optJSONObject(j) ?: continue
                    if (asset.optString("name") != name) continue
                    val url = asset.optString("browser_download_url")
                    if (url != downloadPrefix + tag + "/" + name) continue
                    val size = asset.optLong("size", -1L)
                    if (size <= 0 || size > 200L * 1024L * 1024L) continue
                    return url to size
                }
                return null
            }
            val apk = assetUrl("app-release.apk") ?: continue
            val hash = assetUrl("app-release.apk.sha256") ?: continue
            signed.add(VerifiedRelease(version, tag, page, apk.first, hash.first, apk.second))
        }
        return when {
            signed.isNotEmpty() -> UpdateAvailability.Available(signed.maxBy { it.versionCode })
            hasNewer -> UpdateAvailability.AwaitingSignedRelease
            else -> UpdateAvailability.UpToDate
        }
    }

    fun parseExpectedSha256(value: String): String {
        val hash = value.trim().split(Regex("""\s+""")).firstOrNull().orEmpty()
        if (!Regex("[a-fA-F0-9]{64}").matches(hash))
            throw IOException("Güncelleme dosyasının SHA-256 bilgisi geçersiz.")
        return hash.lowercase(Locale.ROOT)
    }
}

class GithubUpdateClient(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    private fun get(url: String) = client.newCall(Request.Builder().url(url)
        .header("User-Agent", "LANU-Music-Android-Updater")
        .header("Accept", "application/vnd.github+json").build()).execute()

    private fun responseBody(response: okhttp3.Response): String {
        if (!response.isSuccessful) throw IOException(
            if (response.code == 403 || response.code == 429)
                "GitHub istek sınırına ulaşıldı. Daha sonra tekrar deneyin."
            else "GitHub yanıt vermedi (HTTP " + response.code + ")."
        )
        return response.body?.string() ?: throw IOException("GitHub boş yanıt döndürdü.")
    }

    suspend fun check(installedCode: Int): UpdateAvailability = withContext(Dispatchers.IO) {
        get("https://api.github.com/repos/kanunal99-jpg/Lanu-M-zik-Paltformu/releases?per_page=20").use {
            UpdateReleasePolicy.choose(responseBody(it), installedCode)
        }
    }

    suspend fun downloadAndVerify(release: VerifiedRelease, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val folder = File(context.cacheDir, "lanu_updates")
            if (!folder.exists() && !folder.mkdirs()) throw IOException("Güncelleme klasörü oluşturulamadı.")
            val finalApk = File(folder, "lanu-" + release.versionCode + ".apk")
            val temporary = File(folder, "lanu-" + release.versionCode + ".download.apk")
            val prefix = "https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases/download/" + release.tag + "/"
            if (release.apkUrl != prefix + "app-release.apk" ||
                release.sha256Url != prefix + "app-release.apk.sha256")
                throw IOException("Güvenilmeyen güncelleme adresi.")
            try {
                val expected = get(release.sha256Url).use { response ->
                    UpdateReleasePolicy.parseExpectedSha256(responseBody(response))
                }
                finalApk.delete()
                temporary.delete()
                get(release.apkUrl).use { response ->
                    if (!response.isSuccessful) throw IOException("APK indirilemedi (HTTP " + response.code + ").")
                    val body = response.body ?: throw IOException("APK dosyası boş.")
                    val sha = MessageDigest.getInstance("SHA-256")
                    var count = 0L
                    body.byteStream().use { input ->
                        temporary.outputStream().buffered().use { output ->
                            val buffer = ByteArray(65536)
                            while (true) {
                                coroutineContext.ensureActive()
                                val n = input.read(buffer)
                                if (n < 0) break
                                count += n
                                if (count > release.sizeBytes || count > 200L * 1024L * 1024L)
                                    throw IOException("APK boyutu yayın bilgisiyle eşleşmiyor.")
                                sha.update(buffer, 0, n)
                                output.write(buffer, 0, n)
                                onProgress((count * 100 / release.sizeBytes).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                    if (count != release.sizeBytes) throw IOException("APK eksik indirildi.")
                    val digest = sha.digest().joinToString("") { "%02x".format(it) }
                    if (!digest.equals(expected, ignoreCase = true))
                        throw IOException("APK SHA-256 doğrulaması başarısız. Kurulum engellendi.")
                }
                verifyApk(temporary, release.versionCode)
                if (!temporary.renameTo(finalApk)) throw IOException("Doğrulanan APK kaydedilemedi.")
                onProgress(100)
                finalApk
            } catch (e: Exception) {
                temporary.delete()
                finalApk.delete()
                throw e
            }
        }

    private fun signatureDigests(packageInfo: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        return signatures.orEmpty().map {
            MessageDigest.getInstance("SHA-256").digest(it.toByteArray())
                .joinToString("") { b -> "%02x".format(b) }
        }.toSet()
    }

    /** Refuses signature mismatches: uninstalling to update would destroy user data. */
    fun verifyApk(file: File, expectedVersionCode: Int) {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val candidate = pm.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IOException("APK Android paketi olarak okunamadı.")
        @Suppress("DEPRECATION")
        val installed = pm.getPackageInfo(context.packageName, flags)
        if (candidate.packageName != context.packageName)
            throw IOException("APK farklı bir uygulamaya ait. Kurulum engellendi.")
        @Suppress("DEPRECATION")
        val candidateCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            candidate.longVersionCode else candidate.versionCode.toLong()
        @Suppress("DEPRECATION")
        val installedCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            installed.longVersionCode else installed.versionCode.toLong()
        if (candidateCode != expectedVersionCode.toLong() || candidateCode <= installedCode)
            throw IOException("APK yeni bir sürüm değil. Kurulum engellendi.")
        val current = signatureDigests(installed)
        if (current.isEmpty() || current != signatureDigests(candidate))
            throw IOException("APK imzası yüklü LANU sürümüyle eşleşmiyor. " +
                "Verilerinizi korumak için kurulum engellendi; uygulamayı kaldırmayın.")
    }
}
