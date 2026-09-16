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

    private suspend fun <T> mergeLists(call: suspend (CatalogProvider) -> Result<List<T>>): Result<List<T>> {
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
        if (hadSuccess) return Result.success(merged.values.toList())
        return Result.failure(lastError ?: IllegalStateException("NO_CATALOG_FALLBACK_AVAILABLE"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun valueKey(value: Any): Any = when (value) {
        is Song -> value.id
        is Artist -> value.id
        else -> value
    }

    private suspend fun <T> firstResolved(call: suspend (CatalogProvider) -> Result<T?>): Result<T?> {
        var lastError: Throwable? = null
        var hadSuccess = false
        fallbacks.forEach { provider ->
            val result = runCatching { call(provider) }.getOrElse { Result.failure(it) }
            result.onSuccess {
                hadSuccess = true
                if (it != null) return Result.success(it)
            }.onFailure { lastError = it }
        }
        return if (hadSuccess) Result.success(null)
        else Result.failure(lastError ?: IllegalStateException("NO_CATALOG_FALLBACK_AVAILABLE"))
    }

    override suspend fun searchSongs(query: String): Result<List<Song>> = mergeLists { it.searchSongs(query) }
    override suspend fun searchArtists(query: String): Result<List<Artist>> = mergeLists { it.searchArtists(query) }
    override suspend fun getSong(id: String): Result<Song?> = firstResolved { it.getSong(id) }
    override suspend fun getArtist(id: String): Result<Artist?> = firstResolved { it.getArtist(id) }
    override suspend fun getAlbum(id: String): Result<Album?> = firstResolved { it.getAlbum(id) }
    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> = mergeLists { it.getLatestReleases(since) }
}
