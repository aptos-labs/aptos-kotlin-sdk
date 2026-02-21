package com.aptos.core.crypto

import com.aptos.core.types.HexString
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Spec-based tests for Secp256k1 validated against official aptos-sdk-specs test vectors.
 */
class Secp256k1SpecTest {
    // -- Key derivation --

    @Test
    fun `key from 000_001 produces expected uncompressed public key`() {
        val seed = ByteArray(32)
        seed[31] = 0x01
        val privateKey = Secp256k1.PrivateKey(seed)
        val publicKey = privateKey.publicKey()
        publicKey.data.size shouldBe Secp256k1.PUBLIC_KEY_UNCOMPRESSED_LENGTH
        publicKey.toHex() shouldBe
            "0x0479be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798" +
            "483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8"
    }

    @Test
    fun `compressed key from 000_001 is 33 bytes starting with 02 or 03`() {
        val seed = ByteArray(32)
        seed[31] = 0x01
        val privateKey = Secp256k1.PrivateKey(seed)
        val compressed = privateKey.publicKey().compressed()
        compressed.size shouldBe Secp256k1.PUBLIC_KEY_COMPRESSED_LENGTH
        (compressed[0] == 0x02.toByte() || compressed[0] == 0x03.toByte()) shouldBe true
    }

    // -- Signing vectors --

    @Test
    fun `sign hello with seed 000_001 produces expected signature`() {
        val seed = ByteArray(32)
        seed[31] = 0x01
        val privateKey = Secp256k1.PrivateKey(seed)
        val signature = privateKey.sign("hello".toByteArray())
        signature.toHex() shouldBe
            "0x24ea2d66a639d2b8c973094911a1b5d9ccdb6ee48d94487ecda2407edbbf6f45" +
            "13bed23538ec23b13926dc510694564e20639fc4edea1c6c02558d8d2dd24d8c"
    }

    // -- Verification vectors --

    @Test
    fun `verify hello signature with known public key`() {
        val publicKey =
            Secp256k1.PublicKey(
                HexString.decode(
                    "0479be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798" +
                        "483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8",
                ),
            )
        val signature =
            Secp256k1.Signature(
                HexString.decode(
                    "24ea2d66a639d2b8c973094911a1b5d9ccdb6ee48d94487ecda2407edbbf6f45" +
                        "13bed23538ec23b13926dc510694564e20639fc4edea1c6c02558d8d2dd24d8c",
                ),
            )
        publicKey.verify("hello".toByteArray(), signature) shouldBe true
    }

    @Test
    fun `verify rejects wrong message for known signature`() {
        val publicKey =
            Secp256k1.PublicKey(
                HexString.decode(
                    "0479be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798" +
                        "483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8",
                ),
            )
        val signature =
            Secp256k1.Signature(
                HexString.decode(
                    "24ea2d66a639d2b8c973094911a1b5d9ccdb6ee48d94487ecda2407edbbf6f45" +
                        "13bed23538ec23b13926dc510694564e20639fc4edea1c6c02558d8d2dd24d8c",
                ),
            )
        publicKey.verify("wrong".toByteArray(), signature) shouldBe false
    }

    // -- Low-S normalization --

    @Test
    fun `signature from spec has low-S`() {
        val sigBytes =
            HexString.decode(
                "24ea2d66a639d2b8c973094911a1b5d9ccdb6ee48d94487ecda2407edbbf6f45" +
                    "13bed23538ec23b13926dc510694564e20639fc4edea1c6c02558d8d2dd24d8c",
            )
        val s = java.math.BigInteger(1, sigBytes.copyOfRange(32, 64))
        val curveOrder =
            org.bouncycastle.crypto.ec.CustomNamedCurves
                .getByName("secp256k1")
                .n
        val halfN = curveOrder.shiftRight(1)
        (s <= halfN) shouldBe true
    }

    // -- Key length constants --

    @Test
    fun `private key length is 32`() {
        Secp256k1.PRIVATE_KEY_LENGTH shouldBe 32
    }

    @Test
    fun `uncompressed public key length is 65`() {
        Secp256k1.PUBLIC_KEY_UNCOMPRESSED_LENGTH shouldBe 65
    }

    @Test
    fun `compressed public key length is 33`() {
        Secp256k1.PUBLIC_KEY_COMPRESSED_LENGTH shouldBe 33
    }

    @Test
    fun `signature length is 64`() {
        Secp256k1.SIGNATURE_LENGTH shouldBe 64
    }

    // -- Scheme identifier --

    @Test
    fun `Secp256k1 scheme identifier is 0x01`() {
        SignatureScheme.SECP256K1.id shouldBe 0x01.toByte()
    }
}
