package com.example.data.catalog

import com.example.model.Song
import com.example.model.Artist
import com.example.model.Album
import java.time.Instant

interface CatalogProvider {
    suspend fun searchSongs(query: String): Result<List<Song>>
    /** Paginated catalog search used by the user-triggered "Tüm Şarkılar" index expansion. */
    suspend fun searchSongsPage(query: String, page: Int, pageSize: Int): Result<List<Song>> =
        searchSongs(query)
    suspend fun searchArtists(query: String): Result<List<Artist>>
    suspend fun getSong(id: String): Result<Song?>
    suspend fun getArtist(id: String): Result<Artist?>
    suspend fun getAlbum(id: String): Result<Album?>
    /** Returns only tracks resolved from the provider's verified artist identity. */
    suspend fun getArtistDiscography(artistId: String): Result<List<Song>> = Result.success(emptyList())
    suspend fun getLatestReleases(since: Instant?): Result<List<Song>>
}
