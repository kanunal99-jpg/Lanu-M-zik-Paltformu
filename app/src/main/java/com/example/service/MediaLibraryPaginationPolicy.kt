package com.example.service

/**
 * Pagination parameters originate from external MediaLibrary clients (cars, wearables,
 * media browsers), not only LANU's own UI. Clamp requests and use Long arithmetic:
 * Int page * Int size can otherwise wrap negative and crash subList().
 */
internal object MediaLibraryPaginationPolicy {
    const val MAX_PAGE_SIZE = 500

    fun <T> page(items: List<T>, page: Int, pageSize: Int): List<T> {
        if (items.isEmpty()) return emptyList()
        val safePage = page.coerceAtLeast(0).toLong()
        val safeSize = pageSize.coerceIn(1, MAX_PAGE_SIZE).toLong()
        val from = safePage * safeSize
        if (from >= items.size.toLong()) return emptyList()
        val until = minOf(items.size.toLong(), from + safeSize)
        return items.subList(from.toInt(), until.toInt())
    }
}
