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

class JamendoCatalogProvider(
    private val clientId: String,
    private val commercialLicenseConfirmed: Boolean = false,
    private val httpClient: OkHttpClient = OkHttpClient()
) : CatalogProvider {
    companion object {
        private const val TRACKS_URL = "https://api.jamendo.com/v3.0/tracks/"
        private const val ARTISTS_URL = "https://api.jamendo.com/v3.0/artists/"
        private const val DEFAULT_LIMIT = 40
    }
    private suspend fun requestJson(baseUrl: String, params: Map<String, String>): Result<JSONObject> = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) return@withContext Result.failure(IllegalStateException("JAMENDO_CLIENT_ID_NOT_CONFIGURED"))
        runCatching {
            val encoded = params.entries.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
            val separator = if (baseUrl.contains('?')) '&' else '?'
            val url = "$baseUrl${separator}client_id=${encode(clientId)}&format=json&limit=$DEFAULT_LIMIT&$encoded"
            httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) error("JAMENDO_HTTP_${response.code}")
                val json = JSONObject(response.body?.string().orEmpty())
                val headers = json.optJSONObject("headers")
                if (headers?.optInt("code", -1) != 0) error(headers?.optString("error_message").orEmpty().ifBlank { "JAMENDO_API_ERROR" })
                json
            }
        }
    }
    private suspend fun requestTracks(params: Map<String, String>): Result<JSONArray> = requestJson(TRACKS_URL, params).map { it.optJSONArray("results") ?: JSONArray() }
    private suspend fun requestArtists(params: Map<String, String>): Result<JSONArray> = requestJson(ARTISTS_URL, params).map { it.optJSONArray("results") ?: JSONArray() }
    override suspend fun searchSongs(query: String): Result<List<Song>> = requestTracks(mapOf("search" to query, "type" to "single albumtrack", "audioformat" to "mp32", "imagesize" to "300", "include" to "musicinfo")).map { JamendoSongMapper.mapSongs(it, commercialLicenseConfirmed) }
    override suspend fun searchArtists(query: String): Result<List<Artist>> = requestArtists(mapOf("namesearch" to query, "hasimage" to "true", "imagesize" to "300")).map { JamendoArtistMapper.mapArtists(it) }
    override suspend fun getSong(id: String): Result<Song?> = requestTracks(mapOf("id" to id.removePrefix("jamendo:"), "audioformat" to "mp32", "imagesize" to "300", "include" to "musicinfo")).map { JamendoSongMapper.mapSongs(it, commercialLicenseConfirmed).firstOrNull() }
    override suspend fun getArtist(id: String): Result<Artist?> = requestArtists(mapOf("id" to id.removePrefix("jamendo:"), "hasimage" to "true", "imagesize" to "300")).map { JamendoArtistMapper.mapArtists(it).firstOrNull() }
    override suspend fun getAlbum(id: String): Result<Album?> = requestTracks(mapOf("album_id" to id.removePrefix("jamendo:"), "type" to "single albumtrack", "audioformat" to "mp32", "imagesize" to "300", "include" to "musicinfo")).map { results ->
        val songs = JamendoSongMapper.mapSongs(results, commercialLicenseConfirmed)
        songs.firstOrNull()?.let { first -> Album(id = id, title = first.album, artist = first.artist, artistId = first.artistId, coverUrl = first.coverUrl, releaseYear = first.releaseYear, genre = first.category.titleTr, songs = songs) }
    }
    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> = requestTracks(mapOf("order" to "releasedate_desc", "type" to "single albumtrack", "audioformat" to "mp32", "imagesize" to "300", "include" to "musicinfo")).map { results -> JamendoSongMapper.mapSongs(results, commercialLicenseConfirmed).filter { song -> since == null || song.releaseYear >= since.atZone(ZoneOffset.UTC).year } }
    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}

internal object JamendoArtistMapper {
    fun mapArtists(results: JSONArray): List<Artist> = buildList {
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val id = item.optString("id").trim(); val name = item.optString("name").trim()
            if (id.isBlank() || name.isBlank()) continue
            add(Artist(id = "jamendo:$id", name = name, genre = "", bio = "", imageUrl = item.optString("image").trim(), monthlyListeners = ""))
        }
    }
}

internal object JamendoSongMapper {
    fun mapSongs(results: JSONArray, commercialLicenseConfirmed: Boolean = false): List<Song> = buildList {
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val audioUrl = item.optString("audio").trim(); val title = item.optString("name").trim(); val artist = item.optString("artist_name").trim()
            if (audioUrl.isBlank() || title.isBlank() || artist.isBlank()) continue
            val musicInfo = item.optJSONObject("musicinfo"); val tagsObject = musicInfo?.optJSONObject("tags")
            val genres = tagsObject?.optJSONArray("genres").strings(); val variantTags = tagsObject?.optJSONArray("vartags").strings(); val allTags = (genres + variantTags).map(String::lowercase).toSet()
            val language = musicInfo?.optString("lang").orEmpty().lowercase().ifBlank { inferLanguage(allTags, item) }
            val category = inferCategory(allTags, language); val releaseYear = item.optString("releasedate").take(4).toIntOrNull() ?: 0; val id = item.optString("id")
            if (id.isBlank()) continue
            val license = item.optString("license_ccurl").trim()
            val sourceType = if (
                commercialLicenseConfirmed &&
                CatalogLicensePolicy.isPermittedRemoteLicense(license)
            ) SongSourceType.VERIFIED_REMOTE else SongSourceType.UNKNOWN
            add(Song(id = "jamendo:$id", title = title, artist = artist, artistId = "jamendo:${item.optString("artist_id")}", album = item.optString("album_name").ifBlank { "Single" },
                durationMs = item.optLong("duration", 0L) * 1000L, category = category, language = language.ifBlank { "und" }, coverUrl = item.optString("album_image").ifBlank { item.optString("image") }, audioUrl = audioUrl,
                releaseYear = releaseYear, isNewRelease = releaseYear >= LocalDate.now(ZoneOffset.UTC).year, playCount = item.optJSONObject("stats")?.optLong("listens_month", 0L) ?: 0L,
                sourceType = sourceType, license = license))
        }
    }
    private fun inferLanguage(tags: Set<String>, item: JSONObject): String { val text = "${item.optString("name")} ${item.optString("album_name")} ${item.optString("artist_name")}".lowercase(); return when { "turkish" in tags || "türkçe" in tags || text.contains("turkish") -> "tr"; "english" in tags || "english" in text -> "en"; else -> "und" } }
    private fun inferCategory(tags: Set<String>, language: String): MusicCategory = when {
        tags.any { it == "hiphop" || it == "hip-hop" || it == "rap" } -> if (language == "tr") MusicCategory.TURKCE_RAP else MusicCategory.HIP_HOP
        tags.any { it == "rock" || it.contains("rock'n'roll") } -> if (language == "tr") MusicCategory.TURKCE_ROCK else MusicCategory.ROCK_CLASSICS
        tags.any { it == "electronic" || it == "edm" || it == "dance" } -> MusicCategory.EDM_DANCE
        tags.any { it == "classical" || it == "classical music" } -> if (language == "tr") MusicCategory.TURK_SANAT else MusicCategory.CHILL_LOFI
        tags.any { it == "acoustic" || it == "chillout" || it == "relaxation" } -> MusicCategory.CHILL_LOFI
        "pop" in tags -> if (language == "tr") MusicCategory.TURKCE_POP else MusicCategory.GLOBAL_POP
        else -> if (language == "tr") MusicCategory.TURKCE_POP else MusicCategory.GLOBAL_POP
    }
    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
}
