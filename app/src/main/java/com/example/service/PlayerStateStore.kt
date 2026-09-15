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

    fun read(): Snapshot = Snapshot(
        songId = preferences.getString(KEY_SONG_ID, null),
        title = preferences.getString(KEY_TITLE, null),
        artist = preferences.getString(KEY_ARTIST, null),
        positionMs = preferences.getLong(KEY_POSITION, 0L),
        isPlaying = preferences.getBoolean(KEY_PLAYING, false),
        shuffle = preferences.getBoolean(KEY_SHUFFLE, false),
        repeatMode = runCatching { RepeatMode.valueOf(preferences.getString(KEY_REPEAT, RepeatMode.OFF.name)!!) }.getOrDefault(RepeatMode.OFF)
    )

    private companion object {
        const val KEY_SONG_ID = "song_id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_POSITION = "position_ms"
        const val KEY_PLAYING = "is_playing"
        const val KEY_SHUFFLE = "shuffle"
        const val KEY_REPEAT = "repeat_mode"
    }
}
