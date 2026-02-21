package com.aptos.core.crypto

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class Ed25519Test {

    @Test
    fun `generate key pair`() {
        val privateKey = Ed25519.PrivateKey.generate()
        privateKey.data.size shouldBe Ed25519.PRIVATE_KEY_LENGTH
        val publicKey = privateKey.publicKey()
        publicKey.data.size shouldBe Ed25519.PUBLIC_KEY_LENGTH
    }

    @Test
    fun `sign and verify`() {
        val privateKey = Ed25519.PrivateKey.generate()
        val publicKey = privateKey.publicKey()
        val message = "hello aptos".toByteArray()
        val signature = privateKey.sign(message)
        signature.data.size shouldBe Ed25519.SIGNATURE_LENGTH
        publicKey.verify(message, signature) shouldBe true
    }

    @Test
    fun `verify rejects wrong message`() {
        val privateKey = Ed25519.PrivateKey.generate()
        val publicKey = privateKey.publicKey()
        val signature = privateKey.sign("correct message".toByteArray())
        publicKey.verify("wrong message".toByteArray(), signature) shouldBe false
    }

    @Test
    fun `verify rejects wrong key`() {
        val privateKey1 = Ed25519.PrivateKey.generate()
        val privateKey2 = Ed25519.PrivateKey.generate()
        val message = "test".toByteArray()
        val signature = privateKey1.sign(message)
        privateKey2.publicKey().verify(message, signature) shouldBe false
    }

    @Test
    fun `from hex roundtrip`() {
        val original = Ed25519.PrivateKey.generate()
        val hex = original.toHex()
        val restored = Ed25519.PrivateKey.fromHex(hex)
        restored shouldBe original
    }

    @Test
    fun `from seed creates deterministic key`() {
        val seed = ByteArray(32) { it.toByte() }
        val key1 = Ed25519.PrivateKey.fromSeed(seed)
        val key2 = Ed25519.PrivateKey.fromSeed(seed)
        key1 shouldBe key2
    }

    @Test
    fun `deterministic signing`() {
        val seed = ByteArray(32) { it.toByte() }
        val privateKey = Ed25519.PrivateKey.fromSeed(seed)
        val message = "deterministic test".toByteArray()
        val sig1 = privateKey.sign(message)
        val sig2 = privateKey.sign(message)
        sig1 shouldBe sig2
    }

    @Test
    fun `public key from hex roundtrip`() {
        val privateKey = Ed25519.PrivateKey.generate()
        val publicKey = privateKey.publicKey()
        val hex = publicKey.toHex()
        val restored = Ed25519.PublicKey.fromHex(hex)
        restored shouldBe publicKey
    }

    @Test
    fun `different keys produce different public keys`() {
        val key1 = Ed25519.PrivateKey.generate()
        val key2 = Ed25519.PrivateKey.generate()
        key1.publicKey() shouldNotBe key2.publicKey()
    }
}
