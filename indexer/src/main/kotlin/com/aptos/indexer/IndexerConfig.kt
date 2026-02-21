package com.aptos.indexer

/**
 * Configuration for the Aptos Indexer GraphQL client.
 *
 * @property indexerUrl the full URL of the Aptos Indexer GraphQL endpoint
 * @property timeoutMs HTTP request timeout in milliseconds
 * @property maxRetries maximum number of retry attempts
 */
data class IndexerConfig(val indexerUrl: String, val timeoutMs: Long = 30_000, val maxRetries: Int = 3) {
    companion object {
        @JvmStatic
        fun mainnet(): IndexerConfig = IndexerConfig(
            indexerUrl = "https://api.mainnet.aptoslabs.com/v1/graphql",
        )

        @JvmStatic
        fun testnet(): IndexerConfig = IndexerConfig(
            indexerUrl = "https://api.testnet.aptoslabs.com/v1/graphql",
        )
    }
}
