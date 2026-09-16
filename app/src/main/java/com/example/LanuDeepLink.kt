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
            val uri = runCatching { URI(raw) }.getOrNull() ?: return null
            if (!uri.scheme.equals("lanumusic", ignoreCase = true)) return null

            val host = uri.host?.lowercase() ?: return null
            val id = uri.path
                ?.removePrefix("/")
                ?.takeIf { it.isNotBlank() }
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                .orEmpty()

            return when (host) {
                "home" -> Home
                "library" -> Library
                "search" -> Search(parseQuery(uri.rawQuery)["q"].orEmpty())
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

        private fun parseQuery(rawQuery: String?): Map<String, String> =
            rawQuery.orEmpty()
                .split('&')
                .asSequence()
                .filter { it.isNotBlank() }
                .map { pair ->
                    val parts = pair.split('=', limit = 2)
                    val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name())
                    val value = URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
                    key to value
                }
                .toMap()
    }
}
