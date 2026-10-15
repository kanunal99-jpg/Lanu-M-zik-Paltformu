package com.example.model

import java.util.Locale

/**
 * Conservative genre classification. Provider genre/tags are stronger evidence
 * than artist-name heuristics. Unknown genre is NEVER silently labelled Global Pop.
 *
 * Kept at presentation level for legacy cached songs: no destructive database
 * migration, and no fabricated claim of an exact genre.
 */
object MusicGenreResolver {
    private fun normal(value: String): String = value.trim().lowercase(Locale.ROOT)
        .replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
        .replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')

    private val turkishRap = setOf("ceza", "sagopa kajmer", "ezhel", "gazapizm",
        "sehin sah", "sehinsah", "saniser", "fuat", "killa hakan", "ben fero",
        "contra", "hidra", "allame", "patron", "norm ender", "joker", "motive",
        "khontkar", "maestro", "tepki", "sokrat st", "massaka", "mode xl")
    private val turkishPop = setOf("tarkan", "sezen aksu", "simge", "mert demir",
        "ajda pekkan", "sertab erener", "kenan dogulu", "gulsen", "hadise",
        "sila", "yildiz tilbe", "mustafa sandal", "demet akalin")
    private val turkishRock = setOf("duman", "mor ve otesi", "sebnem ferah",
        "teoman", "gripin", "manga", "athena", "yuksek sadakat")
    private val anatolianRock = setOf("cem karaca", "baris manco", "erkin koray",
        "mogollar", "uc hurel", "3 hurel")
    private val turkishArt = setOf("zeki muren", "bulent ersoy", "muazzez ersoy",
        "muzeyyen senar", "safiye ayla")
    private val internationalPop = setOf("britney spears", "madonna", "lady gaga",
        "the weeknd", "dua lipa", "taylor swift", "katy perry", "rihanna")

    private fun curatedArtist(artist: String): MusicCategory? {
        val key = normal(artist).substringBefore(" feat").substringBefore(" ft.")
            .substringBefore(" & ").trim()
        return when(key) {
            in turkishRap -> MusicCategory.TURKCE_RAP
            in turkishPop -> MusicCategory.TURKCE_POP
            in turkishRock -> MusicCategory.TURKCE_ROCK
            in anatolianRock -> MusicCategory.ANADOLU_ROCK
            in turkishArt -> MusicCategory.TURK_SANAT
            in internationalPop -> MusicCategory.GLOBAL_POP
            else -> null
        }
    }

    fun fromMetadata(artist: String, genre: String, tags: List<String>, language: String): MusicCategory {
        val curated = curatedArtist(artist)
        val normalizedGenre = normal(genre + " " + tags.joinToString(" "))
        val turkish = language == "tr" || normalizedGenre.contains("turkish") ||
            normalizedGenre.contains("turkce") || curated in setOf(MusicCategory.TURKCE_POP,
                MusicCategory.TURKCE_RAP, MusicCategory.TURKCE_ROCK,
                MusicCategory.ANADOLU_ROCK, MusicCategory.TURK_SANAT)
        return when {
            normalizedGenre.contains("anatolian rock") || normalizedGenre.contains("anadolu rock") -> MusicCategory.ANADOLU_ROCK
            normalizedGenre.contains("sanat muzigi") -> MusicCategory.TURK_SANAT
            normalizedGenre.contains("rap") || normalizedGenre.contains("hiphop") ||
                normalizedGenre.contains("hip-hop") -> if (turkish) MusicCategory.TURKCE_RAP else MusicCategory.HIP_HOP
            normalizedGenre.contains("rock") || normalizedGenre.contains("metal") -> if (turkish) MusicCategory.TURKCE_ROCK else MusicCategory.ROCK_CLASSICS
            normalizedGenre.contains("r&b") || normalizedGenre.contains("rnb") ||
                normalizedGenre.contains("soul") -> MusicCategory.HIP_HOP
            normalizedGenre.contains("electronic") || normalizedGenre.contains("dance") ||
                normalizedGenre.contains("techno") || normalizedGenre.contains("house") -> MusicCategory.EDM_DANCE
            normalizedGenre.contains("lofi") || normalizedGenre.contains("chill") ||
                normalizedGenre.contains("acoustic") -> MusicCategory.CHILL_LOFI
            normalizedGenre.contains("pop") -> if (turkish) MusicCategory.TURKCE_POP else MusicCategory.GLOBAL_POP
            curated != null -> curated
            else -> MusicCategory.OTHER
        }
    }

    fun resolve(song: Song): Song {
        if (song.category != MusicCategory.GLOBAL_POP && song.category != MusicCategory.OTHER) return song
        // Imported old cache tagged virtually every Deezer/unknown song as Global Pop.
        val corrected = curatedArtist(song.artist)
            ?: if (song.category == MusicCategory.GLOBAL_POP &&
                song.sourceType == SongSourceType.VERIFIED_REMOTE && song.language == "en")
                MusicCategory.GLOBAL_POP else MusicCategory.OTHER
        return if (song.category == corrected) song else song.copy(category = corrected)
    }
}
