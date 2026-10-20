package com.example.data

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.example.model.MusicCategory
import com.example.model.Song
import com.example.model.SongSourceType
import java.util.Locale

/** Resilient mapping for Android MediaStore providers from different OEMs. */
internal object LocalMediaRowMapper {
    fun from(cursor: Cursor): Song? {
        val idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        if (idIndex < 0 || cursor.isNull(idIndex)) return null
        val id = cursor.getLong(idIndex)
        if (id <= 0L) return null

        fun text(column: String, fallback: String): String {
            val idx = cursor.getColumnIndex(column)
            return if (idx < 0 || cursor.isNull(idx)) fallback
                else cursor.getString(idx)?.trim()?.takeIf { it.isNotBlank() } ?: fallback
        }
        fun number(column: String): Long {
            val idx = cursor.getColumnIndex(column)
            return if (idx < 0 || cursor.isNull(idx)) 0L else cursor.getLong(idx)
        }

        val title = text(MediaStore.Audio.Media.TITLE, "Unknown Title")
        val artist = text(MediaStore.Audio.Media.ARTIST, "Unknown Artist")
        val album = text(MediaStore.Audio.Media.ALBUM, "Unknown Album")
        val albumId = number(MediaStore.Audio.Media.ALBUM_ID)
        val artwork = if (albumId > 0L) {
            Uri.parse("content://media/external/audio/albumart/$albumId").toString()
        } else ""
        return Song(
            id = "local_$id",
            title = title,
            artist = artist,
            artistId = artist.lowercase(Locale.ROOT).replace(Regex("\\s+"), "_"),
            album = album,
            durationMs = number(MediaStore.Audio.Media.DURATION).coerceAtLeast(0L),
            category = MusicCategory.LOCAL,
            language = "und",
            coverUrl = artwork,
            audioUrl = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
            ).toString(),
            releaseYear = number(MediaStore.Audio.Media.YEAR)
                .toInt().coerceIn(0, 9999),
            playCount = 0L,
            lyrics = emptyList(),
            sourceType = SongSourceType.LOCAL
        )
    }
}
