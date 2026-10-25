package com.example.data

import kotlinx.coroutines.CancellationException

/**
 * External provider calls may fail independently, but coroutine cancellation
 * must always propagate to the parent search/job. It must never be converted
 * to Result.failure and reported as an ordinary remote API outage.
 */
internal object ProviderResultPolicy {
    suspend fun <T> call(block: suspend () -> Result<T>): Result<T> =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(failure)
        }
}
