package com.aptos.core.transaction

import com.aptos.core.account.Ed25519Account
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.crypto.Hashing
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class MultiAgentTransactionTest {

    @Test
    fun `multi-agent signing message uses RawTransactionWithData prefix`() {
        val sender = Ed25519Account.generate()
        val secondary = Ed25519Account.generate()

        val rawTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .build()

        val rawWithData = RawTransactionWithData.MultiAgent(rawTxn, listOf(secondary.address))
        val signingMessage = rawWithData.signingMessage()

        // Must start with RAW_TRANSACTION_WITH_DATA_PREFIX
        val prefix = signingMessage.copyOfRange(0, 32)
        prefix shouldBe Hashing.RAW_TRANSACTION_WITH_DATA_PREFIX
    }

    @Test
    fun `multi-agent BCS serialization includes variant index and addresses`() {
        val sender = Ed25519Account.generate()
        val secondary = Ed25519Account.generate()

        val rawTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .build()

        val rawWithData = RawTransactionWithData.MultiAgent(rawTxn, listOf(secondary.address))
        val serializer = BcsSerializer()
        rawWithData.serialize(serializer)
        val bytes = serializer.toByteArray()

        // First byte should be variant index 0 (MultiAgent)
        bytes[0] shouldBe 0.toByte()
    }

    @Test
    fun `sign with secondary signers produces MultiAgent authenticator`() {
        val sender = Ed25519Account.generate()
        val secondary = Ed25519Account.generate()

        val signedTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .secondarySigners(listOf(secondary))
            .sign(sender)

        signedTxn.authenticator.shouldBeInstanceOf<TransactionAuthenticator.MultiAgent>()
        val auth = signedTxn.authenticator as TransactionAuthenticator.MultiAgent
        auth.secondarySignerAddresses.size shouldBe 1
        auth.secondarySignerAddresses[0] shouldBe secondary.address
        auth.secondarySigners.size shouldBe 1
    }

    @Test
    fun `multi-agent with multiple secondary signers`() {
        val sender = Ed25519Account.generate()
        val secondary1 = Ed25519Account.generate()
        val secondary2 = Ed25519Account.generate()

        val signedTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .secondarySigners(listOf(secondary1, secondary2))
            .sign(sender)

        val auth = signedTxn.authenticator as TransactionAuthenticator.MultiAgent
        auth.secondarySignerAddresses.size shouldBe 2
        auth.secondarySigners.size shouldBe 2
    }

    @Test
    fun `multi-agent transaction serializes for submission`() {
        val sender = Ed25519Account.generate()
        val secondary = Ed25519Account.generate()

        val signedTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(0uL)
            .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
            .chainId(ChainId.TESTNET)
            .secondarySigners(listOf(secondary))
            .sign(sender)

        val submitBytes = signedTxn.toSubmitBytes()
        submitBytes.isNotEmpty() shouldBe true
    }
}
