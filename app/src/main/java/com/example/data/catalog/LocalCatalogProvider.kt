package com.example.data.catalog

import com.example.data.MusicDao
import com.example.data.toSong
import com.example.model.Song
import com.example.model.Artist
import com.example.model.Album
import com.example.model.SongSourceType
import com.example.search.TypoTolerantSearch
import kotlinx.coroutines.flow.first
import java.time.Instant

class LocalCatalogProvider(private val dao: MusicDao) : CatalogProvider {
    private fun Song.asLocal(): Song = copy(sourceType = SongSourceType.LOCAL)

    override suspend fun searchSongs(query: String): Result<List<Song>> {
        val songs = dao.getAllLocalSongs().first().filter {
            TypoTolerantSearch.matches(query, it.title) || TypoTolerantSearch.matches(query, it.artist) || TypoTolerantSearch.matches(query, it.album)
        }.map { it.toSong().asLocal() }
        return Result.success(songs)
    }

    override suspend fun searchArtists(query: String): Result<List<Artist>> {
        val songs = dao.getAllLocalSongs().first().map { it.toSong().asLocal() }
        val artists = songs.filter { TypoTolerantSearch.matches(query, it.artist) }.distinctBy { it.artistId }.map {
            Artist(id = it.artistId, name = it.artist, genre = it.category.titleTr, bio = "Yerel Sanatçı", imageUrl = it.coverUrl, monthlyListeners = "Yerel Dosya")
        }
        return Result.success(artists)
    }

    override suspend fun getSong(id: String): Result<Song?> = Result.success(dao.getLocalSongByIdSync(id)?.toSong()?.asLocal())

    override suspend fun getArtist(id: String): Result<Artist?> {
        val songs = dao.getAllLocalSongs().first().filter { it.artistId == id }
        if (songs.isNotEmpty()) {
            val first = songs.first()
            return Result.success(Artist(id = first.artistId, name = first.artist, genre = first.genre, bio = "Yerel Sanatçı", imageUrl = first.remoteCoverUrl, monthlyListeners = "Yerel Dosya"))
        }
        return Result.success(null)
    }

    override suspend fun getArtistDiscography(artistId: String): Result<List<Song>> = runCatching {
        dao.getAllLocalSongs().first()
            .filter { it.artistId == artistId }
            .map { it.toSong().asLocal() }
            .distinctBy { it.id }
    }

    override suspend fun getAlbum(id: String): Result<Album?> {
        val songs = dao.getAllLocalSongs().first().filter { it.album.equals(id, ignoreCase = true) }.map { it.toSong().asLocal() }
        if (songs.isNotEmpty()) {
            val first = songs.first()
            return Result.success(Album(id = id, title = first.album, artist = first.artist, artistId = first.artistId, coverUrl = first.coverUrl, releaseYear = first.releaseYear, genre = first.category.titleTr, songs = songs))
        }
        return Result.success(null)
    }

    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> = Result.success(emptyList())
}
