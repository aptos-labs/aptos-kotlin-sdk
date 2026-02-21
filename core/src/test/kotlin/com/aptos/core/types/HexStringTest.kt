package com.aptos.core.types

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HexStringTest {
    @Test
    fun `encode empty bytes`() {
        HexString.encode(byteArrayOf()) shouldBe ""
    }

    @Test
    fun `encode single byte`() {
        HexString.encode(byteArrayOf(0x0A.toByte())) shouldBe "0a"
    }

    @Test
    fun `encode multiple bytes`() {
        HexString.encode(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())) shouldBe "deadbeef"
    }

    @Test
    fun `encode all zeros`() {
        HexString.encode(ByteArray(4)) shouldBe "00000000"
    }

    @Test
    fun `encode all 0xFF`() {
        HexString.encode(ByteArray(2) { 0xFF.toByte() }) shouldBe "ffff"
    }

    @Test
    fun `encodeWithPrefix adds 0x`() {
        HexString.encodeWithPrefix(byteArrayOf(0xCA.toByte(), 0xFE.toByte())) shouldBe "0xcafe"
    }

    @Test
    fun `encodeWithPrefix empty bytes`() {
        HexString.encodeWithPrefix(byteArrayOf()) shouldBe "0x"
    }

    @Test
    fun `decode lowercase hex`() {
        HexString.decode("deadbeef") shouldBe byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
    }

    @Test
    fun `decode uppercase hex`() {
        HexString.decode("DEADBEEF") shouldBe byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
    }

    @Test
    fun `decode mixed case hex`() {
        HexString.decode("DeAdBeEf") shouldBe byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
    }

    @Test
    fun `decode with 0x prefix`() {
        HexString.decode("0xabcd") shouldBe byteArrayOf(0xAB.toByte(), 0xCD.toByte())
    }

    @Test
    fun `decode with 0X prefix`() {
        HexString.decode("0Xabcd") shouldBe byteArrayOf(0xAB.toByte(), 0xCD.toByte())
    }

    @Test
    fun `decode empty string`() {
        HexString.decode("") shouldBe byteArrayOf()
    }

    @Test
    fun `decode 0x only`() {
        HexString.decode("0x") shouldBe byteArrayOf()
    }

    @Test
    fun `decode rejects odd length`() {
        shouldThrow<IllegalArgumentException> {
            HexString.decode("abc")
        }
    }

    @Test
    fun `decode rejects invalid characters`() {
        shouldThrow<IllegalArgumentException> {
            HexString.decode("zzzz")
        }
    }

    @Test
    fun `roundtrip encode then decode`() {
        val original = byteArrayOf(0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte())
        HexString.decode(HexString.encode(original)) shouldBe original
    }

    @Test
    fun `roundtrip decode then encode`() {
        val hex = "0123456789abcdef"
        HexString.encode(HexString.decode(hex)) shouldBe hex
    }
}
