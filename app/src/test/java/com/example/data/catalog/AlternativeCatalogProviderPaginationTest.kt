package com.example.data.catalog

import com.example.model.Album
import com.example.model.Artist
import com.example.model.MusicCategory
import com.example.model.Song
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AlternativeCatalogProviderPaginationTest {

    @Test
    fun paginatedSearchMergesUniqueResultsFromEveryProvider() = runTest {
        val provider = AlternativeCatalogProvider(
            listOf(
                FakeProvider("a"),
                FakeProvider("b")
            )
        )

        val songs = provider.searchSongsPage("rock", page = 2, pageSize = 10).getOrThrow()

        assertEquals(
            listOf("a-2-10", "b-2-10"),
            songs.map { it.id }
        )
    }

    private class FakeProvider(private val prefix: String) : CatalogProvider {
        override suspend fun searchSongs(query: String): Result<List<Song>> = Result.success(emptyList())

        override suspend fun searchSongsPage(query: String, page: Int, pageSize: Int): Result<List<Song>> =
            Result.success(
                listOf(
                    Song(
                        id = "$prefix-$page-$pageSize",
                        title = "$prefix result",
                        artist = "$prefix artist",
                        artistId = "$prefix-artist",
                        album = "Single",
                        durationMs = 1_000,
                        category = MusicCategory.GLOBAL_POP,
                        coverUrl = "",
                        audioUrl = "",
                        releaseYear = 0,
                        sourceType = com.example.model.SongSourceType.VERIFIED_PREVIEW,
                        license = "$prefix:test"
                    )
                )
            )

        override suspend fun searchArtists(query: String): Result<List<Artist>> = Result.success(emptyList())

        override suspend fun getSong(id: String): Result<Song?> = Result.success(null)

        override suspend fun getArtist(id: String): Result<Artist?> = Result.success(null)

        override suspend fun getAlbum(id: String): Result<Album?> = Result.success(null)

        override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> = Result.success(emptyList())
    }
}
