package com.example.data.catalog

import com.example.data.MusicDao
import com.example.data.toSong
import com.example.model.Song
import com.example.model.Artist
import com.example.model.Album
import com.example.model.SongSourceType
import kotlinx.coroutines.flow.first
import java.time.Instant

/** Disk cache fallback. A cached remote record keeps verified provenance only when its
 * provider namespace identifies a provider that explicitly produced a verified source. */
class CachedCatalogProvider(private val dao: MusicDao) : CatalogProvider {
    private fun Song.withCachedProvenance(): Song = copy(sourceType = when {
        id.startsWith("audius:") || id.startsWith("jamendo:") -> SongSourceType.VERIFIED_REMOTE
        id.startsWith("local_") -> SongSourceType.LOCAL
        else -> SongSourceType.UNKNOWN
    })

    override suspend fun searchSongs(query: String): Result<List<Song>> {
        val cached = dao.searchCachedSongsByTitleOrArtist(query).first()
        return Result.success(cached.map { it.toSong().withCachedProvenance() })
    }

    override suspend fun searchArtists(query: String): Result<List<Artist>> {
        val cached = dao.getAllCachedSongs().first().map { it.toSong().withCachedProvenance() }
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
        val song = dao.getAllCachedSongs().first().find { it.id == id }?.toSong()?.withCachedProvenance()
        return Result.success(song)
    }

    override suspend fun getArtist(id: String): Result<Artist?> {
        val song = dao.getAllCachedSongs().first().find { it.artistId == id }?.toSong()?.withCachedProvenance()
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

    override suspend fun getAlbum(id: String): Result<Album?> {
        val songs = dao.getAllCachedSongs().first().filter { it.album.equals(id, ignoreCase = true) }
            .map { it.toSong().withCachedProvenance() }
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
            .map { it.toSong().withCachedProvenance() }
        return Result.success(latest)
    }
}
