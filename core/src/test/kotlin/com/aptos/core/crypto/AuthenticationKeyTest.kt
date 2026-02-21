package com.aptos.core.crypto

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class AuthenticationKeyTest {
    @Test
    fun `derive authentication key from Ed25519 public key`() {
        val privateKey = Ed25519.PrivateKey.generate()
        val publicKey = privateKey.publicKey()
        val authKey = AuthenticationKey.fromEd25519(publicKey)
        authKey.data.size shouldBe AuthenticationKey.LENGTH
    }

    @Test
    fun `derive authentication key from Secp256k1 public key`() {
        val privateKey = Secp256k1.PrivateKey.generate()
        val publicKey = privateKey.publicKey()
        val authKey = AuthenticationKey.fromSecp256k1(publicKey)
        authKey.data.size shouldBe AuthenticationKey.LENGTH
    }

    @Test
    fun `derived address matches authentication key bytes`() {
        val privateKey = Ed25519.PrivateKey.generate()
        val publicKey = privateKey.publicKey()
        val authKey = AuthenticationKey.fromEd25519(publicKey)
        val address = authKey.derivedAddress()
        address.data shouldBe authKey.data
    }

    @Test
    fun `different keys produce different auth keys`() {
        val key1 = Ed25519.PrivateKey.generate()
        val key2 = Ed25519.PrivateKey.generate()
        val authKey1 = AuthenticationKey.fromEd25519(key1.publicKey())
        val authKey2 = AuthenticationKey.fromEd25519(key2.publicKey())
        authKey1 shouldNotBe authKey2
    }

    @Test
    fun `same key always produces same auth key`() {
        val privateKey = Ed25519.PrivateKey.generate()
        val publicKey = privateKey.publicKey()
        val authKey1 = AuthenticationKey.fromEd25519(publicKey)
        val authKey2 = AuthenticationKey.fromEd25519(publicKey)
        authKey1 shouldBe authKey2
    }

    @Test
    fun `Ed25519 and Secp256k1 produce different auth keys for same-length input`() {
        // Even if we use the same bytes, different scheme IDs should produce different auth keys
        val privateKey = Ed25519.PrivateKey.generate()
        val publicKey = privateKey.publicKey()
        val authKeyEd = AuthenticationKey.fromPublicKey(publicKey.data, SignatureScheme.ED25519)
        val authKeySecp = AuthenticationKey.fromPublicKey(publicKey.data, SignatureScheme.SECP256K1)
        authKeyEd shouldNotBe authKeySecp
    }
}
