package com.example.data.catalog

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CatalogJsonResponsePolicyTest {
    private fun response(body: ResponseBody): Response =
        Response.Builder()
            .request(Request.Builder().url("https://example.org/catalog").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body)
            .build()

    @Test fun validSmallJsonStillParses() {
        response("""{"data":[{"id":"verified"}]}""".toResponseBody("application/json".toMediaType())).use {
            assertEquals("verified", CatalogJsonResponsePolicy.parse(it)
                .getJSONArray("data").getJSONObject(0).getString("id"))
        }
    }

    @Test fun oversizedDeclaredBodyIsRejected() {
        val huge = """{"payload":""" + "a".repeat(CatalogJsonResponsePolicy.MAX_JSON_BYTES.toInt()) + """}"""
        response(huge.toResponseBody("application/json".toMediaType())).use {
            val failed = runCatching { CatalogJsonResponsePolicy.parse(it) }.exceptionOrNull()
            assertTrue(failed is IOException)
            assertEquals("CATALOG_RESPONSE_TOO_LARGE", failed?.message)
        }
    }

    @Test fun oversizedChunkedBodyIsRejectedWithoutContentLength() {
        val huge = """{"payload":""" + "b".repeat(CatalogJsonResponsePolicy.MAX_JSON_BYTES.toInt()) + """}"""
        val content = object : ResponseBody() {
            override fun contentType() = "application/json".toMediaType()
            override fun contentLength() = -1L
            override fun source(): BufferedSource = Buffer().writeUtf8(huge)
        }
        response(content).use {
            val failed = runCatching { CatalogJsonResponsePolicy.parse(it) }.exceptionOrNull()
            assertTrue(failed is IOException)
            assertEquals("CATALOG_RESPONSE_TOO_LARGE", failed?.message)
        }
    }

    @Test fun brokenJsonDoesNotBecomeFabricatedCatalog() {
        response("{broken".toResponseBody("application/json".toMediaType())).use {
            assertTrue(runCatching { CatalogJsonResponsePolicy.parse(it) }.isFailure)
        }
    }

    @Test fun missingBodyFailsClosed() {
        val missing = Response.Builder()
            .request(Request.Builder().url("https://example.org/catalog").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()
        missing.use {
            assertEquals("CATALOG_EMPTY_BODY",
                runCatching { CatalogJsonResponsePolicy.parse(it) }.exceptionOrNull()?.message)
        }
    }
}
