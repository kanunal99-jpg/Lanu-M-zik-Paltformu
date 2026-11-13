package com.example

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.auth.LocalAuthBackend
import com.example.data.AppDatabase
import com.example.data.DownloadedSongEntity
import com.example.data.FavoriteSongEntity
import com.example.data.HistoryEntity
import com.example.data.LocalSongEntity
import com.example.data.LocalUserLibraryBackend
import com.example.data.PlaylistEntity
import com.example.data.PlaylistSongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Synthetic CI-only source-version-to-version OTA data preservation proof.
 *
 * Runs ONLY inside the isolated Android 14 emulator workflow with an explicit
 * instrumentation gate. Never run this against a user's installed library.
 * The old RELEASE APK and the new RELEASE APK must both be signed by the
 * permanent production certificate. No uninstall or clear-data is permitted.
 */
@RunWith(AndroidJUnit4::class)
class RoomActualOtaUpgradeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private val evidence get() = context.getSharedPreferences("lanu_ota_ci_fixture", Context.MODE_PRIVATE)

    private fun gate(phase: String) {
        val args = instrumentation.arguments
        assertEquals("isolated-emulator-v1", args.getString("lanu_ota_ci"))
        assertEquals(phase, args.getString("lanu_ota_phase"))
        val expected = args.getString("lanu_expected_version")?.toIntOrNull()
        assertTrue("Expected version is required", expected != null && expected > 0)
        assertEquals(expected, BuildConfig.VERSION_CODE)
        assertEquals("com.aistudio.lanumusic.kpxzqa", context.packageName)
    }

    @Test fun seedPreviouslyInstalledStableRelease() = runBlocking {
        gate("seed")
        val userId = LocalAuthBackend(context).ensureLocalSession().userId
        val library = LocalUserLibraryBackend(context)
        library.addFavorite(userId, "ota-ci-favorite", 101L)
        val list = library.createPlaylist(userId, "OTA CI Kütüphane", nowMs = 102L)
        library.addSongToPlaylist(userId, list.id, "ota-ci-song", 103L)
        library.recordPlay(userId, "ota-ci-played", 104L)

        val dao = AppDatabase.getDatabase(context).musicDao()
        dao.addFavorite(FavoriteSongEntity("ota-ci-room-favorite", 201L))
        dao.insertPlaylist(PlaylistEntity(id = "ota-ci-room-playlist", name = "OTA Room", createdAt = 202L))
        dao.addSongToPlaylist(PlaylistSongEntity("ota-ci-room-playlist", "ota-ci-song", orderIndex = 0))
        dao.addToHistory(HistoryEntity(songId = "ota-ci-room-history", playedAt = 203L))
        dao.insertLocalSong(LocalSongEntity(
            id = "ota-ci-local-song", title = "CI Offline", artist = "CI Fixture",
            localAudioPath = "ci-private-marker", isAvailableOffline = true
        ))
        dao.addDownloadedSong(DownloadedSongEntity("ota-ci-downloaded", "test", "5", 204L))

        val privateAudio = File(context.filesDir, "offline_audio/ota-ci-sentinel.bin")
        assertTrue(privateAudio.parentFile!!.isDirectory || privateAudio.parentFile!!.mkdirs())
        privateAudio.writeBytes(byteArrayOf(0x4c, 0x41, 0x4e, 0x55))
        assertTrue(evidence.edit()
            .putString("user_id", userId).putString("playlist_id", list.id)
            .putInt("version", BuildConfig.VERSION_CODE).commit())
        assertEquals("ota-ci-favorite", library.snapshot(userId).favorites.single().songId)
        assertEquals(1, dao.getFavoriteSongs().first().count { it.songId == "ota-ci-room-favorite" })
    }

    @Test fun verifySameAccountAndLibraryAfterRealApkUpgrade() = runBlocking {
        gate("verify")
        val userId = evidence.getString("user_id", null) ?: error("Missing pre-upgrade CI identity")
        val playlistId = evidence.getString("playlist_id", null) ?: error("Missing pre-upgrade CI playlist")
        val priorVersion = evidence.getInt("version", -1)
        assertTrue(priorVersion > 0 && BuildConfig.VERSION_CODE > priorVersion)
        assertEquals(userId, LocalAuthBackend(context).ensureLocalSession().userId)

        val library = LocalUserLibraryBackend(context).snapshot(userId)
        assertTrue(library.favorites.any { it.songId == "ota-ci-favorite" && it.addedAtMs == 101L })
        assertTrue(library.playlists.any {
            it.id == playlistId && it.name == "OTA CI Kütüphane" && it.songIds == listOf("ota-ci-song")
        })
        assertTrue(library.history.any { it.songId == "ota-ci-played" && it.playedAtMs == 104L })

        val dao = AppDatabase.getDatabase(context).musicDao()
        assertTrue(dao.getFavoriteSongs().first().any { it.songId == "ota-ci-room-favorite" && it.addedAt == 201L })
        assertEquals("OTA Room", dao.getPlaylistByIdSync("ota-ci-room-playlist")?.name)
        assertEquals(listOf("ota-ci-song"),
            dao.getSongsForPlaylist("ota-ci-room-playlist").first().map { it.songId })
        assertTrue(dao.getHistory().first().any { it.songId == "ota-ci-room-history" })
        assertEquals("CI Offline", dao.getLocalSongByIdSync("ota-ci-local-song")?.title)
        assertTrue(dao.getDownloadedSongs().first().any { it.songId == "ota-ci-downloaded" })

        val privateAudio = File(context.filesDir, "offline_audio/ota-ci-sentinel.bin")
        assertTrue(privateAudio.isFile)
        assertTrue(privateAudio.readBytes().contentEquals(byteArrayOf(0x4c, 0x41, 0x4e, 0x55)))
    }
}
