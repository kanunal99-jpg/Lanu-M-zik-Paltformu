package com.example.player

import com.example.data.catalog.CatalogLicensePolicy
import com.example.model.Song
import com.example.model.SongSourceType
import java.io.File
import java.net.URI
import java.net.URISyntaxException

/**
 * Pure playback trust boundary shared by restored and interactive playback.
 *
 * The untrusted catalog ID must NEVER become an unchecked filesystem path.
 * The source URI is independent of the stored ID and must be authorized by
 * provenance + source license. HTTP remote playback is forbidden.
 */
internal object PlaybackUriPolicy {
    // Signed provider URLs can contain query tokens; keep the limit generous while
    // preventing pathological untrusted catalog fields from reaching media clients.
    private const val MAX_REMOTE_AUDIO_URI_LENGTH = 8192

    /**
     * A string beginning with "https://" is not necessarily a usable HTTPS URI.
     * Both the in-app player and external Media3 clients must fail closed for
     * missing authorities, userinfo, malformed percent escapes and controls.
     * Never rewrite valid provider URLs or strip signed query parameters.
     */
    private fun validatedHttpsAudioUri(raw: String): String? {
        if (!raw.startsWith("https://") || raw.length > MAX_REMOTE_AUDIO_URI_LENGTH ||
            raw.any { it.isWhitespace() || Character.isISOControl(it) }
        ) return null
        val uri = try {
            URI(raw)
        } catch (_: URISyntaxException) {
            return null
        }
        return raw.takeIf {
            !uri.isOpaque && uri.scheme == "https" &&
                !uri.host.isNullOrBlank() && uri.rawUserInfo == null
        }
    }

    fun managedOfflineFile(filesDir: File, songId: String): File? =
        if (OfflineFileNamePolicy.accepts(songId)) {
            File(File(filesDir, "offline_audio"), "$songId.bin")
        } else {
            null
        }

    fun verifiedSource(song: Song): String? {
        val raw = song.audioUrl.trim()
        return when (song.sourceType) {
            SongSourceType.LOCAL ->
                raw.takeIf { it.startsWith("content://") || it.startsWith("file://") }
            SongSourceType.VERIFIED_REMOTE ->
                if (CatalogLicensePolicy.isPermittedRemoteLicense(song.license)) {
                    validatedHttpsAudioUri(raw)
                } else null
            SongSourceType.VERIFIED_PREVIEW -> validatedHttpsAudioUri(raw)
            SongSourceType.UNKNOWN -> null
        }
    }
}
