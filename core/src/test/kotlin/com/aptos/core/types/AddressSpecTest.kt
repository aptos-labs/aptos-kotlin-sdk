package com.aptos.core.types

import com.aptos.core.bcs.BcsDeserializer
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.AccountAddressParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Spec-based tests for [AccountAddress] validated against the official aptos-sdk-specs.
 */
@Suppress("MaxLineLength")
class AddressSpecTest {

    // ---- Valid address parsing (relaxed) ----

    @ParameterizedTest(name = "fromHexRelaxed(\"{0}\") -> short=\"{1}\", full=\"{2}\"")
    @CsvSource(
        "0x1, 0x1, 0x0000000000000000000000000000000000000000000000000000000000000001",
        "0x10, 0x10, 0x0000000000000000000000000000000000000000000000000000000000000010",
        "0xff, 0xff, 0x00000000000000000000000000000000000000000000000000000000000000ff",
        "0x100, 0x100, 0x0000000000000000000000000000000000000000000000000000000000000100",
        "0xabcdef, 0xabcdef, 0x0000000000000000000000000000000000000000000000000000000000abcdef",
        "0x0000000000000000000000000000000000000000000000000000000000abcdef, 0xabcdef, 0x0000000000000000000000000000000000000000000000000000000000abcdef",
    )
    fun `valid address parsing with relaxed mode`(input: String, expectedShort: String, expectedFull: String) {
        val addr = AccountAddress.fromHexRelaxed(input)
        addr.toShortString() shouldBe expectedShort
        addr.toHex() shouldBe expectedFull
    }

    // ---- Invalid inputs ----

    @Test
    fun `empty string is rejected`() {
        shouldThrow<AccountAddressParseException> {
            AccountAddress.fromHexRelaxed("")
        }
    }

    @Test
    fun `0x with no digits is rejected`() {
        shouldThrow<AccountAddressParseException> {
            AccountAddress.fromHexRelaxed("0x")
        }
    }

    @Test
    fun `invalid hex characters are rejected`() {
        shouldThrow<AccountAddressParseException> {
            AccountAddress.fromHexRelaxed("0xGHIJKL")
        }
    }

    @Test
    fun `65 hex chars is rejected as too long`() {
        shouldThrow<AccountAddressParseException> {
            AccountAddress.fromHexRelaxed(
                "0x00000000000000000000000000000000000000000000000000000000000000001"
            )
        }
    }

    @Test
    fun `spaces in hex string are rejected`() {
        shouldThrow<AccountAddressParseException> {
            AccountAddress.fromHexRelaxed("0x1 2 3")
        }
    }

    // ---- Short string format ----

    @ParameterizedTest(name = "toShortString of \"{0}\" -> \"{1}\"")
    @CsvSource(
        "0x0000000000000000000000000000000000000000000000000000000000000010, 0x10",
        "0x0000000000000000000000000000000000000000000000000000000000001000, 0x1000",
        "0x1000000000000000000000000000000000000000000000000000000000000000, 0x1000000000000000000000000000000000000000000000000000000000000000",
    )
    fun `short string format`(input: String, expectedShort: String) {
        val addr = AccountAddress.fromHexRelaxed(input)
        addr.toShortString() shouldBe expectedShort
    }

    // ---- Constants ----

    @Test
    fun `ZERO constant has all zero bytes and short string 0x0`() {
        val zero = AccountAddress.ZERO
        zero.data.all { it == 0.toByte() } shouldBe true
        zero.toShortString() shouldBe "0x0"
    }

    @Test
    fun `ONE constant has byte 31 equal to 1 and short string 0x1`() {
        val one = AccountAddress.ONE
        one.data[31] shouldBe 1.toByte()
        for (i in 0 until 31) {
            one.data[i] shouldBe 0.toByte()
        }
        one.toShortString() shouldBe "0x1"
    }

    @Test
    fun `THREE constant has byte 31 equal to 3 and short string 0x3`() {
        val three = AccountAddress.THREE
        three.data[31] shouldBe 3.toByte()
        for (i in 0 until 31) {
            three.data[i] shouldBe 0.toByte()
        }
        three.toShortString() shouldBe "0x3"
    }

    @Test
    fun `FOUR constant has byte 31 equal to 4 and short string 0x4`() {
        val four = AccountAddress.FOUR
        four.data[31] shouldBe 4.toByte()
        for (i in 0 until 31) {
            four.data[i] shouldBe 0.toByte()
        }
        four.toShortString() shouldBe "0x4"
    }

    // ---- Equality ----

    @Test
    fun `short and full form of same address are equal`() {
        val fromShort = AccountAddress.fromHexRelaxed("0x1")
        val fromFull = AccountAddress.fromHexRelaxed(
            "0x0000000000000000000000000000000000000000000000000000000000000001"
        )
        fromShort shouldBe fromFull
    }

    @Test
    fun `different addresses are not equal`() {
        val one = AccountAddress.fromHexRelaxed("0x1")
        val two = AccountAddress.fromHexRelaxed("0x2")
        (one == two) shouldBe false
    }

    // ---- BCS serialization ----

    @Test
    fun `BCS serialize 0x1 produces 32 bytes with byte 31 equal to 1`() {
        val addr = AccountAddress.fromHexRelaxed("0x1")
        val serializer = BcsSerializer()
        addr.serialize(serializer)
        val bytes = serializer.toByteArray()

        bytes.size shouldBe 32
        bytes[31] shouldBe 1.toByte()
        for (i in 0 until 31) {
            bytes[i] shouldBe 0.toByte()
        }
    }

    @Test
    fun `BCS deserialize 32 bytes with byte 31 equal to 0x42 produces address 0x42`() {
        val raw = ByteArray(32)
        raw[31] = 0x42.toByte()

        val deserializer = BcsDeserializer(raw)
        val addr = AccountAddress.fromBcs(deserializer)

        addr.toShortString() shouldBe "0x42"
    }

    @Test
    fun `BCS roundtrip for 0xabcdef1234567890`() {
        val original = AccountAddress.fromHexRelaxed("0xabcdef1234567890")

        val serializer = BcsSerializer()
        original.serialize(serializer)
        val bytes = serializer.toByteArray()

        val deserializer = BcsDeserializer(bytes)
        val restored = AccountAddress.fromBcs(deserializer)

        restored shouldBe original
        restored.toShortString() shouldBe original.toShortString()
        restored.toHex() shouldBe original.toHex()
    }

    // ---- Case-insensitive parsing ----

    @ParameterizedTest(name = "case-insensitive: \"{0}\" -> short \"0xabcdef\"")
    @ValueSource(strings = ["0xABCDEF", "0xAbCdEf"])
    fun `case-insensitive parsing produces lowercase short string`(input: String) {
        val addr = AccountAddress.fromHexRelaxed(input)
        addr.toShortString() shouldBe "0xabcdef"
    }
}
