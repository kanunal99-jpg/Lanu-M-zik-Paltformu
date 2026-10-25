package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryPaginationPolicyTest {
    private val catalog = (0 until 1001).toList()

    @Test fun normalPagesPreserveOriginalOrder() {
        assertEquals(listOf(3, 4, 5), MediaLibraryPaginationPolicy.page(catalog, 1, 3))
    }

    @Test fun endOfCatalogNeverReadsPastLastElement() {
        assertEquals(listOf(999, 1000), MediaLibraryPaginationPolicy.page(catalog, 333, 3))
    }

    @Test fun maliciousHugeInputsReturnEmptyRatherThanOverflow() {
        assertTrue(MediaLibraryPaginationPolicy.page(catalog, Int.MAX_VALUE, Int.MAX_VALUE).isEmpty())
        assertTrue(MediaLibraryPaginationPolicy.page(catalog, 1_073_741_824, 2).isEmpty())
    }

    @Test fun malformedNegativeInputsUseSafeFirstPage() {
        assertEquals(listOf(0), MediaLibraryPaginationPolicy.page(catalog, -10, -30))
        assertEquals(listOf(0), MediaLibraryPaginationPolicy.page(catalog, 0, 0))
    }

    @Test fun hugePageSizeIsBounded() {
        assertEquals(
            MediaLibraryPaginationPolicy.MAX_PAGE_SIZE,
            MediaLibraryPaginationPolicy.page(catalog, 0, Int.MAX_VALUE).size
        )
        assertEquals(listOf(1000), MediaLibraryPaginationPolicy.page(catalog, 2, Int.MAX_VALUE))
    }

    @Test fun rootBrowsePagesRespectRequestedSize() {
        val root = listOf("Cihazımdaki Müzikler", "LANU Kataloğu")
        assertEquals(listOf("Cihazımdaki Müzikler"),
            MediaLibraryPaginationPolicy.page(root, 0, 1))
        assertEquals(listOf("LANU Kataloğu"),
            MediaLibraryPaginationPolicy.page(root, 1, 1))
        assertTrue(MediaLibraryPaginationPolicy.page(root, 2, 1).isEmpty())
    }


    @Test fun onlyRequestedPageAllocatesMappedMediaEntries() {
        var conversions = 0
        val mapped = MediaLibraryPaginationPolicy.mapPage(catalog, 2, 3) {
            conversions++
            "media:${it}"
        }
        assertEquals(listOf("media:6", "media:7", "media:8"), mapped)
        assertEquals(3, conversions)
    }

    @Test fun outOfBoundsPageDoesNotConvertAnything() {
        var conversions = 0
        val mapped = MediaLibraryPaginationPolicy.mapPage(catalog, Int.MAX_VALUE, 50) {
            conversions++
            it.toString()
        }
        assertTrue(mapped.isEmpty())
        assertEquals(0, conversions)
    }

    @Test fun enormousRequestedPageConvertsAtMostPolicyLimit() {
        var conversions = 0
        val mapped = MediaLibraryPaginationPolicy.mapPage(catalog, 0, Int.MAX_VALUE) {
            conversions++
            it.toString()
        }
        assertEquals(MediaLibraryPaginationPolicy.MAX_PAGE_SIZE, mapped.size)
        assertEquals(MediaLibraryPaginationPolicy.MAX_PAGE_SIZE, conversions)
    }

    @Test fun emptyCollectionAlwaysReturnsEmpty() {
        assertTrue(MediaLibraryPaginationPolicy.page(emptyList<Int>(), Int.MAX_VALUE, 1).isEmpty())
    }
}
