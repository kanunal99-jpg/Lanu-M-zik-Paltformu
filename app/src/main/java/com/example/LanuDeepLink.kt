package com.example

import android.net.Uri
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed interface LanuDeepLink {
    data object Home : LanuDeepLink
    data object Library : LanuDeepLink
    data class Search(val query: String = "") : LanuDeepLink
    data class Track(val id: String) : LanuDeepLink
    data class Playlist(val id: String) : LanuDeepLink
    data class Artist(val id: String) : LanuDeepLink
    data class PlayerAction(val action: Action) : LanuDeepLink

    enum class Action { PLAY_PAUSE, NEXT, PREVIOUS, OPEN_PLAYER }

    companion object {
        fun parse(uri: Uri?): LanuDeepLink? {
            if (uri == null) return null
            return parse(uri.toString())
        }

        /** Pure JVM-friendly parser used by tests and any non-Android callers. */
        fun parse(raw: String): LanuDeepLink? {
            // The Activity is exported for deep links. Reject untrusted oversized
            // and malformed URI text before string decoding or Compose navigation.
            if (raw.length > MAX_URI_LENGTH) return null
            val uri = runCatching { URI(raw) }.getOrNull() ?: return null
            if (!uri.scheme.equals("lanumusic", ignoreCase = true)) return null
            if (uri.rawUserInfo != null || uri.port != -1) return null

            val host = uri.host?.lowercase() ?: return null
            // Decode only after size validation. An invalid percent escape
            // must not crash the Activity when opened by another app.
            val rawId = uri.rawPath?.removePrefix("/").orEmpty()
            if (rawId.length > MAX_ID_LENGTH * 3 || rawId.contains('/')) return null
            val id = decodeSafely(rawId) ?: return null
            if (id.length > MAX_ID_LENGTH || id.any { it.isISOControl() }) return null

            return when (host) {
                "home" -> Home
                "library" -> Library
                "search" -> {
                    val params = parseQuery(uri.rawQuery) ?: return null
                    val query = params["q"].orEmpty()
                    if (query.length > MAX_SEARCH_LENGTH ||
                        query.any { it.isISOControl() }) return null
                    Search(query)
                }
                "track" -> id.takeIf { it.isNotBlank() }?.let(::Track)
                "playlist" -> id.takeIf { it.isNotBlank() }?.let(::Playlist)
                "artist" -> id.takeIf { it.isNotBlank() }?.let(::Artist)
                "action" -> when (id.lowercase()) {
                    "playpause" -> PlayerAction(Action.PLAY_PAUSE)
                    "next" -> PlayerAction(Action.NEXT)
                    "previous" -> PlayerAction(Action.PREVIOUS)
                    "open_player" -> PlayerAction(Action.OPEN_PLAYER)
                    else -> null
                }
                else -> null
            }
        }

        private const val MAX_URI_LENGTH = 4096
        private const val MAX_ID_LENGTH = 512
        private const val MAX_SEARCH_LENGTH = 200

        private fun decodeSafely(value: String): String? =
            runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }
                .getOrNull()

        private fun parseQuery(rawQuery: String?): Map<String, String>? {
            val raw = rawQuery ?: return emptyMap()
            if (raw.length > MAX_URI_LENGTH) return null
            val result = linkedMapOf<String, String>()
            // Keep parsing work bounded even for adversarial &-separated inputs.
            val pairs = raw.split('&')
            if (pairs.size > 16) return null
            for (pair in pairs) {
                if (pair.isBlank()) continue
                val parts = pair.split('=', limit = 2)
                val key = decodeSafely(parts[0]) ?: return null
                val value = decodeSafely(parts.getOrElse(1) { "" }) ?: return null
                if (key.length > 32 || value.length > MAX_SEARCH_LENGTH * 3) return null
                result[key] = value
            }
            return result
        }
    }
}
