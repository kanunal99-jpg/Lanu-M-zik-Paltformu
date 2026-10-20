package com.example.data.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.android.apksig.ApkVerifier
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class VerifiedRelease(
    val versionCode: Int,
    val tag: String,
    val pageUrl: String,
    val apkUrl: String,
    val sha256Url: String,
    val sizeBytes: Long,
    val githubDigest: String? = null
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
            fun assetUrl(name: String): Triple<String, Long, String?>? {
                for (j in 0 until assets.length()) {
                    val asset = assets.optJSONObject(j) ?: continue
                    if (asset.optString("name") != name) continue
                    val url = asset.optString("browser_download_url")
                    if (url != downloadPrefix + tag + "/" + name) continue
                    val size = asset.optLong("size", -1L)
                    if (size <= 0 || size > 200L * 1024L * 1024L) continue
                    val rawDigest = asset.optString("digest")
                    val digest = if (rawDigest.isBlank()) null else {
                        if (!Regex("sha256:[a-fA-F0-9]{64}").matches(rawDigest)) continue
                        rawDigest.substringAfter(':').lowercase(Locale.ROOT)
                    }
                    return Triple(url, size, digest)
                }
                return null
            }
            val apk = assetUrl("app-release.apk") ?: continue
            val hash = assetUrl("app-release.apk.sha256") ?: continue
            signed.add(VerifiedRelease(version, tag, page, apk.first, hash.first, apk.second, apk.third))
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


/** Testable resume-range guard: never append if the server returned the wrong byte range. */
internal object OtaTransferPolicy {
    private val contentRange = Regex("""bytes (\d+)-(\d+)/(\d+)""")
    fun matchesRange(value: String?, expectedOffset: Long, expectedTotal: Long): Boolean {
        val match = value?.let { contentRange.matchEntire(it.trim()) } ?: return false
        val (start, end, total) = match.destructured
        val begin = start.toLongOrNull() ?: return false
        val last = end.toLongOrNull() ?: return false
        val size = total.toLongOrNull() ?: return false
        return size == expectedTotal && begin == expectedOffset && last >= begin && last < size
    }
}

class GithubUpdateClient(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // Slow mobile networks need enough time to download a verified APK.
        .callTimeout(10, TimeUnit.MINUTES)
        .build()

    private fun get(url: String, startByte: Long? = null): okhttp3.Response {
        val builder = Request.Builder().url(url)
            .header("User-Agent", "LANU-Music-Android-Updater")
            .header("Accept", if (url.contains("/releases/download/"))
                "application/octet-stream" else "application/vnd.github+json")
            .header("Cache-Control", "no-cache")
        if (startByte != null && startByte > 0) {
            builder.header("Range", "bytes=" + startByte + "-")
        }
        return client.newCall(builder.build()).execute()
    }

    /** Bound JSON/sha256 response sizes to avoid excessive memory use. */
    private fun responseBody(response: okhttp3.Response, maxBytes: Int = 8 * 1024 * 1024): String {
        if (!response.isSuccessful) throw IOException(
            if (response.code == 403 || response.code == 429)
                "GitHub istek sınırına ulaşıldı. Daha sonra tekrar deneyin."
            else "GitHub yanıt vermedi (HTTP " + response.code + ")."
        )
        if (response.body?.contentLength()?.let { it > maxBytes } == true)
            throw IOException("GitHub yanıtı izin verilen boyutu aştı.")
        val input = response.body?.byteStream() ?: throw IOException("GitHub boş yanıt döndürdü.")
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16384)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > maxBytes)
                throw IOException("GitHub yanıtı izin verilen boyutu aştı.")
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }


    /**
     * GitHub's /latest endpoint is the authoritative most recently published
     * stable release. Authenticated release lists may be dominated by old drafts
     * and miss it in their first page: always include /latest independently.
     */
    suspend fun check(installedCode: Int): UpdateAvailability = withContext(Dispatchers.IO) {
        val root = "https://api.github.com/repos/kanunal99-jpg/Lanu-M-zik-Paltformu"
        val latest = get(root + "/releases/latest").use { response ->
            when (response.code) {
                404 -> null
                else -> JSONObject(responseBody(response))
            }
        }
        val newest = JSONArray()
        if (latest != null) newest.put(latest)
        val latestResult = UpdateReleasePolicy.choose(newest.toString(), installedCode)
        if (latestResult is UpdateAvailability.Available) {
            return@withContext latestResult
        }
        // Previous signed stable builds may remain installable even when newest
        // publish only has a debug preview. Best effort; do not discard /latest
        // if listing is temporarily rate limited.
        val combined = runCatching {
            get(root + "/releases?per_page=100").use { response ->
                val history = JSONArray(responseBody(response))
                val result = JSONArray()
                if (latest != null) result.put(latest)
                for (i in 0 until history.length()) result.put(history.getJSONObject(i))
                UpdateReleasePolicy.choose(result.toString(), installedCode)
            }
        }.getOrDefault(latestResult)
        combined
    }

    /** Resume interrupted downloads using HTTP Range, never without checking Content-Range. */
    private suspend fun downloadRemaining(
        url: String, part: File, total: Long, onProgress: (Int) -> Unit
    ) {
        var offset = part.takeIf { it.isFile }?.length() ?: 0L
        if (offset > total) {
            part.delete()
            offset = 0L
        }
        if (offset == total) return
        if (part.parentFile?.usableSpace?.let { it < total - offset + 8L * 1024L * 1024L } == true)
            throw IOException("Güncelleme için yeterli boş depolama alanı bulunmuyor.")
        for (attempt in 0..1) {
            coroutineContext.ensureActive()
            var restart = false
            get(url, if (offset > 0) offset else null).use { response ->
                if (offset > 0 && response.code == 416) {
                    restart = true
                } else {
                    if (!response.isSuccessful)
                        throw IOException("APK indirilemedi (HTTP " + response.code + ").")
                    val append = offset > 0 && response.code == 206
                    if (response.code == 206 &&
                        (!append || !OtaTransferPolicy.matchesRange(
                            response.header("Content-Range"), offset, total
                        ))) throw IOException("Sunucu hatalı indirme aralığı döndürdü.")
                    if (response.code != 200 && response.code != 206)
                        throw IOException("Beklenmeyen indirme yanıtı.")
                    // HTTP 200 means the server ignored Range: restart, NEVER append.
                    if (!append) {
                        part.delete()
                        offset = 0L
                    }
                    var written = offset
                    var lastPercent = -1
                    val source = response.body ?: throw IOException("APK dosyası boş.")
                    source.byteStream().use { input ->
                        FileOutputStream(part, append).buffered().use { output ->
                            val buffer = ByteArray(65536)
                            while (true) {
                                coroutineContext.ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                written += count
                                if (written > total)
                                    throw IOException("APK boyutu yayın bilgisiyle eşleşmiyor.")
                                output.write(buffer, 0, count)
                                val percent = (written * 100 / total).toInt().coerceIn(0, 99)
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(percent)
                                }
                            }
                        }
                    }
                    if (written != total)
                        throw IOException("APK kesintiye uğradı. Tekrar deneyerek kaldığınız yerden devam edebilirsiniz.")
                    return
                }
            }
            if (restart) {
                part.delete()
                offset = 0L
            }
        }
        throw IOException("Sunucu indirme aralığını kabul etmedi.")
    }

    private suspend fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                coroutineContext.ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Keep only recent incomplete transfers; never touch library/database/user content. */
    private fun pruneOldUpdates(directory: File, currentCode: Int) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        directory.listFiles()?.forEach { old ->
            if (old.isFile && old.name != "lanu-" + currentCode + ".download.apk" &&
                old.name != "lanu-" + currentCode + ".apk" &&
                Regex("""lanu-\d+(\.download)?\.apk""").matches(old.name) &&
                old.lastModified() < cutoff
            ) old.delete()
        }
    }

    suspend fun downloadAndVerify(release: VerifiedRelease, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val folder = File(context.cacheDir, "lanu_updates")
            if (!folder.exists() && !folder.mkdirs())
                throw IOException("Güncelleme klasörü oluşturulamadı.")
            val finalApk = File(folder, "lanu-" + release.versionCode + ".apk")
            val partial = File(folder, "lanu-" + release.versionCode + ".download.apk")
            val prefix = "https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases/download/" + release.tag + "/"
            if (!Regex("""lanu-music-\d+-[0-9a-f]{40}""").matches(release.tag) ||
                release.apkUrl != prefix + "app-release.apk" ||
                release.sha256Url != prefix + "app-release.apk.sha256" ||
                release.sizeBytes !in 1L..(200L * 1024L * 1024L)
            ) throw IOException("Güvenilmeyen güncelleme kaynağı veya dosya boyutu.")
            pruneOldUpdates(folder, release.versionCode)
            val expected = get(release.sha256Url).use {
                UpdateReleasePolicy.parseExpectedSha256(responseBody(it, 1024))
            }
            if (release.githubDigest != null &&
                !expected.equals(release.githubDigest, ignoreCase = true)
            ) throw IOException("GitHub yayımlanan SHA-256 bilgileri uyuşmuyor. Kurulum engellendi.")
            // A previously fully verified download can be reused after process death,
            // but ONLY if the freshly fetched SHA-256 still matches.
            if (finalApk.isFile) {
                val cached = runCatching {
                    finalApk.length() == release.sizeBytes &&
                        sha256Of(finalApk).equals(expected, ignoreCase = true)
                }.getOrDefault(false)
                if (cached) {
                    verifyApk(finalApk, release.versionCode)
                    onProgress(100)
                    return@withContext finalApk
                }
                finalApk.delete()
            }
            // One retry from byte zero handles stale or mismatched partial downloads.
            for (attempt in 0..1) {
                coroutineContext.ensureActive()
                if (attempt == 1) partial.delete()
                downloadRemaining(release.apkUrl, partial, release.sizeBytes, onProgress)
                if (partial.length() == release.sizeBytes &&
                    sha256Of(partial).equals(expected, ignoreCase = true)
                ) {
                    try {
                        verifyApk(partial, release.versionCode)
                        if (!partial.renameTo(finalApk))
                            throw IOException("Doğrulanan APK kaydedilemedi.")
                        onProgress(100)
                        return@withContext finalApk
                    } catch (e: Exception) {
                        partial.delete() // Never retain an invalidly signed candidate
                        throw e
                    }
                }
            }
            partial.delete()
            throw IOException("APK SHA-256 doğrulaması başarısız. Kurulum engellendi.")
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
        if (candidate.applicationInfo?.minSdkVersion?.let { it > Build.VERSION.SDK_INT } == true)
            throw IOException("Bu Android sürümü yeni APK ile uyumlu değil.")

        // PackageManager.getPackageArchiveInfo may omit signingInfo for an
        // uninstalled APK even when its signatures are valid. Verify the bytes
        // and signer certificates using Google's official APK verifier.
        val verification = try {
            ApkVerifier.Builder(file)
                .setMinCheckedPlatformVersion(Build.VERSION.SDK_INT)
                .setMaxCheckedPlatformVersion(Build.VERSION.SDK_INT)
                .build()
                .verify()
        } catch (e: Exception) {
            throw IOException("APK imzası doğrulanamadı. Kurulum engellendi.", e)
        }
        if (!verification.isVerified || verification.signerCertificates.isEmpty())
            throw IOException("APK dijital imzası geçersiz. Kurulum engellendi.")
        val candidateSigners = verification.signerCertificates.map { certificate ->
            MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
                .joinToString("") { b -> "%02x".format(b) }
        }.toSet()
        val installedSigners = signatureDigests(installed)
        if (installedSigners.isEmpty() || installedSigners != candidateSigners)
            throw IOException("APK imzası yüklü LANU sürümüyle eşleşmiyor. " +
                "Verilerinizi korumak için kurulum engellendi; uygulamayı kaldırmayın.")

    }
}
