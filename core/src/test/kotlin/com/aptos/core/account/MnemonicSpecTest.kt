package com.aptos.core.account

import com.aptos.core.error.MnemonicException
import com.aptos.core.types.HexString
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

/**
 * Spec-based tests for mnemonic derivation validated against official aptos-sdk-specs test vectors.
 */
@Suppress("MaxLineLength")
class MnemonicSpecTest {
    private val testMnemonic12 =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

    private val testMnemonic24 =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon art"

    // -- BIP-39 seed derivation --

    @Test
    fun `12-word mnemonic produces expected BIP-39 seed`() {
        val mnemonic = Mnemonic.fromPhrase(testMnemonic12)
        val seed = mnemonic.toSeed()
        seed.size shouldBe 64
        HexString.encode(seed) shouldBe
            "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc1" +
            "9a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4"
    }

    // -- Full derivation chain: mnemonic -> private key -> public key -> auth key -> address --

    @Test
    fun `full derivation chain from 12-word mnemonic matches TypeScript SDK`() {
        val mnemonic = Mnemonic.fromPhrase(testMnemonic12)
        val account = Ed25519Account.fromMnemonic(mnemonic)

        // Validated against Aptos TypeScript SDK v2
        account.privateKey.toHex() shouldBe "0xcc92c0eaf80206d817f150e21917f797e49cf644a33ac514de3c316baa2f1bf5"
        account.publicKey.toHex() shouldBe "0xa686f0309ab80312979606cfccc10ea2740147ae6888351488d11c46f08fbf60"
        account.address.toHex() shouldBe "0xeb663b681209e7087d681c5d3eed12aaa8e1915e7c87794542c3f96e94b3d3bf"
    }

    // -- Default derivation path --

    @Test
    fun `default derivation path is m_44_637_0_0_0`() {
        DerivationPath.DEFAULT_APTOS.toString() shouldBe "m/44'/637'/0'/0'/0'"
    }

    @Test
    fun `Aptos BIP-44 coin type is 637`() {
        val path = DerivationPath.DEFAULT_APTOS
        path.components[1].index shouldBe 637u
    }

    // -- Different account indices produce different keys --

    @Test
    fun `different account indices produce different addresses`() {
        val mnemonic = Mnemonic.fromPhrase(testMnemonic12)
        val path0 = DerivationPath.parse("m/44'/637'/0'/0'/0'")
        val path1 = DerivationPath.parse("m/44'/637'/0'/0'/1'")

        val account0 = Ed25519Account.fromMnemonic(mnemonic, path0)
        val account1 = Ed25519Account.fromMnemonic(mnemonic, path1)

        account0.address shouldNotBe account1.address
        account0.privateKey shouldNotBe account1.privateKey
    }

    // -- Passphrase affects seed --

    @Test
    fun `passphrase produces different seed and keys`() {
        val mnemonic = Mnemonic.fromPhrase(testMnemonic12)
        val seedNoPass = mnemonic.toSeed("")
        val seedWithPass = mnemonic.toSeed("TREZOR")

        (seedNoPass.contentEquals(seedWithPass)) shouldBe false
    }

    // -- 24-word mnemonic produces different keys --

    @Test
    fun `24-word mnemonic produces different keys than 12-word`() {
        val mnemonic12 = Mnemonic.fromPhrase(testMnemonic12)
        val mnemonic24 = Mnemonic.fromPhrase(testMnemonic24)

        val account12 = Ed25519Account.fromMnemonic(mnemonic12)
        val account24 = Ed25519Account.fromMnemonic(mnemonic24)

        account12.address shouldNotBe account24.address
    }

    // -- Valid word counts --

    @Test
    fun `valid word counts are 12, 15, 18, 21, 24`() {
        for (count in listOf(12, 15, 18, 21, 24)) {
            val mnemonic = Mnemonic.generate(count)
            mnemonic.wordCount() shouldBe count
        }
    }

    // -- Invalid mnemonics --

    @Test
    fun `11 words is rejected`() {
        shouldThrow<MnemonicException> {
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon",
            )
        }
    }

    @Test
    fun `13 words is rejected`() {
        shouldThrow<MnemonicException> {
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about about",
            )
        }
    }

    @Test
    fun `invalid word is rejected`() {
        shouldThrow<MnemonicException> {
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon xyz",
            )
        }
    }

    @Test
    fun `wrong checksum mnemonic is rejected`() {
        shouldThrow<MnemonicException> {
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon",
            )
        }
    }

    @Test
    fun `isValid returns false for invalid word`() {
        Mnemonic.isValid(
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon xyz",
        ) shouldBe false
    }

    @Test
    fun `isValid returns true for valid 12-word phrase`() {
        Mnemonic.isValid(testMnemonic12) shouldBe true
    }

    // -- Entropy -> mnemonic -> seed is deterministic --

    @Test
    fun `same mnemonic always produces same seed`() {
        val m1 = Mnemonic.fromPhrase(testMnemonic12)
        val m2 = Mnemonic.fromPhrase(testMnemonic12)
        m1.toSeed() shouldBe m2.toSeed()
    }

    // -- PBKDF2 parameters --

    @Test
    fun `seed is 64 bytes from PBKDF2-HMAC-SHA512`() {
        val mnemonic = Mnemonic.fromPhrase(testMnemonic12)
        val seed = mnemonic.toSeed()
        seed.size shouldBe 64
    }
}
