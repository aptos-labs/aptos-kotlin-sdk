package com.aptos.sdk

import com.aptos.client.config.AptosConfig
import com.aptos.core.account.Ed25519Account
import com.aptos.core.account.Secp256k1Account
import com.aptos.core.transaction.TransactionBuilder
import com.aptos.core.transaction.TransactionPayload
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AptosTest {

    private fun mockAptos(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): Aptos {
        val engine = MockEngine(handler)
        return Aptos(AptosConfig.testnet(), engine)
    }

    @Test
    fun `getLedgerInfo via facade`() = runTest {
        val aptos = mockAptos {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "chain_id": 2, "epoch": "100", "ledger_version": "12345678",
                        "oldest_ledger_version": "0", "ledger_timestamp": "1700000000000000",
                        "node_role": "full_node", "oldest_block_height": "0",
                        "block_height": "5000000"
                    }
                    """.trimIndent()
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val info = aptos.getLedgerInfo()
        info.chainId shouldBe 2
        info.blockHeight shouldBe "5000000"
        aptos.close()
    }

    @Test
    fun `getBalance via facade`() = runTest {
        val aptos = mockAptos {
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "type": "0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>",
                        "data": {"coin": {"value": "1000000000"}, "frozen": false}
                    }
                    """.trimIndent()
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val balance = aptos.getBalance(AccountAddress.ONE)
        balance shouldBe 1_000_000_000uL
        aptos.close()
    }

    @Test
    fun `full flow - generate account, build transfer, sign`() = runTest {
        // This test doesn't need network - just building and signing locally
        val account = Ed25519Account.generate()
        val recipient = AccountAddress.fromHexRelaxed("0xdeadbeef")

        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1000uL)
        val rawTxn = TransactionBuilder.builder()
            .sender(account.address)
            .sequenceNumber(0uL)
            .payload(payload)
            .maxGasAmount(200_000uL)
            .gasUnitPrice(100uL)
            .expirationTimestampSecs(1700000000uL)
            .chainId(ChainId.TESTNET)
            .build()

        val signedTxn = TransactionBuilder.signTransaction(account, rawTxn)
        signedTxn.rawTransaction.sender shouldBe account.address
        signedTxn.toSubmitBytes().size shouldNotBe 0
        signedTxn.hash().size shouldBe 32
    }

    @Test
    fun `full flow - Secp256k1 account sign`() = runTest {
        val account = Secp256k1Account.generate()
        val recipient = AccountAddress.fromHexRelaxed("0xcafe")

        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 500uL)
        val rawTxn = TransactionBuilder.builder()
            .sender(account.address)
            .sequenceNumber(0uL)
            .payload(payload)
            .maxGasAmount(200_000uL)
            .gasUnitPrice(100uL)
            .expirationTimestampSecs(1700000000uL)
            .chainId(ChainId.TESTNET)
            .build()

        val signedTxn = TransactionBuilder.signTransaction(account, rawTxn)
        signedTxn.rawTransaction.sender shouldBe account.address
        signedTxn.toSubmitBytes().size shouldNotBe 0
    }

    @Test
    fun `transfer convenience method`() = runTest {
        var requestCount = 0
        val aptos = mockAptos { request ->
            requestCount++
            val path = request.url.encodedPath
            when {
                path.contains("/accounts/") -> respond(
                    content = ByteReadChannel(
                        """{"sequence_number": "5", "authentication_key": "0x01"}"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> respond(
                    content = ByteReadChannel(
                        """
                        {
                            "chain_id": 2, "epoch": "1", "ledger_version": "1",
                            "oldest_ledger_version": "0", "ledger_timestamp": "0",
                            "node_role": "full_node", "oldest_block_height": "0",
                            "block_height": "1"
                        }
                        """.trimIndent()
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }

        val sender = Ed25519Account.generate()
        val recipient = AccountAddress.fromHexRelaxed("0xbob".replace("bob", "b0b"))
        val signedTxn = aptos.transfer(sender, recipient, 1000uL)
        signedTxn.rawTransaction.sequenceNumber shouldBe 5uL
        signedTxn.rawTransaction.chainId shouldBe ChainId.TESTNET
        aptos.close()
    }

    @Test
    fun `builder creates custom Aptos instance`() {
        val aptos = Aptos.builder()
            .nodeUrl("http://localhost:8080/v1")
            .faucetUrl("http://localhost:8081")
            .chainId(ChainId.LOCAL)
            .timeoutMs(5000)
            .build()

        aptos.config.nodeUrl shouldBe "http://localhost:8080/v1"
        aptos.config.faucetUrl shouldBe "http://localhost:8081"
        aptos.config.chainId shouldBe ChainId.LOCAL
        aptos.config.timeoutMs shouldBe 5000
        aptos.close()
    }

    @Test
    fun `static factory methods`() {
        val testnet = Aptos.testnet()
        testnet.config.chainId shouldBe ChainId.TESTNET
        testnet.close()

        val mainnet = Aptos.mainnet()
        mainnet.config.chainId shouldBe ChainId.MAINNET
        mainnet.close()
    }
}
