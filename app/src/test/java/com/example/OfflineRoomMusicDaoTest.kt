package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.LocalSongEntity
import com.example.data.MusicDao
import com.example.data.PlaylistEntity
import com.example.data.PlaylistSongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineRoomMusicDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: MusicDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.musicDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndQueryLocalSongMetadata_forOfflineListening() = runBlocking {
        val song = LocalSongEntity(
            id = "offline_1",
            title = "Şımarık",
            artist = "Tarkan",
            artistId = "artist_tarkan",
            album = "Ölürüm Sana",
            durationMs = 235000L,
            trackNumber = 3,
            discNumber = 1,
            genre = "Türkçe Pop",
            categoryName = "TURKCE_POP",
            releaseYear = 1997,
            language = "tr",
            localAudioPath = "/data/user/0/com.example/files/offline/offline_1.mp3",
            localCoverPath = "/data/user/0/com.example/files/covers/offline_1.jpg",
            remoteAudioUrl = "https://example.com/audio/simarik.mp3",
            remoteCoverUrl = "https://example.com/cover/simarik.jpg",
            mimeType = "audio/mpeg",
            bitrate = "320 kbps",
            fileSizeBytes = 9437184L,
            audioQuality = "Hi-Fi Kayıpsız",
            isAvailableOffline = true,
            lyricsText = "Yakalarsam tık tık...",
            downloadedAt = 1700000000000L
        )

        dao.insertLocalSong(song)

        val retrieved = dao.getLocalSongById("offline_1").first()
        assertNotNull(retrieved)
        assertEquals("Şımarık", retrieved?.title)
        assertEquals("Tarkan", retrieved?.artist)
        assertEquals("/data/user/0/com.example/files/offline/offline_1.mp3", retrieved?.localAudioPath)
        assertEquals(9437184L, retrieved?.fileSizeBytes)
        assertTrue(retrieved?.isAvailableOffline == true)
        assertEquals("320 kbps", retrieved?.bitrate)
    }

    @Test
    fun searchOfflineSongsByTitleArtistAlbum() = runBlocking {
        val songs = listOf(
            LocalSongEntity(
                id = "song_a",
                title = "Gülümse",
                artist = "Sezen Aksu",
                album = "Gülümse Albümü",
                isAvailableOffline = true
            ),
            LocalSongEntity(
                id = "song_b",
                title = "Hadi Bakalım",
                artist = "Sezen Aksu",
                album = "Gülümse Albümü",
                isAvailableOffline = true
            ),
            LocalSongEntity(
                id = "song_c",
                title = "Kuzu Kuzu",
                artist = "Tarkan",
                album = "Karma",
                isAvailableOffline = true
            ),
            LocalSongEntity(
                id = "song_d",
                title = "Online Only",
                artist = "Online Artist",
                album = "Stream",
                isAvailableOffline = false // Not available offline
            )
        )
        dao.insertLocalSongs(songs)

        // Search by artist
        val sezenResults = dao.searchOfflineSongs("Sezen").first()
        assertEquals(2, sezenResults.size)

        // Search by title
        val kuzuResults = dao.searchOfflineSongs("Kuzu").first()
        assertEquals(1, kuzuResults.size)
        assertEquals("Kuzu Kuzu", kuzuResults.first().title)

        // Search by album
        val albumResults = dao.getOfflineSongsByAlbum("Gülümse Albümü").first()
        assertEquals(2, albumResults.size)

        // Total offline count ignores non-offline songs
        val offlineCount = dao.getOfflineSongsCount().first()
        assertEquals(3, offlineCount)
    }

    @Test
    fun updateLocalAudioPathAndOfflineStatus() = runBlocking {
        val song = LocalSongEntity(
            id = "song_mod",
            title = "Dönence",
            artist = "Barış Manço",
            album = "Sözüm Meclisten Dışarı",
            isAvailableOffline = false
        )
        dao.insertLocalSong(song)

        // Update local file path when download completes
        dao.updateLocalAudioPath("song_mod", "/local/storage/music/donence.mp3", 8192000L)

        val updated = dao.getLocalSongById("song_mod").first()
        assertNotNull(updated)
        assertEquals("/local/storage/music/donence.mp3", updated?.localAudioPath)
        assertEquals(8192000L, updated?.fileSizeBytes)
        assertTrue(updated?.isAvailableOffline == true)

        // Toggle availability back to false
        dao.setSongOfflineAvailability("song_mod", false)
        val toggled = dao.getLocalSongById("song_mod").first()
        assertFalse(toggled?.isAvailableOffline == true)
    }

    @Test
    fun playlistMetadataAndOfflineSupport() = runBlocking {
        val playlist = PlaylistEntity(
            id = "pl_offline_test",
            name = "Yol Şarkıları (Çevrimdışı)",
            description = "İnternetsiz yolculuk listesi",
            createdAt = 1000L,
            isOfflineAvailable = true,
            coverUri = "file:///covers/road.jpg",
            totalSongsCount = 2,
            totalDurationMs = 450000L
        )
        dao.insertPlaylist(playlist)

        val retrieved = dao.getPlaylistById("pl_offline_test").first()
        assertNotNull(retrieved)
        assertEquals("Yol Şarkıları (Çevrimdışı)", retrieved?.name)
        assertTrue(retrieved?.isOfflineAvailable == true)

        // Query offline-specific playlists
        val offlinePlaylists = dao.getOfflinePlaylists().first()
        assertEquals(1, offlinePlaylists.size)
        assertEquals("pl_offline_test", offlinePlaylists.first().id)

        // Update playlist offline status
        dao.setPlaylistOfflineStatus("pl_offline_test", false)
        val emptyOfflinePlaylists = dao.getOfflinePlaylists().first()
        assertTrue(emptyOfflinePlaylists.isEmpty())
    }

    @Test
    fun playlistSongJoinQueryAndOrdering() = runBlocking {
        val playlist = PlaylistEntity(
            id = "pl_mix",
            name = "Akustik Akşamlar",
            description = "Sakin akustik parçalar",
            isOfflineAvailable = true
        )
        dao.insertPlaylist(playlist)

        val song1 = LocalSongEntity(id = "s_1", title = "İlk Şarkı", artist = "Sanatçı A", isAvailableOffline = true)
        val song2 = LocalSongEntity(id = "s_2", title = "İkinci Şarkı", artist = "Sanatçı B", isAvailableOffline = true)
        val song3 = LocalSongEntity(id = "s_3", title = "Üçüncü Şarkı", artist = "Sanatçı C", isAvailableOffline = true)
        dao.insertLocalSongs(listOf(song1, song2, song3))

        // Add songs with explicit ordering
        dao.addSongToPlaylist(PlaylistSongEntity(playlistId = "pl_mix", songId = "s_3", orderIndex = 0))
        dao.addSongToPlaylist(PlaylistSongEntity(playlistId = "pl_mix", songId = "s_1", orderIndex = 1))
        dao.addSongToPlaylist(PlaylistSongEntity(playlistId = "pl_mix", songId = "s_2", orderIndex = 2))

        // Query using direct JOIN
        val playlistSongs = dao.getLocalSongsForPlaylist("pl_mix").first()
        assertEquals(3, playlistSongs.size)
        assertEquals("s_3", playlistSongs[0].id)
        assertEquals("s_1", playlistSongs[1].id)
        assertEquals("s_2", playlistSongs[2].id)

        // Remove a song
        dao.removeSongFromPlaylist("pl_mix", "s_1")
        val afterRemoval = dao.getLocalSongsForPlaylist("pl_mix").first()
        assertEquals(2, afterRemoval.size)
        assertEquals(listOf("s_3", "s_2"), afterRemoval.map { it.id })
    }

    @Test
    fun playlistCascadeDeletionRemovesJunctionRows() = runBlocking {
        val playlist = PlaylistEntity(id = "pl_temp", name = "Geçici Liste", description = "")
        dao.insertPlaylist(playlist)

        val song = LocalSongEntity(id = "s_temp", title = "Temp Song", artist = "Temp Artist")
        dao.insertLocalSong(song)

        dao.addSongToPlaylist(PlaylistSongEntity("pl_temp", "s_temp", 0))

        val songsBefore = dao.getSongsForPlaylist("pl_temp").first()
        assertEquals(1, songsBefore.size)

        // Delete playlist
        dao.deletePlaylist("pl_temp")

        val playlistAfter = dao.getPlaylistById("pl_temp").first()
        assertNull(playlistAfter)

        // ForeignKey CASCADE should automatically purge junction rows
        val songsAfter = dao.getSongsForPlaylist("pl_temp").first()
        assertTrue(songsAfter.isEmpty())
    }

    @Test
    fun totalOfflineStorageCalculation() = runBlocking {
        val song1 = LocalSongEntity(id = "sz_1", title = "S1", artist = "A1", fileSizeBytes = 5_000_000L, isAvailableOffline = true)
        val song2 = LocalSongEntity(id = "sz_2", title = "S2", artist = "A2", fileSizeBytes = 7_500_000L, isAvailableOffline = true)
        val song3 = LocalSongEntity(id = "sz_3", title = "S3", artist = "A3", fileSizeBytes = 10_000_000L, isAvailableOffline = false)

        dao.insertLocalSongs(listOf(song1, song2, song3))

        val totalBytes = dao.getTotalOfflineStorageBytes().first()
        // Only song1 + song2 = 12_500_000L because song3 is not available offline
        assertEquals(12_500_000L, totalBytes)
    }
}
