package com.aptos.core.crypto

import com.aptos.core.types.HexString
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Spec-based tests for Ed25519 validated against official aptos-sdk-specs test vectors.
 */
class Ed25519SpecTest {
    // -- Key derivation from seed --

    @Test
    fun `key from seed 000_001 produces expected public key`() {
        val seed = ByteArray(32)
        seed[31] = 0x01
        val privateKey = Ed25519.PrivateKey(seed)
        val publicKey = privateKey.publicKey()
        publicKey.toHex() shouldBe "0x4cb5abf6ad79fbf5abbccafcc269d85cd2651ed4b885b5869f241aedf0a5ba29"
    }

    @Test
    fun `key from random seed produces expected public key`() {
        // Validated against Aptos TypeScript SDK v2
        val seed = HexString.decode("82001573a003fd3b7fd72ffb0eaf63aac62f12deb629dca72785a66268ec758b")
        val privateKey = Ed25519.PrivateKey(seed)
        val publicKey = privateKey.publicKey()
        publicKey.toHex() shouldBe "0x664f6e8f36eacb1770fa879d86c2c1d0fafea145e84fa7d671ab7a011a54d509"
    }

    // -- Signing vectors --

    @Test
    fun `sign hello with seed 000_001 produces expected signature`() {
        val seed = ByteArray(32)
        seed[31] = 0x01
        val privateKey = Ed25519.PrivateKey(seed)
        val signature = privateKey.sign("hello".toByteArray())
        signature.toHex() shouldBe
            "0xc6cb9b70e0ee28cea926c251aa06b131b51dcdc52b6cc05df6235a56478852a5" +
            "c3f737b12c3f4fca6e020c714100e712c1c22cb0402e9ef446aa2669831a9106"
    }

    @Test
    fun `sign empty message with seed 000_001 produces expected signature`() {
        val seed = ByteArray(32)
        seed[31] = 0x01
        val privateKey = Ed25519.PrivateKey(seed)
        val signature = privateKey.sign(byteArrayOf())
        signature.toHex() shouldBe
            "0x7e1b9dc1e332c4238edcd07a68101474b640fdcb1b7b84fb711ac4bfbc85eb85" +
            "a77480950d69398dcd19f61e1ea74d0f183cfbf34df8f6e7733ebfb9f944f106"
    }

    @Test
    fun `sign long message with seed 000_001 produces expected signature`() {
        val seed = ByteArray(32)
        seed[31] = 0x01
        val privateKey = Ed25519.PrivateKey(seed)
        val message = "The quick brown fox jumps over the lazy dog".toByteArray()
        val signature = privateKey.sign(message)
        signature.toHex() shouldBe
            "0xb22f61ee39fca21a76bc01e6fedae2648e727020dcb092cc76af83aa9ea6f087" +
            "6f843114f100bc1a0fb17be126cdb31438fbd9b0cb1583b6d00fcf31842b3d0c"
    }

    // -- Verification vectors --

    @Test
    fun `verify hello signature with known public key`() {
        val publicKey =
            Ed25519.PublicKey.fromHex(
                "0x4cb5abf6ad79fbf5abbccafcc269d85cd2651ed4b885b5869f241aedf0a5ba29",
            )
        val signature =
            Ed25519.Signature.fromHex(
                "0xc6cb9b70e0ee28cea926c251aa06b131b51dcdc52b6cc05df6235a56478852a5" +
                    "c3f737b12c3f4fca6e020c714100e712c1c22cb0402e9ef446aa2669831a9106",
            )
        publicKey.verify("hello".toByteArray(), signature) shouldBe true
    }

    @Test
    fun `verify empty message signature with known public key`() {
        val publicKey =
            Ed25519.PublicKey.fromHex(
                "0x4cb5abf6ad79fbf5abbccafcc269d85cd2651ed4b885b5869f241aedf0a5ba29",
            )
        val signature =
            Ed25519.Signature.fromHex(
                "0x7e1b9dc1e332c4238edcd07a68101474b640fdcb1b7b84fb711ac4bfbc85eb85" +
                    "a77480950d69398dcd19f61e1ea74d0f183cfbf34df8f6e7733ebfb9f944f106",
            )
        publicKey.verify(byteArrayOf(), signature) shouldBe true
    }

    @Test
    fun `verify long message signature with known public key`() {
        val publicKey =
            Ed25519.PublicKey.fromHex(
                "0x4cb5abf6ad79fbf5abbccafcc269d85cd2651ed4b885b5869f241aedf0a5ba29",
            )
        val signature =
            Ed25519.Signature.fromHex(
                "0xb22f61ee39fca21a76bc01e6fedae2648e727020dcb092cc76af83aa9ea6f087" +
                    "6f843114f100bc1a0fb17be126cdb31438fbd9b0cb1583b6d00fcf31842b3d0c",
            )
        val message = "The quick brown fox jumps over the lazy dog".toByteArray()
        publicKey.verify(message, signature) shouldBe true
    }

    @Test
    fun `verify rejects wrong message for known signature`() {
        val publicKey =
            Ed25519.PublicKey.fromHex(
                "0x4cb5abf6ad79fbf5abbccafcc269d85cd2651ed4b885b5869f241aedf0a5ba29",
            )
        val signature =
            Ed25519.Signature.fromHex(
                "0xc6cb9b70e0ee28cea926c251aa06b131b51dcdc52b6cc05df6235a56478852a5" +
                    "c3f737b12c3f4fca6e020c714100e712c1c22cb0402e9ef446aa2669831a9106",
            )
        publicKey.verify("wrong".toByteArray(), signature) shouldBe false
    }

    // -- Key length constants --

    @Test
    fun `private key length is 32`() {
        Ed25519.PRIVATE_KEY_LENGTH shouldBe 32
    }

    @Test
    fun `public key length is 32`() {
        Ed25519.PUBLIC_KEY_LENGTH shouldBe 32
    }

    @Test
    fun `signature length is 64`() {
        Ed25519.SIGNATURE_LENGTH shouldBe 64
    }

    // -- Scheme identifier --

    @Test
    fun `Ed25519 scheme identifier is 0x00`() {
        SignatureScheme.ED25519.id shouldBe 0x00.toByte()
    }
}
