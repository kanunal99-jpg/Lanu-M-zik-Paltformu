package com.example.data.catalog

import com.example.data.MusicDao
import com.example.data.toSong
import com.example.model.Song
import com.example.model.Artist
import com.example.model.Album
import kotlinx.coroutines.flow.first
import java.time.Instant

class CachedCatalogProvider(private val dao: MusicDao) : CatalogProvider {
    override suspend fun searchSongs(query: String): Result<List<Song>> {
        val cached = dao.searchCachedSongsByTitleOrArtist(query).first()
        return Result.success(cached.map { it.toSong() })
    }

    override suspend fun searchArtists(query: String): Result<List<Artist>> {
        val cached = dao.getAllCachedSongs().first().map { it.toSong() }
        val filtered = cached.filter { it.artist.contains(query, ignoreCase = true) }
            .distinctBy { it.artistId }
            .map {
                Artist(
                    id = it.artistId,
                    name = it.artist,
                    genre = it.category.titleTr,
                    // Cached catalog does not contain a verified artist biography.
                    // Never manufacture metadata just to fill the UI.
                    bio = "",
                    imageUrl = it.coverUrl,
                    monthlyListeners = "N/A"
                )
            }
        return Result.success(filtered)
    }

    override suspend fun getSong(id: String): Result<Song?> {
        val song = dao.getAllCachedSongs().first().find { it.id == id }?.toSong()
        return Result.success(song)
    }

    override suspend fun getArtist(id: String): Result<Artist?> {
        val song = dao.getAllCachedSongs().first().find { it.artistId == id }?.toSong()
        if (song != null) {
            val artist = Artist(
                id = song.artistId,
                name = song.artist,
                genre = song.category.titleTr,
                // Cached catalog does not contain a verified artist biography.
                bio = "",
                imageUrl = song.coverUrl,
                monthlyListeners = "N/A"
            )
            return Result.success(artist)
        }
        return Result.success(null)
    }

    override suspend fun getAlbum(id: String): Result<Album?> {
        val songs = dao.getAllCachedSongs().first().filter { it.album.equals(id, ignoreCase = true) }.map { it.toSong() }
        if (songs.isNotEmpty()) {
            val first = songs.first()
            val album = Album(
                id = id,
                title = first.album,
                artist = first.artist,
                artistId = first.artistId,
                coverUrl = first.coverUrl,
                releaseYear = first.releaseYear,
                genre = first.category.titleTr,
                songs = songs
            )
            return Result.success(album)
        }
        return Result.success(null)
    }

    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> {
        val latest = dao.getAllCachedSongs().first().filter { it.isNewRelease }.map { it.toSong() }
        return Result.success(latest)
    }
}
