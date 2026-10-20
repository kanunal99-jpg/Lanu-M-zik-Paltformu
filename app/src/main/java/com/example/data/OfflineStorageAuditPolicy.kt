package com.example.data

/**
 * Read-only, non-destructive reconciliation of downloaded music against disk.
 * No operation here can mutate files, user library entries, Room or download state.
 */
internal data class OfflineStorageAudit(
    val missingDownloadRecords: Set<String>,
    val unindexedFiles: Set<String>
)

internal object OfflineStorageAuditPolicy {
    fun inspect(downloadIds: Collection<String>, storedFiles: Collection<String>): OfflineStorageAudit {
        val indexed = downloadIds.toSet()
        val diskIds = storedFiles.asSequence()
            .filter { it.endsWith(".bin") }
            .map { it.removeSuffix(".bin") }
            .toSet()
        return OfflineStorageAudit(
            missingDownloadRecords = indexed - diskIds,
            unindexedFiles = diskIds - indexed
        )
    }
}
