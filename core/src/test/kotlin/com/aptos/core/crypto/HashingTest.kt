package com.aptos.core.crypto

import com.aptos.core.types.HexString
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HashingTest {
    @Test
    fun `sha3-256 of empty input`() {
        val hash = Hashing.sha3256(byteArrayOf())
        hash.size shouldBe 32
        // Known SHA3-256 of empty string
        HexString.encode(hash) shouldBe "a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a"
    }

    @Test
    fun `sha256 of empty input`() {
        val hash = Hashing.sha256(byteArrayOf())
        hash.size shouldBe 32
        // Known SHA-256 of empty string
        HexString.encode(hash) shouldBe "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }

    @Test
    fun `sha3-256 of hello`() {
        val hash = Hashing.sha3256("hello".toByteArray())
        hash.size shouldBe 32
    }

    @Test
    fun `domain separated hash is deterministic`() {
        val data = "test data".toByteArray()
        val h1 = Hashing.domainSeparatedHash(data, "APTOS")
        val h2 = Hashing.domainSeparatedHash(data, "APTOS")
        h1 shouldBe h2
    }

    @Test
    fun `different domains produce different hashes`() {
        val data = "test data".toByteArray()
        val h1 = Hashing.domainSeparatedHash(data, "APTOS")
        val h2 = Hashing.domainSeparatedHash(data, "OTHER")
        (h1.contentEquals(h2)) shouldBe false
    }

    @Test
    fun `RAW_TRANSACTION_PREFIX is 32 bytes`() {
        Hashing.RAW_TRANSACTION_PREFIX.size shouldBe 32
    }

    @Test
    fun `RAW_TRANSACTION_PREFIX is SHA3-256 of APTOS RawTransaction`() {
        val expected = Hashing.sha3256("APTOS::RawTransaction".toByteArray(Charsets.UTF_8))
        Hashing.RAW_TRANSACTION_PREFIX shouldBe expected
    }
}
