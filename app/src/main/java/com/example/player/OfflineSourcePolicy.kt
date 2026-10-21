package com.example.player

import android.net.Uri
import com.example.model.Song
import com.example.model.SongSourceType

object OfflineSourcePolicy {
    /**
     * Reject remotely sourced catalog records that spoof a local content://
     * or file:// URI. Scheme alone is not a provenance check.
     *
     * Remote downloading is intentionally not offered by the current policy.
     * Never open or copy a local private file as a side effect of untrusted
     * catalog metadata claiming that it is local audio.
     */
    fun isAuthorized(song: Song): Boolean =
        song.sourceType == SongSourceType.LOCAL && isAuthorized(song.audioUrl)

    fun isAuthorized(uri: Uri): Boolean = isAuthorized(uri.toString())

    /**
     * Sources that may be persisted for offline playback.
     *
     * Offline persistence is restricted to media already available to the
     * device (MediaStore/content URIs or local files). Remote HTTP(S) URLs
     * are intentionally rejected here; a verified catalog may use remote
     * playback through its playback/source-validation layer, but arbitrary
     * remote URLs must never become offline files through this policy.
     */
    fun isAuthorized(source: String): Boolean {
        val scheme = source.substringBefore(':', missingDelimiterValue = "")
            .trim()
            .lowercase()
        return scheme == "content" || scheme == "file"
    }
}
