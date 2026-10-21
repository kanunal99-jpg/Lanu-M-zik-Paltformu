package com.example.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 29, 35])
class LocalAuthCorruptBackupTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val prefs get() = context.getSharedPreferences("lanu_auth", Context.MODE_PRIVATE)

    @Test fun corruptedOptionalProfileTypesMustPreserveCurrentUserId() = runBlocking {
        try {
            assertTrue(prefs.edit().clear()
                .putString("user_id", "local-stable-existing")
                .putString("last_local_user_id", "local-stable-existing")
                .putInt("display_name", 123)
                .putBoolean("email", false)
                .putString("created_at", "not-a-timestamp")
                .commit())
            val auth = LocalAuthBackend(context)
            val restored = auth.currentSession()
            assertNotNull(restored)
            assertEquals("local-stable-existing", restored?.userId)
            assertEquals("LANU Kullanıcısı", restored?.displayName)
            assertEquals(null, restored?.email)
            assertEquals(0L, restored?.createdAtMs)
            assertEquals("local-stable-existing", auth.ensureLocalSession().userId)
            // Fail-safe decoding never erases the backed-up original record.
            assertTrue(prefs.contains("display_name"))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun corruptedRequiredUserIdMustNotGenerateReplacementIdentity() = runBlocking {
        try {
            prefs.edit().clear()
                .putInt("user_id", 4321)
                .putString("last_local_user_id", "local-previous")
                .commit()
            val auth = LocalAuthBackend(context)
            val failure = runCatching { auth.ensureLocalSession() }.exceptionOrNull()
            assertTrue("A wrong-typed current identity must fail closed", failure is IOException)
            assertTrue(prefs.contains("user_id"))
            assertEquals("local-previous", prefs.getString("last_local_user_id", null))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun corruptedRetainedIdentityMustNotSilentlyCreateAnotherUser() = runBlocking {
        try {
            prefs.edit().clear().putLong("last_local_user_id", 123L).commit()
            val failure = runCatching { LocalAuthBackend(context).ensureLocalSession() }.exceptionOrNull()
            assertTrue("A wrong-typed retained ID cannot be replaced", failure is IOException)
            assertFalse(prefs.contains("user_id"))
            assertTrue(prefs.contains("last_local_user_id"))
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
