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

class EventsTest {

    private fun testConfig() = AptosConfig(
        nodeUrl = "https://fullnode.testnet.aptoslabs.com/v1",
        retryConfig = RetryConfig(maxRetries = 0),
    )

    @Test
    fun `getEvents returns event list`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    [
                        {
                            "type": "0x1::coin::DepositEvent",
                            "data": {"amount": "1000"},
                            "sequence_number": "0",
                            "version": "12345"
                        },
                        {
                            "type": "0x1::coin::DepositEvent",
                            "data": {"amount": "2000"},
                            "sequence_number": "1",
                            "version": "12346"
                        }
                    ]
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val events = client.getEvents(
            AccountAddress.ONE,
            "0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>",
            "deposit_events",
        )
        events.size shouldBe 2
        events[0].type shouldBe "0x1::coin::DepositEvent"
        events[0].sequenceNumber shouldBe "0"
        events[0].version shouldBe "12345"
        events[1].sequenceNumber shouldBe "1"
        client.close()
    }

    @Test
    fun `getEvents with pagination parameters`() = runTest {
        val engine = MockEngine { request ->
            request.url.parameters["start"] shouldBe "10"
            request.url.parameters["limit"] shouldBe "5"
            respond(
                content = ByteReadChannel("[]"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosRestClient(testConfig(), engine)
        val events = client.getEvents(
            AccountAddress.ONE,
            "0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>",
            "deposit_events",
            start = 10,
            limit = 5,
        )
        events.size shouldBe 0
        client.close()
    }
}
