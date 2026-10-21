package com.example.player

import com.example.data.catalog.CatalogLicensePolicy
import com.example.model.Song
import com.example.model.SongSourceType
import java.io.File

/**
 * Pure playback trust boundary shared by restored and interactive playback.
 *
 * The untrusted catalog ID must NEVER become an unchecked filesystem path.
 * The source URI is independent of the stored ID and must be authorized by
 * provenance + source license. HTTP remote playback is forbidden.
 */
internal object PlaybackUriPolicy {
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
                raw.takeIf {
                    it.startsWith("https://") &&
                        CatalogLicensePolicy.isPermittedRemoteLicense(song.license)
                }
            SongSourceType.VERIFIED_PREVIEW -> raw.takeIf { it.startsWith("https://") }
            SongSourceType.UNKNOWN -> null
        }
    }
}
