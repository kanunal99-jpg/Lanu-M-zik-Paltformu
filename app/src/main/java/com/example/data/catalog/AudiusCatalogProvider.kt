package com.example.data.catalog

import com.example.model.Album
import com.example.model.Artist
import com.example.model.MusicCategory
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
import java.time.LocalDate
import java.time.ZoneOffset

class AudiusCatalogProvider(
    private val httpClient: OkHttpClient = OkHttpClient()
) : CatalogProvider {
    companion object {
        private const val BASE_URL = "https://api.audius.co/v1"
        private const val FALLBACK_BASE_URL = "https://discoveryprovider.audius.co/v1"
        private const val DEFAULT_LIMIT = 40
        private const val DISCOGRAPHY_PAGE_SIZE = 100
    }

    private suspend fun requestJson(path: String, params: Map<String, String> = emptyMap()): Result<JSONObject> = withContext(Dispatchers.IO) {
        val urls = listOf(BASE_URL, FALLBACK_BASE_URL)
        var lastFailure: Throwable? = null
        for (baseUrl in urls) {
            val query = if (params.isEmpty()) "" else params.entries.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
            val url = if (query.isBlank()) "$baseUrl$path" else "$baseUrl$path?$query"
            try {
                val request = Request.Builder().url(url).get().build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("AUDIUS_HTTP_${response.code}")
                    return@withContext Result.success(JSONObject(response.body?.string().orEmpty()))
                }
            } catch (failure: Throwable) {
                lastFailure = failure
            }
        }
        Result.failure(lastFailure ?: IllegalStateException("AUDIUS_REQUEST_FAILED"))
    }

    private suspend fun requestArray(path: String, params: Map<String, String> = emptyMap()): Result<JSONArray> =
        requestJson(path, params).map { it.optJSONArray("data") ?: JSONArray() }

    override suspend fun searchSongs(query: String): Result<List<Song>> = searchSongsPage(query, 0, DEFAULT_LIMIT)

    override suspend fun searchSongsPage(query: String, page: Int, pageSize: Int): Result<List<Song>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = pageSize.coerceIn(1, 100)
        val directResult = requestArray(
            "/tracks/search",
            mapOf(
                "query" to query,
                "limit" to safeSize.toString(),
                "offset" to (safePage * safeSize).toString()
            )
        ).map { AudiusSongMapper.mapSongs(it) }
        if (query.isBlank()) return directResult
        val directSongs = directResult.getOrElse { return directResult }
        if (safePage > 0) return Result.success(directSongs)
        val artistSearch = requestArray("/users/search", mapOf("query" to query, "limit" to "10"))
            .getOrElse { return Result.success(directSongs) }
        val normalizedQuery = query.trim().lowercase()
        val exactArtists = AudiusArtistMapper.mapArtists(artistSearch)
            .filter { it.name.trim().lowercase() == normalizedQuery }
            .take(3)
        if (exactArtists.isEmpty()) return Result.success(directSongs)
        val artistTracks = buildList {
            exactArtists.forEach { artist -> getArtistDiscography(artist.id).onSuccess { addAll(it) } }
        }
        return Result.success((directSongs + artistTracks).distinctBy { it.id })
    }

    override suspend fun searchArtists(query: String): Result<List<Artist>> =
        requestArray("/users/search", mapOf("query" to query, "limit" to DEFAULT_LIMIT.toString())).map { AudiusArtistMapper.mapArtists(it) }

    override suspend fun getSong(id: String): Result<Song?> = requestJson("/tracks/${id.removePrefix("audius:")}").map { json ->
        val data = json.optJSONObject("data") ?: return@map null
        AudiusSongMapper.mapSongs(JSONArray().put(data)).firstOrNull()
    }

    override suspend fun getArtist(id: String): Result<Artist?> = requestJson("/users/${id.removePrefix("audius:")}").map { json ->
        val data = json.optJSONObject("data") ?: return@map null
        AudiusArtistMapper.mapArtists(JSONArray().put(data)).firstOrNull()
    }

    override suspend fun getAlbum(id: String): Result<Album?> = requestJson("/playlists/${id.removePrefix("audius_album:")}").map { json ->
        val data = json.optJSONObject("data") ?: return@map null
        if (!data.optBooleanAny("isAlbum", "is_album")) return@map null
        val tracks = AudiusSongMapper.mapSongs(data.optJSONArray("tracks") ?: JSONArray())
        if (tracks.isEmpty()) null else {
            val first = tracks.first()
            Album(id = id, title = data.optStringAny("playlist_name", "playlistName").ifBlank { "Album" }, artist = first.artist,
                artistId = first.artistId, coverUrl = data.optStringAny("playlist_image", "playlistImage").ifBlank { first.coverUrl },
                releaseYear = first.releaseYear, genre = first.category.titleTr, songs = tracks)
        }
    }

    override suspend fun getArtistDiscography(artistId: String): Result<List<Song>> = runCatching {
        val providerArtistId = artistId.removePrefix("audius:")

        val canonical = runCatching {
            val allSongs = buildList {
                var offset = 0
                while (true) {
                    val page = requestArray(
                        "/users/$providerArtistId/tracks",
                        mapOf("limit" to DISCOGRAPHY_PAGE_SIZE.toString(), "offset" to offset.toString())
                    ).getOrThrow()
                    addAll(AudiusSongMapper.mapSongs(page).filter { it.artistId == "audius:$providerArtistId" })
                    if (page.length() < DISCOGRAPHY_PAGE_SIZE) break
                    offset += DISCOGRAPHY_PAGE_SIZE
                }
            }.distinctBy { it.id }
            allSongs.takeIf { it.isNotEmpty() }
        }.getOrNull()

        if (!canonical.isNullOrEmpty()) return@runCatching canonical

        // Audius can return live search results for an artist whose legacy user-tracks
        // route is no longer resolvable. Keep the chain working by resolving the profile,
        // searching the live catalog by the verified artist name, and requiring an exact id.
        val artist = getArtist("audius:$providerArtistId").getOrNull()
            ?: return@runCatching emptyList()
        searchSongs(artist.name).getOrElse { emptyList() }
            .filter { it.artistId == "audius:$providerArtistId" }
            .distinctBy { it.id }
    }

    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> =
        requestArray("/tracks/latest", mapOf("limit" to DEFAULT_LIMIT.toString())).map { results ->
            AudiusSongMapper.mapSongs(results).filter { song -> since == null || song.releaseYear >= since.atZone(ZoneOffset.UTC).year }
        }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}

internal object AudiusArtistMapper {
    fun mapArtists(results: JSONArray): List<Artist> = buildList {
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val id = item.optStringAny("id", "user_id").trim()
            val name = item.optStringAny("name", "handle").trim()
            val handle = item.optStringAny("handle").trim()
            if (id.isBlank() || name.isBlank()) continue
            val image = item.optJSONObject("profile_picture")
            add(Artist(id = "audius:$id", name = name, genre = "", bio = item.optStringAny("bio").trim(),
                imageUrl = image?.optStringAny("_480x480", "480x480", "_150x150", "150x150").orEmpty(), monthlyListeners = "", handle = handle))
        }
    }
}

internal object AudiusSongMapper {
    private const val STREAM_BASE = "https://discoveryprovider.audius.co/v1/tracks"

    fun mapSongs(results: JSONArray): List<Song> = buildList {
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val id = item.optStringAny("id").trim()
            val title = item.optStringAny("title", "name").trim()
            val streamableValue = item.optAny("isStreamable", "is_streamable")
            val streamable = streamableValue?.toString()?.lowercase()?.let { it == "true" || it == "1" }
            val streamGated = item.optBooleanAny("isStreamGated", "is_stream_gated")
            val unlisted = item.optBooleanAny("isUnlisted", "is_unlisted")
            if (id.isBlank() || title.isBlank() || streamable == false || streamGated || unlisted) continue
            val user = item.optJSONObject("user")
            val artist = user?.optStringAny("name", "handle").orEmpty().trim().ifBlank { item.optStringAny("artist_name").trim() }
            val artistId = user?.optStringAny("id", "user_id").orEmpty().trim().ifBlank { item.optStringAny("artist_id").trim() }
            if (artist.isBlank() || artistId.isBlank()) continue
            val rawLicense = item.optStringAny("license", "license_info").trim()
            if (!CatalogLicensePolicy.isPermittedAudiusLicense(rawLicense)) continue
            val license = CatalogLicensePolicy.effectiveAudiusLicense(rawLicense)
            val genre = item.optStringAny("genre").trim().lowercase()
            val tags = item.optJSONArrayAny("tags").strings().map(String::lowercase)
            val language = inferLanguage(tags)
            val category = inferCategory(genre, tags, language)
            val releaseYear = item.optStringAny("releaseDate", "release_date", "releasedate").take(4).toIntOrNull() ?: 0
            val artwork = item.optJSONObject("artwork")
            val coverUrl = artwork?.optStringAny("_480x480", "480x480", "_150x150", "150x150").orEmpty()
            add(Song(id = "audius:$id", title = title, artist = artist, artistId = "audius:$artistId",
                album = item.optStringAny("album_name", "albumName").trim().ifBlank { "Single" },
                durationMs = item.optLongAny("duration") * 1000L, category = category, language = language,
                coverUrl = coverUrl, audioUrl = "$STREAM_BASE/$id/stream", releaseYear = releaseYear,
                isNewRelease = releaseYear >= LocalDate.now(ZoneOffset.UTC).year,
                playCount = item.optLongAny("playCount", "play_count", "plays"),
                license = license,
                sourceType = SongSourceType.VERIFIED_REMOTE))
        }
    }

    private fun inferCategory(genre: String, tags: List<String>, language: String): MusicCategory = when {
        genre.contains("rap") || genre.contains("hip") || tags.any { it.contains("rap") || it.contains("hiphop") } ->
            if (language == "tr") MusicCategory.TURKCE_RAP else MusicCategory.HIP_HOP
        genre.contains("rock") || genre.contains("metal") -> MusicCategory.ROCK_CLASSICS
        genre.contains("electronic") || genre.contains("dance") || genre.contains("house") || genre.contains("techno") -> MusicCategory.EDM_DANCE
        genre.contains("r&b") || genre.contains("rnb") || genre.contains("soul") -> MusicCategory.HIP_HOP
        genre.contains("acoustic") || genre.contains("chill") || genre.contains("ambient") -> MusicCategory.CHILL_LOFI
        genre.contains("pop") -> MusicCategory.GLOBAL_POP
        genre.contains("classical") -> MusicCategory.CHILL_LOFI
        else -> MusicCategory.GLOBAL_POP
    }

    private fun inferLanguage(tags: List<String>): String = when {
        tags.any { it == "turkish" || it == "türkçe" || it == "turkey" || it == "türkiye" || it == "tr" } -> "tr"
        tags.any { it == "english" } -> "en"
        else -> "und"
    }

    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
}

private fun JSONObject.optAny(vararg keys: String): Any? = keys.firstNotNullOfOrNull { key -> if (has(key) && !isNull(key)) opt(key) else null }
private fun JSONObject.optStringAny(vararg keys: String): String = keys.firstNotNullOfOrNull { key -> if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null }.orEmpty()
private fun JSONObject.optBooleanAny(vararg keys: String): Boolean = keys.firstNotNullOfOrNull { key -> if (has(key) && !isNull(key)) when (val value = opt(key)) { is Boolean -> value; is Number -> value.toInt() != 0; else -> value.toString().equals("true", true) || value.toString() == "1" } else null } ?: false
private fun JSONObject.optLongAny(vararg keys: String): Long = keys.firstNotNullOfOrNull { key -> if (has(key) && !isNull(key)) when (val value = opt(key)) { is Number -> value.toLong(); else -> value.toString().toLongOrNull() } else null } ?: 0L
private fun JSONObject.optJSONArrayAny(vararg keys: String): JSONArray? = keys.firstNotNullOfOrNull { key -> if (has(key) && !isNull(key)) optJSONArray(key) else null }
