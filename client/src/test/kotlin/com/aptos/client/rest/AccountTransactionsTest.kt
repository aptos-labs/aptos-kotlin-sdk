package com.aptos.client.rest

import com.aptos.client.config.AptosConfig
import com.aptos.client.config.RetryConfig
import com.aptos.core.types.AccountAddress
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AccountTransactionsTest {

    private fun testConfig() = AptosConfig(
        nodeUrl = "https://fullnode.testnet.aptoslabs.com/v1",
        retryConfig = RetryConfig(maxRetries = 0),
    )

    @Test
    fun `getAccountTransactions returns list`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    [
                        {
                            "hash": "0x111",
                            "type": "user_transaction",
                            "sender": "0x1",
                            "sequence_number": "0",
                            "success": true,
                            "vm_status": "Executed successfully"
                        },
                        {
                            "hash": "0x222",
                            "type": "user_transaction",
                            "sender": "0x1",
                            "sequence_number": "1",
                            "success": true,
                            "vm_status": "Executed successfully"
                        }
                    ]
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val txns = client.getAccountTransactions(AccountAddress.ONE)
        txns.size shouldBe 2
        txns[0].hash shouldBe "0x111"
        txns[1].hash shouldBe "0x222"
        client.close()
    }

    @Test
    fun `getAccountTransactions with start and limit parameters`() = runTest {
        val engine = MockEngine { request ->
            request.url.parameters["start"] shouldBe "5"
            request.url.parameters["limit"] shouldBe "10"
            respond(
                content = ByteReadChannel("[]"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val txns = client.getAccountTransactions(AccountAddress.ONE, start = 5, limit = 10)
        txns.size shouldBe 0
        client.close()
    }

    @Test
    fun `getAccountTransactions empty result`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("[]"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val txns = client.getAccountTransactions(AccountAddress.ONE)
        txns.size shouldBe 0
        client.close()
    }
}
