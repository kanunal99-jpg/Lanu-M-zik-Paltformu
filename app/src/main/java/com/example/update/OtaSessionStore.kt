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

    fun save(release: VerifiedRelease, expectedSha256: String) {
        require(tagPattern.matches(release.tag))
        require(shaPattern.matches(expectedSha256))
        prefs.edit()
            .putString("tag", release.tag)
            .putInt("code", release.versionCode)
            .putString("sha256", expectedSha256)
            .putLong("size", release.sizeBytes)
            .putString("github_digest", release.githubDigest)
            .apply()
    }

    fun restore(installedCode: Int): RestorableUpdate? {
        val tag = prefs.getString("tag", null) ?: return null
        val sha256 = prefs.getString("sha256", null) ?: return null
        val code = prefs.getInt("code", -1)
        val bytes = prefs.getLong("size", -1)
        val digest = prefs.getString("github_digest", null)
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
        prefs.edit().clear().apply()
    }
}
