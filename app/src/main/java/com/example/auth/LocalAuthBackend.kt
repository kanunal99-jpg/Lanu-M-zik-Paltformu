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

    override suspend fun signIn(identifier: String, secret: String): AuthResult {
        if (identifier.isBlank() || secret.isBlank()) {
            return AuthResult.Failure(
                AuthFailure.INVALID_INPUT,
                "Kimlik bilgileri boş bırakılamaz"
            )
        }

        // This is deliberately NOT a remote login. It only establishes a local identity.
        val existingId = preferences.getString(KEY_USER_ID, null)
        val userId = existingId ?: "local-${UUID.randomUUID()}"
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
        preferences.edit().clear().apply()
        return AuthResult.Success(current)
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_EMAIL = "email"
        const val KEY_CREATED_AT = "created_at"
    }
}
