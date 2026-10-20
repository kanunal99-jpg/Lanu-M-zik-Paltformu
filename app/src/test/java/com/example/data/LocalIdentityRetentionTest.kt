package com.example.data

import android.content.Context
import com.example.auth.AuthResult
import com.example.auth.LocalAuthBackend
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocalIdentityRetentionTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test fun logoutAndReopenKeepsPreviouslySavedLibrary() = runTest {
        context.getSharedPreferences("lanu_auth", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("lanu_user_library", Context.MODE_PRIVATE).edit().clear().commit()
        try {
            val identity = LocalAuthBackend(context)
            val first = identity.ensureLocalSession()
            val library = LocalUserLibraryBackend(context)
            library.addFavorite(first.userId, "safe-local-song")
            val playlist = library.createPlaylist(first.userId, "Favori Listem", "Yerel")
            assertTrue(identity.signOut() is AuthResult.Success)
            assertNull(identity.currentSession())

            val next = LocalAuthBackend(context).ensureLocalSession()
            assertEquals(first.userId, next.userId)
            assertEquals("safe-local-song", library.snapshot(next.userId).favorites.single().songId)
            assertEquals(playlist.id, library.snapshot(next.userId).playlists.single().id)
        } finally {
            context.getSharedPreferences("lanu_auth", Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("lanu_user_library", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun localSignInReusesDeviceIdentityNotAnUnrelatedLibrary() = runTest {
        context.getSharedPreferences("lanu_auth", Context.MODE_PRIVATE).edit().clear().commit()
        try {
            val backend = LocalAuthBackend(context)
            val original = backend.ensureLocalSession()
            backend.signOut()
            val signedIn = backend.signIn("local@example.com", "non-remote-placeholder") as AuthResult.Success
            assertEquals(original.userId, signedIn.session.userId)
        } finally {
            context.getSharedPreferences("lanu_auth", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
