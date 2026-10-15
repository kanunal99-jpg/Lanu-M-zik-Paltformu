package com.example.data.catalog

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeezerDiscographyPaginationTest {
    @Test
    fun loadsAlbumPagesBeyondFormerSixPageLimit() = runTest {
        val (provider, requestPaths) = providerWithAlbumFixture(totalAlbums = 301, tracksPerAlbum = 1)
        val tracks = provider.getArtistDiscography("deezer:77").getOrThrow()

        assertEquals(301, tracks.size)
        assertTrue(tracks.any { it.id == "deezer:track-album-300-0" })
        assertTrue(requestPaths.any { it.contains("/artist/77/albums?") && it.contains("index=300") })
        assertTrue(tracks.all { it.artistId == "deezer:77" })
    }

    @Test
    fun loadsAllAlbumTracksAfterEmbeddedFirstPage() = runTest {
        val (provider, requestPaths) = providerWithAlbumFixture(totalAlbums = 1, tracksPerAlbum = 55)
        val tracks = provider.getArtistDiscography("deezer:77").getOrThrow()

        assertEquals(55, tracks.size)
        assertTrue(tracks.any { it.id == "deezer:track-album-0-54" })
        assertTrue(requestPaths.any { it.contains("/album/album-0/tracks?") && it.contains("index=50") })
    }

    private fun providerWithAlbumFixture(
        totalAlbums: Int,
        tracksPerAlbum: Int
    ): Pair<DeezerCatalogProvider, MutableList<String>> {
        val requests = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val url = chain.request().url
            requests.add(url.toString())
            val offset = url.queryParameter("index")?.toIntOrNull() ?: 0
            val json = when {
                url.encodedPath == "/artist/77/top" ->
                    JSONObject().put("data", JSONArray())
                url.encodedPath == "/artist/77/albums" -> {
                    val entries = JSONArray()
                    val end = (offset + 50).coerceAtMost(totalAlbums)
                    for (index in offset until end) {
                        entries.put(JSONObject().put("id", "album-" + index))
                    }
                    JSONObject().put("data", entries).put("total", totalAlbums)
                }
                url.encodedPath.startsWith("/album/") -> {
                    val segments = url.pathSegments
                    val albumId = segments[1]
                    val firstIndex = if (segments.size > 2 && segments[2] == "tracks") offset else 0
                    val limit = if (segments.size > 2) 100 else 50
                    val end = (firstIndex + limit).coerceAtMost(tracksPerAlbum)
                    val songs = JSONArray()
                    for (index in firstIndex until end) {
                        songs.put(JSONObject()
                            .put("id", "track-" + albumId + "-" + index)
                            .put("title", "Şarkı " + index)
                            .put("artist", JSONObject().put("id", "77").put("name", "Ceza"))
                            .put("album", JSONObject().put("title", albumId))
                            .put("preview", "https://example.com/preview-" + index + ".mp3")
                        )
                    }
                    if (segments.size > 2) {
                        JSONObject().put("data", songs).put("total", tracksPerAlbum)
                    } else {
                        JSONObject().put("tracks", JSONObject().put("data", songs).put("total", tracksPerAlbum))
                    }
                }
                else -> error("Unexpected request: " + url)
            }
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toString().toResponseBody("application/json".toMediaType()))
                .build()
        }).build()
        return DeezerCatalogProvider(client) to requests
    }
}
