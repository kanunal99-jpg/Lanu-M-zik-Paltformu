package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
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
    suspend fun createPlaylist(
        userId: String,
        name: String,
        description: String = "",
        nowMs: Long = System.currentTimeMillis()
    ): PlaylistRecord
    suspend fun renamePlaylist(userId: String, playlistId: String, name: String, nowMs: Long = System.currentTimeMillis())
    suspend fun reorderPlaylist(userId: String, playlistId: String, fromIndex: Int, toIndex: Int, nowMs: Long = System.currentTimeMillis())
    suspend fun deletePlaylist(userId: String, playlistId: String)
    suspend fun addSongToPlaylist(userId: String, playlistId: String, songId: String, nowMs: Long = System.currentTimeMillis())
    suspend fun removeSongFromPlaylist(userId: String, playlistId: String, songId: String)
    suspend fun recordPlay(userId: String, songId: String, playedAtMs: Long = System.currentTimeMillis())
    suspend fun clearHistory(userId: String)
    suspend fun clearUser(userId: String)
}

data class FavoriteRecord(
    val songId: String,
    val addedAtMs: Long
)

data class PlaylistRecord(
    val id: String,
    val name: String,
    val description: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val songIds: List<String>
)

data class HistoryRecord(
    val songId: String,
    val playedAtMs: Long
)

data class UserLibrarySnapshot(
    val userId: String,
    val favorites: List<FavoriteRecord> = emptyList(),
    val playlists: List<PlaylistRecord> = emptyList(),
    val history: List<HistoryRecord> = emptyList()
)

/**
 * Persistent local implementation. Each user has an isolated preference namespace.
 * This is intentionally an offline fallback, not a fake cloud account.
 */
class LocalUserLibraryBackend(context: Context) : UserLibraryBackend {
    private val preferences = context.applicationContext.getSharedPreferences(
        "lanu_user_library",
        Context.MODE_PRIVATE
    )

    // Process-wide because UI/service can construct separate backend instances for the
    // SAME named preferences. Each read-modify-write must be atomic.
    private suspend fun <T> transaction(block: () -> T): T =
        withContext(Dispatchers.IO) { libraryWriteMutex.withLock { block() } }

    // Do not acknowledge user data saves before Android confirms persistence.
    private fun SharedPreferences.Editor.persist() {
        if (!commit()) throw IOException("Yerel müzik arşivi diske kaydedilemedi.")
    }

    override suspend fun snapshot(userId: String): UserLibrarySnapshot = transaction {
        requireValidUserId(userId)
        UserLibrarySnapshot(
            userId = userId,
            favorites = readFavorites(userId),
            playlists = readPlaylists(userId),
            history = readHistory(userId)
        )
    }


    /**
     * Legacy Room data is strictly a one-time import source. An existing modern
     * preferences key (even "[]") is proof that the user has already edited the
     * modern library; importing old rows then could resurrect deleted records.
     *
     * An atomic SharedPreferences commit stores all three sections + marker.
     * If the commit fails, Room rows remain intact and the next start may retry.
     * The process-wide libraryWriteMutex serializes it against user edits and
     * imports running in another repository instance.
     */
    suspend fun needsLegacyImport(userId: String): Boolean = transaction {
        requireValidUserId(userId)
        !hasModernLibraryFootprint(userId)
    }

    suspend fun importLegacyIfUninitialized(legacy: UserLibrarySnapshot): Boolean = transaction {
        val userId = legacy.userId
        requireValidUserId(userId)
        if (hasModernLibraryFootprint(userId)) return@transaction false

        // Do not split this into addFavorite/createPlaylist/recordPlay calls:
        // that would expose a half-import and could duplicate playlists on retry.
        preferences.edit()
            .putString(favoritesKey(userId), serializeFavorites(legacy.favorites))
            .putString(playlistsKey(userId), serializePlaylists(legacy.playlists))
            .putString(historyKey(userId), serializeHistory(legacy.history))
            .putBoolean(legacyImportedKey(userId), true)
            .persist()
        true
    }

    private fun hasModernLibraryFootprint(userId: String): Boolean =
        preferences.contains(legacyImportedKey(userId)) ||
            preferences.contains(favoritesKey(userId)) ||
            preferences.contains(playlistsKey(userId)) ||
            preferences.contains(historyKey(userId))

    private fun legacyImportedKey(userId: String) = "legacy_imported_v1::$userId"

    private fun serializeFavorites(favorites: List<FavoriteRecord>): String {
        val array = JSONArray()
        favorites.forEach {
            array.put(JSONObject().put("songId", it.songId).put("addedAtMs", it.addedAtMs))
        }
        return array.toString()
    }

    private fun serializePlaylists(playlists: List<PlaylistRecord>): String {
        val array = JSONArray()
        playlists.forEach { playlist ->
            val songArray = JSONArray().apply { playlist.songIds.forEach(::put) }
            array.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("description", playlist.description)
                    .put("createdAtMs", playlist.createdAtMs)
                    .put("updatedAtMs", playlist.updatedAtMs)
                    .put("songIds", songArray)
            )
        }
        return array.toString()
    }

    private fun serializeHistory(history: List<HistoryRecord>): String {
        val array = JSONArray()
        history.take(MAX_HISTORY_ITEMS).forEach {
            array.put(JSONObject().put("songId", it.songId).put("playedAtMs", it.playedAtMs))
        }
        return array.toString()
    }

    override suspend fun addFavorite(userId: String, songId: String, nowMs: Long) = transaction {
        requireValidUserId(userId)
        require(songId.isNotBlank()) { "songId boş olamaz" }
        val favorites = readFavorites(userId).filterNot { it.songId == songId } + FavoriteRecord(songId, nowMs)
        writeFavorites(userId, favorites.sortedByDescending { it.addedAtMs })
    }

    override suspend fun removeFavorite(userId: String, songId: String) = transaction {
        requireValidUserId(userId)
        writeFavorites(userId, readFavorites(userId).filterNot { it.songId == songId })
    }

    override suspend fun createPlaylist(
        userId: String,
        name: String,
        description: String,
        nowMs: Long
    ): PlaylistRecord = transaction {
        requireValidUserId(userId)
        require(name.isNotBlank()) { "Playlist adı boş olamaz" }
        val playlist = PlaylistRecord(
            id = "pl-${UUID.randomUUID()}",
            name = name.trim(),
            description = description.trim(),
            createdAtMs = nowMs,
            updatedAtMs = nowMs,
            songIds = emptyList()
        )
        writePlaylists(userId, readPlaylists(userId) + playlist)
        return@transaction playlist
    }

    override suspend fun renamePlaylist(userId: String, playlistId: String, name: String, nowMs: Long) = transaction {
        requireValidUserId(userId)
        require(name.isNotBlank()) { "Playlist adı boş olamaz" }
        val playlists = readPlaylists(userId)
        val target = playlists.firstOrNull { it.id == playlistId }
            ?: error("Playlist bulunamadı: $playlistId")
        writePlaylists(
            userId,
            playlists.map {
                if (it.id == target.id) it.copy(name = name.trim(), updatedAtMs = nowMs) else it
            }
        )
    }

    override suspend fun reorderPlaylist(userId: String, playlistId: String, fromIndex: Int, toIndex: Int, nowMs: Long) = transaction {
        requireValidUserId(userId)
        val playlists = readPlaylists(userId)
        val target = playlists.firstOrNull { it.id == playlistId }
            ?: error("Playlist bulunamadı: $playlistId")
        if (target.songIds.isEmpty()) return@transaction
        require(fromIndex in target.songIds.indices) { "fromIndex geçersiz" }
        require(toIndex in target.songIds.indices) { "toIndex geçersiz" }
        if (fromIndex == toIndex) return@transaction
        val reorderedSongs = target.songIds.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        val updated = target.copy(songIds = reorderedSongs, updatedAtMs = nowMs)
        writePlaylists(userId, playlists.map { if (it.id == playlistId) updated else it })
    }

    override suspend fun deletePlaylist(userId: String, playlistId: String) = transaction {
        requireValidUserId(userId)
        writePlaylists(userId, readPlaylists(userId).filterNot { it.id == playlistId })
    }

    override suspend fun addSongToPlaylist(userId: String, playlistId: String, songId: String, nowMs: Long) = transaction {
        requireValidUserId(userId)
        require(songId.isNotBlank()) { "songId boş olamaz" }
        val playlists = readPlaylists(userId)
        val target = playlists.firstOrNull { it.id == playlistId }
            ?: error("Playlist bulunamadı: $playlistId")
        val updated = target.copy(
            updatedAtMs = nowMs,
            songIds = (target.songIds + songId).distinct()
        )
        writePlaylists(userId, playlists.map { if (it.id == playlistId) updated else it })
    }

    override suspend fun removeSongFromPlaylist(userId: String, playlistId: String, songId: String) = transaction {
        requireValidUserId(userId)
        val playlists = readPlaylists(userId)
        writePlaylists(
            userId,
            playlists.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(
                    updatedAtMs = System.currentTimeMillis(),
                    songIds = playlist.songIds.filterNot { it == songId }
                ) else playlist
            }
        )
    }

    override suspend fun recordPlay(userId: String, songId: String, playedAtMs: Long) = transaction {
        requireValidUserId(userId)
        require(songId.isNotBlank()) { "songId boş olamaz" }
        val history = listOf(HistoryRecord(songId, playedAtMs)) + readHistory(userId).filterNot { it.songId == songId }
        writeHistory(userId, history.take(MAX_HISTORY_ITEMS))
    }

    override suspend fun clearHistory(userId: String) = transaction {
        requireValidUserId(userId)
        preferences.edit().putString(historyKey(userId), "[]").persist()
    }

    override suspend fun clearUser(userId: String) = transaction {
        requireValidUserId(userId)
        preferences.edit()
            .putString(favoritesKey(userId), "[]")
            .putString(playlistsKey(userId), "[]")
            .putString(historyKey(userId), "[]")
            .putBoolean(legacyImportedKey(userId), true)
            .persist()
    }

    private fun readFavorites(userId: String): List<FavoriteRecord> = runCatching {
        val array = JSONArray(preferences.getString(favoritesKey(userId), "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            FavoriteRecord(item.getString("songId"), item.getLong("addedAtMs"))
        }
    }.getOrElse { cause ->
        throw IOException("Yerel favori verisi okunamadı; kayıtların üzerine yazılmadı.", cause)
    }

    private fun writeFavorites(userId: String, favorites: List<FavoriteRecord>) {
        preferences.edit().putString(favoritesKey(userId), serializeFavorites(favorites)).persist()
    }

    private fun readPlaylists(userId: String): List<PlaylistRecord> = runCatching {
        val array = JSONArray(preferences.getString(playlistsKey(userId), "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val songs = item.optJSONArray("songIds") ?: JSONArray()
            PlaylistRecord(
                id = item.getString("id"),
                name = item.getString("name"),
                description = item.optString("description"),
                createdAtMs = item.getLong("createdAtMs"),
                updatedAtMs = item.getLong("updatedAtMs"),
                songIds = (0 until songs.length()).map(songs::getString)
            )
        }
    }.getOrElse { cause ->
        throw IOException("Yerel çalma listesi verisi okunamadı; kayıtların üzerine yazılmadı.", cause)
    }

    private fun writePlaylists(userId: String, playlists: List<PlaylistRecord>) {
        preferences.edit().putString(playlistsKey(userId), serializePlaylists(playlists)).persist()
    }

    private fun readHistory(userId: String): List<HistoryRecord> = runCatching {
        val array = JSONArray(preferences.getString(historyKey(userId), "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            HistoryRecord(item.getString("songId"), item.getLong("playedAtMs"))
        }
    }.getOrElse { cause ->
        throw IOException("Yerel dinleme geçmişi verisi okunamadı; kayıtların üzerine yazılmadı.", cause)
    }

    private fun writeHistory(userId: String, history: List<HistoryRecord>) {
        preferences.edit().putString(historyKey(userId), serializeHistory(history)).persist()
    }

    private fun requireValidUserId(userId: String) {
        require(userId.isNotBlank()) { "userId boş olamaz" }
    }

    private fun favoritesKey(userId: String) = "favorites::$userId"
    private fun playlistsKey(userId: String) = "playlists::$userId"
    private fun historyKey(userId: String) = "history::$userId"

    private companion object {
        private val libraryWriteMutex = Mutex()
        const val MAX_HISTORY_ITEMS = 50
    }
}
