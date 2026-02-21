package com.aptos.core.account

import com.aptos.core.crypto.Ed25519
import com.aptos.core.crypto.MultiEd25519
import com.aptos.core.crypto.SignatureScheme
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MultiEd25519AccountTest {

    private fun generateAccounts(n: Int): List<Ed25519Account> = (0 until n).map { Ed25519Account.generate() }

    @Test
    fun `create multi-ed25519 account with 2-of-3`() {
        val accounts = generateAccounts(3)
        val allPubKeys = accounts.map { it.publicKey }
        val signers = listOf(accounts[0], accounts[1])
        val signerIndices = listOf(0, 1)

        val multiAccount = MultiEd25519Account.create(signers, signerIndices, allPubKeys, 2u)
        multiAccount.scheme shouldBe SignatureScheme.MULTI_ED25519
        multiAccount.multiPublicKey.keys.size shouldBe 3
        multiAccount.multiPublicKey.threshold shouldBe 2.toUByte()
    }

    @Test
    fun `address is derived from multi-ed25519 auth key`() {
        val accounts = generateAccounts(3)
        val allPubKeys = accounts.map { it.publicKey }
        val multiAccount = MultiEd25519Account.create(
            listOf(accounts[0], accounts[2]),
            listOf(0, 2),
            allPubKeys,
            2u,
        )
        val expectedAuthKey = MultiEd25519.PublicKey(allPubKeys, 2u).authKey()
        multiAccount.address shouldBe expectedAuthKey.derivedAddress()
    }

    @Test
    fun `sign produces valid multi-signature`() {
        val accounts = generateAccounts(3)
        val allPubKeys = accounts.map { it.publicKey }
        val signers = listOf(accounts[0], accounts[2])
        val signerIndices = listOf(0, 2)

        val multiAccount = MultiEd25519Account.create(signers, signerIndices, allPubKeys, 2u)
        val message = "hello multi account".toByteArray()
        val sigBytes = multiAccount.sign(message)

        // Should be 2 signatures + 4-byte bitmap
        sigBytes.size shouldBe (2 * Ed25519.SIGNATURE_LENGTH + MultiEd25519.BITMAP_LENGTH)
    }

    @Test
    fun `signMultiEd25519 returns typed signature`() {
        val accounts = generateAccounts(3)
        val allPubKeys = accounts.map { it.publicKey }
        val multiAccount = MultiEd25519Account.create(
            listOf(accounts[0], accounts[1]),
            listOf(0, 1),
            allPubKeys,
            2u,
        )
        val message = "typed sign".toByteArray()
        val multiSig = multiAccount.signMultiEd25519(message)

        multiSig.signatures.size shouldBe 2
        MultiEd25519.verify(multiAccount.multiPublicKey, message, multiSig) shouldBe true
    }

    @Test
    fun `create rejects mismatched signers and indices`() {
        val accounts = generateAccounts(3)
        shouldThrow<IllegalArgumentException> {
            MultiEd25519Account.create(
                listOf(accounts[0]),
                listOf(0, 1),
                accounts.map { it.publicKey },
                1u,
            )
        }
    }

    @Test
    fun `create rejects insufficient signers for threshold`() {
        val accounts = generateAccounts(3)
        shouldThrow<IllegalArgumentException> {
            MultiEd25519Account.create(
                listOf(accounts[0]),
                listOf(0),
                accounts.map { it.publicKey },
                2u,
            )
        }
    }

    @Test
    fun `publicKeyBytes returns multi-ed25519 public key bytes`() {
        val accounts = generateAccounts(2)
        val allPubKeys = accounts.map { it.publicKey }
        val multiAccount = MultiEd25519Account.create(
            accounts,
            listOf(0, 1),
            allPubKeys,
            2u,
        )
        multiAccount.publicKeyBytes.size shouldBe (2 * Ed25519.PUBLIC_KEY_LENGTH + 1)
    }
}
