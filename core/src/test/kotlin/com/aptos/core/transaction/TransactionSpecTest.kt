package com.aptos.core.transaction

import com.aptos.core.account.Ed25519Account
import com.aptos.core.account.Mnemonic
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.crypto.Hashing
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId
import com.aptos.core.types.HexString
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

/**
 * Spec-based tests for transaction building and signing validated against
 * official aptos-sdk-specs test vectors.
 */
class TransactionSpecTest {
    // -- Chain ID constants --

    @Test
    fun `mainnet chain ID is 1`() {
        ChainId.MAINNET.value shouldBe 1u.toUByte()
    }

    @Test
    fun `testnet chain ID is 2`() {
        ChainId.TESTNET.value shouldBe 2u.toUByte()
    }

    @Test
    fun `local chain ID is 4`() {
        ChainId.LOCAL.value shouldBe 4u.toUByte()
    }

    // -- Signing message construction --

    @Test
    fun `signing message starts with RAW_TRANSACTION_PREFIX`() {
        val sender =
            AccountAddress.fromHexRelaxed(
                "0x9c3a0eeb9f91075eefa4d1f58c02e59e9d34e41320a3ccb357e6a5c7bfa540fa",
            )
        val payload =
            TransactionPayload.EntryFunction.aptTransfer(
                AccountAddress.fromHexRelaxed("0x2"),
                1_000_000uL,
            )
        val rawTxn =
            RawTransaction(
                sender = sender,
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
    fun `signing message prefix is SHA3-256 of APTOS RawTransaction`() {
        HexString.encode(Hashing.RAW_TRANSACTION_PREFIX) shouldBe
            "b5e97db07fa0bd0e5598aa3643a9bc6f6693bddc1a9fec9e674a461eaa00b193"
    }

    // -- BCS field order for RawTransaction --

    @Test
    fun `RawTransaction BCS starts with 32-byte sender address`() {
        val sender = AccountAddress.fromHexRelaxed("0x1")
        val payload =
            TransactionPayload.EntryFunction.aptTransfer(
                AccountAddress.fromHexRelaxed("0x2"),
                100uL,
            )
        val rawTxn =
            RawTransaction(
                sender = sender,
                sequenceNumber = 5uL,
                payload = payload,
                maxGasAmount = 100_000uL,
                gasUnitPrice = 150uL,
                expirationTimestampSecs = 1700001000uL,
                chainId = ChainId.MAINNET,
            )

        val bcs = rawTxn.toBcs()
        // First 32 bytes should be the sender address
        val senderBytes = bcs.copyOfRange(0, 32)
        senderBytes shouldBe sender.data
    }

    @Test
    fun `RawTransaction BCS ends with chain ID byte`() {
        val payload =
            TransactionPayload.EntryFunction.aptTransfer(
                AccountAddress.fromHexRelaxed("0x2"),
                100uL,
            )
        val rawTxn =
            RawTransaction(
                sender = AccountAddress.fromHexRelaxed("0x1"),
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )

        val bcs = rawTxn.toBcs()
        // Last byte should be chain ID (2 for testnet)
        bcs.last() shouldBe 0x02.toByte()
    }

    // -- TransactionPayload variant indices --

    @Test
    fun `EntryFunction payload BCS variant index is 2`() {
        val payload =
            TransactionPayload.EntryFunction.aptTransfer(
                AccountAddress.fromHexRelaxed("0x2"),
                100uL,
            )
        val serializer = BcsSerializer()
        payload.serialize(serializer)
        val bytes = serializer.toByteArray()
        // First byte is ULEB128-encoded variant index 2
        bytes[0].toInt() and 0xFF shouldBe 2
    }

    @Test
    fun `Script payload BCS variant index is 0`() {
        val payload =
            TransactionPayload.Script(
                code = byteArrayOf(0x01, 0x02),
                typeArgs = emptyList(),
                args = emptyList(),
            )
        val serializer = BcsSerializer()
        payload.serialize(serializer)
        val bytes = serializer.toByteArray()
        bytes[0].toInt() and 0xFF shouldBe 0
    }

    @Test
    fun `Multisig payload BCS variant index is 3`() {
        val payload =
            TransactionPayload.Multisig(
                multisigAddress = AccountAddress.fromHexRelaxed("0x3"),
                entryFunction = null,
            )
        val serializer = BcsSerializer()
        payload.serialize(serializer)
        val bytes = serializer.toByteArray()
        bytes[0].toInt() and 0xFF shouldBe 3
    }

    // -- TransactionAuthenticator variant indices --

    @Test
    fun `Ed25519Auth authenticator BCS variant index is 0`() {
        val mnemonic =
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            )
        val account = Ed25519Account.fromMnemonic(mnemonic)
        val payload =
            TransactionPayload.EntryFunction.aptTransfer(
                AccountAddress.fromHexRelaxed("0x2"),
                100uL,
            )
        val rawTxn =
            RawTransaction(
                sender = account.address,
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )
        val signedTxn = TransactionBuilder.signTransaction(account, rawTxn)
        val serializer = BcsSerializer()
        signedTxn.authenticator.serialize(serializer)
        val authBytes = serializer.toByteArray()
        // Ed25519Auth variant index is 0
        authBytes[0].toInt() and 0xFF shouldBe 0
    }

    // -- Transaction hash --

    @Test
    fun `transaction hash uses TRANSACTION_PREFIX`() {
        HexString.encode(Hashing.TRANSACTION_PREFIX) shouldBe
            "fa210a9417ef3e7fa45bfa1d17a8dbd4d883711910a550d265fee189e9266dd4"
    }

    @Test
    fun `signed transaction hash is 32 bytes`() {
        val mnemonic =
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            )
        val account = Ed25519Account.fromMnemonic(mnemonic)
        val payload =
            TransactionPayload.EntryFunction.aptTransfer(
                AccountAddress.fromHexRelaxed("0x2"),
                1_000_000uL,
            )
        val rawTxn =
            RawTransaction(
                sender = account.address,
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )
        val signedTxn = TransactionBuilder.signTransaction(account, rawTxn)
        signedTxn.hash().size shouldBe 32
    }

    // -- Deterministic signing with known key --

    @Test
    fun `signing with known mnemonic account is deterministic`() {
        val mnemonic =
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            )
        val account = Ed25519Account.fromMnemonic(mnemonic)
        val payload =
            TransactionPayload.EntryFunction.aptTransfer(
                AccountAddress.fromHexRelaxed("0x2"),
                1_000_000uL,
            )
        val rawTxn =
            RawTransaction(
                sender = account.address,
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )
        val signed1 = TransactionBuilder.signTransaction(account, rawTxn)
        val signed2 = TransactionBuilder.signTransaction(account, rawTxn)
        signed1.toSubmitBytes() shouldBe signed2.toSubmitBytes()
        signed1.hash() shouldBe signed2.hash()
    }

    // -- APT transfer payload structure --

    @Test
    fun `aptTransfer uses 0x1 aptos_account transfer`() {
        val recipient = AccountAddress.fromHexRelaxed("0x2")
        val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1_000_000uL)
        payload.moduleId.address shouldBe AccountAddress.ONE
        payload.moduleId.name shouldBe "aptos_account"
        payload.functionName shouldBe "transfer"
        payload.typeArgs shouldBe emptyList()
        payload.args.size shouldBe 2
    }

    @Test
    fun `aptTransfer amount arg is BCS-encoded u64`() {
        val payload =
            TransactionPayload.EntryFunction.aptTransfer(
                AccountAddress.fromHexRelaxed("0x2"),
                1_000_000uL,
            )
        // Second arg is the BCS-encoded amount
        val amountBytes = payload.args[1]
        // 1_000_000 in little-endian u64: 0x40420f0000000000
        amountBytes shouldBe
            byteArrayOf(
                0x40,
                0x42,
                0x0F,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
            )
    }

    // -- SignedTransaction submit bytes --

    @Test
    fun `submit bytes are non-empty and deterministic`() {
        val mnemonic =
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            )
        val account = Ed25519Account.fromMnemonic(mnemonic)
        val payload =
            TransactionPayload.EntryFunction.aptTransfer(
                AccountAddress.fromHexRelaxed("0x2"),
                100uL,
            )
        val rawTxn =
            RawTransaction(
                sender = account.address,
                sequenceNumber = 0uL,
                payload = payload,
                maxGasAmount = 200_000uL,
                gasUnitPrice = 100uL,
                expirationTimestampSecs = 1700000000uL,
                chainId = ChainId.TESTNET,
            )
        val signed = TransactionBuilder.signTransaction(account, rawTxn)
        val bytes = signed.toSubmitBytes()
        bytes.size shouldNotBe 0
    }
}
