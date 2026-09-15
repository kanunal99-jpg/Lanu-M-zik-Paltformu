package com.example

import com.example.auth.AuthFailure
import com.example.auth.AuthProvider
import com.example.auth.AuthResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class InMemoryAuthBackend : com.example.auth.AuthBackend {
    private var session: com.example.auth.AuthSession? = null

    override suspend fun currentSession() = session

    override suspend fun signIn(identifier: String, secret: String): AuthResult {
        if (identifier.isBlank() || secret.isBlank()) {
            return AuthResult.Failure(AuthFailure.INVALID_INPUT, "invalid")
        }
        session = com.example.auth.AuthSession(
            userId = "test-user",
            displayName = identifier,
            provider = AuthProvider.REMOTE,
            createdAtMs = 1L
        )
        return AuthResult.Success(session!!)
    }

    override suspend fun signOut(): AuthResult {
        val current = session ?: return AuthResult.Failure(AuthFailure.UNKNOWN, "none")
        session = null
        return AuthResult.Success(current)
    }
}

class AuthBackendTest {
    @Test
    fun successfulLoginPersistsThroughSessionContract() = runTest {
        val backend = InMemoryAuthBackend()
        val result = backend.signIn("test@example.com", "secret")

        assertTrue(result is AuthResult.Success)
        assertNotNull(backend.currentSession())
        assertEquals("test-user", backend.currentSession()?.userId)
    }

    @Test
    fun invalidInputIsRejectedWithoutSession() = runTest {
        val backend = InMemoryAuthBackend()
        val result = backend.signIn("", "")

        assertEquals(AuthFailure.INVALID_INPUT, (result as AuthResult.Failure).reason)
        assertEquals(null, backend.currentSession())
    }

    @Test
    fun signOutClearsSession() = runTest {
        val backend = InMemoryAuthBackend()
        backend.signIn("test@example.com", "secret")

        assertTrue(backend.signOut() is AuthResult.Success)
        assertEquals(null, backend.currentSession())
    }
}
