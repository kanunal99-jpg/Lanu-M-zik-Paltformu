package com.example.data.catalog

import com.example.model.Album
import com.example.model.Artist
import com.example.model.Song
import java.time.Instant

class PrimaryCatalogProvider : CatalogProvider {
    private fun unavailable(): Result<Nothing> = Result.failure(IllegalStateException("NO_APPROVED_PRIMARY_CATALOG_CONFIGURED"))
    override suspend fun searchSongs(query: String): Result<List<Song>> = unavailable()
    override suspend fun searchArtists(query: String): Result<List<Artist>> = unavailable()
    override suspend fun getSong(id: String): Result<Song?> = unavailable()
    override suspend fun getArtist(id: String): Result<Artist?> = unavailable()
    override suspend fun getAlbum(id: String): Result<Album?> = unavailable()
    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> = unavailable()
}
