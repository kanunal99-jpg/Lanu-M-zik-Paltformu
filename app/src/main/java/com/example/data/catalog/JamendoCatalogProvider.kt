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
 * Jamendo adapter for verified, licensed/CC-compatible streaming music.
 * The provider is isolated behind CatalogProvider so another licensed source
 * can be added without coupling the UI/player to a vendor.
 */
class JamendoCatalogProvider(
    private val clientId: String,
    private val httpClient: OkHttpClient = OkHttpClient()
) : CatalogProvider {

    companion object {
        private const val BASE_URL = "https://api.jamendo.com/v3.0/tracks/"
        private const val DEFAULT_LIMIT = 40
    }

    private suspend fun request(params: Map<String, String>): Result<JSONArray> = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("JAMENDO_CLIENT_ID_NOT_CONFIGURED"))
        }
        runCatching {
            val encoded = params.entries.joinToString("&") { (key, value) ->
                "${encode(key)}=${encode(value)}"
            }
            val url = "$BASE_URL?client_id=${encode(clientId)}&format=json&audioformat=mp32&imagesize=300&include=musicinfo&limit=$DEFAULT_LIMIT&$encoded"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("JAMENDO_HTTP_${response.code}")
                val json = JSONObject(response.body?.string().orEmpty())
                val headers = json.optJSONObject("headers")
                if (headers?.optInt("code", -1) != 0) {
                    error(headers?.optString("error_message").orEmpty().ifBlank { "JAMENDO_API_ERROR" })
                }
                json.optJSONArray("results") ?: JSONArray()
            }
        }
    }

    override suspend fun searchSongs(query: String): Result<List<Song>> =
        request(mapOf("search" to query, "type" to "single albumtrack"))
            .map { JamendoSongMapper.mapSongs(it) }

    override suspend fun searchArtists(query: String): Result<List<Artist>> =
        request(mapOf("search" to query, "type" to "single albumtrack", "groupby" to "artist_id"))
            .map { JamendoSongMapper.mapSongs(it).map(::toArtist) }

    override suspend fun getSong(id: String): Result<Song?> =
        request(mapOf("id" to id.removePrefix("jamendo:")))
            .map { JamendoSongMapper.mapSongs(it).firstOrNull() }

    override suspend fun getArtist(id: String): Result<Artist?> =
        request(mapOf("artist_id" to id.removePrefix("jamendo:"), "groupby" to "artist_id"))
            .map { JamendoSongMapper.mapSongs(it).firstOrNull()?.let(::toArtist) }

    override suspend fun getAlbum(id: String): Result<Album?> =
        request(mapOf("album_id" to id.removePrefix("jamendo:"), "type" to "single albumtrack"))
            .map { results ->
                val songs = JamendoSongMapper.mapSongs(results)
                songs.firstOrNull()?.let { first ->
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
                }
            }

    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> =
        request(mapOf("order" to "releasedate_desc", "type" to "single albumtrack"))
            .map { results ->
                JamendoSongMapper.mapSongs(results).filter { song ->
                    since == null || song.releaseYear >= since.atZone(ZoneOffset.UTC).year
                }
            }

    private fun toArtist(song: Song): Artist = Artist(
        id = song.artistId,
        name = song.artist,
        genre = song.category.titleTr,
        bio = "",
        imageUrl = song.coverUrl,
        monthlyListeners = ""
    )

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}

internal object JamendoSongMapper {
    fun mapSongs(results: JSONArray): List<Song> = buildList {
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val audioUrl = item.optString("audio").trim()
            val title = item.optString("name").trim()
            val artist = item.optString("artist_name").trim()
            if (audioUrl.isBlank() || title.isBlank() || artist.isBlank()) continue

            val musicInfo = item.optJSONObject("musicinfo")
            val tagsObject = musicInfo?.optJSONObject("tags")
            val genres = tagsObject?.optJSONArray("genres").strings()
            val variantTags = tagsObject?.optJSONArray("vartags").strings()
            val allTags = (genres + variantTags).map(String::lowercase).toSet()
            val language = musicInfo?.optString("lang").orEmpty().lowercase().ifBlank { inferLanguage(allTags, item) }
            val category = inferCategory(allTags, language)
            val releaseYear = item.optString("releasedate").take(4).toIntOrNull() ?: 0
            val id = item.optString("id")
            if (id.isBlank()) continue

            add(
                Song(
                    id = "jamendo:$id",
                    title = title,
                    artist = artist,
                    artistId = "jamendo:${item.optString("artist_id")}",
                    album = item.optString("album_name").ifBlank { "Single" },
                    durationMs = item.optLong("duration", 0L) * 1000L,
                    category = category,
                    language = language.ifBlank { "und" },
                    coverUrl = item.optString("album_image").ifBlank { item.optString("image") },
                    audioUrl = audioUrl,
                    releaseYear = releaseYear,
                    isNewRelease = releaseYear >= LocalDate.now(ZoneOffset.UTC).year,
                    playCount = item.optJSONObject("stats")?.optLong("listens_month", 0L) ?: 0L
                )
            )
        }
    }

    private fun inferLanguage(tags: Set<String>, item: JSONObject): String {
        val text = "${item.optString("name")} ${item.optString("album_name")} ${item.optString("artist_name")}".lowercase()
        return when {
            "turkish" in tags || "türkçe" in tags || text.contains("turkish") -> "tr"
            "english" in tags || "english" in text -> "en"
            else -> "und"
        }
    }

    private fun inferCategory(tags: Set<String>, language: String): MusicCategory = when {
        tags.any { it == "hiphop" || it == "hip-hop" || it == "rap" } -> if (language == "tr") MusicCategory.TURKCE_RAP else MusicCategory.HIP_HOP
        tags.any { it == "rock" || it.contains("rock'n'roll") } -> if (language == "tr") MusicCategory.TURKCE_ROCK else MusicCategory.ROCK_CLASSICS
        tags.any { it == "electronic" || it == "edm" || it == "dance" } -> MusicCategory.EDM_DANCE
        tags.any { it == "classical" || it == "classical music" } -> if (language == "tr") MusicCategory.TURK_SANAT else MusicCategory.CHILL_LOFI
        tags.any { it == "acoustic" || it == "chillout" || it == "relaxation" } -> MusicCategory.CHILL_LOFI
        "pop" in tags -> if (language == "tr") MusicCategory.TURKCE_POP else MusicCategory.GLOBAL_POP
        else -> if (language == "tr") MusicCategory.TURKCE_POP else MusicCategory.GLOBAL_POP
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
    }
}
