package com.aptos.core.transaction

import com.aptos.core.account.Ed25519Account
import com.aptos.core.account.Secp256k1Account
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.crypto.Hashing
import com.aptos.core.error.TransactionBuildException
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId
import com.aptos.core.types.MoveModuleId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class TransactionTest {
    private val testAccount = Ed25519Account.generate()
    private val recipient = AccountAddress.fromHexRelaxed("0xBOB".replace("BOB", "b0b"))

    @Test
    fun `build raw transaction`() {
        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1000uL)
        val rawTxn =
            TransactionBuilder
                .builder()
                .sender(testAccount.address)
                .sequenceNumber(0uL)
                .payload(payload)
                .chainId(ChainId.TESTNET)
                .build()

        rawTxn.sender shouldBe testAccount.address
        rawTxn.sequenceNumber shouldBe 0uL
        rawTxn.maxGasAmount shouldBe 200_000uL
        rawTxn.gasUnitPrice shouldBe 100uL
        rawTxn.chainId shouldBe ChainId.TESTNET
    }

    @Test
    fun `builder validates required fields`() {
        shouldThrow<TransactionBuildException> {
            TransactionBuilder.builder().build()
        }
    }

    @Test
    fun `builder validates sender required`() {
        shouldThrow<TransactionBuildException> {
            TransactionBuilder
                .builder()
                .sequenceNumber(0uL)
                .payload(TransactionPayload.EntryFunction.aptTransfer(recipient, 100uL))
                .chainId(ChainId.TESTNET)
                .build()
        }
    }

    @Test
    fun `raw transaction BCS serialization`() {
        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1000uL)
        val rawTxn =
            RawTransaction(
                sender = testAccount.address,
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )

        val bytes = rawTxn.toBcs()
        bytes.size shouldNotBe 0
    }

    @Test
    fun `signing message starts with RAW_TRANSACTION_PREFIX`() {
        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1000uL)
        val rawTxn =
            RawTransaction(
                sender = testAccount.address,
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )

        val signingMessage = rawTxn.signingMessage()
        val prefix = signingMessage.copyOfRange(0, 32)
        prefix shouldBe Hashing.RAW_TRANSACTION_PREFIX
    }

    @Test
    fun `sign transaction with Ed25519`() {
        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1000uL)
        val rawTxn =
            RawTransaction(
                sender = testAccount.address,
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )

        val signedTxn = TransactionBuilder.signTransaction(testAccount, rawTxn)
        signedTxn.rawTransaction shouldBe rawTxn

        // Verify the signature is valid
        val auth = signedTxn.authenticator as TransactionAuthenticator.Ed25519Auth
        val isValid = auth.publicKey.verify(rawTxn.signingMessage(), auth.signature)
        isValid shouldBe true
    }

    @Test
    fun `sign transaction with Secp256k1`() {
        val secpAccount = Secp256k1Account.generate()
        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1000uL)
        val rawTxn =
            RawTransaction(
                sender = secpAccount.address,
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )

        val signedTxn = TransactionBuilder.signTransaction(secpAccount, rawTxn)
        signedTxn.authenticator shouldNotBe null
        val auth = signedTxn.authenticator as TransactionAuthenticator.SingleSender
        auth.accountAuthenticator shouldNotBe null
    }

    @Test
    fun `signed transaction produces submit bytes`() {
        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1000uL)
        val rawTxn =
            RawTransaction(
                sender = testAccount.address,
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )

        val signedTxn = TransactionBuilder.signTransaction(testAccount, rawTxn)
        val bytes = signedTxn.toSubmitBytes()
        bytes.size shouldNotBe 0
    }

    @Test
    fun `signed transaction hash is 32 bytes`() {
        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1000uL)
        val rawTxn =
            RawTransaction(
                sender = testAccount.address,
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )

        val signedTxn = TransactionBuilder.signTransaction(testAccount, rawTxn)
        signedTxn.hash().size shouldBe 32
    }

    @Test
    fun `entry function aptTransfer`() {
        val ef = TransactionPayload.EntryFunction.aptTransfer(recipient, 5000uL)
        ef.moduleId shouldBe MoveModuleId(AccountAddress.ONE, "aptos_account")
        ef.functionName shouldBe "transfer"
        ef.typeArgs shouldBe emptyList()
        ef.args.size shouldBe 2
    }

    @Test
    fun `entry function serialization deterministic`() {
        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1000uL)
        val s1 = BcsSerializer()
        payload.serialize(s1)
        val s2 = BcsSerializer()
        payload.serialize(s2)
        s1.toByteArray() shouldBe s2.toByteArray()
    }

    @Test
    fun `builder fluent API with sign`() {
        val signedTxn =
            TransactionBuilder
                .builder()
                .sender(testAccount.address)
                .sequenceNumber(5uL)
                .payload(TransactionPayload.EntryFunction.aptTransfer(recipient, 100uL))
                .maxGasAmount(50_000uL)
                .gasUnitPrice(200uL)
                .expirationTimestampSecs(1700000000uL)
                .chainId(ChainId.TESTNET)
                .sign(testAccount)

        signedTxn.rawTransaction.sequenceNumber shouldBe 5uL
        signedTxn.rawTransaction.maxGasAmount shouldBe 50_000uL
        signedTxn.rawTransaction.gasUnitPrice shouldBe 200uL
    }
}
