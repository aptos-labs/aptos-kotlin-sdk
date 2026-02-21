package com.aptos.core.crypto

import com.aptos.core.bcs.BcsSerializer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class MultiKeyTest {

    @Test
    fun `create multi-key with mixed Ed25519 and Secp256k1`() {
        val ed25519Key = Ed25519.PrivateKey.generate().publicKey()
        val secp256k1Key = Secp256k1.PrivateKey.generate().publicKey()

        val multiPk = MultiKey.PublicKey(
            listOf(
                MultiKey.AnyPublicKey.ed25519(ed25519Key),
                MultiKey.AnyPublicKey.secp256k1(secp256k1Key),
            ),
            2u,
        )
        multiPk.keys.size shouldBe 2
        multiPk.sigsRequired shouldBe 2.toUByte()
    }

    @Test
    fun `auth key derivation produces 32 bytes`() {
        val ed25519Key = Ed25519.PrivateKey.generate().publicKey()
        val secp256k1Key = Secp256k1.PrivateKey.generate().publicKey()

        val multiPk = MultiKey.PublicKey(
            listOf(
                MultiKey.AnyPublicKey.ed25519(ed25519Key),
                MultiKey.AnyPublicKey.secp256k1(secp256k1Key),
            ),
            1u,
        )
        val authKey = multiPk.authKey()
        authKey.data.size shouldBe 32
    }

    @Test
    fun `auth key matches manual computation`() {
        val ed25519Key = Ed25519.PrivateKey.generate().publicKey()

        val multiPk = MultiKey.PublicKey(
            listOf(MultiKey.AnyPublicKey.ed25519(ed25519Key)),
            1u,
        )
        val authKey = multiPk.authKey()

        // Manually compute: SHA3-256(BCS(publicKey) || 0x03)
        val serializer = BcsSerializer()
        multiPk.serialize(serializer)
        val expected = Hashing.sha3256(serializer.toByteArray() + byteArrayOf(0x03))
        authKey.data shouldBe expected
    }

    @Test
    fun `different keys produce different auth keys`() {
        val key1 = MultiKey.PublicKey(
            listOf(MultiKey.AnyPublicKey.ed25519(Ed25519.PrivateKey.generate().publicKey())),
            1u,
        )
        val key2 = MultiKey.PublicKey(
            listOf(MultiKey.AnyPublicKey.ed25519(Ed25519.PrivateKey.generate().publicKey())),
            1u,
        )
        key1.authKey() shouldNotBe key2.authKey()
    }

    @Test
    fun `rejects empty key list`() {
        shouldThrow<IllegalArgumentException> {
            MultiKey.PublicKey(emptyList(), 1u)
        }
    }

    @Test
    fun `rejects threshold exceeding key count`() {
        val key = MultiKey.AnyPublicKey.ed25519(Ed25519.PrivateKey.generate().publicKey())
        shouldThrow<IllegalArgumentException> {
            MultiKey.PublicKey(listOf(key), 2u)
        }
    }

    @Test
    fun `BCS serialization of public key`() {
        val ed25519Key = Ed25519.PrivateKey.generate().publicKey()
        val multiPk = MultiKey.PublicKey(
            listOf(MultiKey.AnyPublicKey.ed25519(ed25519Key)),
            1u,
        )
        val serializer = BcsSerializer()
        multiPk.serialize(serializer)
        val bytes = serializer.toByteArray()
        // ULEB128(1) + variant_index(0) + bytes(32-byte key with ULEB128 len) + u8(1)
        bytes.size shouldBe (1 + 1 + 1 + Ed25519.PUBLIC_KEY_LENGTH + 1)
    }

    @Test
    fun `BCS serialization of signature`() {
        val privKey = Ed25519.PrivateKey.generate()
        val sig = privKey.sign("test".toByteArray())
        val anySig = MultiKey.AnySignature.ed25519(sig)
        val bitmap = MultiEd25519.Bitmap.fromIndices(listOf(0), 1)
        val multiSig = MultiKey.Signature(listOf(anySig), bitmap)

        val serializer = BcsSerializer()
        multiSig.serialize(serializer)
        val bytes = serializer.toByteArray()
        // ULEB128(1) + variant_index(0) + bytes(64-byte sig with ULEB128 len) + bitmap(4 bytes)
        bytes.size shouldBe (1 + 1 + 1 + Ed25519.SIGNATURE_LENGTH + MultiEd25519.BITMAP_LENGTH)
    }

    @Test
    fun `AnyPublicKey factory methods set correct types`() {
        val ed25519Key = Ed25519.PrivateKey.generate().publicKey()
        val secp256k1Key = Secp256k1.PrivateKey.generate().publicKey()

        val anyEd = MultiKey.AnyPublicKey.ed25519(ed25519Key)
        anyEd.type shouldBe MultiKey.KEY_TYPE_ED25519

        val anySecp = MultiKey.AnyPublicKey.secp256k1(secp256k1Key)
        anySecp.type shouldBe MultiKey.KEY_TYPE_SECP256K1
    }

    @Test
    fun `AnySignature factory methods set correct types`() {
        val edSig = Ed25519.PrivateKey.generate().sign("test".toByteArray())
        val anyEdSig = MultiKey.AnySignature.ed25519(edSig)
        anyEdSig.type shouldBe MultiKey.KEY_TYPE_ED25519

        val secpSig = Secp256k1.PrivateKey.generate().sign("test".toByteArray())
        val anySecpSig = MultiKey.AnySignature.secp256k1(secpSig)
        anySecpSig.type shouldBe MultiKey.KEY_TYPE_SECP256K1
    }
}
