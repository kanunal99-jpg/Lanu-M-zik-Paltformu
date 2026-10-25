package com.example.data.catalog

import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/**
 * Do not let a third-party catalog return unbounded JSON into the process.
 * Responses larger than 4 MiB are rejected, including chunked bodies with
 * unknown Content-Length. Existing provider fallback handles this failure.
 */
internal object CatalogJsonResponsePolicy {
    const val MAX_JSON_BYTES: Long = 4L * 1024L * 1024L

    fun parse(response: Response): JSONObject {
        val body = response.body ?: throw IOException("CATALOG_EMPTY_BODY")
        if (body.contentLength() > MAX_JSON_BYTES) {
            throw IOException("CATALOG_RESPONSE_TOO_LARGE")
        }
        val payload = response.peekBody(MAX_JSON_BYTES + 1L).bytes()
        if (payload.size.toLong() > MAX_JSON_BYTES) {
            throw IOException("CATALOG_RESPONSE_TOO_LARGE")
        }
        return JSONObject(String(payload, Charsets.UTF_8))
    }
}
