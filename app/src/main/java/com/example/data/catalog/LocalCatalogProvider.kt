package com.example.data.catalog

import com.example.data.MusicDao
import com.example.data.toSong
import com.example.model.Song
import com.example.model.Artist
import com.example.model.Album
import kotlinx.coroutines.flow.first
import java.time.Instant

class LocalCatalogProvider(private val dao: MusicDao) : CatalogProvider {

    override suspend fun searchSongs(query: String): Result<List<Song>> {
        val normalizedQuery = normalizeSearch(query)
        val songs = dao.getAllLocalSongs().first().filter {
            normalizedQuery.isBlank() ||
                listOf(it.title, it.artist, it.album).any { value -> normalizeSearch(value).contains(normalizedQuery) }
        }.map { it.toSong() }
        return Result.success(songs)
    }

    override suspend fun searchArtists(query: String): Result<List<Artist>> {
        val normalizedQuery = normalizeSearch(query)
        val songs = dao.getAllLocalSongs().first().map { it.toSong() }
        val artists = songs.filter { normalizedQuery.isBlank() || normalizeSearch(it.artist).contains(normalizedQuery) }
            .distinctBy { it.artistId }
            .map {
                Artist(
                    id = it.artistId,
                    name = it.artist,
                    genre = it.category.titleTr,
                    bio = "Yerel Sanatçı",
                    imageUrl = it.coverUrl,
                    monthlyListeners = "Yerel Dosya"
                )
            }
        return Result.success(artists)
    }

    override suspend fun getSong(id: String): Result<Song?> {
        val song = dao.getLocalSongByIdSync(id)?.toSong()
        return Result.success(song)
    }

    override suspend fun getArtist(id: String): Result<Artist?> {
        val songs = dao.getAllLocalSongs().first().filter { it.artistId == id }
        if (songs.isNotEmpty()) {
            val first = songs.first()
            val artist = Artist(
                id = first.artistId,
                name = first.artist,
                genre = first.genre,
                bio = "Yerel Sanatçı",
                imageUrl = first.remoteCoverUrl,
                monthlyListeners = "Yerel Dosya"
            )
            return Result.success(artist)
        }
        return Result.success(null)
    }

    override suspend fun getAlbum(id: String): Result<Album?> {
        val songs = dao.getAllLocalSongs().first().filter { it.album.equals(id, ignoreCase = true) }.map { it.toSong() }
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
        return Result.success(emptyList())
    }

    private fun normalizeSearch(value: String): String = value.trim().lowercase()
        .replace('ı', 'i').replace('İ', 'i').replace('ş', 's').replace('Ş', 's')
        .replace('ğ', 'g').replace('Ğ', 'g').replace('ü', 'u').replace('Ü', 'u')
        .replace('ö', 'o').replace('Ö', 'o').replace('ç', 'c').replace('Ç', 'c')
}
