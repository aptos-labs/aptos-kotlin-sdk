package com.aptos.core.crypto

import com.aptos.core.types.HexString
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Spec-based tests for hashing validated against official aptos-sdk-specs test vectors.
 */
class HashingSpecTest {
    // -- SHA3-256 vectors --

    @ParameterizedTest(name = "SHA3-256({0}) = {2}")
    @MethodSource("sha3Vectors")
    @Suppress("UNUSED_PARAMETER")
    fun `SHA3-256 matches spec vector`(name: String, input: ByteArray, expectedHex: String) {
        val hash = Hashing.sha3256(input)
        HexString.encode(hash) shouldBe expectedHex
    }

    // -- SHA2-256 vectors --

    @ParameterizedTest(name = "SHA-256({0}) = {2}")
    @MethodSource("sha256Vectors")
    @Suppress("UNUSED_PARAMETER")
    fun `SHA-256 matches spec vector`(name: String, input: ByteArray, expectedHex: String) {
        val hash = Hashing.sha256(input)
        HexString.encode(hash) shouldBe expectedHex
    }

    // -- Domain separator prefix vectors --

    @Test
    fun `RAW_TRANSACTION_PREFIX matches spec vector`() {
        HexString.encode(Hashing.RAW_TRANSACTION_PREFIX) shouldBe
            "b5e97db07fa0bd0e5598aa3643a9bc6f6693bddc1a9fec9e674a461eaa00b193"
    }

    @Test
    fun `RAW_TRANSACTION_WITH_DATA_PREFIX matches spec vector`() {
        HexString.encode(Hashing.RAW_TRANSACTION_WITH_DATA_PREFIX) shouldBe
            "5efa3c4f02f83a0f4b2d69fc95c607cc02825cc4e7be536ef0992df050d9e67c"
    }

    @Test
    fun `TRANSACTION_PREFIX matches spec vector`() {
        HexString.encode(Hashing.TRANSACTION_PREFIX) shouldBe
            "fa210a9417ef3e7fa45bfa1d17a8dbd4d883711910a550d265fee189e9266dd4"
    }

    // -- Domain-separated hash construction --

    @Test
    fun `domain separated hash uses SHA3-256 of prefix string then SHA3-256 of prefix plus data`() {
        // domainSeparatedHash(data, "APTOS") = SHA3-256(SHA3-256("APTOS::APTOS") || data)
        val data = "test".toByteArray()
        val prefixHash = Hashing.sha3256("APTOS::APTOS".toByteArray(Charsets.UTF_8))
        val expected = Hashing.sha3256(prefixHash + data)
        Hashing.domainSeparatedHash(data, "APTOS") shouldBe expected
    }

    @Test
    fun `RAW_TRANSACTION_PREFIX equals SHA3-256 of APTOS RawTransaction string`() {
        val expected = Hashing.sha3256("APTOS::RawTransaction".toByteArray(Charsets.UTF_8))
        Hashing.RAW_TRANSACTION_PREFIX shouldBe expected
    }

    @Test
    fun `RAW_TRANSACTION_WITH_DATA_PREFIX equals SHA3-256 of APTOS RawTransactionWithData string`() {
        val expected = Hashing.sha3256("APTOS::RawTransactionWithData".toByteArray(Charsets.UTF_8))
        Hashing.RAW_TRANSACTION_WITH_DATA_PREFIX shouldBe expected
    }

    @Test
    fun `TRANSACTION_PREFIX equals SHA3-256 of APTOS Transaction string`() {
        val expected = Hashing.sha3256("APTOS::Transaction".toByteArray(Charsets.UTF_8))
        Hashing.TRANSACTION_PREFIX shouldBe expected
    }

    // -- All prefixes are 32 bytes --

    @Test
    fun `all prefixes are 32 bytes`() {
        Hashing.RAW_TRANSACTION_PREFIX.size shouldBe 32
        Hashing.RAW_TRANSACTION_WITH_DATA_PREFIX.size shouldBe 32
        Hashing.TRANSACTION_PREFIX.size shouldBe 32
    }

    companion object {
        @JvmStatic
        fun sha3Vectors(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "empty",
                byteArrayOf(),
                "a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a",
            ),
            Arguments.of(
                "hello",
                "hello".toByteArray(),
                "3338be694f50c5f338814986cdf0686453a888b84f424d792af4b9202398f392",
            ),
            Arguments.of(
                "hello world",
                "hello world".toByteArray(),
                "644bcc7e564373040999aac89e7622f3ca71fba1d972fd94a31c3bfbf24e3938",
            ),
            Arguments.of(
                "single byte 0x00",
                byteArrayOf(0x00),
                "5d53469f20fef4f8eab52b88044ede69c77a6a68a60728609fc4a65ff531e7d0",
            ),
            Arguments.of(
                "32 zeros",
                ByteArray(32),
                "9e6291970cb44dd94008c79bcaf9d86f18b4b49ba5b2a04781db7199ed3b9e4e",
            ),
        )

        @JvmStatic
        fun sha256Vectors(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "empty",
                byteArrayOf(),
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            ),
            Arguments.of(
                "hello",
                "hello".toByteArray(),
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            ),
            Arguments.of(
                "hello world",
                "hello world".toByteArray(),
                "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
            ),
        )
    }
}
