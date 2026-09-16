package com.example.data.catalog

import com.example.model.Album
import com.example.model.Artist
import com.example.model.Song
import java.time.Instant

class PrimaryCatalogProvider(
    clientId: String
) : CatalogProvider {
    private val delegate: CatalogProvider = JamendoCatalogProvider(clientId)

    override suspend fun searchSongs(query: String): Result<List<Song>> = delegate.searchSongs(query)
    override suspend fun searchArtists(query: String): Result<List<Artist>> = delegate.searchArtists(query)
    override suspend fun getSong(id: String): Result<Song?> = delegate.getSong(id)
    override suspend fun getArtist(id: String): Result<Artist?> = delegate.getArtist(id)
    override suspend fun getAlbum(id: String): Result<Album?> = delegate.getAlbum(id)
    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> = delegate.getLatestReleases(since)
}
