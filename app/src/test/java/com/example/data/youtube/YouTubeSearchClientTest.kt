package com.example.data.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeSearchClientTest {

    @Test
    fun requestUrlEncodesTurkishQueriesAndFiltersForEmbeddableVideos() {
        val url = YouTubeSearchClient.requestUrl("  Ceza Şarkıları & Sagopa  ", "test_key")
        assertEquals("https", url.scheme)
        assertEquals("www.googleapis.com", url.host)
        assertEquals("/youtube/v3/search", url.encodedPath)
        assertEquals("Ceza Şarkıları & Sagopa", url.queryParameter("q"))
        assertEquals("snippet", url.queryParameter("part"))
        assertEquals("video", url.queryParameter("type"))
        assertEquals("true", url.queryParameter("videoEmbeddable"))
        assertEquals("25", url.queryParameter("maxResults"))
        assertNull(url.queryParameter("pageToken"))
    }

    @Test
    fun pagingUsesReturnedTokenRatherThanRepeatedFirstPage() {
        val url = YouTubeSearchClient.requestUrl("Sezen Aksu", "test_key", "CAUQAA")
        assertEquals("CAUQAA", url.queryParameter("pageToken"))
    }

    @Test
    fun missingCredentialsAndTooShortQueriesFailEarly() {
        assertTrue(runCatching { YouTubeSearchClient.requestUrl("x", "test") }.isFailure)
        assertTrue(runCatching { YouTubeSearchClient.requestUrl("Ceza", "") }.isFailure)
    }

    @Test
    fun parseSearchResponseSkipsMalformedEntriesAndDeduplicates() {
        val json = """
            {
              "nextPageToken":"NEXT_PAGE",
              "items":[
                {"id":{"videoId":"AbCdEfGhIj0"},"snippet":{"title":"Şarkı &amp; Klip","channelTitle":"Resmî kanal","thumbnails":{"medium":{"url":"https://i.ytimg.com/vi/test/mqdefault.jpg"}}}},
                {"id":{"videoId":"AbCdEfGhIj0"},"snippet":{"title":"Duplicate","channelTitle":"Copy"}},
                {"id":{"videoId":"invalid!"},"snippet":{"title":"Invalid"}},
                {"id":{},"snippet":{"title":"Missing"}},
                {"id":{"videoId":"KLMNopqr123"},"snippet":{"title":"İkinci şarkı","channelTitle":"İkinci sanatçı","thumbnails":{"default":{"url":"http://unsafe.example/pic.png"}}}}
              ]
            }
        """.trimIndent()
        val page = YouTubeSearchClient.parseResponse(json)
        assertEquals("NEXT_PAGE", page.nextPageToken)
        assertEquals(2, page.videos.size)
        assertEquals("AbCdEfGhIj0", page.videos[0].id)
        assertEquals("Resmî kanal", page.videos[0].channel)
        assertTrue(page.videos[0].thumbnailUrl.startsWith("https://"))
        assertEquals("", page.videos[1].thumbnailUrl)
    }

    @Test
    fun noResultsOrNextPageIsAllowed() {
        val result = YouTubeSearchClient.parseResponse("""{"items":[]}""")
        assertTrue(result.videos.isEmpty())
        assertNull(result.nextPageToken)
    }

    @Test
    fun knownErrorsBecomeActionableTurkishMessagesWithoutLeakingApiKey() {
        val quota = YouTubeSearchClient.errorMessage(403, """{"error":{"errors":[{"reason":"quotaExceeded"}]}}""")
        val invalid = YouTubeSearchClient.errorMessage(400, """{"error":{"errors":[{"reason":"keyInvalid"}]}}""")
        assertTrue(quota.contains("kota"))
        assertTrue(invalid.contains("anahtarı"))
        assertFalse(quota.contains("test_key"))
    }

    @Test
    fun boundedBodyAcceptsExactByteLimitAndTurkishUtf8() {
        val text = "Şarkı — İyi"
        val encoded = text.toByteArray(Charsets.UTF_8)
        assertEquals(text, YouTubeResponseBodyPolicy.readUtf8(
            java.io.ByteArrayInputStream(encoded), encoded.size))
    }

    @Test
    fun oversizedBodyIsRejectedWithFriendlyError() {
        val input = java.io.ByteArrayInputStream(ByteArray(1024 * 1024 + 1) { 65 })
        val thrown = runCatching { YouTubeResponseBodyPolicy.readUtf8(input) }.exceptionOrNull()
        assertTrue(thrown is YouTubeSearchException)
        assertTrue(thrown?.message.orEmpty().contains("sınırını aştı"))
    }

    @Test
    fun missingBodyAndInvalidLimitsFailClosed() {
        assertTrue(runCatching { YouTubeResponseBodyPolicy.readUtf8(null) }.exceptionOrNull()
            is YouTubeSearchException)
        assertTrue(runCatching {
            YouTubeResponseBodyPolicy.readUtf8(java.io.ByteArrayInputStream(byteArrayOf(1)), 0)
        }.isFailure)
        assertEquals("", YouTubeResponseBodyPolicy.readUtf8(
            java.io.ByteArrayInputStream(byteArrayOf()), 16))
    }
}
