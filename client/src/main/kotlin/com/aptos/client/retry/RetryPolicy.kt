package com.aptos.client.retry

import com.aptos.client.config.RetryConfig
import com.aptos.core.error.ApiException
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

/**
 * Retry policy with exponential backoff and jitter for transient API failures.
 *
 * Retryable HTTP status codes: 429 (Too Many Requests), 500, 502, 503, 504.
 */
object RetryPolicy {

    private val RETRYABLE_STATUS_CODES = setOf(429, 500, 502, 503, 504)

    /** Returns `true` if the given HTTP [statusCode] is considered retryable. */
    fun isRetryable(statusCode: Int): Boolean = statusCode in RETRYABLE_STATUS_CODES

    /**
     * Executes [block] with automatic retries on retryable [ApiException] errors.
     *
     * Uses exponential backoff with random jitter between retries.
     */
    suspend fun <T> withRetry(
        config: RetryConfig,
        block: suspend () -> T,
    ): T {
        var lastException: Exception? = null
        var currentDelay = config.initialDelayMs

        repeat(config.maxRetries + 1) { attempt ->
            try {
                return block()
            } catch (e: ApiException) {
                val code = e.statusCode
                if (code != null && isRetryable(code) && attempt < config.maxRetries) {
                    lastException = e
                    val jitter = Random.nextLong(0, currentDelay / 2 + 1)
                    delay(currentDelay + jitter)
                    currentDelay = min(
                        (currentDelay * config.backoffMultiplier).toLong(),
                        config.maxDelayMs,
                    )
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: ApiException("Retry exhausted with no exception")
    }
}
