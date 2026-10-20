package com.example.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTransferSizePolicyTest {
    @Test fun exactBoundaryIsAcceptedButOverflowIsRejected() {
        val max = OfflineTransferSizePolicy.MAX_BYTES
        OfflineTransferSizePolicy.requireExpectedSize(max)
        OfflineTransferSizePolicy.requireNextChunk(max - 8192, 8192)
        assertTrue(runCatching {
            OfflineTransferSizePolicy.requireExpectedSize(max + 1)
        }.isFailure)
        assertTrue(runCatching {
            OfflineTransferSizePolicy.requireNextChunk(max - 1, 2)
        }.isFailure)
    }

    @Test fun unknownLengthIsAllowedButIncrementalChunkLimitStillApplies() {
        OfflineTransferSizePolicy.requireExpectedSize(-1L)
        OfflineTransferSizePolicy.requireNextChunk(0L, 8192)
        assertTrue(runCatching {
            OfflineTransferSizePolicy.requireNextChunk(Long.MAX_VALUE, 8192)
        }.isFailure)
    }

    @Test fun malformedNegativeChunkAndOverlargeHeaderAreRejected() {
        assertTrue(runCatching {
            OfflineTransferSizePolicy.requireNextChunk(12L, -1)
        }.isFailure)
        assertTrue(runCatching {
            OfflineTransferSizePolicy.requireExpectedSize(Long.MAX_VALUE)
        }.isFailure)
        assertEquals(512L * 1024L * 1024L, OfflineTransferSizePolicy.MAX_BYTES)
    }
}
