package com.aptos.core.account

import com.aptos.core.error.MnemonicException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class MnemonicTest {
    @Test
    fun `generate 12-word mnemonic`() {
        val mnemonic = Mnemonic.generate(12)
        mnemonic.wordCount() shouldBe 12
    }

    @Test
    fun `generate 24-word mnemonic`() {
        val mnemonic = Mnemonic.generate(24)
        mnemonic.wordCount() shouldBe 24
    }

    @Test
    fun `from phrase parses valid mnemonic`() {
        val phrase = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val mnemonic = Mnemonic.fromPhrase(phrase)
        mnemonic.wordCount() shouldBe 12
        mnemonic.phrase() shouldBe phrase
    }

    @Test
    fun `from phrase rejects invalid word`() {
        val phrase = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon zzzzz"
        shouldThrow<MnemonicException> {
            Mnemonic.fromPhrase(phrase)
        }
    }

    @Test
    fun `from phrase rejects wrong word count`() {
        shouldThrow<MnemonicException> {
            Mnemonic.fromPhrase("abandon abandon abandon")
        }
    }

    @Test
    fun `toSeed produces 64-byte seed`() {
        val mnemonic =
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            )
        val seed = mnemonic.toSeed()
        seed.size shouldBe 64
    }

    @Test
    fun `toSeed is deterministic`() {
        val mnemonic =
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            )
        val seed1 = mnemonic.toSeed()
        val seed2 = mnemonic.toSeed()
        seed1 shouldBe seed2
    }

    @Test
    fun `toSeed with passphrase differs from without`() {
        val mnemonic =
            Mnemonic.fromPhrase(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            )
        val seedNoPass = mnemonic.toSeed()
        val seedWithPass = mnemonic.toSeed("my passphrase")
        (seedNoPass.contentEquals(seedWithPass)) shouldBe false
    }

    @Test
    fun `isValid returns true for valid phrase`() {
        Mnemonic.isValid(
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
        ) shouldBe true
    }

    @Test
    fun `isValid returns false for invalid phrase`() {
        Mnemonic.isValid("not a valid mnemonic phrase") shouldBe false
    }

    @Test
    fun `different generated mnemonics are different`() {
        val m1 = Mnemonic.generate(12)
        val m2 = Mnemonic.generate(12)
        m1 shouldNotBe m2
    }
}
