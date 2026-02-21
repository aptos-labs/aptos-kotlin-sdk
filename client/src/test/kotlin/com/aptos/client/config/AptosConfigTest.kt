package com.aptos.client.config

import com.aptos.core.types.ChainId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class AptosConfigTest {

    @Test
    fun `mainnet config`() {
        val config = AptosConfig.mainnet()
        config.nodeUrl shouldBe "https://fullnode.mainnet.aptoslabs.com/v1"
        config.faucetUrl shouldBe null
        config.chainId shouldBe ChainId.MAINNET
    }

    @Test
    fun `testnet config`() {
        val config = AptosConfig.testnet()
        config.nodeUrl shouldBe "https://fullnode.testnet.aptoslabs.com/v1"
        config.faucetUrl shouldNotBe null
        config.chainId shouldBe ChainId.TESTNET
    }

    @Test
    fun `devnet config`() {
        val config = AptosConfig.devnet()
        config.nodeUrl shouldBe "https://fullnode.devnet.aptoslabs.com/v1"
        config.faucetUrl shouldNotBe null
    }

    @Test
    fun `localnet config defaults`() {
        val config = AptosConfig.localnet()
        config.nodeUrl shouldBe "http://localhost:8080/v1"
        config.faucetUrl shouldBe "http://localhost:8081"
        config.chainId shouldBe ChainId.LOCAL
    }

    @Test
    fun `localnet config custom URLs`() {
        val config = AptosConfig.localnet(
            nodeUrl = "http://custom:9090/v1",
            faucetUrl = "http://custom:9091",
        )
        config.nodeUrl shouldBe "http://custom:9090/v1"
        config.faucetUrl shouldBe "http://custom:9091"
    }

    @Test
    fun `default timeout`() {
        val config = AptosConfig.mainnet()
        config.timeoutMs shouldBe 30_000
    }

    @Test
    fun `default retry config`() {
        val config = AptosConfig.mainnet()
        config.retryConfig.maxRetries shouldBe 3
        config.retryConfig.initialDelayMs shouldBe 200
        config.retryConfig.maxDelayMs shouldBe 10_000
        config.retryConfig.backoffMultiplier shouldBe 2.0
    }

    @Test
    fun `custom config`() {
        val config = AptosConfig(
            nodeUrl = "http://example.com/v1",
            faucetUrl = "http://example.com/faucet",
            chainId = ChainId(99u),
            timeoutMs = 5000,
            retryConfig = RetryConfig(maxRetries = 5),
        )
        config.nodeUrl shouldBe "http://example.com/v1"
        config.chainId shouldBe ChainId(99u)
        config.timeoutMs shouldBe 5000
        config.retryConfig.maxRetries shouldBe 5
    }
}
