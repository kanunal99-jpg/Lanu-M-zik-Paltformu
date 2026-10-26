package com.example.player

import android.net.Uri
import com.example.BuildConfig
import com.example.model.Song
import com.example.model.SongSourceType
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

object OfflineSourcePolicy {
    private const val MAX_LOCAL_SOURCE_LENGTH = 8192

    /**
     * Only genuine LOCAL song records may be copied. File URIs must resolve to
     * shared/removable media, never an app-private or platform-private file.
     * This gates NEW offline copies; existing downloads and playback are not changed.
     */
    fun isAuthorized(song: Song): Boolean =
        song.sourceType == SongSourceType.LOCAL && isAuthorized(song.audioUrl)

    fun isAuthorized(uri: Uri): Boolean = isAuthorized(uri.toString())

    /**
     * Keep Android MediaStore / document content URIs and historical shared-storage
     * file URIs available, while rejecting opaque/malformed sources, path traversal
     * and symlinks that escape shared media into private application directories.
     */
    fun isAuthorized(source: String): Boolean {
        if (source.isEmpty() || source.length > MAX_LOCAL_SOURCE_LENGTH ||
            source.any { Character.isISOControl(it) }) return false
        // Spaces can legitimately occur in local filesystem paths. Encode only
        // literal spaces for URI parsing; preserve the original input for callers.
        val parsed = try {
            URI(source.replace(" ", "%20"))
        } catch (_: URISyntaxException) {
            return false
        }
        if (parsed.isOpaque || parsed.rawUserInfo != null ||
            parsed.rawFragment != null || parsed.rawQuery != null) return false
        return when (parsed.scheme?.lowercase(Locale.ROOT)) {
            "content" -> {
                val authority = parsed.host?.lowercase(Locale.ROOT) ?: return false
                val ownAuthority = BuildConfig.APPLICATION_ID.lowercase(Locale.ROOT)
                // A corrupt LOCAL row must not copy the app's own FileProvider
                // cache/OTA or any other private content provider into offline music.
                authority.isNotBlank() && !parsed.path.isNullOrBlank() &&
                    authority != ownAuthority && !authority.startsWith("$ownAuthority.")
            }
            "file" -> {
                // A file://hostname/path may designate a remote share.
                if (!parsed.host.isNullOrEmpty()) return false
                val decodedPath = parsed.path ?: return false
                if (!decodedPath.startsWith("/")) return false
                val canonicalPath = try {
                    File(decodedPath).canonicalPath
                } catch (_: Exception) {
                    return false
                }
                isSharedMediaPath(decodedPath) && isSharedMediaPath(canonicalPath)
            }
            else -> false
        }
    }

    private fun isSharedMediaPath(path: String): Boolean = listOf(
        "/storage/", "/sdcard/", "/mnt/media_rw/", "/mnt/sdcard/",
        "/mnt/user/", "/mnt/runtime/", "/data/media/"
    ).any(path::startsWith)
}
