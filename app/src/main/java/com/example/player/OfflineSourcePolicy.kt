package com.example.player

import android.net.Uri

object OfflineSourcePolicy {
    fun isAuthorized(uri: Uri): Boolean = isAuthorized(uri.toString())

    /** Pure JVM-friendly policy check. Only local content/file sources are allowed. */
    fun isAuthorized(source: String): Boolean {
        val scheme = source.substringBefore(':', missingDelimiterValue = "")
            .trim()
            .lowercase()
        return scheme == "content" || scheme == "file"
    }
}
