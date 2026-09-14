package com.example.sync.release

import android.util.Log
import com.example.data.MusicDao
import com.example.data.ReleaseSyncStateEntity
import com.example.data.catalog.CatalogProvider
import com.example.data.toCachedEntity
import com.example.data.toSong
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.time.Instant

data class ReleaseSyncResult(
    val added: Int,
    val updated: Int,
    val skipped: Int,
    val failed: Int,
    val source: String
)

class ReleaseSyncCoordinator(
    private val dao: MusicDao,
    private val catalogProvider: CatalogProvider
) {
    private val _newReleasesFlow = MutableSharedFlow<List<Song>>(replay = 1)
    val newReleasesFlow: SharedFlow<List<Song>> = _newReleasesFlow.asSharedFlow()

    suspend fun syncLatestReleases(): Result<ReleaseSyncResult> = withContext(Dispatchers.IO) {
        Log.i("ReleaseSyncCoordinator", "CATALOG_SYNC_STARTED")
        try {
            // 1. Read last successful sync state
            val lastSyncState = dao.getReleaseSyncState()
            val lastSyncTime = lastSyncState?.lastSyncTimeMs?.let { Instant.ofEpochMilli(it) }

            // 2. Query provider for latest releases
            val releasesResult = catalogProvider.getLatestReleases(lastSyncTime)
            if (releasesResult.isFailure) {
                dao.insertReleaseSyncState(
                    ReleaseSyncStateEntity(
                        lastSyncTimeMs = lastSyncState?.lastSyncTimeMs ?: 0L,
                        addedCount = 0,
                        status = "FAILED"
                    )
                )
                Log.e("ReleaseSyncCoordinator", "CATALOG_SYNC_FAILED: Provider error")
                return@withContext Result.failure(releasesResult.exceptionOrNull() ?: Exception("Provider error"))
            }

            val remoteSongs = releasesResult.getOrDefault(emptyList())

            // 3. Retrieve currently cached songs for deduplication
            val currentCachedEntities = dao.getAllCachedSongs()
            val currentCachedList = mutableListOf<Song>()
            currentCachedEntities.collect { list ->
                currentCachedList.addAll(list.map { entity ->
                    entity.toSong()
                })
            }

            var added = 0
            var skipped = 0
            val verifiedToInsert = mutableListOf<Song>()

            for (song in remoteSongs) {
                // Deduplication criteria
                val isDuplicate = currentCachedList.any { cached ->
                    // Priority deduplication
                    val matchesIsrc = song.id == cached.id
                    val matchesTitleAndArtist = song.title.equals(cached.title, ignoreCase = true) && 
                            song.artist.equals(cached.artist, ignoreCase = true)
                    
                    matchesIsrc || matchesTitleAndArtist
                }

                if (!isDuplicate) {
                    // 4. Validate metadata (Do not allow any empty title or empty artist)
                    if (song.title.isNotBlank() && song.artist.isNotBlank()) {
                        // 6. Mark isNewRelease = true
                        val preparedSong = song.copy(isNewRelease = true)
                        verifiedToInsert.add(preparedSong)
                        added++
                    } else {
                        skipped++
                    }
                } else {
                    skipped++
                }
            }

            if (verifiedToInsert.isNotEmpty()) {
                // 5. Write verified songs to Room Database
                dao.insertCachedSongs(verifiedToInsert.map { it.toCachedEntity() })
                
                // 7. Notify UI via flow
                _newReleasesFlow.emit(verifiedToInsert)
            }

            // Record successful synchronization state
            val result = ReleaseSyncResult(
                added = added,
                updated = 0,
                skipped = skipped,
                failed = 0,
                source = "PrimaryCatalogProvider"
            )

            dao.insertReleaseSyncState(
                ReleaseSyncStateEntity(
                    lastSyncTimeMs = System.currentTimeMillis(),
                    addedCount = added,
                    status = "SUCCESS"
                )
            )

            Log.i("ReleaseSyncCoordinator", "CATALOG_SYNC_SUCCESS Added: $added, Skipped: $skipped")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("ReleaseSyncCoordinator", "CATALOG_SYNC_FAILED", e)
            Result.failure(e)
        }
    }
}
