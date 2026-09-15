package com.example.player

import android.net.Uri

object OfflineSourcePolicy {
    fun isAuthorized(uri: Uri): Boolean = when (uri.scheme?.lowercase()) {
        "content", "file" -> true
        else -> false
    }
}
