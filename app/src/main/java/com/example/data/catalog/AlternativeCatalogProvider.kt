package com.example.data.catalog

import com.example.model.Artist
import com.example.model.Song
import com.example.model.Album
import java.time.Instant

class AlternativeCatalogProvider(
    private val primary: CatalogProvider = PrimaryCatalogProvider()
) : CatalogProvider {
    override suspend fun searchSongs(query: String): Result<List<Song>> = primary.searchSongs(query)
    override suspend fun searchArtists(query: String): Result<List<Artist>> = primary.searchArtists(query)
    override suspend fun getSong(id: String): Result<Song?> = primary.getSong(id)
    override suspend fun getArtist(id: String): Result<Artist?> = primary.getArtist(id)
    override suspend fun getAlbum(id: String): Result<Album?> = primary.getAlbum(id)
    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> = primary.getLatestReleases(since)
}
