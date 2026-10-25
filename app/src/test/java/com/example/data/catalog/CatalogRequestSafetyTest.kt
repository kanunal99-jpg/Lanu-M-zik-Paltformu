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

class CatalogRequestSafetyTest {
    @Test fun jamendoUsesOneLimitParameterAndRealRequestedPageSize() = runTest {
        val requests = mutableListOf<String>()
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val url = chain.request().url
            requests.add(url.toString())
            assertEquals(listOf("70"), url.queryParameterValues("limit"))
            assertEquals("140", url.queryParameter("offset"))
            val result = JSONObject()
                .put("headers", JSONObject().put("code", 0))
                .put("results", JSONArray())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(result.toString().toResponseBody("application/json".toMediaType()))
                .build()
        }).build()
        val provider = JamendoCatalogProvider(clientId = "test-public-client", httpClient = http)
        assertTrue(provider.searchSongsPage("rock", page = 2, pageSize = 70).getOrThrow().isEmpty())
        assertEquals(1, requests.size)
    }

    @Test fun hugePagesDoNotMakeAnyProviderNetworkRequest() = runTest {
        var requestCount = 0
        val rejectNetwork = OkHttpClient.Builder().addInterceptor(Interceptor {
            requestCount++
            error("Huge page must never reach the network")
        }).build()
        assertTrue(AudiusCatalogProvider(rejectNetwork)
            .searchSongsPage("Ceza", Int.MAX_VALUE, Int.MAX_VALUE).getOrThrow().isEmpty())
        assertTrue(DeezerCatalogProvider(rejectNetwork)
            .searchSongsPage("Ceza", Int.MAX_VALUE, Int.MAX_VALUE).getOrThrow().isEmpty())
        assertTrue(JamendoCatalogProvider("test-public-client", httpClient = rejectNetwork)
            .searchSongsPage("Ceza", Int.MAX_VALUE, Int.MAX_VALUE).getOrThrow().isEmpty())
        assertEquals(0, requestCount)
    }
}
