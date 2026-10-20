package com.example.auth

import android.content.Context
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

    override suspend fun currentSession(): AuthSession? {
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        return AuthSession(
            userId = userId,
            displayName = preferences.getString(KEY_DISPLAY_NAME, "LANU Kullanıcısı")
                ?: "LANU Kullanıcısı",
            email = preferences.getString(KEY_EMAIL, null),
            provider = AuthProvider.LOCAL,
            createdAtMs = preferences.getLong(KEY_CREATED_AT, 0L)
        )
    }

    /**
     * Creates/returns a device-local identity without pretending that a cloud account exists.
     * This is the safe default used until a real remote provider is configured.
     */
    suspend fun ensureLocalSession(): AuthSession {
        currentSession()?.let { return it }
        val createdAt = System.currentTimeMillis()
        val userId = retainedLocalIdentity()
        preferences.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_LAST_LOCAL_ID, userId)
            .putString(KEY_DISPLAY_NAME, "LANU Kullanıcısı")
            .putLong(KEY_CREATED_AT, createdAt)
            .apply()
        return AuthSession(
            userId = userId,
            displayName = "LANU Kullanıcısı",
            provider = AuthProvider.LOCAL,
            createdAtMs = createdAt
        )
    }

    override suspend fun signIn(identifier: String, secret: String): AuthResult {
        if (identifier.isBlank() || secret.isBlank()) {
            return AuthResult.Failure(
                AuthFailure.INVALID_INPUT,
                "Kimlik bilgileri boş bırakılamaz"
            )
        }

        // This is deliberately NOT a remote login. It only establishes a local identity.
        val existingId = preferences.getString(KEY_USER_ID, null)
        val userId = existingId ?: retainedLocalIdentity()
        val createdAt = preferences.getLong(KEY_CREATED_AT, 0L).takeIf { it > 0L }
            ?: System.currentTimeMillis()

        preferences.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_DISPLAY_NAME, identifier.substringBefore('@').ifBlank { "LANU Kullanıcısı" })
            .putString(KEY_EMAIL, identifier.takeIf { it.contains('@') })
            .putLong(KEY_CREATED_AT, createdAt)
            .apply()

        return AuthResult.Success(
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

    override suspend fun signOut(): AuthResult {
        val current = currentSession()
        if (current == null) {
            return AuthResult.Failure(AuthFailure.UNKNOWN, "Aktif LANU oturumu yok")
        }
        // Sign-out ends the active session but must not orphan locally stored playlists.
        // The retained ID is NOT a login credential or a remote authenticated session.
        preferences.edit().clear()
            .putString(KEY_LAST_LOCAL_ID, current.userId)
            .apply()
        return AuthResult.Success(current)
    }

    private fun retainedLocalIdentity(): String =
        preferences.getString(KEY_LAST_LOCAL_ID, null)
            ?.takeIf { it.startsWith("local-") && it.length < 120 }
            ?: "local-${UUID.randomUUID()}"

    private companion object {
        const val KEY_LAST_LOCAL_ID = "last_local_user_id"
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_EMAIL = "email"
        const val KEY_CREATED_AT = "created_at"
    }
}
