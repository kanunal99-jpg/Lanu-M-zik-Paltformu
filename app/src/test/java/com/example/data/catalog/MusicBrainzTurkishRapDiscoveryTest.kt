package com.example.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicBrainzTurkishRapDiscoveryTest {
    @Test
    fun parseArtists_keeps_real_identity_fields_and_drops_invalid_rows() {
        val json = """
            {
              "artists": [
                {"id":"mbid-1","name":"Test Rapçi","disambiguation":"İstanbul"},
                {"id":"","name":"invalid"},
                {"id":"mbid-2","name":"Yeraltı MC"}
              ]
            }
        """.trimIndent()

        val result = MusicBrainzTurkishRapDiscovery.parseArtists(json)

        assertEquals(listOf("mbid-1", "mbid-2"), result.map { it.id })
        assertEquals(listOf("Test Rapçi", "Yeraltı MC"), result.map { it.name })
        assertTrue(result.first().disambiguation.contains("İstanbul"))
    }
}
