package com.example.update

import android.content.Context
import com.example.data.update.VerifiedRelease
import java.io.File

data class RestorableUpdate(val release: VerifiedRelease, val expectedSha256: String, val apk: File)

/**
 * Only a resume hint: persisted state is NEVER trusted without recalculating
 * SHA-256 and validating the Android package/signature at startup.
 * This is private app storage; it never modifies users' playlists or songs.
 */
class OtaSessionStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("lanu_ota_verified_session", Context.MODE_PRIVATE)
    private val tagPattern = Regex("""lanu-music-(\d+)-[0-9a-f]{40}""")
    private val shaPattern = Regex("[a-fA-F0-9]{64}")

    private data class StoredOtaHint(
        val tag: String?,
        val sha256: String?,
        val code: Int,
        val bytes: Long,
        val digest: String?
    )

    fun save(release: VerifiedRelease, expectedSha256: String) {
        // Session state is a resume hint, but it must be internally coherent
        // before reaching disk. Never accept an unrelated release or asset URL.
        val code = tagPattern.matchEntire(release.tag)
            ?.groupValues?.get(1)?.toIntOrNull()
        require(code != null && code == release.versionCode) { "OTA sürüm kimliği uyuşmuyor." }
        require(shaPattern.matches(expectedSha256)) { "OTA SHA-256 geçersiz." }
        require(release.sizeBytes in 1L..(200L * 1024L * 1024L)) {
            "OTA APK boyutu geçersiz."
        }
        require(release.githubDigest == null || shaPattern.matches(release.githubDigest)) {
            "OTA GitHub özeti geçersiz."
        }
        val releases = "https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases"
        val prefix = releases + "/download/" + release.tag + "/"
        require(release.pageUrl == releases + "/tag/" + release.tag &&
            release.apkUrl == prefix + "app-release.apk" &&
            release.sha256Url == prefix + "app-release.apk.sha256"
        ) { "OTA kaynak bağlantısı güvenilir değil." }
        val saved = prefs.edit()
            .putString("tag", release.tag)
            .putInt("code", release.versionCode)
            .putString("sha256", expectedSha256)
            .putLong("size", release.sizeBytes)
            .putString("github_digest", release.githubDigest)
            .commit()
        if (!saved) throw java.io.IOException("OTA durumu diske kaydedilemedi.")
    }

    /**
     * Restore is a hint only, not a trust boundary. OEM backup/import can leave
     * a field with an unexpected SharedPreferences type, e.g. size as String.
     * Drop ONLY the unusable OTA hint. Keep APK cache and all user content.
     */
    fun restore(installedCode: Int): RestorableUpdate? {
        val stored = try {
            StoredOtaHint(
                tag = prefs.getString("tag", null),
                sha256 = prefs.getString("sha256", null),
                code = prefs.getInt("code", -1),
                bytes = prefs.getLong("size", -1),
                digest = prefs.getString("github_digest", null)
            )
        } catch (_: ClassCastException) {
            clear()
            return null
        }
        val tag = stored.tag ?: return null
        val sha256 = stored.sha256 ?: return null
        val code = stored.code
        val bytes = stored.bytes
        val digest = stored.digest
        if (!tagPattern.matches(tag) || tagPattern.matchEntire(tag)?.groupValues?.get(1)?.toIntOrNull() != code ||
            code <= installedCode || !shaPattern.matches(sha256) ||
            bytes !in 1L..(200L * 1024L * 1024L) ||
            (digest != null && !shaPattern.matches(digest))
        ) {
            clear()
            return null
        }
        val releases = "https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases"
        val prefix = releases + "/download/" + tag + "/"
        val release = VerifiedRelease(code, tag, releases + "/tag/" + tag,
            prefix + "app-release.apk", prefix + "app-release.apk.sha256", bytes, digest)
        return RestorableUpdate(release, sha256, File(context.cacheDir, "lanu_updates/lanu-" + code + ".apk"))
    }

    fun clear() {
        prefs.edit().clear().commit()
    }
}
