package com.aptos.client.retry

import com.aptos.client.config.RetryConfig
import com.aptos.core.error.ApiException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RetryPolicyTest {
    @Test
    fun `isRetryable for 429`() {
        RetryPolicy.isRetryable(429) shouldBe true
    }

    @Test
    fun `isRetryable for 500`() {
        RetryPolicy.isRetryable(500) shouldBe true
    }

    @Test
    fun `isRetryable for 502`() {
        RetryPolicy.isRetryable(502) shouldBe true
    }

    @Test
    fun `isRetryable for 503`() {
        RetryPolicy.isRetryable(503) shouldBe true
    }

    @Test
    fun `isRetryable for 504`() {
        RetryPolicy.isRetryable(504) shouldBe true
    }

    @Test
    fun `not retryable for 400`() {
        RetryPolicy.isRetryable(400) shouldBe false
    }

    @Test
    fun `not retryable for 401`() {
        RetryPolicy.isRetryable(401) shouldBe false
    }

    @Test
    fun `not retryable for 404`() {
        RetryPolicy.isRetryable(404) shouldBe false
    }

    @Test
    fun `not retryable for 200`() {
        RetryPolicy.isRetryable(200) shouldBe false
    }

    @Test
    fun `withRetry succeeds on first try`() = runTest {
        val config = RetryConfig(maxRetries = 3, initialDelayMs = 10)
        var callCount = 0
        val result =
            RetryPolicy.withRetry(config) {
                callCount++
                "success"
            }
        result shouldBe "success"
        callCount shouldBe 1
    }

    @Test
    fun `withRetry retries on retryable error`() = runTest {
        val config = RetryConfig(maxRetries = 3, initialDelayMs = 10, maxDelayMs = 50)
        var callCount = 0
        val result =
            RetryPolicy.withRetry(config) {
                callCount++
                if (callCount < 3) throw ApiException("server error", statusCode = 500)
                "success"
            }
        result shouldBe "success"
        callCount shouldBe 3
    }

    @Test
    fun `withRetry does not retry non-retryable errors`() = runTest {
        val config = RetryConfig(maxRetries = 3, initialDelayMs = 10)
        var callCount = 0
        shouldThrow<ApiException> {
            RetryPolicy.withRetry(config) {
                callCount++
                throw ApiException("not found", statusCode = 404)
            }
        }
        callCount shouldBe 1
    }

    @Test
    fun `withRetry does not retry null status code`() = runTest {
        val config = RetryConfig(maxRetries = 3, initialDelayMs = 10)
        var callCount = 0
        shouldThrow<ApiException> {
            RetryPolicy.withRetry(config) {
                callCount++
                throw ApiException("unknown error")
            }
        }
        callCount shouldBe 1
    }

    @Test
    fun `withRetry exhausts retries`() = runTest {
        val config = RetryConfig(maxRetries = 2, initialDelayMs = 10, maxDelayMs = 50)
        var callCount = 0
        shouldThrow<ApiException> {
            RetryPolicy.withRetry(config) {
                callCount++
                throw ApiException("server error", statusCode = 500)
            }
        }
        callCount shouldBe 3 // initial + 2 retries
    }
}
