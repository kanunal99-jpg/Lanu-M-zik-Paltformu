package com.example.data

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import com.example.model.Song
import kotlinx.coroutines.CancellationException
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
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR
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
                while (c.moveToNext()) {
                    // Some OEM MediaStore implementations omit optional metadata
                    // columns even when requested. Still expose the readable audio.
                    LocalMediaRowMapper.from(c)?.let(localSongs::add)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Log.e("LocalMusicScanner", "Error scanning local music", e)
        }
        localSongs
    }
}
