package com.aptos.client.faucet

import com.aptos.client.config.AptosConfig
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

class FaucetClientTest {

    private fun testConfig() = AptosConfig(
        nodeUrl = "https://fullnode.testnet.aptoslabs.com/v1",
        faucetUrl = "https://faucet.testnet.aptoslabs.com",
    )

    @Test
    fun `fundAccount success`() = runTest {
        val engine = MockEngine { request ->
            request.url.encodedPath shouldBe "/fund"
            respond(
                content = ByteReadChannel("""{"txn_hashes": ["0xabc"]}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = FaucetClient(testConfig(), engine)
        client.fundAccount(AccountAddress.ONE, 100_000_000uL)
        client.close()
    }

    @Test
    fun `fundAccount fallback to legacy mint endpoint`() = runTest {
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            if (request.url.encodedPath == "/fund") {
                respond(
                    content = ByteReadChannel("Not Found"),
                    status = HttpStatusCode.NotFound,
                )
            } else {
                request.url.encodedPath shouldBe "/mint"
                respond(
                    content = ByteReadChannel("""["0xabc"]"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }

        val client = FaucetClient(testConfig(), engine)
        client.fundAccount(AccountAddress.ONE)
        callCount shouldBe 2
        client.close()
    }

    @Test
    fun `fundAccount throws when both endpoints fail`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("Server Error"),
                status = HttpStatusCode.InternalServerError,
            )
        }

        val client = FaucetClient(testConfig(), engine)
        shouldThrow<ApiException> {
            client.fundAccount(AccountAddress.ONE)
        }
        client.close()
    }

    @Test
    fun `constructor throws without faucet URL`() {
        shouldThrow<ApiException> {
            FaucetClient(AptosConfig(nodeUrl = "http://localhost:8080/v1"))
        }
    }
}
