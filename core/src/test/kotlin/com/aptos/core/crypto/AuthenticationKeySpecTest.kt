package com.aptos.core.crypto

import com.aptos.core.types.HexString
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Spec-based tests for AuthenticationKey validated against official aptos-sdk-specs test vectors.
 */
class AuthenticationKeySpecTest {
    // -- Ed25519 auth key derivation from known public key --

    @Test
    fun `Ed25519 auth key from known public key matches TypeScript SDK`() {
        // Validated against Aptos TypeScript SDK v2
        val publicKey =
            Ed25519.PublicKey.fromHex(
                "0xea526ba1710343d953461ff68641f1b7df5f23b9042ffa2d2a798d3adb3f3d6c",
            )
        val authKey = AuthenticationKey.fromEd25519(publicKey)
        authKey.toHex() shouldBe "0x07968dab936c1bad187c60ce4082f307d030d780e91e694ae03aef16aba73f30"
    }

    @Test
    fun `Ed25519 auth key derivation formula is SHA3-256 of pubkey plus 0x00`() {
        val publicKey =
            Ed25519.PublicKey.fromHex(
                "0xea526ba1710343d953461ff68641f1b7df5f23b9042ffa2d2a798d3adb3f3d6c",
            )
        // Manual computation: SHA3-256(pubkey_bytes || 0x00)
        val input = publicKey.data + byteArrayOf(0x00)
        val expectedHash = Hashing.sha3256(input)
        val authKey = AuthenticationKey.fromEd25519(publicKey)
        authKey.data shouldBe expectedHash
    }

    // -- Derived address equals auth key --

    @Test
    fun `derived address equals authentication key bytes`() {
        // Validated against Aptos TypeScript SDK v2
        val publicKey =
            Ed25519.PublicKey.fromHex(
                "0xea526ba1710343d953461ff68641f1b7df5f23b9042ffa2d2a798d3adb3f3d6c",
            )
        val authKey = AuthenticationKey.fromEd25519(publicKey)
        val address = authKey.derivedAddress()
        address.toHex() shouldBe "0x07968dab936c1bad187c60ce4082f307d030d780e91e694ae03aef16aba73f30"
    }

    // -- Secp256k1 auth key uses uncompressed key + 0x01 --

    @Test
    fun `Secp256k1 auth key uses uncompressed public key plus 0x01`() {
        val seed = ByteArray(32)
        seed[31] = 0x01
        val privateKey = Secp256k1.PrivateKey(seed)
        val publicKey = privateKey.publicKey()

        // Manual computation: SHA3-256(65_byte_uncompressed_pubkey || 0x01)
        val uncompressed = publicKey.uncompressed()
        uncompressed.size shouldBe 65
        val input = uncompressed + byteArrayOf(0x01)
        val expectedHash = Hashing.sha3256(input)

        val authKey = AuthenticationKey.fromSecp256k1(publicKey)
        authKey.data shouldBe expectedHash
    }

    // -- Auth key is always 32 bytes --

    @Test
    fun `authentication key is always 32 bytes`() {
        AuthenticationKey.LENGTH shouldBe 32
    }

    // -- Scheme identifiers --

    @Test
    fun `Ed25519 scheme id is 0x00`() {
        SignatureScheme.ED25519.id shouldBe 0x00.toByte()
    }

    @Test
    fun `Secp256k1 scheme id is 0x01`() {
        SignatureScheme.SECP256K1.id shouldBe 0x01.toByte()
    }

    // -- Different schemes produce different auth keys for same public key bytes --

    @Test
    fun `different schemes produce different auth keys for same bytes`() {
        val publicKeyBytes =
            HexString.decode(
                "ea526ba1710343d953461ff68641f1b7df5f23b9042ffa2d2a798d3adb3f3d6c",
            )
        val authKeyEd = AuthenticationKey.fromPublicKey(publicKeyBytes, SignatureScheme.ED25519)
        val authKeySecp = AuthenticationKey.fromPublicKey(publicKeyBytes, SignatureScheme.SECP256K1)
        (authKeyEd == authKeySecp) shouldBe false
    }

    // -- End-to-end: seed -> private key -> public key -> auth key --

    @Test
    fun `end-to-end Ed25519 derivation chain`() {
        // Validated against Aptos TypeScript SDK v2
        val seed = HexString.decode("82001573a003fd3b7fd72ffb0eaf63aac62f12deb629dca72785a66268ec758b")
        val privateKey = Ed25519.PrivateKey(seed)
        val publicKey = privateKey.publicKey()
        publicKey.toHex() shouldBe "0x664f6e8f36eacb1770fa879d86c2c1d0fafea145e84fa7d671ab7a011a54d509"

        // Public Key -> Auth Key
        val authKey = AuthenticationKey.fromEd25519(publicKey)
        authKey.toHex() shouldBe "0x231a656a51c1792efdb10f42ddbca0f80468de5bb622c235a681b898929cecf7"

        // Auth Key -> Address (they are the same for new accounts)
        val address = authKey.derivedAddress()
        address.toHex() shouldBe "0x231a656a51c1792efdb10f42ddbca0f80468de5bb622c235a681b898929cecf7"
    }
}
