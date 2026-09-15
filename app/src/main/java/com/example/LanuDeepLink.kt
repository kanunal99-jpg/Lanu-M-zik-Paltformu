package com.example

import android.net.Uri

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
            if (uri == null || uri.scheme != "lanumusic") return null
            val id = uri.pathSegments.firstOrNull().orEmpty()
            return when (uri.host?.lowercase()) {
                "home" -> Home
                "library" -> Library
                "search" -> Search(uri.getQueryParameter("q").orEmpty())
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
    }
}
