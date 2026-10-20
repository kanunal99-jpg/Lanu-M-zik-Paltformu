package com.example.data

import android.database.MatrixCursor
import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaRowMapperTest {
    @Test fun missingOptionalOEMMetadataDoesNotHideUserAudio() {
        val cursor = MatrixCursor(arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE
        ))
        cursor.addRow(arrayOf(123L, "Şarkım"))
        cursor.moveToFirst()
        val song = LocalMediaRowMapper.from(cursor)
        assertNotNull(song)
        assertEquals("local_123", song?.id)
        assertEquals("Şarkım", song?.title)
        assertEquals("Unknown Artist", song?.artist)
        assertEquals("Unknown Album", song?.album)
        assertEquals("", song?.coverUrl)
        assertEquals(0L, song?.durationMs)
        assertTrue(song?.audioUrl.orEmpty().endsWith("/123"))
        cursor.close()
    }

    @Test fun missingStableMediaStoreIdIsNotInvented() {
        val cursor = MatrixCursor(arrayOf(MediaStore.Audio.Media.TITLE))
        cursor.addRow(arrayOf("Some audio"))
        cursor.moveToFirst()
        assertNull(LocalMediaRowMapper.from(cursor))
        cursor.close()
    }

    @Test fun invalidIdsAreRejectedAndKnownMetadataIsPreserved() {
        val cursor = MatrixCursor(arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR
        ))
        cursor.addRow(arrayOf(0L, "Invalid", "Artist", "Album", 100L, 6L, 2024))
        cursor.addRow(arrayOf(543L, "Good", "Artist", "Album", 147000L, 9L, 2024))
        cursor.moveToFirst()
        assertNull(LocalMediaRowMapper.from(cursor))
        cursor.moveToNext()
        val good = LocalMediaRowMapper.from(cursor)
        assertEquals("Good", good?.title)
        assertEquals("Artist", good?.artist)
        assertEquals("Album", good?.album)
        assertEquals(147000L, good?.durationMs)
        assertEquals(2024, good?.releaseYear)
        assertTrue(good?.coverUrl.orEmpty().endsWith("/9"))
        cursor.close()
    }
}
