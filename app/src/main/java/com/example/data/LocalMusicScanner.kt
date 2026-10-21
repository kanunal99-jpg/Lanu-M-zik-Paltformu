package com.example.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMusicScanner(
    private val context: Context,
    // Row transformation is injectable for deterministic partial-cursor tests;
    // the actual production mapper is unchanged across Android manufacturers.
    private val mapRow: (Cursor) -> Song? = LocalMediaRowMapper::from,
    // Injectable query for reproducible OEM cursor-failure regressions.
    private val audioQuery: (Array<String>, String, String) -> Cursor? =
        { columns, filter, order ->
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                columns, filter, null, order
            )
        }
) {
    suspend fun scanLocalMusic(): List<Song> = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
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
            val cursor = audioQuery(projection, selection, sortOrder)
            cursor?.use { c ->
                while (c.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    // Some OEM MediaStore implementations omit optional metadata
                    // columns even when requested. Still expose the readable audio.
                    mapRow(c)?.let(localSongs::add)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (denied: SecurityException) {
            // Never claim the user's device library is empty when an OEM revoked
            // MediaStore access mid-scan. The existing Room cache is retained.
            Log.w("LocalMusicScanner", "MediaStore permission denied")
            throw denied
        } catch (error: Exception) {
            Log.e("LocalMusicScanner", "Local music scan incomplete", error)
            // Fail closed instead of exposing a partial list as successful sync.
            throw IOException("Android medya taraması tamamlanamadı; yerel arşiv korundu.", error)
        }
        localSongs
    }
}
