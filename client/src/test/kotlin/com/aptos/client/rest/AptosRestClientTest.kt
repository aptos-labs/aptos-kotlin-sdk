package com.aptos.client.rest

import com.aptos.client.config.AptosConfig
import com.aptos.client.config.RetryConfig
import com.aptos.core.error.ApiException
import com.aptos.core.types.AccountAddress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AptosRestClientTest {

    private fun mockEngine(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): MockEngine =
        MockEngine(handler)

    private fun testConfig(retryConfig: RetryConfig = RetryConfig(maxRetries = 0)) = AptosConfig(
        nodeUrl = "https://fullnode.testnet.aptoslabs.com/v1",
        faucetUrl = "https://faucet.testnet.aptoslabs.com",
        retryConfig = retryConfig,
    )

    @Test
    fun `getLedgerInfo success`() = runTest {
        val engine = mockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "chain_id": 2,
                        "epoch": "100",
                        "ledger_version": "12345678",
                        "oldest_ledger_version": "0",
                        "ledger_timestamp": "1700000000000000",
                        "node_role": "full_node",
                        "oldest_block_height": "0",
                        "block_height": "5000000",
                        "git_hash": "abc123"
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val info = client.getLedgerInfo()
        info.chainId shouldBe 2
        info.epoch shouldBe "100"
        info.ledgerVersion shouldBe "12345678"
        info.nodeRole shouldBe "full_node"
        client.close()
    }

    @Test
    fun `getAccount success`() = runTest {
        val engine = mockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "sequence_number": "42",
                        "authentication_key": "0x0000000000000000000000000000000000000000000000000000000000000001"
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val account = client.getAccount(AccountAddress.ONE)
        account.sequenceNumber shouldBe "42"
        client.close()
    }

    @Test
    fun `getAccountResources success`() = runTest {
        val engine = mockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    [
                        {
                            "type": "0x1::account::Account",
                            "data": {"sequence_number": "0"}
                        }
                    ]
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val resources = client.getAccountResources(AccountAddress.ONE)
        resources.size shouldBe 1
        resources[0].type shouldBe "0x1::account::Account"
        client.close()
    }

    @Test
    fun `getTransactionByHash success`() = runTest {
        val engine = mockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "type": "user_transaction",
                        "hash": "0xabc123",
                        "sender": "0x1",
                        "sequence_number": "5",
                        "success": true,
                        "vm_status": "Executed successfully"
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val txn = client.getTransactionByHash("0xabc123")
        txn.hash shouldBe "0xabc123"
        txn.success shouldBe true
        client.close()
    }

    @Test
    fun `estimateGasPrice success`() = runTest {
        val engine = mockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "gas_estimate": 100,
                        "deprioritized_gas_estimate": 50,
                        "prioritized_gas_estimate": 200
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val estimate = client.estimateGasPrice()
        estimate.gasEstimate shouldBe 100
        estimate.deprioritizedGasEstimate shouldBe 50
        estimate.prioritizedGasEstimate shouldBe 200
        client.close()
    }

    @Test
    fun `API error maps to ApiException`() = runTest {
        val engine = mockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "message": "Account not found",
                        "error_code": "account_not_found"
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val exception = shouldThrow<ApiException> {
            client.getAccount(AccountAddress.fromHexRelaxed("0xdeadbeef"))
        }
        exception.statusCode shouldBe 404
        exception.errorCode shouldBe "account_not_found"
        client.close()
    }

    @Test
    fun `retry on 429`() = runTest {
        var callCount = 0
        val engine = mockEngine {
            callCount++
            if (callCount == 1) {
                respond(
                    content = ByteReadChannel("""{"message":"rate limited"}"""),
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                respond(
                    content = ByteReadChannel(
                        """
                        {
                            "chain_id": 2, "epoch": "1", "ledger_version": "1",
                            "oldest_ledger_version": "0", "ledger_timestamp": "0",
                            "node_role": "full_node", "oldest_block_height": "0",
                            "block_height": "1"
                        }
                        """.trimIndent(),
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }

        val retryConfig = RetryConfig(maxRetries = 2, initialDelayMs = 10, maxDelayMs = 50)
        val client = AptosRestClient(testConfig(retryConfig), engine)
        val info = client.getLedgerInfo()
        info.chainId shouldBe 2
        callCount shouldBe 2
        client.close()
    }

    @Test
    fun `retry exhaustion throws`() = runTest {
        val engine = mockEngine {
            respond(
                content = ByteReadChannel("""{"message":"server error"}"""),
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val retryConfig = RetryConfig(maxRetries = 2, initialDelayMs = 10, maxDelayMs = 50)
        val client = AptosRestClient(testConfig(retryConfig), engine)
        shouldThrow<ApiException> {
            client.getLedgerInfo()
        }
        client.close()
    }

    @Test
    fun `view function call`() = runTest {
        val engine = mockEngine {
            respond(
                content = ByteReadChannel("""["1000"]"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val result = client.view(
            function = "0x1::coin::balance",
            typeArguments = listOf("0x1::aptos_coin::AptosCoin"),
            arguments = listOf("0x1"),
        )
        result.size shouldBe 1
        client.close()
    }

    @Test
    fun `getBalance parses coin value`() = runTest {
        val engine = mockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "type": "0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>",
                        "data": {
                            "coin": {
                                "value": "500000000"
                            },
                            "frozen": false
                        }
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val balance = client.getBalance(AccountAddress.ONE)
        balance shouldBe 500_000_000uL
        client.close()
    }
}
