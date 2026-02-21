package com.aptos.core.types

import com.aptos.core.bcs.BcsDeserializer
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.AccountAddressParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AccountAddressTest {
    @Test
    fun `parse full hex address`() {
        val hex = "0x0000000000000000000000000000000000000000000000000000000000000001"
        val addr = AccountAddress.fromHex(hex)
        addr shouldBe AccountAddress.ONE
    }

    @Test
    fun `parse short special addresses`() {
        AccountAddress.fromHex("0x0") shouldBe AccountAddress.ZERO
        AccountAddress.fromHex("0x1") shouldBe AccountAddress.ONE
        AccountAddress.fromHex("0x3") shouldBe AccountAddress.THREE
        AccountAddress.fromHex("0x4") shouldBe AccountAddress.FOUR
    }

    @ParameterizedTest
    @ValueSource(strings = ["0xa", "0xb", "0xc", "0xd", "0xe", "0xf"])
    fun `parse short special addresses a through f`(hex: String) {
        val addr = AccountAddress.fromHex(hex)
        addr shouldNotBe null
    }

    @Test
    fun `short form rejected for non-special addresses`() {
        shouldThrow<AccountAddressParseException> {
            AccountAddress.fromHex("0x10") // 16, not special
        }
    }

    @Test
    fun `relaxed parse pads short addresses`() {
        val addr = AccountAddress.fromHexRelaxed("0x1234")
        addr.toHex() shouldBe "0x0000000000000000000000000000000000000000000000000000000000001234"
    }

    @Test
    fun `toHex returns full form`() {
        AccountAddress.ONE.toHex() shouldBe
            "0x0000000000000000000000000000000000000000000000000000000000000001"
    }

    @Test
    fun `toShortString trims leading zeros`() {
        AccountAddress.ONE.toShortString() shouldBe "0x1"
        AccountAddress.ZERO.toShortString() shouldBe "0x0"
    }

    @Test
    fun `case insensitive parsing`() {
        val lower = "0x000000000000000000000000000000000000000000000000000000000000000a"
        val upper = "0x000000000000000000000000000000000000000000000000000000000000000A"
        AccountAddress.fromHex(lower) shouldBe AccountAddress.fromHex(upper)
    }

    @Test
    fun `reject too long hex`() {
        shouldThrow<AccountAddressParseException> {
            AccountAddress.fromHex("0x" + "0".repeat(65))
        }
    }

    @Test
    fun `reject empty hex`() {
        shouldThrow<AccountAddressParseException> {
            AccountAddress.fromHex("0x")
        }
    }

    @Test
    fun `reject invalid hex characters`() {
        shouldThrow<AccountAddressParseException> {
            AccountAddress.fromHexRelaxed("0xGG")
        }
    }

    @Test
    fun `BCS roundtrip`() {
        val original = AccountAddress.fromHexRelaxed("0xCAFE")
        val serializer = BcsSerializer()
        original.serialize(serializer)
        val bytes = serializer.toByteArray()
        bytes.size shouldBe 32

        val deserializer = BcsDeserializer(bytes)
        val restored = AccountAddress.fromBcs(deserializer)
        restored shouldBe original
    }

    @Test
    fun `comparable ordering`() {
        val a = AccountAddress.ZERO
        val b = AccountAddress.ONE
        (a < b) shouldBe true
        (b > a) shouldBe true
        (a.compareTo(a)) shouldBe 0
    }

    @Test
    fun `isValid returns true for valid`() {
        AccountAddress.isValid("0x1") shouldBe true
        AccountAddress.isValid(
            "0x0000000000000000000000000000000000000000000000000000000000000001",
        ) shouldBe true
    }

    @Test
    fun `isValid returns false for invalid`() {
        AccountAddress.isValid("0x") shouldBe false
        AccountAddress.isValid("0xGG") shouldBe false
        AccountAddress.isValid("0x10") shouldBe false // non-special short
    }
}
