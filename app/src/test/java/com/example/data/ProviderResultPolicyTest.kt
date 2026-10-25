package com.example.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderResultPolicyTest {
    @Test fun successfulProviderResultPassesThrough() = runTest {
        assertEquals("song", ProviderResultPolicy.call { Result.success("song") }.getOrThrow())
    }

    @Test fun providerFailureStillReturnsFailureWithoutCrashingSearch() = runTest {
        val result = ProviderResultPolicy.call<String> { throw IllegalStateException("provider offline") }
        assertTrue(result.isFailure)
        assertEquals("provider offline", result.exceptionOrNull()?.message)
    }

    @Test fun resultFailureDoesNotGetFabricatedIntoSuccess() = runTest {
        val result = ProviderResultPolicy.call { Result.failure<String>(IllegalArgumentException("not licensed")) }
        assertEquals("not licensed", result.exceptionOrNull()?.message)
    }

    @Test fun cancellingParentCannotBeMisreportedAsApiFailure() = runTest {
        var cancelled = false
        try {
            ProviderResultPolicy.call<String> { throw CancellationException("replaced query") }
        } catch (error: CancellationException) {
            cancelled = true
            assertEquals("replaced query", error.message)
        }
        assertTrue(cancelled)
    }

    @Test fun timeoutsAreAlsoCancellationNotApiFailures() = runTest {
        var wasRethrown = false
        try {
            ProviderResultPolicy.call<String> {
                kotlinx.coroutines.withTimeout(1L) {
                    kotlinx.coroutines.delay(1_000L)
                    Result.success("should never complete")
                }
            }
        } catch (_: CancellationException) {
            wasRethrown = true
        }
        assertTrue(wasRethrown)
    }
}
