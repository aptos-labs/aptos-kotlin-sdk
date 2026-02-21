package com.aptos.indexer

import com.aptos.core.error.ApiException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AptosIndexerClientTest {

    private fun testConfig() = IndexerConfig(
        indexerUrl = "https://api.testnet.aptoslabs.com/v1/graphql",
    )

    @Test
    fun `getAccountTokens returns tokens`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "data": {
                            "current_token_ownerships_v2": [
                                {
                                    "token_data_id": "0xabc",
                                    "token_name": "My Token",
                                    "collection_name": "My Collection",
                                    "amount": "1"
                                }
                            ]
                        }
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosIndexerClient(testConfig(), engine)
        val tokens = client.getAccountTokens("0x1")
        tokens.size shouldBe 1
        tokens[0].tokenName shouldBe "My Token"
        tokens[0].collectionName shouldBe "My Collection"
        client.close()
    }

    @Test
    fun `getCollections returns collections`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "data": {
                            "current_collections_v2": [
                                {
                                    "collection_id": "0xdef",
                                    "collection_name": "Test Collection",
                                    "creator_address": "0x1",
                                    "description": "A test",
                                    "current_supply": "10"
                                }
                            ]
                        }
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosIndexerClient(testConfig(), engine)
        val collections = client.getCollections("0x1")
        collections.size shouldBe 1
        collections[0].collectionName shouldBe "Test Collection"
        collections[0].currentSupply shouldBe "10"
        client.close()
    }

    @Test
    fun `getAccountTransactionsWithPayload returns transactions`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "data": {
                            "user_transactions": [
                                {
                                    "version": "12345",
                                    "hash": "0xabc",
                                    "sender": "0x1",
                                    "success": true
                                }
                            ]
                        }
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosIndexerClient(testConfig(), engine)
        val txns = client.getAccountTransactionsWithPayload("0x1")
        txns.size shouldBe 1
        txns[0].hash shouldBe "0xabc"
        txns[0].success shouldBe true
        client.close()
    }

    @Test
    fun `getEvents returns events`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "data": {
                            "events": [
                                {
                                    "account_address": "0x1",
                                    "sequence_number": "0",
                                    "type": "0x1::coin::DepositEvent",
                                    "transaction_version": "12345"
                                }
                            ]
                        }
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosIndexerClient(testConfig(), engine)
        val events = client.getEvents("0x1", "0x1::coin::DepositEvent")
        events.size shouldBe 1
        events[0].type shouldBe "0x1::coin::DepositEvent"
        client.close()
    }

    @Test
    fun `GraphQL errors throw ApiException`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "data": null,
                        "errors": [{"message": "field not found"}]
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = AptosIndexerClient(testConfig(), engine)
        shouldThrow<ApiException> {
            client.getAccountTokens("0x1")
        }
        client.close()
    }

    @Test
    fun `HTTP error throws ApiException`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("Internal Server Error"),
                status = HttpStatusCode.InternalServerError,
            )
        }

        val client = AptosIndexerClient(testConfig(), engine)
        shouldThrow<ApiException> {
            client.getAccountTokens("0x1")
        }
        client.close()
    }
}
