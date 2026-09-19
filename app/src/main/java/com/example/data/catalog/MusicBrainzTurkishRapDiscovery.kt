package com.example.data.catalog

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MusicBrainzArtistCandidate(
    val id: String,
    val name: String,
    val disambiguation: String = ""
)

/**
 * Historical discovery layer for Turkish rap.
 *
 * MusicBrainz is metadata/discovery only: it never becomes a playback source.
 * Discovered artist names are subsequently resolved against playable catalog
 * providers by MusicRepository.
 */
class MusicBrainzTurkishRapDiscovery(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://musicbrainz.org/ws/2/"
) {
    suspend fun searchArtists(offset: Int, limit: Int = 50): Result<List<MusicBrainzArtistCandidate>> =
        runCatching {
            val query = """country:TR AND (tag:rap OR tag:hip-hop OR tag:hiphop)"""
            val url = (baseUrl + "artist/").toHttpUrl().newBuilder()
                .addQueryParameter("query", query)
                .addQueryParameter("fmt", "json")
                .addQueryParameter("limit", limit.coerceIn(1, 100).toString())
                .addQueryParameter("offset", offset.coerceAtLeast(0).toString())
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            client.newCall(request).execute().use { httpResponse ->
                if (!httpResponse.isSuccessful) {
                    error("MUSICBRAINZ_HTTP_" + httpResponse.code)
                }
                parseArtists(httpResponse.body?.string().orEmpty())
            }
        }

    companion object {
        const val USER_AGENT = "LANU-Music/1.0 (https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu)"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        fun parseArtists(json: String): List<MusicBrainzArtistCandidate> {
            val root = JSONObject(json)
            val artists = root.optJSONArray("artists") ?: JSONArray()
            return buildList(artists.length()) {
                for (index in 0 until artists.length()) {
                    val artist = artists.optJSONObject(index) ?: continue
                    val id = artist.optString("id").trim()
                    val name = artist.optString("name").trim()
                    if (id.isNotEmpty() && name.isNotEmpty()) {
                        add(MusicBrainzArtistCandidate(
                            id = id,
                            name = name,
                            disambiguation = artist.optString("disambiguation").trim()
                        ))
                    }
                }
            }
        }
    }
}
