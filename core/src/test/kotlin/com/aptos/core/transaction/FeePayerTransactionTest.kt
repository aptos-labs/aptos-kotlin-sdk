package com.aptos.core.transaction

import com.aptos.core.account.Ed25519Account
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.crypto.Hashing
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class FeePayerTransactionTest {

    @Test
    fun `fee-payer signing message uses RawTransactionWithData prefix`() {
        val sender = Ed25519Account.generate()
        val feePayer = Ed25519Account.generate()

        val rawTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .build()

        val rawWithData = RawTransactionWithData.FeePayer(
            rawTxn,
            emptyList(),
            feePayer.address,
        )
        val signingMessage = rawWithData.signingMessage()
        val prefix = signingMessage.copyOfRange(0, 32)
        prefix shouldBe Hashing.RAW_TRANSACTION_WITH_DATA_PREFIX
    }

    @Test
    fun `fee-payer BCS serialization includes variant index 1`() {
        val sender = Ed25519Account.generate()
        val feePayer = Ed25519Account.generate()

        val rawTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .build()

        val rawWithData = RawTransactionWithData.FeePayer(
            rawTxn,
            emptyList(),
            feePayer.address,
        )
        val serializer = BcsSerializer()
        rawWithData.serialize(serializer)
        val bytes = serializer.toByteArray()

        // First byte should be variant index 1 (FeePayer)
        bytes[0] shouldBe 1.toByte()
    }

    @Test
    fun `sign with fee payer produces FeePayer authenticator`() {
        val sender = Ed25519Account.generate()
        val feePayer = Ed25519Account.generate()

        val signedTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .feePayer(feePayer)
            .sign(sender)

        signedTxn.authenticator.shouldBeInstanceOf<TransactionAuthenticator.FeePayer>()
        val auth = signedTxn.authenticator as TransactionAuthenticator.FeePayer
        auth.feePayerAddress shouldBe feePayer.address
        auth.secondarySignerAddresses.size shouldBe 0
        auth.secondarySigners.size shouldBe 0
    }

    @Test
    fun `fee payer with secondary signers`() {
        val sender = Ed25519Account.generate()
        val secondary = Ed25519Account.generate()
        val feePayer = Ed25519Account.generate()

        val signedTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .secondarySigners(listOf(secondary))
            .feePayer(feePayer)
            .sign(sender)

        val auth = signedTxn.authenticator as TransactionAuthenticator.FeePayer
        auth.feePayerAddress shouldBe feePayer.address
        auth.secondarySignerAddresses.size shouldBe 1
        auth.secondarySignerAddresses[0] shouldBe secondary.address
        auth.secondarySigners.size shouldBe 1
    }

    @Test
    fun `fee-payer transaction serializes for submission`() {
        val sender = Ed25519Account.generate()
        val feePayer = Ed25519Account.generate()

        val signedTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .feePayer(feePayer)
            .sign(sender)

        val submitBytes = signedTxn.toSubmitBytes()
        submitBytes.isNotEmpty() shouldBe true
    }

    @Test
    fun `all parties sign the same message`() {
        val sender = Ed25519Account.generate()
        val secondary = Ed25519Account.generate()
        val feePayer = Ed25519Account.generate()

        val rawTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .build()

        val rawWithData = RawTransactionWithData.FeePayer(
            rawTxn,
            listOf(secondary.address),
            feePayer.address,
        )
        val signingMessage = rawWithData.signingMessage()

        // All parties get the same signing message
        val senderSig = sender.sign(signingMessage)
        val secondarySig = secondary.sign(signingMessage)
        val feePayerSig = feePayer.sign(signingMessage)

        senderSig.isNotEmpty() shouldBe true
        secondarySig.isNotEmpty() shouldBe true
        feePayerSig.isNotEmpty() shouldBe true
    }
}
