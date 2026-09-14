package com.example.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.model.MusicCategory
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMusicScanner(private val context: Context) {

    suspend fun scanLocalMusic(): List<Song> = withContext(Dispatchers.IO) {
        val localSongs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val yearColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val title = c.getString(titleColumn) ?: "Unknown Title"
                    val artist = c.getString(artistColumn) ?: "Unknown Artist"
                    val album = c.getString(albumColumn) ?: "Unknown Album"
                    val durationMs = c.getLong(durationColumn)
                    val dataPath = c.getString(dataColumn) ?: ""
                    val albumId = c.getLong(albumIdColumn)
                    val year = if (yearColumn >= 0) c.getInt(yearColumn) else 0

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val artworkUri = Uri.parse("content://media/external/audio/albumart/$albumId").toString()

                    val song = Song(
                        id = "local_$id",
                        title = title,
                        artist = artist,
                        artistId = artist.lowercase().replace(" ", "_"),
                        album = album,
                        durationMs = durationMs,
                        category = MusicCategory.LOCAL,
                        language = "tr", // Default
                        coverUrl = artworkUri,
                        audioUrl = contentUri.toString(),
                        releaseYear = year,
                        playCount = 0L,
                        lyrics = emptyList()
                    )
                    localSongs.add(song)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalMusicScanner", "Error scanning local music", e)
        }

        return@withContext localSongs
    }
}
