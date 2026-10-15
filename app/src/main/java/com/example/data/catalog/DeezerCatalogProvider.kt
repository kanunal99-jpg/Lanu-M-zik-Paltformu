package com.example.data.catalog

import com.example.model.Album
import com.example.model.Artist
import com.example.model.MusicCategory
import com.example.model.MusicGenreResolver
import com.example.model.Song
import com.example.model.SongSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant

/** Deezer public catalog adapter. Playback is limited to official preview URLs. */
class DeezerCatalogProvider(private val httpClient: OkHttpClient = OkHttpClient()) : CatalogProvider {
    companion object {
        private const val BASE_URL = "https://api.deezer.com"
        private const val LIMIT = 50
    }

    private suspend fun request(path: String, params: Map<String, String> = emptyMap()): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val query = params.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
            val url = if (query.isBlank()) "$BASE_URL$path" else "$BASE_URL$path?$query"
            httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) error("DEEZER_HTTP_${response.code}")
                JSONObject(response.body?.string().orEmpty()).also { json ->
                    if (json.has("error")) error("DEEZER_API_${json.optJSONObject("error")?.optString("type").orEmpty()}")
                }
            }
        }
    }

    override suspend fun searchSongs(query: String): Result<List<Song>> = searchSongsPage(query, 0, LIMIT)

    override suspend fun searchSongsPage(query: String, page: Int, pageSize: Int): Result<List<Song>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = pageSize.coerceIn(1, 100)
        return request("/search", mapOf(
            "q" to query,
            "limit" to safeSize.toString(),
            "index" to (safePage * safeSize).toString()
        )).map { json -> mapTracks(json.optJSONArray("data") ?: JSONArray()) }
    }

    override suspend fun searchArtists(query: String): Result<List<Artist>> =
        request("/search/artist", mapOf("q" to query, "limit" to LIMIT.toString())).map { json -> mapArtists(json.optJSONArray("data") ?: JSONArray()) }

    override suspend fun getSong(id: String): Result<Song?> = request("/track/${id.removePrefix("deezer:")}").map { json -> mapTracks(JSONArray().put(json)).firstOrNull() }

    override suspend fun getArtist(id: String): Result<Artist?> = request("/artist/${id.removePrefix("deezer:")}").map { json -> mapArtists(JSONArray().put(json)).firstOrNull() }

    override suspend fun getAlbum(id: String): Result<Album?> = request("/album/${id.removePrefix("deezer_album:")}").map(::mapAlbum)

    override suspend fun getArtistDiscography(artistId: String): Result<List<Song>> = runCatching {
        val providerArtistId = artistId.removePrefix("deezer:")
        val verified = linkedMapOf<String, Song>()
        // Singles and collaborations can be missing from an album-only index.
        request("/artist/$providerArtistId/top", mapOf("limit" to "100"))
            .getOrNull()?.optJSONArray("data")?.let { top ->
                mapTracks(top).filter { it.artistId == "deezer:$providerArtistId" }
                    .forEach { verified[it.id] = it }
            }

        val visitedAlbums = hashSetOf<String>()
        var albumOffset = 0
        val albumPageSize = 50
        // Continue until the provider reaches its real end, not a fixed six-page cap.
        while (true) {
            val albumsJson = request(
                "/artist/$providerArtistId/albums",
                mapOf("limit" to albumPageSize.toString(), "index" to albumOffset.toString())
            ).getOrElse { error ->
                if (albumOffset == 0 && verified.isEmpty()) throw error
                break // Keep already fetched, verified songs if a later page fails.
            }
            val albums = albumsJson.optJSONArray("data") ?: JSONArray()
            if (albums.length() == 0) break
            var discovered = 0
            for (i in 0 until albums.length()) {
                val albumId = albums.optJSONObject(i)?.optString("id").orEmpty()
                if (albumId.isBlank() || !visitedAlbums.add(albumId)) continue
                discovered++
                val albumJson = request("/album/$albumId").getOrNull() ?: continue
                fetchAlbumTracks(albumId, albumJson, "deezer:$providerArtistId")
                    .forEach { verified[it.id] = it }
            }
            albumOffset += albums.length()
            // Protect against provider responses that repeat the same page forever.
            if (discovered == 0 || albums.length() < albumPageSize) break
            if (albumsJson.has("total") && albumOffset >= albumsJson.optInt("total")) break
        }
        verified.values.toList()
    }

    /** Deezer's embedded album track list can contain only the first page. */
    private suspend fun fetchAlbumTracks(albumId: String, albumJson: JSONObject, artistId: String): List<Song> {
        val trackListing = albumJson.optJSONObject("tracks") ?: return emptyList()
        val firstPage = trackListing.optJSONArray("data") ?: JSONArray()
        val verified = linkedMapOf<String, Song>()
        fun addPage(page: JSONArray) {
            mapTracks(page).filter { it.artistId == artistId }
                .forEach { verified[it.id] = it }
        }
        addPage(firstPage)
        val total = trackListing.optInt("total", firstPage.length()).coerceAtLeast(0)
        var offset = firstPage.length()
        // The offset advances by returned rows, not playable rows: preview-less
        // recordings must not stall pagination or turn into fabricated audio.
        while (offset < total) {
            val nextPage = request(
                "/album/$albumId/tracks",
                mapOf("limit" to "100", "index" to offset.toString())
            ).getOrNull()?.optJSONArray("data") ?: break
            if (nextPage.length() == 0) break
            addPage(nextPage)
            offset += nextPage.length()
        }
        return verified.values.toList()
    }

    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> =
        request("/chart/0/tracks", mapOf("limit" to LIMIT.toString())).map { json -> mapTracks(json.optJSONArray("data") ?: JSONArray()) }

    private fun mapArtists(data: JSONArray): List<Artist> = buildList {
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val id = item.optString("id").trim()
            val name = item.optString("name").trim()
            if (id.isBlank() || name.isBlank()) continue
            add(Artist(id = "deezer:$id", name = name, genre = "", bio = "Doğrulanmış Deezer katalog sanatçısı",
                imageUrl = item.optString("picture_xl").ifBlank { item.optString("picture_big") },
                monthlyListeners = item.optLong("nb_fan").takeIf { it > 0 }?.toString().orEmpty()))
        }
    }

    private fun mapTracks(data: JSONArray): List<Song> = buildList {
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val id = item.optString("id").trim()
            val title = item.optString("title").trim()
            val artistObj = item.optJSONObject("artist")
            val artistId = artistObj?.optString("id").orEmpty().trim()
            val artist = artistObj?.optString("name").orEmpty().trim()
            val preview = item.optString("preview").trim()
            if (id.isBlank() || title.isBlank() || artistId.isBlank() || artist.isBlank() || preview.isBlank()) continue
            val albumObj = item.optJSONObject("album")
            val album = albumObj?.optString("title").orEmpty().ifBlank { "Single" }
            val cover = albumObj?.optString("cover_xl").orEmpty().ifBlank { albumObj?.optString("cover_big").orEmpty() }
            add(Song(id = "deezer:$id", title = title, artist = artist, artistId = "deezer:$artistId", album = album,
                durationMs = item.optLong("duration", 0L) * 1000L, category = MusicGenreResolver.fromMetadata(artist, "", emptyList(), "und"), language = "und",
                coverUrl = cover, audioUrl = preview, releaseYear = 0, license = "deezer:official-preview", sourceType = SongSourceType.VERIFIED_PREVIEW))
        }
    }

    private fun mapAlbum(json: JSONObject): Album? {
        val id = json.optString("id").trim()
        val title = json.optString("title").trim()
        val artistObj = json.optJSONObject("artist")
        val artistId = artistObj?.optString("id").orEmpty().trim()
        val artist = artistObj?.optString("name").orEmpty().trim()
        if (id.isBlank() || title.isBlank() || artistId.isBlank() || artist.isBlank()) return null
        val tracks = mapTracks(json.optJSONObject("tracks")?.optJSONArray("data") ?: JSONArray())
        return Album(id = "deezer_album:$id", title = title, artist = artist, artistId = "deezer:$artistId",
            coverUrl = json.optString("cover_xl").ifBlank { json.optString("cover_big") },
            releaseYear = json.optString("release_date").take(4).toIntOrNull() ?: 0, genre = "", songs = tracks)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
