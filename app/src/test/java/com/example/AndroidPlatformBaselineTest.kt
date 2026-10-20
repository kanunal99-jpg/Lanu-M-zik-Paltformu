package com.example

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthResult
import com.example.auth.LocalAuthBackend
import com.example.data.LocalUserLibraryBackend
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Android-platform tests, intentionally using NO Samsung/Xiaomi/Huawei or GMS SDK. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 29, 33, 35])
class AndroidPlatformBaselineTest {
    @Test fun coreOfflineIdentityFavoritesAndPlaylistsWorkAcrossApiLevels() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.aistudio.lanumusic.kpxzqa", context.packageName)

        val id = "compat-" + Build.VERSION.SDK_INT
        val accountPrefs = context.getSharedPreferences("lanu_auth", Context.MODE_PRIVATE)
        val libraryPrefs = context.getSharedPreferences("lanu_user_library", Context.MODE_PRIVATE)
        assertTrue(accountPrefs.edit().clear().commit())
        assertTrue(libraryPrefs.edit().clear().commit())

        val auth = LocalAuthBackend(context)
        val session = auth.ensureLocalSession()
        val library = LocalUserLibraryBackend(context)
        library.addFavorite(session.userId, id, nowMs = 1L)
        val playlist = library.createPlaylist(session.userId, "Android OEM testi", nowMs = 2L)
        library.addSongToPlaylist(session.userId, playlist.id, id, nowMs = 3L)

        assertTrue(auth.signOut() is AuthResult.Success)
        val restored = LocalAuthBackend(context).ensureLocalSession()
        assertEquals(session.userId, restored.userId)

        val afterRestart = LocalUserLibraryBackend(context).snapshot(restored.userId)
        assertEquals(listOf(id), afterRestart.favorites.map { it.songId })
        assertEquals(listOf(id), afterRestart.playlists.single().songIds)
    }
}
