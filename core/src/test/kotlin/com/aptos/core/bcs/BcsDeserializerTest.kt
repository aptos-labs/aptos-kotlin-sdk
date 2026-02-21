package com.aptos.core.bcs

import com.aptos.core.error.BcsDeserializationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigInteger

class BcsDeserializerTest {
    @Test
    fun `deserialize bool true`() {
        val d = BcsDeserializer(byteArrayOf(1))
        d.deserializeBool() shouldBe true
    }

    @Test
    fun `deserialize bool false`() {
        val d = BcsDeserializer(byteArrayOf(0))
        d.deserializeBool() shouldBe false
    }

    @Test
    fun `deserialize bool invalid value throws`() {
        val d = BcsDeserializer(byteArrayOf(2))
        shouldThrow<BcsDeserializationException> {
            d.deserializeBool()
        }
    }

    @Test
    fun `deserialize u8`() {
        val d = BcsDeserializer(byteArrayOf(0xFF.toByte()))
        d.deserializeU8() shouldBe 0xFFu.toUByte()
    }

    @Test
    fun `deserialize u16`() {
        val d = BcsDeserializer(byteArrayOf(0x02, 0x01))
        d.deserializeU16() shouldBe 0x0102u.toUShort()
    }

    @Test
    fun `deserialize u32`() {
        val d = BcsDeserializer(byteArrayOf(0x04, 0x03, 0x02, 0x01))
        d.deserializeU32() shouldBe 0x01020304u
    }

    @Test
    fun `deserialize u64`() {
        val d = BcsDeserializer(byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0))
        d.deserializeU64() shouldBe 1uL
    }

    @Test
    fun `deserialize u64 max`() {
        val d = BcsDeserializer(ByteArray(8) { 0xFF.toByte() })
        d.deserializeU64() shouldBe ULong.MAX_VALUE
    }

    @Test
    fun `deserialize u128`() {
        val input = ByteArray(16)
        input[0] = 1
        val d = BcsDeserializer(input)
        d.deserializeU128() shouldBe BigInteger.ONE
    }

    @Test
    fun `deserialize u128 zero`() {
        val d = BcsDeserializer(ByteArray(16))
        d.deserializeU128() shouldBe BigInteger.ZERO
    }

    @Test
    fun `deserialize u256`() {
        val d = BcsDeserializer(ByteArray(32))
        d.deserializeU256() shouldBe BigInteger.ZERO
    }

    @Test
    fun `deserialize string`() {
        val payload = "hello".toByteArray(Charsets.UTF_8)
        val input = byteArrayOf(payload.size.toByte()) + payload
        val d = BcsDeserializer(input)
        d.deserializeString() shouldBe "hello"
    }

    @Test
    fun `deserialize empty string`() {
        val d = BcsDeserializer(byteArrayOf(0))
        d.deserializeString() shouldBe ""
    }

    @Test
    fun `deserialize bytes`() {
        val d = BcsDeserializer(byteArrayOf(3, 1, 2, 3))
        d.deserializeBytes() shouldBe byteArrayOf(1, 2, 3)
    }

    @Test
    fun `deserialize fixed bytes`() {
        val d = BcsDeserializer(byteArrayOf(0xAB.toByte(), 0xCD.toByte()))
        d.deserializeFixedBytes(2) shouldBe byteArrayOf(0xAB.toByte(), 0xCD.toByte())
    }

    @Test
    fun `deserialize ULEB128 single byte`() {
        val d = BcsDeserializer(byteArrayOf(0))
        d.deserializeUleb128() shouldBe 0u
    }

    @Test
    fun `deserialize ULEB128 multi byte`() {
        val d = BcsDeserializer(byteArrayOf(0x80.toByte(), 0x01))
        d.deserializeUleb128() shouldBe 128u
    }

    @Test
    fun `deserialize ULEB128 300`() {
        val d = BcsDeserializer(byteArrayOf(0xAC.toByte(), 0x02))
        d.deserializeUleb128() shouldBe 300u
    }

    @Test
    fun `deserialize option with value`() {
        val d = BcsDeserializer(byteArrayOf(1, 42))
        d.deserializeOptionTag() shouldBe true
        d.deserializeU8() shouldBe 42u.toUByte()
    }

    @Test
    fun `deserialize option without value`() {
        val d = BcsDeserializer(byteArrayOf(0))
        d.deserializeOptionTag() shouldBe false
    }

    @Test
    fun `deserialize past end throws`() {
        val d = BcsDeserializer(byteArrayOf())
        shouldThrow<BcsDeserializationException> {
            d.deserializeU8()
        }
    }

    @Test
    fun `deserialize insufficient bytes throws`() {
        val d = BcsDeserializer(byteArrayOf(0x01))
        shouldThrow<BcsDeserializationException> {
            d.deserializeU32()
        }
    }

    @Test
    fun `roundtrip serialization`() {
        val serializer = BcsSerializer()
        serializer.serializeBool(true)
        serializer.serializeU8(255u)
        serializer.serializeU16(1000u)
        serializer.serializeU32(123456u)
        serializer.serializeU64(9876543210uL)
        serializer.serializeU128(BigInteger("340282366920938463463374607431768211455")) // u128 max
        serializer.serializeString("aptos")

        val bytes = serializer.toByteArray()
        val d = BcsDeserializer(bytes)
        d.deserializeBool() shouldBe true
        d.deserializeU8() shouldBe 255u.toUByte()
        d.deserializeU16() shouldBe 1000u.toUShort()
        d.deserializeU32() shouldBe 123456u
        d.deserializeU64() shouldBe 9876543210uL
        d.deserializeU128() shouldBe BigInteger("340282366920938463463374607431768211455")
        d.deserializeString() shouldBe "aptos"
        d.remaining shouldBe 0
    }

    @Test
    fun `remaining bytes tracking`() {
        val d = BcsDeserializer(byteArrayOf(1, 2, 3, 4))
        d.remaining shouldBe 4
        d.deserializeU8()
        d.remaining shouldBe 3
        d.deserializeU8()
        d.remaining shouldBe 2
    }
}
