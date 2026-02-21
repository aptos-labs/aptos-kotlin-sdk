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
    fun `entries has exactly 2 schemes`() {
        SignatureScheme.entries.size shouldBe 2
    }
}
