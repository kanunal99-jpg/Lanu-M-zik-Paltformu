package com.example.data.catalog

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JamendoCatalogProviderTest {
    @Test
    fun maps_verified_track_to_playable_song() {
        val item = JSONObject()
            .put("id", "123")
            .put("name", "Gerçek Şarkı")
            .put("artist_id", "55")
            .put("artist_name", "Test Artist")
            .put("album_name", "Test Album")
            .put("duration", 180)
            .put("releasedate", "2026-01-02")
            .put("album_image", "https://usercontent.jamendo.com/cover.jpg")
            .put("image", "https://usercontent.jamendo.com/cover-track.jpg")
            .put("audio", "https://prod-1.storage.jamendo.com/?trackid=123&format=mp32")
            .put(
                "musicinfo",
                JSONObject()
                    .put("lang", "en")
                    .put(
                        "tags",
                        JSONObject().put("genres", JSONArray().put("pop")).put("vartags", JSONArray().put("dance"))
                    )
            )

        val songs = JamendoSongMapper.mapSongs(JSONArray().put(item))
        assertEquals(1, songs.size)
        assertEquals("jamendo:123", songs.single().id)
        assertEquals("https://prod-1.storage.jamendo.com/?trackid=123&format=mp32", songs.single().audioUrl)
        assertEquals("en", songs.single().language)
        assertTrue(songs.single().durationMs == 180_000L)
    }

    @Test
    fun drops_catalog_entries_without_verified_stream_url() {
        val item = JSONObject()
            .put("id", "124")
            .put("name", "Metadata Only")
            .put("artist_id", "56")
            .put("artist_name", "Artist")
            .put("album_name", "Album")
            .put("duration", 200)

        val songs = JamendoSongMapper.mapSongs(JSONArray().put(item))
        assertTrue(songs.isEmpty())
    }

    @Test
    fun maps_verified_artist_response_without_inventing_metadata() {
        val artist = JSONObject()
            .put("id", "55")
            .put("name", "Gerçek Sanatçı")
            .put("image", "https://usercontent.jamendo.com/artist.jpg")
            .put("website", "https://example.test")

        val artists = JamendoArtistMapper.mapArtists(JSONArray().put(artist))
        assertEquals(1, artists.size)
        assertEquals("jamendo:55", artists.single().id)
        assertEquals("Gerçek Sanatçı", artists.single().name)
        assertEquals("https://usercontent.jamendo.com/artist.jpg", artists.single().imageUrl)
        assertEquals("", artists.single().bio)
    }

    @Test
    fun drops_artist_entries_without_id_or_name() {
        val missingId = JSONObject().put("name", "Artist")
        val missingName = JSONObject().put("id", "99")
        val artists = JamendoArtistMapper.mapArtists(JSONArray().put(missingId).put(missingName))
        assertTrue(artists.isEmpty())
    }

    @Test
    fun maps_streamable_audius_track_to_verified_stream_endpoint() {
        val user = JSONObject()
            .put("id", "user-1")
            .put("name", "Gerçek Audius Sanatçısı")
        val artwork = JSONObject().put("_480x480", "https://cdn.audius.co/art.jpg")
        val item = JSONObject()
            .put("id", "track-1")
            .put("title", "Gerçek Audius Şarkısı")
            .put("duration", 200)
            .put("genre", "Pop")
            .put("releaseDate", "2026-03-01")
            .put("isStreamable", true)
            .put("isStreamGated", false)
            .put("isUnlisted", false)
            .put("user", user)
            .put("artwork", artwork)
        val songs = AudiusSongMapper.mapSongs(JSONArray().put(item))
        assertEquals(1, songs.size)
        assertEquals("audius:track-1", songs.single().id)
        assertEquals("Gerçek Audius Sanatçısı", songs.single().artist)
        assertEquals("https://discoveryprovider.audius.co/v1/tracks/track-1/stream", songs.single().audioUrl)
        assertTrue(songs.single().durationMs == 200_000L)
    }

    @Test
    fun rejects_audius_gated_unlisted_or_non_streamable_tracks() {
        val base = JSONObject()
            .put("id", "track")
            .put("title", "Track")
            .put("duration", 100)
            .put("genre", "Pop")
            .put("isStreamable", true)
            .put("isStreamGated", false)
            .put("isUnlisted", false)
            .put("user", JSONObject().put("id", "u").put("name", "Artist"))
        val gated = JSONObject(base.toString()).put("id", "gated").put("isStreamGated", true)
        val unlisted = JSONObject(base.toString()).put("id", "unlisted").put("isUnlisted", true)
        val nonStreamable = JSONObject(base.toString()).put("id", "nonstream").put("isStreamable", false)
        val songs = AudiusSongMapper.mapSongs(JSONArray().put(gated).put(unlisted).put(nonStreamable))
        assertTrue(songs.isEmpty())
    }
}
