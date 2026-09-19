package com.example.data.catalog

import com.example.model.Album
import com.example.model.Artist
import com.example.model.Song
import java.time.Instant

class PrimaryCatalogProvider(
    clientId: String,
    commercialLicenseConfirmed: Boolean = false
) : CatalogProvider {
    private val delegate: CatalogProvider = JamendoCatalogProvider(clientId, commercialLicenseConfirmed)

    override suspend fun searchSongs(query: String): Result<List<Song>> = delegate.searchSongs(query)
    override suspend fun searchSongsPage(query: String, page: Int, pageSize: Int): Result<List<Song>> =
        delegate.searchSongsPage(query, page, pageSize)
    override suspend fun searchArtists(query: String): Result<List<Artist>> = delegate.searchArtists(query)
    override suspend fun getSong(id: String): Result<Song?> = delegate.getSong(id)
    override suspend fun getArtist(id: String): Result<Artist?> = delegate.getArtist(id)
    override suspend fun getAlbum(id: String): Result<Album?> = delegate.getAlbum(id)
    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> = delegate.getLatestReleases(since)
}
