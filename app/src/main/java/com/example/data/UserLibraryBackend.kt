package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Provider-agnostic user library boundary.
 *
 * The application talks to this contract instead of a specific cloud SDK.
 * LocalUserLibraryBackend is the safe offline implementation. A remote provider
 * can be layered above it later without changing player/UI code.
 */
interface UserLibraryBackend {
    suspend fun snapshot(userId: String): UserLibrarySnapshot
    suspend fun addFavorite(userId: String, songId: String, nowMs: Long = System.currentTimeMillis())
    suspend fun removeFavorite(userId: String, songId: String)
    suspend fun createPlaylist(userId: String, name: String, description: String = "", nowMs: Long = System.currentTimeMillis()): PlaylistRecord
    suspend fun deletePlaylist(userId: String, playlistId: String)
    suspend fun addSongToPlaylist(userId: String, playlistId: String, songId: String, nowMs: Long = System.currentTimeMillis())
    suspend fun removeSongFromPlaylist(userId: String, playlistId: String, songId: String)
    suspend fun renamePlaylist(userId: String, playlistId: String, name: String, description: String = "", nowMs: Long = System.currentTimeMillis()) {
        error("Playlist rename operation is not supported by this provider")
    }
    suspend fun moveSongInPlaylist(userId: String, playlistId: String, songId: String, targetIndex: Int, nowMs: Long = System.currentTimeMillis()) {
        error("Playlist reorder operation is not supported by this provider")
    }
    suspend fun recordPlay(userId: String, songId: String, playedAtMs: Long = System.currentTimeMillis())
    suspend fun clearHistory(userId: String)
    suspend fun clearUser(userId: String)
}

data class FavoriteRecord(val songId: String, val addedAtMs: Long)

data class PlaylistRecord(
    val id: String,
    val name: String,
    val description: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val songIds: List<String>
)

data class HistoryRecord(val songId: String, val playedAtMs: Long)

data class UserLibrarySnapshot(
    val userId: String,
    val favorites: List<FavoriteRecord> = emptyList(),
    val playlists: List<PlaylistRecord> = emptyList(),
    val history: List<HistoryRecord> = emptyList()
)

/** Persistent local implementation. Each user has an isolated preference namespace. */
class LocalUserLibraryBackend(context: Context) : UserLibraryBackend {
    private val preferences = context.applicationContext.getSharedPreferences("lanu_user_library", Context.MODE_PRIVATE)

    override suspend fun snapshot(userId: String): UserLibrarySnapshot {
        requireValidUserId(userId)
        return UserLibrarySnapshot(userId, readFavorites(userId), readPlaylists(userId), readHistory(userId))
    }

    override suspend fun addFavorite(userId: String, songId: String, nowMs: Long) {
        requireValidUserId(userId); require(songId.isNotBlank()) { "songId boş olamaz" }
        val favorites = readFavorites(userId).filterNot { it.songId == songId } + FavoriteRecord(songId, nowMs)
        writeFavorites(userId, favorites.sortedByDescending { it.addedAtMs })
    }

    override suspend fun removeFavorite(userId: String, songId: String) {
        requireValidUserId(userId); writeFavorites(userId, readFavorites(userId).filterNot { it.songId == songId })
    }

    override suspend fun createPlaylist(userId: String, name: String, description: String, nowMs: Long): PlaylistRecord {
        requireValidUserId(userId); require(name.isNotBlank()) { "Playlist adı boş olamaz" }
        val playlist = PlaylistRecord("pl-${UUID.randomUUID()}", name.trim(), description.trim(), nowMs, nowMs, emptyList())
        writePlaylists(userId, readPlaylists(userId) + playlist)
        return playlist
    }

    override suspend fun deletePlaylist(userId: String, playlistId: String) {
        requireValidUserId(userId); writePlaylists(userId, readPlaylists(userId).filterNot { it.id == playlistId })
    }

    override suspend fun addSongToPlaylist(userId: String, playlistId: String, songId: String, nowMs: Long) {
        requireValidUserId(userId); require(songId.isNotBlank()) { "songId boş olamaz" }
        val playlists = readPlaylists(userId)
        val target = playlists.firstOrNull { it.id == playlistId } ?: error("Playlist bulunamadı: $playlistId")
        val updated = target.copy(updatedAtMs = nowMs, songIds = (target.songIds + songId).distinct())
        writePlaylists(userId, playlists.map { if (it.id == playlistId) updated else it })
    }

    override suspend fun removeSongFromPlaylist(userId: String, playlistId: String, songId: String) {
        requireValidUserId(userId)
        val playlists = readPlaylists(userId)
        writePlaylists(userId, playlists.map { playlist ->
            if (playlist.id == playlistId) playlist.copy(updatedAtMs = System.currentTimeMillis(), songIds = playlist.songIds.filterNot { it == songId }) else playlist
        })
    }

    override suspend fun renamePlaylist(userId: String, playlistId: String, name: String, description: String, nowMs: Long) {
        requireValidUserId(userId); require(name.isNotBlank()) { "Playlist adı boş olamaz" }
        val playlists = readPlaylists(userId)
        if (playlists.none { it.id == playlistId }) error("Playlist bulunamadı: $playlistId")
        writePlaylists(userId, playlists.map { playlist ->
            if (playlist.id == playlistId) playlist.copy(name = name.trim(), description = description.trim(), updatedAtMs = nowMs) else playlist
        })
    }

    override suspend fun moveSongInPlaylist(userId: String, playlistId: String, songId: String, targetIndex: Int, nowMs: Long) {
        requireValidUserId(userId)
        val playlists = readPlaylists(userId)
        val playlist = playlists.firstOrNull { it.id == playlistId } ?: error("Playlist bulunamadı: $playlistId")
        if (songId !in playlist.songIds) error("Parça playlist içinde bulunamadı: $songId")
        val songs = playlist.songIds.toMutableList()
        val fromIndex = songs.indexOf(songId)
        songs.removeAt(fromIndex)
        songs.add(targetIndex.coerceIn(0, songs.size), songId)
        writePlaylists(userId, playlists.map { item ->
            if (item.id == playlistId) item.copy(songIds = songs, updatedAtMs = nowMs) else item
        })
    }

    override suspend fun recordPlay(userId: String, songId: String, playedAtMs: Long) {
        requireValidUserId(userId); require(songId.isNotBlank()) { "songId boş olamaz" }
        val history = listOf(HistoryRecord(songId, playedAtMs)) + readHistory(userId).filterNot { it.songId == songId }
        writeHistory(userId, history.take(MAX_HISTORY_ITEMS))
    }

    override suspend fun clearHistory(userId: String) {
        requireValidUserId(userId); preferences.edit().remove(historyKey(userId)).apply()
    }

    override suspend fun clearUser(userId: String) {
        requireValidUserId(userId)
        preferences.edit().remove(favoritesKey(userId)).remove(playlistsKey(userId)).remove(historyKey(userId)).apply()
    }

    private fun readFavorites(userId: String): List<FavoriteRecord> = runCatching {
        val array = JSONArray(preferences.getString(favoritesKey(userId), "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index); FavoriteRecord(item.getString("songId"), item.getLong("addedAtMs"))
        }
    }.getOrDefault(emptyList())

    private fun writeFavorites(userId: String, favorites: List<FavoriteRecord>) {
        val array = JSONArray(); favorites.forEach { array.put(JSONObject().put("songId", it.songId).put("addedAtMs", it.addedAtMs)) }
        preferences.edit().putString(favoritesKey(userId), array.toString()).apply()
    }

    private fun readPlaylists(userId: String): List<PlaylistRecord> = runCatching {
        val array = JSONArray(preferences.getString(playlistsKey(userId), "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index); val songs = item.optJSONArray("songIds") ?: JSONArray()
            PlaylistRecord(item.getString("id"), item.getString("name"), item.optString("description"), item.getLong("createdAtMs"), item.getLong("updatedAtMs"), (0 until songs.length()).map(songs::getString))
        }
    }.getOrDefault(emptyList())

    private fun writePlaylists(userId: String, playlists: List<PlaylistRecord>) {
        val array = JSONArray(); playlists.forEach { playlist ->
            val songArray = JSONArray().apply { playlist.songIds.forEach(::put) }
            array.put(JSONObject().put("id", playlist.id).put("name", playlist.name).put("description", playlist.description).put("createdAtMs", playlist.createdAtMs).put("updatedAtMs", playlist.updatedAtMs).put("songIds", songArray))
        }
        preferences.edit().putString(playlistsKey(userId), array.toString()).apply()
    }

    private fun readHistory(userId: String): List<HistoryRecord> = runCatching {
        val array = JSONArray(preferences.getString(historyKey(userId), "[]"))
        (0 until array.length()).map { index -> val item = array.getJSONObject(index); HistoryRecord(item.getString("songId"), item.getLong("playedAtMs")) }
    }.getOrDefault(emptyList())

    private fun writeHistory(userId: String, history: List<HistoryRecord>) {
        val array = JSONArray(); history.take(MAX_HISTORY_ITEMS).forEach { array.put(JSONObject().put("songId", it.songId).put("playedAtMs", it.playedAtMs)) }
        preferences.edit().putString(historyKey(userId), array.toString()).apply()
    }

    private fun requireValidUserId(userId: String) { require(userId.isNotBlank()) { "userId boş olamaz" } }
    private fun favoritesKey(userId: String) = "favorites::$userId"
    private fun playlistsKey(userId: String) = "playlists::$userId"
    private fun historyKey(userId: String) = "history::$userId"

    private companion object { const val MAX_HISTORY_ITEMS = 50 }
}
