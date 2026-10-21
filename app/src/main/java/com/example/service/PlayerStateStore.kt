package com.example.service

import android.content.Context

/** Small durable playback snapshot; it never fabricates a playable source. */
class PlayerStateStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("lanu_player_state", Context.MODE_PRIVATE)

    data class Snapshot(
        val songId: String?,
        val title: String?,
        val artist: String?,
        val positionMs: Long,
        val isPlaying: Boolean,
        val shuffle: Boolean,
        val repeatMode: RepeatMode
    )

    fun save(snapshot: Snapshot) {
        preferences.edit()
            .putString(KEY_SONG_ID, snapshot.songId)
            .putString(KEY_TITLE, snapshot.title)
            .putString(KEY_ARTIST, snapshot.artist)
            .putLong(KEY_POSITION, snapshot.positionMs.coerceAtLeast(0L))
            .putBoolean(KEY_PLAYING, snapshot.isPlaying)
            .putBoolean(KEY_SHUFFLE, snapshot.shuffle)
            .putString(KEY_REPEAT, snapshot.repeatMode.name)
            .apply()
    }

    fun read(): Snapshot {
        // An OEM backup/restore or interrupted preference migration can leave
        // a field with the wrong Android SharedPreferences type. Keep all other
        // valid fields rather than crashing the media service on startup.
        fun storedText(key: String, maxLength: Int): String? =
            runCatching { preferences.getString(key, null) }.getOrNull()
                ?.takeIf { it.isNotBlank() && it.length <= maxLength }
        fun storedBoolean(key: String): Boolean =
            runCatching { preferences.getBoolean(key, false) }.getOrDefault(false)
        val id = storedText(KEY_SONG_ID, 512)
        val position = runCatching { preferences.getLong(KEY_POSITION, 0L) }
            .getOrDefault(0L).coerceIn(0L, MAX_RESTORED_POSITION_MS)
        val repeat = runCatching {
            RepeatMode.valueOf(preferences.getString(KEY_REPEAT, RepeatMode.OFF.name)!!)
        }.getOrDefault(RepeatMode.OFF)
        return Snapshot(
            songId = id,
            title = storedText(KEY_TITLE, 500),
            artist = storedText(KEY_ARTIST, 500),
            positionMs = if (id == null) 0L else position,
            isPlaying = id != null && storedBoolean(KEY_PLAYING),
            shuffle = storedBoolean(KEY_SHUFFLE),
            repeatMode = repeat
        )
    }

    private companion object {
        const val MAX_RESTORED_POSITION_MS = 24L * 60L * 60L * 1000L
        const val KEY_SONG_ID = "song_id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_POSITION = "position_ms"
        const val KEY_PLAYING = "is_playing"
        const val KEY_SHUFFLE = "shuffle"
        const val KEY_REPEAT = "repeat_mode"
    }
}
