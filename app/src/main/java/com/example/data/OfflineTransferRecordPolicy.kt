package com.example.data

import com.example.player.DownloadStatus

/** Non-destructive persistence rule for retry and cancellation callbacks. */
internal object OfflineTransferRecordPolicy {
    fun shouldPersistCompletion(status: DownloadStatus, localPath: String?): Boolean =
        status == DownloadStatus.COMPLETED && !localPath.isNullOrBlank()
}
