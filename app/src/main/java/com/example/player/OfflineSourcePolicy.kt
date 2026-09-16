package com.example.player

import android.net.Uri

object OfflineSourcePolicy {
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
