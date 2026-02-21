package com.aptos.core.account

import com.aptos.core.error.MnemonicException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * BIP-39 mnemonic phrase for deterministic key generation.
 *
 * Supports 12, 15, 18, 21, or 24-word phrases using the English BIP-39 wordlist.
 * Use [generate] to create a new random mnemonic or [fromPhrase] to restore from words.
 *
 * @property words the individual mnemonic words
 */
class Mnemonic private constructor(val words: List<String>) {

    /** Returns the mnemonic as a single space-separated string. */
    fun phrase(): String = words.joinToString(" ")

    /**
     * Derives a 64-byte seed using PBKDF2-HMAC-SHA512 (2048 iterations).
     *
     * @param passphrase optional BIP-39 passphrase (default: empty string)
     * @return the 64-byte seed suitable for key derivation
     */
    fun toSeed(passphrase: String = ""): ByteArray {
        val mnemonic = phrase().toCharArray()
        val salt = "mnemonic$passphrase".toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(mnemonic, salt, PBKDF2_ITERATIONS, SEED_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        return factory.generateSecret(spec).encoded
    }

    fun wordCount(): Int = words.size

    override fun toString(): String = "Mnemonic(${wordCount()} words)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Mnemonic) return false
        return words == other.words
    }

    override fun hashCode(): Int = words.hashCode()

    companion object {
        private const val PBKDF2_ITERATIONS = 2048
        private const val SEED_LENGTH_BITS = 512

        /**
         * Generates a new random mnemonic with the given [wordCount].
         *
         * @param wordCount number of words (12, 15, 18, 21, or 24)
         * @throws IllegalArgumentException if [wordCount] is not a valid BIP-39 word count
         */
        @JvmStatic
        fun generate(wordCount: Int = 12): Mnemonic {
            require(wordCount == 12 || wordCount == 15 || wordCount == 18 || wordCount == 21 || wordCount == 24) {
                "Word count must be 12, 15, 18, 21, or 24"
            }
            val entropyBits = wordCount * 11 * 32 / 33
            val entropyBytes = entropyBits / 8
            val random = SecureRandom()
            val entropy = ByteArray(entropyBytes)
            random.nextBytes(entropy)
            return fromEntropy(entropy)
        }

        /**
         * Parses and validates a mnemonic from a space-separated phrase.
         *
         * @throws MnemonicException if the phrase has invalid word count or unknown words
         */
        @JvmStatic
        fun fromPhrase(phrase: String): Mnemonic {
            val words = phrase.trim().lowercase().split("\\s+".toRegex())
            validate(words)
            return Mnemonic(words)
        }

        /** Creates a mnemonic from raw entropy bytes with SHA-256 checksum. */
        @JvmStatic
        fun fromEntropy(entropy: ByteArray): Mnemonic {
            val wordList = BIP39_ENGLISH_WORDLIST
            val checksumBits = entropy.size / 4
            val hash = com.aptos.core.crypto.Hashing.sha256(entropy)

            // Convert entropy + checksum to bit string
            val bits = StringBuilder()
            for (b in entropy) {
                bits.append(Integer.toBinaryString(b.toInt() and 0xFF).padStart(8, '0'))
            }
            for (i in 0 until checksumBits) {
                val bit = (hash[i / 8].toInt() shr (7 - i % 8)) and 1
                bits.append(bit)
            }

            val words = mutableListOf<String>()
            for (i in 0 until bits.length / 11) {
                val index = bits.substring(i * 11, (i + 1) * 11).toInt(2)
                words.add(wordList[index])
            }

            return Mnemonic(words)
        }

        @JvmStatic
        fun validate(words: List<String>) {
            if (words.size !in setOf(12, 15, 18, 21, 24)) {
                throw MnemonicException("Invalid word count: ${words.size}. Must be 12, 15, 18, 21, or 24")
            }
            val wordList = BIP39_ENGLISH_WORDLIST
            val wordSet = wordList.toSet()
            for (word in words) {
                if (word !in wordSet) {
                    throw MnemonicException("Invalid mnemonic word: '$word'")
                }
            }
        }

        /** Returns `true` if [phrase] is a valid BIP-39 mnemonic. */
        @JvmStatic
        fun isValid(phrase: String): Boolean {
            return try {
                fromPhrase(phrase)
                true
            } catch (_: MnemonicException) {
                false
            }
        }
    }
}
