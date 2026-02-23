package com.aptos.sdk

import com.aptos.client.config.AptosConfig
import com.aptos.client.config.RetryConfig
import com.aptos.core.types.ChainId

/**
 * Builder for creating customized [Aptos] instances.
 *
 * ```kotlin
 * val aptos = Aptos.builder()
 *     .nodeUrl("http://localhost:8080/v1")
 *     .faucetUrl("http://localhost:8081")
 *     .chainId(ChainId.LOCAL)
 *     .timeoutMs(5_000)
 *     .build()
 * ```
 *
 * At minimum, [nodeUrl] must be set before calling [build].
 */
class AptosBuilder {
    private var nodeUrl: String? = null
    private var faucetUrl: String? = null
    private var chainId: ChainId? = null
    private var timeoutMs: Long = 30_000
    private var retryConfig: RetryConfig = RetryConfig()

    /**
     * Sets the Aptos REST API node URL (required).
     *
     * @param url the full node URL including version path (e.g. `"https://fullnode.testnet.aptoslabs.com/v1"`)
     * @return this builder for chaining
     */
    fun nodeUrl(url: String): AptosBuilder = apply { this.nodeUrl = url }

    /**
     * Sets the faucet URL for funding accounts (optional, testnet/devnet/localnet only).
     *
     * @param url the faucet endpoint URL
     * @return this builder for chaining
     */
    fun faucetUrl(url: String): AptosBuilder = apply { this.faucetUrl = url }

    /**
     * Sets the chain ID. If not set, it will be fetched from the node on first use.
     *
     * @param chainId the chain identifier (e.g. [ChainId.TESTNET])
     * @return this builder for chaining
     */
    fun chainId(chainId: ChainId): AptosBuilder = apply { this.chainId = chainId }

    /**
     * Sets the HTTP request timeout in milliseconds (default: 30,000).
     *
     * @param timeout timeout in milliseconds
     * @return this builder for chaining
     */
    fun timeoutMs(timeout: Long): AptosBuilder = apply { this.timeoutMs = timeout }

    /**
     * Sets the retry configuration for transient HTTP errors.
     *
     * @param config retry policy with backoff parameters
     * @return this builder for chaining
     */
    fun retryConfig(config: RetryConfig): AptosBuilder = apply { this.retryConfig = config }

    /**
     * Builds the [Aptos] instance with the configured settings.
     *
     * @return a new [Aptos] instance
     * @throws IllegalStateException if [nodeUrl] has not been set
     */
    fun build(): Aptos {
        val url = checkNotNull(nodeUrl) { "nodeUrl is required" }
        val config =
            AptosConfig(
                nodeUrl = url,
                faucetUrl = faucetUrl,
                chainId = chainId,
                timeoutMs = timeoutMs,
                retryConfig = retryConfig,
            )
        return Aptos(config)
    }
}
