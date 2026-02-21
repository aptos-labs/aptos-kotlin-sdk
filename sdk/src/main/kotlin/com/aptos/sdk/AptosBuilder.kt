package com.aptos.sdk

import com.aptos.client.config.AptosConfig
import com.aptos.client.config.RetryConfig
import com.aptos.core.types.ChainId

/**
 * Builder for creating customized [Aptos] instances.
 */
class AptosBuilder {
    private var nodeUrl: String? = null
    private var faucetUrl: String? = null
    private var chainId: ChainId? = null
    private var timeoutMs: Long = 30_000
    private var retryConfig: RetryConfig = RetryConfig()

    fun nodeUrl(url: String): AptosBuilder = apply { this.nodeUrl = url }
    fun faucetUrl(url: String): AptosBuilder = apply { this.faucetUrl = url }
    fun chainId(chainId: ChainId): AptosBuilder = apply { this.chainId = chainId }
    fun timeoutMs(timeout: Long): AptosBuilder = apply { this.timeoutMs = timeout }
    fun retryConfig(config: RetryConfig): AptosBuilder = apply { this.retryConfig = config }

    fun build(): Aptos {
        val url = checkNotNull(nodeUrl) { "nodeUrl is required" }
        val config = AptosConfig(
            nodeUrl = url,
            faucetUrl = faucetUrl,
            chainId = chainId,
            timeoutMs = timeoutMs,
            retryConfig = retryConfig,
        )
        return Aptos(config)
    }
}
