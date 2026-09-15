package com.example.sync

import com.example.data.PlaylistEntity
import com.example.model.Song

/**
 * Backend-agnostic contract for cross-device LANU library synchronization.
 *
 * The Android app remains fully usable without a network backend. A future
 * authenticated backend adapter (for example Supabase) must implement this
 * contract without changing the player, Room schema, or UI layer.
 */
interface MusicSyncBackend {
    suspend fun pullLibrary(): SyncSnapshot

    suspend fun pushLibrary(snapshot: SyncSnapshot): SyncPushResult
}

data class SyncSnapshot(
    val favoriteSongIds: Set<String> = emptySet(),
    val playlists: List<SyncPlaylist> = emptyList(),
    val history: List<SyncHistoryItem> = emptyList(),
    val generatedAtMs: Long = System.currentTimeMillis()
)

data class SyncPlaylist(
    val id: String,
    val name: String,
    val description: String,
    val songs: List<String>,
    val updatedAtMs: Long
) {
    fun toLocalEntity(): PlaylistEntity = PlaylistEntity(
        id = id,
        name = name,
        description = description,
        updatedAt = updatedAtMs,
        totalSongsCount = songs.size
    )
}

data class SyncHistoryItem(
    val songId: String,
    val playedAtMs: Long
)

data class SyncPushResult(
    val accepted: Boolean,
    val serverGeneratedAtMs: Long? = null,
    val errorMessage: String? = null
)

/**
 * Safe fallback used until a real authenticated backend is configured.
 * It never invents remote data and therefore preserves the local/offline path.
 */
class LocalOnlyMusicSyncBackend : MusicSyncBackend {
    override suspend fun pullLibrary(): SyncSnapshot = SyncSnapshot()

    override suspend fun pushLibrary(snapshot: SyncSnapshot): SyncPushResult =
        SyncPushResult(
            accepted = false,
            errorMessage = "LANU cloud sync backend is not configured"
        )
}
