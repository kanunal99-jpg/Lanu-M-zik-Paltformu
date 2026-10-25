package com.example.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogPageBoundsTest {
    @Test fun normalPagesProduceStableOffsets() {
        assertEquals(CatalogPageBounds.Window(0, 50), CatalogPageBounds.window(0, 50, 100))
        assertEquals(CatalogPageBounds.Window(200, 100), CatalogPageBounds.window(2, 300, 100))
    }

    @Test fun malformedNegativeValuesAreBounded() {
        assertEquals(CatalogPageBounds.Window(0, 1), CatalogPageBounds.window(-2, -8, 100))
    }

    @Test fun hugePageNeverOverflowsToNegative() {
        assertNull(CatalogPageBounds.window(Int.MAX_VALUE, Int.MAX_VALUE, 200))
        assertNull(CatalogPageBounds.window(21_474_837, 100, 100))
    }

    @Test fun nearIntegerLimitIsStillRepresentable() {
        assertEquals(
            CatalogPageBounds.Window(2_147_483_600, 100),
            CatalogPageBounds.window(21_474_836, 100, 100)
        )
    }
}
