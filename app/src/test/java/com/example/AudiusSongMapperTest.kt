package com.example

import com.example.data.catalog.AudiusSongMapper
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiusSongMapperTest {
    @Test
    fun mapsVerifiedSnakeCaseTrackToPlayableAudiusSource() {
        val track = """
            {
              "id": "test-track-123",
              "title": "Verified Rock Track",
              "is_streamable": true,
              "is_stream_gated": false,
              "is_unlisted": false,
              "duration": 180,
              "genre": "Rock",
              "release_date": "2026-09-17",
              "user": { "id": "artist-9", "name": "Verified Artist" },
              "artwork": { "480x480": "https://example.com/art.jpg" }
            }
        """.trimIndent()

        val song = AudiusSongMapper.mapSongs(JSONArray().put(org.json.JSONObject(track))).single()

        assertEquals("audius:test-track-123", song.id)
        assertEquals("Verified Rock Track", song.title)
        assertEquals("Verified Artist", song.artist)
        assertEquals("audius:artist-9", song.artistId)
        assertEquals(180_000L, song.durationMs)
        assertEquals("https://discoveryprovider.audius.co/v1/tracks/test-track-123/stream", song.audioUrl)
        assertTrue(song.coverUrl.contains("example.com/art.jpg"))
    }

    @Test
    fun rejectsNonStreamableTracks() {
        val track = org.json.JSONObject()
            .put("id", "not-streamable")
            .put("title", "Blocked Track")
            .put("is_streamable", false)
            .put("user", org.json.JSONObject().put("id", "artist-1").put("name", "Artist"))

        val songs = AudiusSongMapper.mapSongs(JSONArray().put(track))

        assertTrue(songs.isEmpty())
    }
}
