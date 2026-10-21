package com.example.data

import com.example.player.DownloadStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTransferRecordPolicyTest {
    @Test fun failedCancelledOrIncompleteTransferNeverMutatesExistingRoomRecord() {
        for (status in DownloadStatus.entries.filter { it != DownloadStatus.COMPLETED }) {
            assertFalse(status.toString(),
                OfflineTransferRecordPolicy.shouldPersistCompletion(status, "/offline/user.bin"))
        }
        assertFalse(OfflineTransferRecordPolicy.shouldPersistCompletion(DownloadStatus.COMPLETED, null))
        assertFalse(OfflineTransferRecordPolicy.shouldPersistCompletion(DownloadStatus.COMPLETED, ""))
    }

    @Test fun verifiedCompletedFileMayBeSavedToRoom() {
        assertTrue(OfflineTransferRecordPolicy.shouldPersistCompletion(
            DownloadStatus.COMPLETED, "/offline/user.bin"))
    }
}
