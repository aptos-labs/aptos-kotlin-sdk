package com.aptos.core.account

import com.aptos.core.error.MnemonicException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KeyDerivationTest {
    private val testMnemonic =
        Mnemonic.fromPhrase(
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
        )

    // --- SLIP-0010 (Ed25519) ---

    @Test
    fun `SLIP-0010 derives 32-byte key`() {
        val seed = testMnemonic.toSeed()
        val key = Slip0010.deriveEd25519(seed, DerivationPath.DEFAULT_APTOS)
        key.size shouldBe 32
    }

    @Test
    fun `SLIP-0010 derivation is deterministic`() {
        val seed = testMnemonic.toSeed()
        val key1 = Slip0010.deriveEd25519(seed, DerivationPath.DEFAULT_APTOS)
        val key2 = Slip0010.deriveEd25519(seed, DerivationPath.DEFAULT_APTOS)
        key1 shouldBe key2
    }

    @Test
    fun `SLIP-0010 different paths produce different keys`() {
        val seed = testMnemonic.toSeed()
        val key1 = Slip0010.deriveEd25519(seed, DerivationPath.parse("m/44'/637'/0'/0'/0'"))
        val key2 = Slip0010.deriveEd25519(seed, DerivationPath.parse("m/44'/637'/0'/0'/1'"))
        (key1.contentEquals(key2)) shouldBe false
    }

    @Test
    fun `SLIP-0010 different seeds produce different keys`() {
        val seed1 = testMnemonic.toSeed()
        val seed2 = testMnemonic.toSeed("passphrase")
        val key1 = Slip0010.deriveEd25519(seed1, DerivationPath.DEFAULT_APTOS)
        val key2 = Slip0010.deriveEd25519(seed2, DerivationPath.DEFAULT_APTOS)
        (key1.contentEquals(key2)) shouldBe false
    }

    @Test
    fun `SLIP-0010 rejects non-hardened paths`() {
        val seed = testMnemonic.toSeed()
        shouldThrow<MnemonicException> {
            Slip0010.deriveEd25519(seed, DerivationPath.parse("m/44'/637'/0'/0/0"))
        }
    }

    // --- BIP-32 (Secp256k1) ---

    @Test
    fun `BIP-32 derives 32-byte key`() {
        val seed = testMnemonic.toSeed()
        val key = Bip32.deriveSecp256k1(seed, DerivationPath.DEFAULT_APTOS)
        key.size shouldBe 32
    }

    @Test
    fun `BIP-32 derivation is deterministic`() {
        val seed = testMnemonic.toSeed()
        val key1 = Bip32.deriveSecp256k1(seed, DerivationPath.DEFAULT_APTOS)
        val key2 = Bip32.deriveSecp256k1(seed, DerivationPath.DEFAULT_APTOS)
        key1 shouldBe key2
    }

    @Test
    fun `BIP-32 different paths produce different keys`() {
        val seed = testMnemonic.toSeed()
        val key1 = Bip32.deriveSecp256k1(seed, DerivationPath.parse("m/44'/637'/0'/0'/0'"))
        val key2 = Bip32.deriveSecp256k1(seed, DerivationPath.parse("m/44'/637'/0'/0'/1'"))
        (key1.contentEquals(key2)) shouldBe false
    }

    @Test
    fun `BIP-32 supports non-hardened derivation`() {
        val seed = testMnemonic.toSeed()
        val key = Bip32.deriveSecp256k1(seed, DerivationPath.parse("m/44'/637'/0'/0/0"))
        key.size shouldBe 32
    }

    @Test
    fun `BIP-32 different seeds produce different keys`() {
        val seed1 = testMnemonic.toSeed()
        val seed2 = testMnemonic.toSeed("passphrase")
        val key1 = Bip32.deriveSecp256k1(seed1, DerivationPath.DEFAULT_APTOS)
        val key2 = Bip32.deriveSecp256k1(seed2, DerivationPath.DEFAULT_APTOS)
        (key1.contentEquals(key2)) shouldBe false
    }

    // --- Cross-scheme ---

    @Test
    fun `SLIP-0010 and BIP-32 produce different keys from same seed`() {
        val seed = testMnemonic.toSeed()
        val edKey = Slip0010.deriveEd25519(seed, DerivationPath.DEFAULT_APTOS)
        val secpKey = Bip32.deriveSecp256k1(seed, DerivationPath.DEFAULT_APTOS)
        (edKey.contentEquals(secpKey)) shouldBe false
    }
}
