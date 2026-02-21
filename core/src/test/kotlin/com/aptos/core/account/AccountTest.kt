package com.aptos.core.account

import com.aptos.core.crypto.SignatureScheme
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class AccountTest {
    @Test
    fun `Ed25519Account generate`() {
        val account = Ed25519Account.generate()
        account.scheme shouldBe SignatureScheme.ED25519
        account.publicKeyBytes.size shouldBe 32
        account.address.data.size shouldBe 32
    }

    @Test
    fun `Ed25519Account sign and verify`() {
        val account = Ed25519Account.generate()
        val message = "test message".toByteArray()
        val signature = account.sign(message)
        signature.size shouldBe 64
    }

    @Test
    fun `Ed25519Account from mnemonic is deterministic`() {
        val mnemonic =
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            )
        val account1 = Ed25519Account.fromMnemonic(mnemonic)
        val account2 = Ed25519Account.fromMnemonic(mnemonic)
        account1.address shouldBe account2.address
        account1.publicKeyBytes shouldBe account2.publicKeyBytes
    }

    @Test
    fun `Ed25519Account from mnemonic with different paths gives different accounts`() {
        val mnemonic =
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            )
        val account1 = Ed25519Account.fromMnemonic(mnemonic, DerivationPath.parse("m/44'/637'/0'/0'/0'"))
        val account2 = Ed25519Account.fromMnemonic(mnemonic, DerivationPath.parse("m/44'/637'/0'/0'/1'"))
        account1.address shouldNotBe account2.address
    }

    @Test
    fun `Secp256k1Account generate`() {
        val account = Secp256k1Account.generate()
        account.scheme shouldBe SignatureScheme.SECP256K1
        account.publicKeyBytes.size shouldBe 65 // uncompressed
        account.address.data.size shouldBe 32
    }

    @Test
    fun `Secp256k1Account sign and verify`() {
        val account = Secp256k1Account.generate()
        val message = "test message".toByteArray()
        val signature = account.sign(message)
        signature.size shouldBe 64
    }

    @Test
    fun `Secp256k1Account from mnemonic is deterministic`() {
        val mnemonic =
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            )
        val account1 = Secp256k1Account.fromMnemonic(mnemonic)
        val account2 = Secp256k1Account.fromMnemonic(mnemonic)
        account1.address shouldBe account2.address
    }

    @Test
    fun `AnyAccount wrapping Ed25519`() {
        val ed = Ed25519Account.generate()
        val any = AnyAccount.from(ed)
        any.address shouldBe ed.address
        any.scheme shouldBe SignatureScheme.ED25519
    }

    @Test
    fun `AnyAccount wrapping Secp256k1`() {
        val secp = Secp256k1Account.generate()
        val any = AnyAccount.from(secp)
        any.address shouldBe secp.address
        any.scheme shouldBe SignatureScheme.SECP256K1
    }

    @Test
    fun `authentication key derives correct address`() {
        val account = Ed25519Account.generate()
        val authKey = account.authenticationKey
        authKey.derivedAddress() shouldBe account.address
    }
}
