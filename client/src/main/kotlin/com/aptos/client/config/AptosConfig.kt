package com.aptos.client.config

import com.aptos.core.types.ChainId

/**
 * Configuration for connecting to an Aptos network.
 */
data class AptosConfig(
    val nodeUrl: String,
    val faucetUrl: String? = null,
    val chainId: ChainId? = null,
    val timeoutMs: Long = 30_000,
    val retryConfig: RetryConfig = RetryConfig(),
) {
    companion object {
        @JvmStatic
        fun mainnet(): AptosConfig = AptosConfig(
            nodeUrl = "https://fullnode.mainnet.aptoslabs.com/v1",
            chainId = ChainId.MAINNET,
        )

        @JvmStatic
        fun testnet(): AptosConfig = AptosConfig(
            nodeUrl = "https://fullnode.testnet.aptoslabs.com/v1",
            faucetUrl = "https://faucet.testnet.aptoslabs.com",
            chainId = ChainId.TESTNET,
        )

        @JvmStatic
        fun devnet(): AptosConfig = AptosConfig(
            nodeUrl = "https://fullnode.devnet.aptoslabs.com/v1",
            faucetUrl = "https://faucet.devnet.aptoslabs.com",
        )

        @JvmStatic
        @JvmOverloads
        fun localnet(
            nodeUrl: String = "http://localhost:8080/v1",
            faucetUrl: String = "http://localhost:8081",
        ): AptosConfig = AptosConfig(
            nodeUrl = nodeUrl,
            faucetUrl = faucetUrl,
            chainId = ChainId.LOCAL,
        )
    }
}

data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 200,
    val maxDelayMs: Long = 10_000,
    val backoffMultiplier: Double = 2.0,
)
