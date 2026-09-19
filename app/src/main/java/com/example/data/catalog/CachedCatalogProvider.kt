package com.example.data.catalog

import com.example.data.MusicDao
import com.example.data.toSong
import com.example.model.Song
import com.example.model.Artist
import com.example.model.Album
import kotlinx.coroutines.flow.first
import java.time.Instant

/** Disk cache fallback. Only cache rows carrying persisted verified provenance are exposed. */
class CachedCatalogProvider(private val dao: MusicDao) : CatalogProvider {
    private fun List<Song>.verifiedOnly(): List<Song> =
        filter { it.sourceType != com.example.model.SongSourceType.UNKNOWN }


    override suspend fun searchSongs(query: String): Result<List<Song>> {
        val cached = dao.searchCachedSongsByTitleOrArtist(query).first()
        return Result.success(cached.map { it.toSong() }.verifiedOnly())
    }

    /** Cache is a page-zero fallback; remote providers own progressive pagination. */
    override suspend fun searchSongsPage(query: String, page: Int, pageSize: Int): Result<List<Song>> {
        if (page > 0) return Result.success(emptyList())
        return searchSongs(query)
    }

    override suspend fun searchArtists(query: String): Result<List<Artist>> {
        val cached = dao.getAllCachedSongs().first().map { it.toSong() }.verifiedOnly()
        val filtered = cached.filter { it.artist.contains(query, ignoreCase = true) }
            .distinctBy { it.artistId }
            .map {
                Artist(
                    id = it.artistId,
                    name = it.artist,
                    genre = it.category.titleTr,
                    bio = "",
                    imageUrl = it.coverUrl,
                    monthlyListeners = "N/A"
                )
            }
        return Result.success(filtered)
    }

    override suspend fun getSong(id: String): Result<Song?> {
        val song = dao.getAllCachedSongs().first().find { it.id == id }?.toSong()?.takeIf { it.sourceType != com.example.model.SongSourceType.UNKNOWN }
        return Result.success(song)
    }

    override suspend fun getArtist(id: String): Result<Artist?> {
        val song = dao.getAllCachedSongs().first().find { it.artistId == id }?.toSong()?.takeIf { it.sourceType != com.example.model.SongSourceType.UNKNOWN }
        if (song != null) {
            return Result.success(
                Artist(
                    id = song.artistId,
                    name = song.artist,
                    genre = song.category.titleTr,
                    bio = "",
                    imageUrl = song.coverUrl,
                    monthlyListeners = "N/A"
                )
            )
        }
        return Result.success(null)
    }

    override suspend fun getArtistDiscography(artistId: String): Result<List<Song>> = runCatching {
        dao.getAllCachedSongs().first()
            .map { it.toSong() }
            .verifiedOnly()
            .filter { it.artistId == artistId }
            .distinctBy { it.id }
    }

    override suspend fun getAlbum(id: String): Result<Album?> {
        val songs = dao.getAllCachedSongs().first().filter { it.album.equals(id, ignoreCase = true) }
            .map { it.toSong() }
            .verifiedOnly()
        if (songs.isNotEmpty()) {
            val first = songs.first()
            return Result.success(
                Album(
                    id = id,
                    title = first.album,
                    artist = first.artist,
                    artistId = first.artistId,
                    coverUrl = first.coverUrl,
                    releaseYear = first.releaseYear,
                    genre = first.category.titleTr,
                    songs = songs
                )
            )
        }
        return Result.success(null)
    }

    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> {
        val latest = dao.getAllCachedSongs().first().filter { it.isNewRelease }
            .map { it.toSong() }
            .verifiedOnly()
        return Result.success(latest)
    }
}
