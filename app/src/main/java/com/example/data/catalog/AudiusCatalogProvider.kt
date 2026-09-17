package com.example.data.catalog

import com.example.model.Album
import com.example.model.Artist
import com.example.model.MusicCategory
import com.example.model.Song
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

/**
 * Audius public read-only catalog adapter.
 * Audius documents most read endpoints as credential-free; stream URLs are
 * resolved through the official /tracks/{id}/stream endpoint.
 */
class AudiusCatalogProvider(
    private val httpClient: OkHttpClient = OkHttpClient()
) : CatalogProvider {

    companion object {
        private const val BASE_URL = "https://discoveryprovider.audius.co/v1"
        private const val DEFAULT_LIMIT = 40
    }

    private suspend fun requestJson(path: String, params: Map<String, String> = emptyMap()): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            runCatching {
                val query = if (params.isEmpty()) "" else params.entries.joinToString("&") { (key, value) ->
                    "${encode(key)}=${encode(value)}"
                }
                val url = if (query.isBlank()) "$BASE_URL$path" else "$BASE_URL$path?$query"
                val request = Request.Builder().url(url).get().build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("AUDIUS_HTTP_${response.code}")
                    JSONObject(response.body?.string().orEmpty())
                }
            }
        }

    private suspend fun requestArray(path: String, params: Map<String, String> = emptyMap()): Result<JSONArray> =
        requestJson(path, params).map { it.optJSONArray("data") ?: JSONArray() }

    override suspend fun searchSongs(query: String): Result<List<Song>> =
        requestArray("/tracks/search", mapOf("query" to query, "limit" to DEFAULT_LIMIT.toString()))
            .map { AudiusSongMapper.mapSongs(it) }

    override suspend fun searchArtists(query: String): Result<List<Artist>> =
        requestArray("/users/search", mapOf("query" to query, "limit" to DEFAULT_LIMIT.toString()))
            .map { AudiusArtistMapper.mapArtists(it) }

    override suspend fun getSong(id: String): Result<Song?> =
        requestJson("/tracks/${id.removePrefix("audius:")}")
            .map { AudiusSongMapper.mapSongs(JSONArray().put(it.optJSONObject("data"))).firstOrNull() }

    override suspend fun getArtist(id: String): Result<Artist?> =
        requestJson("/users/${id.removePrefix("audius:")}")
            .map { AudiusArtistMapper.mapArtists(JSONArray().put(it.optJSONObject("data"))).firstOrNull() }

    override suspend fun getAlbum(id: String): Result<Album?> =
        requestJson("/playlists/${id.removePrefix("audius_album:")}")
            .map { json ->
                val data = json.optJSONObject("data") ?: return@map null
                if (!data.optBoolean("isAlbum", false)) return@map null
                val tracks = AudiusSongMapper.mapSongs(data.optJSONArray("tracks") ?: JSONArray())
                if (tracks.isEmpty()) null else {
                    val first = tracks.first()
                    Album(
                        id = id,
                        title = data.optString("playlist_name").ifBlank { data.optString("playlist_name", "Album") },
                        artist = first.artist,
                        artistId = first.artistId,
                        coverUrl = data.optString("playlist_image").ifBlank { first.coverUrl },
                        releaseYear = first.releaseYear,
                        genre = first.category.titleTr,
                        songs = tracks
                    )
                }
            }

    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> =
        requestArray("/tracks/latest", mapOf("limit" to DEFAULT_LIMIT.toString()))
            .map { results ->
                AudiusSongMapper.mapSongs(results).filter { song ->
                    since == null || song.releaseYear >= since.atZone(ZoneOffset.UTC).year
                }
            }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}

internal object AudiusArtistMapper {
    fun mapArtists(results: JSONArray): List<Artist> = buildList {
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val id = item.optString("id").trim().ifBlank { item.optString("user_id").trim() }
            val name = item.optString("name").trim().ifBlank { item.optString("handle").trim() }
            if (id.isBlank() || name.isBlank()) continue
            val image = item.optJSONObject("profile_picture")
            add(
                Artist(
                    id = "audius:$id",
                    name = name,
                    genre = "",
                    bio = item.optString("bio").trim(),
                    imageUrl = image?.optString("_480x480").orEmpty().ifBlank { image?.optString("_150x150").orEmpty() },
                    monthlyListeners = ""
                )
            )
        }
    }
}

internal object AudiusSongMapper {
    private const val STREAM_BASE = "https://discoveryprovider.audius.co/v1/tracks"

    fun mapSongs(results: JSONArray): List<Song> = buildList {
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val id = item.optString("id").trim()
            val title = item.optString("title").trim()
            val streamable = item.opt("isStreamable")?.toString()?.lowercase() == "true"
            val streamGated = item.optBoolean("isStreamGated", false)
            val unlisted = item.optBoolean("isUnlisted", false)
            if (id.isBlank() || title.isBlank() || !streamable || streamGated || unlisted) continue

            val user = item.optJSONObject("user")
            val artist = user?.optString("name").orEmpty().trim().ifBlank { user?.optString("handle").orEmpty().trim() }
            val artistId = user?.optString("id").orEmpty().trim().ifBlank { user?.optString("user_id").orEmpty().trim() }
            if (artist.isBlank() || artistId.isBlank()) continue

            val genre = item.optString("genre").trim().lowercase()
            val tags = item.optJSONArray("tags").strings().map(String::lowercase)
            val category = inferCategory(genre, tags)
            val releaseYear = item.optString("releaseDate").take(4).toIntOrNull() ?: 0
            val artwork = item.optJSONObject("artwork")
            val coverUrl = artwork?.optString("_480x480").orEmpty().ifBlank {
                artwork?.optString("_150x150").orEmpty()
            }
            val audioUrl = "$STREAM_BASE/$id/stream"

            add(
                Song(
                    id = "audius:$id",
                    title = title,
                    artist = artist,
                    artistId = "audius:$artistId",
                    album = item.optString("album_name").trim().ifBlank { "Single" },
                    durationMs = item.optLong("duration", 0L) * 1000L,
                    category = category,
                    language = inferLanguage(tags),
                    coverUrl = coverUrl,
                    audioUrl = audioUrl,
                    releaseYear = releaseYear,
                    isNewRelease = releaseYear >= LocalDate.now(ZoneOffset.UTC).year,
                    playCount = item.optLong("playCount", 0L)
                )
            )
        }
    }

    private fun inferCategory(genre: String, tags: List<String>): MusicCategory = when {
        genre.contains("rap") || genre.contains("hip") || tags.any { it.contains("rap") || it.contains("hiphop") } -> MusicCategory.HIP_HOP
        genre.contains("rock") || genre.contains("metal") -> MusicCategory.ROCK_CLASSICS
        genre.contains("electronic") || genre.contains("dance") || genre.contains("house") || genre.contains("techno") -> MusicCategory.EDM_DANCE
        genre.contains("r&b") || genre.contains("rnb") || genre.contains("soul") -> MusicCategory.HIP_HOP
        genre.contains("acoustic") || genre.contains("chill") || genre.contains("ambient") -> MusicCategory.CHILL_LOFI
        genre.contains("pop") -> MusicCategory.GLOBAL_POP
        genre.contains("classical") -> MusicCategory.CHILL_LOFI
        else -> MusicCategory.GLOBAL_POP
    }

    private fun inferLanguage(tags: List<String>): String = when {
        tags.any { it == "turkish" || it == "türkçe" } -> "tr"
        tags.any { it == "english" } -> "en"
        else -> "und"
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
    }
}
