package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID

/**
 * Offline-safe identity provider used when no remote authentication provider is configured.
 * It creates a stable local identity only; it never pretends to be a cloud account.
 */
class LocalAuthBackend(context: Context) : AuthBackend {
    private val preferences = context.applicationContext.getSharedPreferences(
        "lanu_auth",
        Context.MODE_PRIVATE
    )

    // Shared across all LocalAuthBackend instances in the Android process.
    // Session creation, login and logout must observe a single atomic identity.
    private suspend fun <T> transaction(block: () -> T): T =
        withContext(Dispatchers.IO) { identityMutex.withLock { block() } }

    private fun SharedPreferences.Editor.persist() {
        if (!commit()) throw IOException("Yerel kimlik diske kaydedilemedi.")
    }

    override suspend fun currentSession(): AuthSession? = transaction { readSession() }

    /**
     * A malformed restored user ID must NEVER trigger automatic creation of a
     * different local account: that would orphan the existing user's library.
     */
    private fun storedUserId(): String? {
        val id = try {
            preferences.getString(KEY_USER_ID, null)
        } catch (error: ClassCastException) {
            throw IOException("Yerel kimlik okunamadı; kullanıcı arşivi korundu.", error)
        }
        if (id != null && id.isBlank()) {
            throw IOException("Yerel kimlik boş; kullanıcı arşivi korundu.")
        }
        return id
    }

    private fun optionalText(key: String, default: String? = null): String? =
        try { preferences.getString(key, default) } catch (_: ClassCastException) { default }

    private fun optionalTime(key: String, default: Long): Long =
        try { preferences.getLong(key, default) } catch (_: ClassCastException) { default }

    private fun readSession(): AuthSession? {
        val userId = storedUserId() ?: return null
        return AuthSession(
            userId = userId,
            displayName = optionalText(KEY_DISPLAY_NAME, "LANU Kullanıcısı")
                ?: "LANU Kullanıcısı",
            email = optionalText(KEY_EMAIL),
            provider = AuthProvider.LOCAL,
            createdAtMs = optionalTime(KEY_CREATED_AT, 0L)
        )
    }

    /**
     * Creates/returns a device-local identity without pretending that a cloud account exists.
     * This is the safe default used until a real remote provider is configured.
     */
    suspend fun ensureLocalSession(): AuthSession = transaction {
        readSession()?.let { return@transaction it }
        val createdAt = System.currentTimeMillis()
        val userId = retainedLocalIdentity()
        preferences.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_LAST_LOCAL_ID, userId)
            .putString(KEY_DISPLAY_NAME, "LANU Kullanıcısı")
            .putLong(KEY_CREATED_AT, createdAt)
            .persist()
        AuthSession(
            userId = userId,
            displayName = "LANU Kullanıcısı",
            provider = AuthProvider.LOCAL,
            createdAtMs = createdAt
        )
    }

    override suspend fun signIn(identifier: String, secret: String): AuthResult = transaction {
        if (identifier.isBlank() || secret.isBlank()) {
            return@transaction AuthResult.Failure(
                AuthFailure.INVALID_INPUT,
                "Kimlik bilgileri boş bırakılamaz"
            )
        }

        // This is deliberately NOT a remote login. It only establishes a local identity.
        val existingId = storedUserId()
        val userId = existingId ?: retainedLocalIdentity()
        val createdAt = optionalTime(KEY_CREATED_AT, 0L).takeIf { it > 0L }
            ?: System.currentTimeMillis()

        preferences.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_LAST_LOCAL_ID, userId)
            .putString(KEY_DISPLAY_NAME, identifier.substringBefore('@').ifBlank { "LANU Kullanıcısı" })
            .putString(KEY_EMAIL, identifier.takeIf { it.contains('@') })
            .putLong(KEY_CREATED_AT, createdAt)
            .persist()

        AuthResult.Success(
            AuthSession(
                userId = userId,
                displayName = preferences.getString(KEY_DISPLAY_NAME, "LANU Kullanıcısı")
                    ?: "LANU Kullanıcısı",
                email = preferences.getString(KEY_EMAIL, null),
                provider = AuthProvider.LOCAL,
                createdAtMs = createdAt
            )
        )
    }

    override suspend fun signOut(): AuthResult = transaction {
        val current = readSession()
        if (current == null) {
            return@transaction AuthResult.Failure(AuthFailure.UNKNOWN, "Aktif LANU oturumu yok")
        }
        // Sign-out ends the active session but must not orphan locally stored playlists.
        // The retained ID is NOT a login credential or a remote authenticated session.
        preferences.edit().clear()
            .putString(KEY_LAST_LOCAL_ID, current.userId)
            .persist()
        AuthResult.Success(current)
    }

    private fun retainedLocalIdentity(): String {
        val retained = try {
            preferences.getString(KEY_LAST_LOCAL_ID, null)
        } catch (error: ClassCastException) {
            throw IOException("Önceki yerel kimlik okunamadı; kullanıcı arşivi korundu.", error)
        }
        return retained?.takeIf { it.startsWith("local-") && it.length < 120 }
            ?: "local-${UUID.randomUUID()}"
    }

    private companion object {
        private val identityMutex = Mutex()
        const val KEY_LAST_LOCAL_ID = "last_local_user_id"
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_EMAIL = "email"
        const val KEY_CREATED_AT = "created_at"
    }
}
