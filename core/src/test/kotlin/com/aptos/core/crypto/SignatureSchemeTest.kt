package com.aptos.core.crypto

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SignatureSchemeTest {
    @Test
    fun `ED25519 has id 0x00`() {
        SignatureScheme.ED25519.id shouldBe 0x00.toByte()
    }

    @Test
    fun `SECP256K1 has id 0x01`() {
        SignatureScheme.SECP256K1.id shouldBe 0x01.toByte()
    }

    @Test
    fun `fromId ED25519`() {
        SignatureScheme.fromId(0x00) shouldBe SignatureScheme.ED25519
    }

    @Test
    fun `fromId SECP256K1`() {
        SignatureScheme.fromId(0x01) shouldBe SignatureScheme.SECP256K1
    }

    @Test
    fun `MULTI_ED25519 has id 0x01`() {
        SignatureScheme.MULTI_ED25519.id shouldBe 0x01.toByte()
    }

    @Test
    fun `MULTI_KEY has id 0x03`() {
        SignatureScheme.MULTI_KEY.id shouldBe 0x03.toByte()
    }

    @Test
    fun `KEYLESS has id 0x05`() {
        SignatureScheme.KEYLESS.id shouldBe 0x05.toByte()
    }

    @Test
    fun `entries has exactly 5 schemes`() {
        SignatureScheme.entries.size shouldBe 5
    }
}
