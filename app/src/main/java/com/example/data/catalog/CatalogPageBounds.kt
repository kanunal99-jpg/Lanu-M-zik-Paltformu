package com.example.data.catalog

/** Pure, overflow-safe pagination for requests sent to third-party catalog APIs. */
internal object CatalogPageBounds {
    data class Window(val offset: Int, val size: Int)

    fun window(page: Int, pageSize: Int, maxPageSize: Int): Window? {
        require(maxPageSize > 0)
        val safeSize = pageSize.coerceIn(1, maxPageSize)
        val offset = page.coerceAtLeast(0).toLong() * safeSize.toLong()
        // Never wrap negative, issue a bogus page-zero request or hammer a provider.
        if (offset > Int.MAX_VALUE) return null
        return Window(offset.toInt(), safeSize)
    }
}
