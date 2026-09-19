package com.example.data.catalog

import com.example.model.Album
import com.example.model.Artist
import com.example.model.Song
import java.time.Instant

/**
 * Resilient catalog chain. List operations merge all healthy providers so an
 * empty primary response never hides cached or local music. Entity lookups use
 * the first provider that can actually resolve the entity.
 */
class AlternativeCatalogProvider(
    private val fallbacks: List<CatalogProvider>
) : CatalogProvider {

    private suspend fun <T : Any> mergeLists(
        call: suspend (CatalogProvider) -> Result<List<T>>
    ): Result<List<T>> {
        val merged = linkedMapOf<Any, T>()
        var lastError: Throwable? = null
        var hadSuccess = false

        fallbacks.forEach { provider ->
            val result = runCatching { call(provider) }.getOrElse { Result.failure(it) }
            result.onSuccess { values ->
                hadSuccess = true
                values.forEach { value -> merged.putIfAbsent(valueKey(value), value) }
            }.onFailure { lastError = it }
        }

        return if (hadSuccess) Result.success(merged.values.toList())
        else Result.failure(lastError ?: IllegalStateException("NO_CATALOG_FALLBACK_AVAILABLE"))
    }

    private fun valueKey(value: Any): Any = when (value) {
        is Song -> value.id
        is Artist -> value.id
        is Album -> value.id
        else -> value
    }

    private suspend fun <T : Any> firstResolved(
        call: suspend (CatalogProvider) -> Result<T?>
    ): Result<T?> {
        var lastError: Throwable? = null
        var hadSuccess = false
        fallbacks.forEach { provider ->
            val result = runCatching { call(provider) }.getOrElse { Result.failure(it) }
            result.onSuccess { value ->
                hadSuccess = true
                if (value != null) return Result.success(value)
            }.onFailure { lastError = it }
        }
        return if (hadSuccess) Result.success(null)
        else Result.failure(lastError ?: IllegalStateException("NO_CATALOG_FALLBACK_AVAILABLE"))
    }

    override suspend fun searchSongs(query: String): Result<List<Song>> = mergeLists { it.searchSongs(query) }
    override suspend fun searchSongsPage(query: String, page: Int, pageSize: Int): Result<List<Song>> =
        mergeLists { it.searchSongsPage(query, page, pageSize) }
    override suspend fun searchArtists(query: String): Result<List<Artist>> = mergeLists { it.searchArtists(query) }
    override suspend fun getSong(id: String): Result<Song?> = firstResolved { it.getSong(id) }
    override suspend fun getArtist(id: String): Result<Artist?> = firstResolved { it.getArtist(id) }
    override suspend fun getAlbum(id: String): Result<Album?> = firstResolved { it.getAlbum(id) }

    override suspend fun getArtistDiscography(artistId: String): Result<List<Song>> =
        mergeLists { provider -> provider.getArtistDiscography(artistId) }

    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> =
        mergeLists { it.getLatestReleases(since) }
}
