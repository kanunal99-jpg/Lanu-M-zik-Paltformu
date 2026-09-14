package com.example.data.catalog

import com.example.model.Album
import com.example.model.Artist
import com.example.model.Song
import java.time.Instant

/**
 * Independent fallback adapter. It does not silently call the primary provider.
 * Construct it with real fallback providers such as CachedCatalogProvider and LocalCatalogProvider.
 */
class AlternativeCatalogProvider(
    private val fallbacks: List<CatalogProvider>
) : CatalogProvider {
    private suspend inline fun <T> firstSuccess(crossinline call: suspend (CatalogProvider) -> Result<T>): Result<T> {
        var lastError: Throwable? = null
        fallbacks.forEach { provider ->
            val result = runCatching { call(provider) }.getOrElse { Result.failure(it) }
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
        }
        return Result.failure(lastError ?: IllegalStateException("NO_CATALOG_FALLBACK_AVAILABLE"))
    }

    override suspend fun searchSongs(query: String): Result<List<Song>> = firstSuccess { it.searchSongs(query) }
    override suspend fun searchArtists(query: String): Result<List<Artist>> = firstSuccess { it.searchArtists(query) }
    override suspend fun getSong(id: String): Result<Song?> = firstSuccess { it.getSong(id) }
    override suspend fun getArtist(id: String): Result<Artist?> = firstSuccess { it.getArtist(id) }
    override suspend fun getAlbum(id: String): Result<Album?> = firstSuccess { it.getAlbum(id) }
    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> = firstSuccess { it.getLatestReleases(since) }
}
