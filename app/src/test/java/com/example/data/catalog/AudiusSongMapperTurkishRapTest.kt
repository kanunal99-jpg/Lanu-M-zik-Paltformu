package com.example.data.catalog

import com.example.model.MusicCategory
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiusSongMapperTurkishRapTest {

    @Test
    fun turkishRapTagProducesTurkishRapCategoryAndVerifiedRemoteSource() {
        val track = JSONObject()
            .put("id", "tr-rap-1")
            .put("title", "Gerçek Kayıt")
            .put("duration", 180)
            .put("genre", "Hip-Hop")
            .put("is_streamable", true)
            .put("license", CatalogLicensePolicy.AUDIUS_OML)
            .put("release_date", "2026-01-01")
            .put("tags", JSONArray().put("turkish"))
            .put(
                "user",
                JSONObject()
                    .put("id", "artist-1")
                    .put("name", "Bağımsız Sanatçı")
            )

        val songs = AudiusSongMapper.mapSongs(JSONArray().put(track))
        assertTrue("Expected the permitted Audius fixture to map", songs.isNotEmpty())

        val song = songs.single()
        assertEquals("tr", song.language)
        assertEquals(MusicCategory.TURKCE_RAP, song.category)
        assertEquals(com.example.model.SongSourceType.VERIFIED_REMOTE, song.sourceType)
        assertEquals("audius:tr-rap-1", song.id)
        assertTrue(song.audioUrl.endsWith("/tr-rap-1/stream"))
    }
}
