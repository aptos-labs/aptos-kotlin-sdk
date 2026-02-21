package com.aptos.core.crypto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class Secp256k1Test {
    @Test
    fun `generate key pair`() {
        val privateKey = Secp256k1.PrivateKey.generate()
        privateKey.data.size shouldBe Secp256k1.PRIVATE_KEY_LENGTH
        val publicKey = privateKey.publicKey()
        publicKey.data.size shouldBe Secp256k1.PUBLIC_KEY_UNCOMPRESSED_LENGTH
    }

    @Test
    fun `sign and verify`() {
        val privateKey = Secp256k1.PrivateKey.generate()
        val publicKey = privateKey.publicKey()
        val message = "hello aptos".toByteArray()
        val signature = privateKey.sign(message)
        signature.data.size shouldBe Secp256k1.SIGNATURE_LENGTH
        publicKey.verify(message, signature) shouldBe true
    }

    @Test
    fun `verify rejects wrong message`() {
        val privateKey = Secp256k1.PrivateKey.generate()
        val publicKey = privateKey.publicKey()
        val signature = privateKey.sign("correct message".toByteArray())
        publicKey.verify("wrong message".toByteArray(), signature) shouldBe false
    }

    @Test
    fun `verify rejects wrong key`() {
        val privateKey1 = Secp256k1.PrivateKey.generate()
        val privateKey2 = Secp256k1.PrivateKey.generate()
        val message = "test".toByteArray()
        val signature = privateKey1.sign(message)
        privateKey2.publicKey().verify(message, signature) shouldBe false
    }

    @Test
    fun `compressed and uncompressed conversions`() {
        val privateKey = Secp256k1.PrivateKey.generate()
        val publicKey = privateKey.publicKey()

        val compressed = publicKey.compressed()
        compressed.size shouldBe Secp256k1.PUBLIC_KEY_COMPRESSED_LENGTH

        val uncompressed = publicKey.uncompressed()
        uncompressed.size shouldBe Secp256k1.PUBLIC_KEY_UNCOMPRESSED_LENGTH

        // Roundtrip: compressed -> PublicKey -> uncompressed should equal original
        val fromCompressed = Secp256k1.PublicKey(compressed)
        fromCompressed.uncompressed() shouldBe publicKey.uncompressed()
    }

    @Test
    fun `from hex roundtrip`() {
        val original = Secp256k1.PrivateKey.generate()
        val hex = original.toHex()
        val restored = Secp256k1.PrivateKey.fromHex(hex)
        restored shouldBe original
    }

    @Test
    fun `low-S normalization`() {
        // Sign multiple messages and verify all signatures have low-S
        val privateKey = Secp256k1.PrivateKey.generate()
        repeat(10) { i ->
            val message = "message $i".toByteArray()
            val signature = privateKey.sign(message)
            val s = java.math.BigInteger(1, signature.data.copyOfRange(32, 64))
            val curveOrder =
                org.bouncycastle.crypto.ec.CustomNamedCurves
                    .getByName("secp256k1")
                    .n
            val halfN = curveOrder.shiftRight(1)
            (s <= halfN) shouldBe true
        }
    }

    @Test
    fun `deterministic RFC 6979 signing`() {
        val privateKey = Secp256k1.PrivateKey.generate()
        val message = "deterministic test".toByteArray()
        val sig1 = privateKey.sign(message)
        val sig2 = privateKey.sign(message)
        sig1 shouldBe sig2
    }

    @Test
    fun `reject zero private key`() {
        shouldThrow<IllegalArgumentException> {
            Secp256k1.PrivateKey(ByteArray(32))
        }
    }

    @Test
    fun `reject private key equal to curve order`() {
        val n = org.bouncycastle.crypto.ec.CustomNamedCurves.getByName("secp256k1").n
        val nBytes =
            n.toByteArray().let {
                if (it.size == 33 && it[0] == 0.toByte()) it.copyOfRange(1, 33) else it
            }
        nBytes.size shouldBe 32
        shouldThrow<IllegalArgumentException> {
            Secp256k1.PrivateKey(nBytes)
        }
    }
}
