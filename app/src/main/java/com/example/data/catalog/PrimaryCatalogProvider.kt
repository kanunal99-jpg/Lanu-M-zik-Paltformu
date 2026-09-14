package com.example.data.catalog

import com.example.model.Artist
import com.example.model.Song
import com.example.model.Album
import com.example.model.MusicCategory
import com.example.model.FriendActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class PrimaryCatalogProvider : CatalogProvider {

    // Seed data corresponding to the authentic artists and albums
    private val artistsList = listOf(
        Artist(
            id = "tarkan",
            name = "Tarkan",
            genre = "Türkçe Pop",
            bio = "Megastar Tarkan, modern Türk pop müziğinin küresel simgesi ve gelmiş geçmiş en çok dinlenen sanatçılarından biridir.",
            imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "sezen_aksu",
            name = "Sezen Aksu",
            genre = "Türkçe Pop / Klasik",
            bio = "Minik Serçe, yüzlerce unutulmaz bestesi ve derin sözleriyle Türk müziğinin kalbinde yer edinmiş efsanedir.",
            imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "ceza",
            name = "Ceza",
            genre = "Türkçe Rap & Hip-Hop",
            bio = "Türkçe rap müziğin öncüsü ve hız rekorları kıran flow tekniğiyle efsaneleşen MC.",
            imageUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "ezhel",
            name = "Ezhel",
            genre = "Trap & Reggae",
            bio = "Ankara'dan çıkıp dünyada yankı uyandıran, trap ve Anadolu melodilerini harmanlayan yenilikçi sanatçı.",
            imageUrl = "https://images.unsplash.com/photo-1520523839898-507125cd53c1?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "duman",
            name = "Duman",
            genre = "Türkçe Rock / Grunge",
            bio = "Kaan Tangöze önderliğindeki Duman, Türk rock müziğinin en köklü ve tutkulu gruplarından biridir.",
            imageUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "manga",
            name = "maNga",
            genre = "Nu-Metal & Rock",
            bio = "Eurovision ikincisi, Türk motiflerini nu-metal ve elektronik öğelerle birleştiren ikonik grup.",
            imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "baris_manco",
            name = "Barış Manço",
            genre = "Anadolu Rock",
            bio = "Kültürel elçimiz, 7'den 77'ye herkesin sevgilisi ve Anadolu Rock akımının en büyük mimarı.",
            imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "cem_karaca",
            name = "Cem Karaca",
            genre = "Anadolu Rock",
            bio = "Türk rock müziğinin efsanevi sesi, toplumsal mesajları ve benzersiz yorumuyla unutulmaz bir ikon.",
            imageUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "mor_ve_otesi",
            name = "Mor ve Ötesi",
            genre = "Alternatif Rock",
            bio = "Felsefi sözleri ve güçlü sahne performansıyla çeyrek asırdır Türk rock sahnesine yön veren grup.",
            imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "the_weeknd",
            name = "The Weeknd",
            genre = "R&B / Synthwave",
            bio = "Grammy ödüllü, 'Blinding Lights' ve 'Starboy' ile küresel müzik listelerini altüst eden megastar.",
            imageUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "dua_lipa",
            name = "Dua Lipa",
            genre = "Disco & Pop",
            bio = "Modern disco popun kraliçesi, Future Nostalgia albümüyle dünya çapında milyarlarca dinlenme kazandı.",
            imageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "coldplay",
            name = "Coldplay",
            genre = "Alternatif / Arena Rock",
            bio = "Chris Martin ve grubunun stadyumları dolduran renkli ve dokunaklı melodileri.",
            imageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "daft_punk",
            name = "Daft Punk",
            genre = "Elektronik / French House",
            bio = "Kaskları ve zamansız synthesizer melodileriyle elektronik müziği yeniden tanımlayan ikili.",
            imageUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        ),
        Artist(
            id = "zekimuren",
            name = "Zeki Müren",
            genre = "Türk Sanat Müziği",
            bio = "Sanat Güneşi, eşsiz sesi, zarafeti ve kusursuz Türkçesiyle sanat müziğinin zirvesidir.",
            imageUrl = "https://images.unsplash.com/photo-1530669922240-272e61df3f70?w=600&auto=format&fit=crop&q=80",
            monthlyListeners = "N/A"
        )
    )

    private val songsList = listOf(
        Song(
            id = "tarkan_1",
            title = "Kış Güneşi",
            artist = "Tarkan",
            artistId = "tarkan",
            album = "Aacayipsin",
            durationMs = 238000L,
            category = MusicCategory.TURKCE_POP,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
            audioUrl = "", // Empty indicates playback unavailable (not licensed)
            releaseYear = 1994,
            playCount = 1000000L,
            lyrics = emptyList()
        ),
        Song(
            id = "tarkan_2",
            title = "Şımarık",
            artist = "Tarkan",
            artistId = "tarkan",
            album = "Ölürüm Sana",
            durationMs = 215000L,
            category = MusicCategory.TURKCE_POP,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 1997,
            playCount = 1450000L,
            lyrics = emptyList()
        ),
        Song(
            id = "tarkan_3",
            title = "Dudu",
            artist = "Tarkan",
            artistId = "tarkan",
            album = "Dudu",
            durationMs = 250000L,
            category = MusicCategory.TURKCE_POP,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2003,
            playCount = 1900000L,
            lyrics = emptyList()
        ),
        Song(
            id = "sezen_1",
            title = "Firuze",
            artist = "Sezen Aksu",
            artistId = "sezen_aksu",
            album = "Firuze",
            durationMs = 305000L,
            category = MusicCategory.TURKCE_POP,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 1982,
            playCount = 2350000L,
            lyrics = emptyList()
        ),
        Song(
            id = "sezen_2",
            title = "Gülümse",
            artist = "Sezen Aksu",
            artistId = "sezen_aksu",
            album = "Gülümse",
            durationMs = 312000L,
            category = MusicCategory.TURKCE_POP,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 1991,
            playCount = 2800000L,
            lyrics = emptyList()
        ),
        Song(
            id = "sezen_3",
            title = "Vazgeçtim",
            artist = "Sezen Aksu",
            artistId = "sezen_aksu",
            album = "Gülümse",
            durationMs = 298000L,
            category = MusicCategory.TURKCE_POP,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 1991,
            playCount = 3250000L,
            lyrics = emptyList()
        ),
        Song(
            id = "ceza_1",
            title = "Holocaust",
            artist = "Ceza",
            artistId = "ceza",
            album = "Rapstar",
            durationMs = 208000L,
            category = MusicCategory.TURKCE_RAP,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2004,
            playCount = 3700000L,
            lyrics = emptyList()
        ),
        Song(
            id = "ceza_2",
            title = "Yerli Plaka",
            artist = "Ceza",
            artistId = "ceza",
            album = "Yerli Plaka",
            durationMs = 255000L,
            category = MusicCategory.TURKCE_RAP,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2006,
            playCount = 4150000L,
            lyrics = emptyList()
        ),
        Song(
            id = "ezhel_1",
            title = "Geceler",
            artist = "Ezhel",
            artistId = "ezhel",
            album = "Müptezhel",
            durationMs = 235000L,
            category = MusicCategory.TURKCE_RAP,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1520523839898-507125cd53c1?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2017,
            playCount = 4600000L,
            lyrics = emptyList()
        ),
        Song(
            id = "ezhel_2",
            title = "Felaket",
            artist = "Ezhel",
            artistId = "ezhel",
            album = "Single",
            durationMs = 190000L,
            category = MusicCategory.TURKCE_RAP,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1520523839898-507125cd53c1?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2019,
            playCount = 5050000L,
            lyrics = emptyList()
        ),
        Song(
            id = "duman_1",
            title = "Seni Kendime Sakladım",
            artist = "Duman",
            artistId = "duman",
            album = "Seni Kendime Sakladım",
            durationMs = 240000L,
            category = MusicCategory.TURKCE_ROCK,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2005,
            playCount = 5500000L,
            lyrics = emptyList()
        ),
        Song(
            id = "duman_2",
            title = "Aman Aman",
            artist = "Duman",
            artistId = "duman",
            album = "Seni Kendime Sakladım",
            durationMs = 252000L,
            category = MusicCategory.TURKCE_ROCK,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2005,
            playCount = 5950000L,
            lyrics = emptyList()
        ),
        Song(
            id = "manga_1",
            title = "Bir Kadın Çizeceksin",
            artist = "maNga",
            artistId = "manga",
            album = "maNga",
            durationMs = 238000L,
            category = MusicCategory.TURKCE_ROCK,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2004,
            playCount = 6400000L,
            lyrics = emptyList()
        ),
        Song(
            id = "manga_2",
            title = "Cevapsız Sorular",
            artist = "maNga",
            artistId = "manga",
            album = "Şehr-i Hüzün",
            durationMs = 270000L,
            category = MusicCategory.TURKCE_ROCK,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2009,
            playCount = 6850000L,
            lyrics = emptyList()
        ),
        Song(
            id = "baris_1",
            title = "Dönence",
            artist = "Barış Manço",
            artistId = "baris_manco",
            album = "Sözüm Meclisten Dışarı",
            durationMs = 372000L,
            category = MusicCategory.ANADOLU_ROCK,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 1981,
            playCount = 7300000L,
            lyrics = emptyList()
        ),
        Song(
            id = "baris_2",
            title = "Gülpembe",
            artist = "Barış Manço",
            artistId = "baris_manco",
            album = "Sözüm Meclisten Dışarı",
            durationMs = 305000L,
            category = MusicCategory.ANADOLU_ROCK,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 1981,
            playCount = 7750000L,
            lyrics = emptyList()
        ),
        Song(
            id = "cem_1",
            title = "Tamirci Çırağı",
            artist = "Cem Karaca",
            artistId = "cem_karaca",
            album = "Tamirci Çırağı",
            durationMs = 298000L,
            category = MusicCategory.ANADOLU_ROCK,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 1975,
            playCount = 8200000L,
            lyrics = emptyList()
        ),
        Song(
            id = "cem_2",
            title = "Islak Islak",
            artist = "Cem Karaca",
            artistId = "cem_karaca",
            album = "Islak Islak",
            durationMs = 270000L,
            category = MusicCategory.ANADOLU_ROCK,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 1992,
            playCount = 8650000L,
            lyrics = emptyList()
        ),
        Song(
            id = "zekimuren_1",
            title = "Gitme Sana Muhtacım",
            artist = "Zeki Müren",
            artistId = "zekimuren",
            album = "Sorma",
            durationMs = 260000L,
            category = MusicCategory.TURK_SANAT,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1530669922240-272e61df3f70?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 1982,
            playCount = 9100000L,
            lyrics = emptyList()
        ),
        Song(
            id = "zekimuren_2",
            title = "Şimdi Uzaklardasın",
            artist = "Zeki Müren",
            artistId = "zekimuren",
            album = "Klasikler",
            durationMs = 240000L,
            category = MusicCategory.TURK_SANAT,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1530669922240-272e61df3f70?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 1970,
            playCount = 9550000L,
            lyrics = emptyList()
        ),
        Song(
            id = "weeknd_1",
            title = "Blinding Lights",
            artist = "The Weeknd",
            artistId = "the_weeknd",
            album = "After Hours",
            durationMs = 200000L,
            category = MusicCategory.GLOBAL_POP,
            language = "en",
            coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2019,
            playCount = 10000000L,
            lyrics = emptyList()
        ),
        Song(
            id = "weeknd_2",
            title = "Starboy",
            artist = "The Weeknd",
            artistId = "the_weeknd",
            album = "Starboy",
            durationMs = 230000L,
            category = MusicCategory.HIP_HOP,
            language = "en",
            coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2016,
            playCount = 10450000L,
            lyrics = emptyList()
        ),
        Song(
            id = "dua_1",
            title = "Don't Start Now",
            artist = "Dua Lipa",
            artistId = "dua_lipa",
            album = "Future Nostalgia",
            durationMs = 183000L,
            category = MusicCategory.GLOBAL_POP,
            language = "en",
            coverUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2019,
            playCount = 10900000L,
            lyrics = emptyList()
        ),
        Song(
            id = "dua_2",
            title = "Levitating",
            artist = "Dua Lipa",
            artistId = "dua_lipa",
            album = "Future Nostalgia",
            durationMs = 203000L,
            category = MusicCategory.GLOBAL_POP,
            language = "en",
            coverUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2020,
            playCount = 11350000L,
            lyrics = emptyList()
        ),
        Song(
            id = "coldplay_1",
            title = "Yellow",
            artist = "Coldplay",
            artistId = "coldplay",
            album = "Parachutes",
            durationMs = 266000L,
            category = MusicCategory.ROCK_CLASSICS,
            language = "en",
            coverUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2000,
            playCount = 11800000L,
            lyrics = emptyList()
        ),
        Song(
            id = "coldplay_2",
            title = "Viva La Vida",
            artist = "Coldplay",
            artistId = "coldplay",
            album = "Viva La Vida",
            durationMs = 242000L,
            category = MusicCategory.ROCK_CLASSICS,
            language = "en",
            coverUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2008,
            playCount = 12250000L,
            lyrics = emptyList()
        ),
        Song(
            id = "daft_1",
            title = "Get Lucky",
            artist = "Daft Punk",
            artistId = "daft_punk",
            album = "Random Access Memories",
            durationMs = 248000L,
            category = MusicCategory.EDM_DANCE,
            language = "en",
            coverUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2013,
            playCount = 12700000L,
            lyrics = emptyList()
        ),
        Song(
            id = "daft_2",
            title = "Harder, Better, Faster, Stronger",
            artist = "Daft Punk",
            artistId = "daft_punk",
            album = "Discovery",
            durationMs = 224000L,
            category = MusicCategory.EDM_DANCE,
            language = "en",
            coverUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2001,
            playCount = 13150000L,
            lyrics = emptyList()
        ),
        Song(
            id = "mor_ve_otesi_1",
            title = "Cambaz",
            artist = "Mor ve Ötesi",
            artistId = "mor_ve_otesi",
            album = "Dünya Yalan Söylüyor",
            durationMs = 245000L,
            category = MusicCategory.TURKCE_ROCK,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2004,
            playCount = 13600000L,
            lyrics = emptyList()
        ),
        Song(
            id = "mor_ve_otesi_2",
            title = "Bir Derdim Var",
            artist = "Mor ve Ötesi",
            artistId = "mor_ve_otesi",
            album = "Dünya Yalan Söylüyor",
            durationMs = 290000L,
            category = MusicCategory.TURKCE_ROCK,
            language = "tr",
            coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
            audioUrl = "",
            releaseYear = 2004,
            playCount = 14050000L,
            lyrics = emptyList()
        )
    )

    override suspend fun searchSongs(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        val filtered = songsList.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
        Result.success(filtered)
    }

    override suspend fun searchArtists(query: String): Result<List<Artist>> = withContext(Dispatchers.IO) {
        val filtered = artistsList.filter {
            it.name.contains(query, ignoreCase = true)
        }
        Result.success(filtered)
    }

    override suspend fun getSong(id: String): Result<Song?> = withContext(Dispatchers.IO) {
        Result.success(songsList.find { it.id == id })
    }

    override suspend fun getArtist(id: String): Result<Artist?> = withContext(Dispatchers.IO) {
        Result.success(artistsList.find { it.id == id })
    }

    override suspend fun getAlbum(id: String): Result<Album?> = withContext(Dispatchers.IO) {
        val albumSongs = songsList.filter { it.album.equals(id, ignoreCase = true) || it.id.startsWith(id) }
        if (albumSongs.isEmpty()) {
            return@withContext Result.success(null)
        }
        val first = albumSongs.first()
        val album = Album(
            id = id,
            title = first.album,
            artist = first.artist,
            artistId = first.artistId,
            coverUrl = first.coverUrl,
            releaseYear = first.releaseYear,
            genre = first.category.titleTr,
            songs = albumSongs
        )
        Result.success(album)
    }

    override suspend fun getLatestReleases(since: Instant?): Result<List<Song>> = withContext(Dispatchers.IO) {
        // Return latest releases in our catalog
        val latest = songsList.filter { it.releaseYear >= 2020 }
        Result.success(latest)
    }

    fun getStaticSongs(): List<Song> = songsList
    fun getStaticArtists(): List<Artist> = artistsList

    fun getInitialFriendActivities(): List<FriendActivity> {
        val weekndSong = songsList.find { it.id == "weeknd_1" } ?: songsList[0]
        val dumanSong = songsList.find { it.id == "duman_1" } ?: songsList[1]
        val tarkanSong = songsList.find { it.id == "tarkan_1" } ?: songsList[2]
        val cezaSong = songsList.find { it.id == "ceza_1" } ?: songsList[3]
        val duaSong = songsList.find { it.id == "dua_1" } ?: songsList[4]

        return listOf(
            FriendActivity(
                id = "act_1",
                friendName = "Deniz Yılmaz",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80",
                song = weekndSong,
                statusText = "Şu an dinliyor",
                isCurrentlyPlaying = true,
                mutualNote = "🔥 'Bu şarkının nakaratındaki baslar muazzam!'",
                recommendationLikes = 14
            ),
            FriendActivity(
                id = "act_2",
                friendName = "Emre Kaya",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
                song = dumanSong,
                statusText = "8 dk önce dinledi",
                isCurrentlyPlaying = false,
                mutualNote = "🎸 Rock listene mutlaka ekle!",
                recommendationLikes = 9
            ),
            FriendActivity(
                id = "act_3",
                friendName = "Zeynep Demir",
                avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200&auto=format&fit=crop&q=80",
                song = tarkanSong,
                statusText = "Şu an dinliyor",
                isCurrentlyPlaying = true,
                mutualNote = "✨ 90'lar nostaljisi paha biçilemez...",
                recommendationLikes = 27
            ),
            FriendActivity(
                id = "act_4",
                friendName = "Caner Özkan",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
                song = cezaSong,
                statusText = "25 dk önce dinledi",
                isCurrentlyPlaying = false,
                mutualNote = "🎤 Efsane flow",
                recommendationLikes = 18
            ),
            FriendActivity(
                id = "act_5",
                friendName = "Selin Aydın",
                avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&auto=format&fit=crop&q=80",
                song = duaSong,
                statusText = "Şu an dinliyor",
                isCurrentlyPlaying = true,
                mutualNote = "💃 Enerji deposu!",
                recommendationLikes = 31
            )
        )
    }
}
