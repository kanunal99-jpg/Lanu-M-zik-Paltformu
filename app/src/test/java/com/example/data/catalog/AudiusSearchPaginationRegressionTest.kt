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

class AudiusSearchPaginationRegressionTest {
    @Test
    fun firstSearchPageNeverWaitsForArtistDiscography() = runTest {
        val paths = mutableListOf<String>()
        val provider = fixture(paths, repeatHundred = false)
        val songs = provider.searchSongsPage("Ceza", 0, 50).getOrThrow()
        assertEquals(1, songs.size)
        assertEquals("audius:one", songs.single().id)
        assertEquals(1, paths.size)
        assertTrue(paths.single().contains("/tracks/search"))
    }

    @Test
    fun repeatedFullDiscographyPageDoesNotLoopForever() = runTest {
        val paths = mutableListOf<String>()
        val provider = fixture(paths, repeatHundred = true)
        val songs = provider.getArtistDiscography("audius:77").getOrThrow()
        assertEquals(100, songs.size)
        assertEquals(2, paths.count { it.contains("/users/77/tracks") })
        assertTrue(songs.all { it.artistId == "audius:77" })
    }

    private fun fixture(paths: MutableList<String>, repeatHundred: Boolean): AudiusCatalogProvider {
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val url = chain.request().url
            paths.add(url.toString())
            val payload = when {
                url.encodedPath.endsWith("/tracks/search") -> JSONArray().put(track("one"))
                url.encodedPath.endsWith("/users/77/tracks") && repeatHundred -> JSONArray().apply {
                    repeat(100) { put(track("track-" + it)) }
                }
                else -> error("Unexpected API request: $url")
            }
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(JSONObject().put("data", payload).toString()
                    .toResponseBody("application/json".toMediaType()))
                .build()
        }).build()
        return AudiusCatalogProvider(client)
    }

    private fun track(id: String): JSONObject = JSONObject()
        .put("id", id)
        .put("title", "Gerçek örnek kayıt")
        .put("genre", "Hip-Hop")
        .put("is_streamable", true)
        .put("license", CatalogLicensePolicy.AUDIUS_OML)
        .put("duration", 190)
        .put("tags", JSONArray().put("turkish"))
        .put("user", JSONObject().put("id", "77").put("name", "Ceza"))
}
