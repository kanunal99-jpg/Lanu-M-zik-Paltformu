package com.example.player

import android.net.Uri

object OfflineSourcePolicy {
    fun isAuthorized(uri: Uri): Boolean = isAuthorized(uri.toString())

    /**
     * Sources that may be persisted for offline playback.
     * HTTP(S) is permitted only for verified catalog providers; arbitrary
     * playback URLs must still be rejected by the catalog layer.
     */
    fun isAuthorized(source: String): Boolean {
        val scheme = source.substringBefore(':', missingDelimiterValue = "")
            .trim()
            .lowercase()
        return scheme == "content" || scheme == "file" || scheme == "http" || scheme == "https"
    }
}
