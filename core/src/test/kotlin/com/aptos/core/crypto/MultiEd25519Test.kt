package com.aptos.core.crypto

import com.aptos.core.bcs.BcsSerializer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MultiEd25519Test {

    private fun generateKeys(n: Int): List<Ed25519.PrivateKey> = (0 until n).map { Ed25519.PrivateKey.generate() }

    @Test
    fun `public key validates threshold`() {
        val keys = generateKeys(3).map { it.publicKey() }
        val pk = MultiEd25519.PublicKey(keys, 2u)
        pk.threshold shouldBe 2.toUByte()
        pk.keys.size shouldBe 3
    }

    @Test
    fun `public key rejects threshold greater than key count`() {
        val keys = generateKeys(2).map { it.publicKey() }
        shouldThrow<IllegalArgumentException> {
            MultiEd25519.PublicKey(keys, 3u)
        }
    }

    @Test
    fun `public key rejects empty key list`() {
        shouldThrow<IllegalArgumentException> {
            MultiEd25519.PublicKey(emptyList(), 1u)
        }
    }

    @Test
    fun `public key rejects more than MAX_KEYS`() {
        val keys = generateKeys(33).map { it.publicKey() }
        shouldThrow<IllegalArgumentException> {
            MultiEd25519.PublicKey(keys, 1u)
        }
    }

    @Test
    fun `public key toBytes produces correct length`() {
        val keys = generateKeys(3).map { it.publicKey() }
        val pk = MultiEd25519.PublicKey(keys, 2u)
        val bytes = pk.toBytes()
        bytes.size shouldBe (3 * Ed25519.PUBLIC_KEY_LENGTH + 1)
        bytes.last() shouldBe 2.toByte()
    }

    @Test
    fun `auth key is derived correctly`() {
        val keys = generateKeys(3).map { it.publicKey() }
        val pk = MultiEd25519.PublicKey(keys, 2u)
        val authKey = pk.authKey()
        authKey.data.size shouldBe 32

        // Verify it matches manual computation
        val input = pk.toBytes() + byteArrayOf(SignatureScheme.MULTI_ED25519.id)
        val expected = Hashing.sha3256(input)
        authKey.data shouldBe expected
    }

    @Test
    fun `auth key matches AuthenticationKey fromMultiEd25519`() {
        val keys = generateKeys(3).map { it.publicKey() }
        val pk = MultiEd25519.PublicKey(keys, 2u)
        pk.authKey() shouldBe AuthenticationKey.fromMultiEd25519(pk)
    }

    @Test
    fun `bitmap fromIndices sets correct bits`() {
        val bitmap = MultiEd25519.Bitmap.fromIndices(listOf(0, 2, 4), 5)
        val bytes = bitmap.toBytes()
        // Bit 0 → bit 31 in int → 0x80000000
        // Bit 2 → bit 29 in int → 0x20000000
        // Bit 4 → bit 27 in int → 0x08000000
        // Combined: 0xA8000000
        val expected = (0x80000000.toInt() or 0x20000000 or 0x08000000)
        bitmap.bits shouldBe expected
        bytes.size shouldBe 4
    }

    @Test
    fun `bitmap rejects unsorted indices`() {
        shouldThrow<IllegalArgumentException> {
            MultiEd25519.Bitmap.fromIndices(listOf(2, 0), 3)
        }
    }

    @Test
    fun `bitmap rejects duplicate indices`() {
        shouldThrow<IllegalArgumentException> {
            MultiEd25519.Bitmap.fromIndices(listOf(0, 0), 3)
        }
    }

    @Test
    fun `bitmap rejects out of range index`() {
        shouldThrow<IllegalArgumentException> {
            MultiEd25519.Bitmap.fromIndices(listOf(0, 5), 3)
        }
    }

    @Test
    fun `signature BCS serialization produces correct bytes`() {
        val privKeys = generateKeys(3)
        val message = "hello multi".toByteArray()
        val sigs = listOf(0, 2).map { i ->
            MultiEd25519.IndexedSignature(i, privKeys[i].sign(message))
        }
        val bitmap = MultiEd25519.Bitmap.fromIndices(listOf(0, 2), 3)
        val multiSig = MultiEd25519.Signature(sigs, bitmap)
        val bytes = multiSig.toBytes()
        bytes.size shouldBe (2 * Ed25519.SIGNATURE_LENGTH + MultiEd25519.BITMAP_LENGTH)
    }

    @Test
    fun `signature BCS via serializer`() {
        val privKeys = generateKeys(2)
        val message = "test".toByteArray()
        val sigs = listOf(0, 1).map { i ->
            MultiEd25519.IndexedSignature(i, privKeys[i].sign(message))
        }
        val bitmap = MultiEd25519.Bitmap.fromIndices(listOf(0, 1), 2)
        val multiSig = MultiEd25519.Signature(sigs, bitmap)

        val serializer = BcsSerializer()
        multiSig.serialize(serializer)
        val bcsBytes = serializer.toByteArray()
        // BCS: ULEB128 length prefix (2 bytes for 132) + raw bytes
        bcsBytes.size shouldBe (2 + 2 * Ed25519.SIGNATURE_LENGTH + MultiEd25519.BITMAP_LENGTH)
    }

    @Test
    fun `verify succeeds with sufficient signatures`() {
        val privKeys = generateKeys(3)
        val pubKeys = privKeys.map { it.publicKey() }
        val multiPk = MultiEd25519.PublicKey(pubKeys, 2u)
        val message = "verify me".toByteArray()

        val sigs = listOf(0, 1).map { i ->
            MultiEd25519.IndexedSignature(i, privKeys[i].sign(message))
        }
        val bitmap = MultiEd25519.Bitmap.fromIndices(listOf(0, 1), 3)
        val multiSig = MultiEd25519.Signature(sigs, bitmap)

        MultiEd25519.verify(multiPk, message, multiSig) shouldBe true
    }

    @Test
    fun `verify fails with insufficient signatures`() {
        val privKeys = generateKeys(3)
        val pubKeys = privKeys.map { it.publicKey() }
        val multiPk = MultiEd25519.PublicKey(pubKeys, 2u)
        val message = "verify me".toByteArray()

        val sigs = listOf(MultiEd25519.IndexedSignature(0, privKeys[0].sign(message)))
        val bitmap = MultiEd25519.Bitmap.fromIndices(listOf(0), 3)
        val multiSig = MultiEd25519.Signature(sigs, bitmap)

        MultiEd25519.verify(multiPk, message, multiSig) shouldBe false
    }

    @Test
    fun `verify fails with wrong message`() {
        val privKeys = generateKeys(2)
        val pubKeys = privKeys.map { it.publicKey() }
        val multiPk = MultiEd25519.PublicKey(pubKeys, 2u)

        val sigs = listOf(0, 1).map { i ->
            MultiEd25519.IndexedSignature(i, privKeys[i].sign("correct".toByteArray()))
        }
        val bitmap = MultiEd25519.Bitmap.fromIndices(listOf(0, 1), 2)
        val multiSig = MultiEd25519.Signature(sigs, bitmap)

        MultiEd25519.verify(multiPk, "wrong".toByteArray(), multiSig) shouldBe false
    }
}
