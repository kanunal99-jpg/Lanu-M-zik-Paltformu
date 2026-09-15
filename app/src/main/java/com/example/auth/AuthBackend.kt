package com.example.auth

/**
 * Authentication boundary for LANU Music.
 *
 * UI and playback code must depend on this contract rather than a provider SDK.
 * A real provider can be added later without changing Room, Media3 or screens.
 */
interface AuthBackend {
    suspend fun currentSession(): AuthSession?

    suspend fun signIn(identifier: String, secret: String): AuthResult

    suspend fun signOut(): AuthResult
}

data class AuthSession(
    val userId: String,
    val displayName: String,
    val email: String? = null,
    val provider: AuthProvider = AuthProvider.LOCAL,
    val createdAtMs: Long
)

enum class AuthProvider {
    LOCAL,
    REMOTE
}

sealed interface AuthResult {
    data class Success(val session: AuthSession) : AuthResult
    data class Failure(val reason: AuthFailure, val message: String) : AuthResult
}

enum class AuthFailure {
    INVALID_INPUT,
    NOT_CONFIGURED,
    INVALID_CREDENTIALS,
    NETWORK,
    UNKNOWN
}
