package com.aptos.client.rest

import com.aptos.client.config.AptosConfig
import com.aptos.client.config.RetryConfig
import com.aptos.core.account.Ed25519Account
import com.aptos.core.transaction.TransactionBuilder
import com.aptos.core.transaction.TransactionPayload
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SimulationTest {

    private fun testConfig() = AptosConfig(
        nodeUrl = "https://fullnode.testnet.aptoslabs.com/v1",
        retryConfig = RetryConfig(maxRetries = 0),
    )

    @Test
    fun `simulateTransaction returns simulation results`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    [{
                        "hash": "0xabc",
                        "success": true,
                        "vm_status": "Executed successfully",
                        "gas_used": "5"
                    }]
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val account = Ed25519Account.generate()
        val signedTxn = TransactionBuilder.builder()
            .sender(account.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .sign(account)

        val results = client.simulateTransaction(signedTxn)
        results.size shouldBe 1
        results[0].hash shouldBe "0xabc"
        results[0].success shouldBe true
        results[0].gasUsed shouldBe "5"
        client.close()
    }

    @Test
    fun `simulateTransaction with failed result`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    [{
                        "hash": "0xdef",
                        "success": false,
                        "vm_status": "INSUFFICIENT_BALANCE_FOR_TRANSACTION_FEE",
                        "gas_used": "0"
                    }]
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val account = Ed25519Account.generate()
        val signedTxn = TransactionBuilder.builder()
            .sender(account.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .sign(account)

        val results = client.simulateTransaction(signedTxn)
        results[0].success shouldBe false
        results[0].vmStatus shouldBe "INSUFFICIENT_BALANCE_FOR_TRANSACTION_FEE"
        client.close()
    }
}
