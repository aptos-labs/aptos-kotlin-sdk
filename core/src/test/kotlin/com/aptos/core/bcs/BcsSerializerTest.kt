package com.aptos.core.bcs

import com.aptos.core.error.BcsSerializationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigInteger

class BcsSerializerTest {
    @Test
    fun `serialize bool true`() {
        val s = BcsSerializer()
        s.serializeBool(true)
        s.toByteArray() shouldBe byteArrayOf(1)
    }

    @Test
    fun `serialize bool false`() {
        val s = BcsSerializer()
        s.serializeBool(false)
        s.toByteArray() shouldBe byteArrayOf(0)
    }

    @Test
    fun `serialize u8`() {
        val s = BcsSerializer()
        s.serializeU8(0xFFu)
        s.toByteArray() shouldBe byteArrayOf(0xFF.toByte())
    }

    @Test
    fun `serialize u8 zero`() {
        val s = BcsSerializer()
        s.serializeU8(0u)
        s.toByteArray() shouldBe byteArrayOf(0)
    }

    @Test
    fun `serialize u16 little endian`() {
        val s = BcsSerializer()
        s.serializeU16(0x0102u)
        s.toByteArray() shouldBe byteArrayOf(0x02, 0x01)
    }

    @Test
    fun `serialize u32 little endian`() {
        val s = BcsSerializer()
        s.serializeU32(0x01020304u)
        s.toByteArray() shouldBe byteArrayOf(0x04, 0x03, 0x02, 0x01)
    }

    @Test
    fun `serialize u64 little endian`() {
        val s = BcsSerializer()
        s.serializeU64(1uL)
        s.toByteArray() shouldBe byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0)
    }

    @Test
    fun `serialize u64 max`() {
        val s = BcsSerializer()
        s.serializeU64(ULong.MAX_VALUE)
        s.toByteArray() shouldBe ByteArray(8) { 0xFF.toByte() }
    }

    @Test
    fun `serialize u128`() {
        val s = BcsSerializer()
        s.serializeU128(BigInteger.ONE)
        val expected = ByteArray(16)
        expected[0] = 1
        s.toByteArray() shouldBe expected
    }

    @Test
    fun `serialize u128 large`() {
        val s = BcsSerializer()
        // 2^127 - 1
        val value = BigInteger.TWO.pow(127).subtract(BigInteger.ONE)
        s.serializeU128(value)
        val bytes = s.toByteArray()
        bytes.size shouldBe 16
    }

    @Test
    fun `serialize u128 rejects negative`() {
        val s = BcsSerializer()
        shouldThrow<BcsSerializationException> {
            s.serializeU128(BigInteger.valueOf(-1))
        }
    }

    @Test
    fun `serialize u256`() {
        val s = BcsSerializer()
        s.serializeU256(BigInteger.ZERO)
        s.toByteArray() shouldBe ByteArray(32)
    }

    @Test
    fun `serialize string`() {
        val s = BcsSerializer()
        s.serializeString("hello")
        val bytes = s.toByteArray()
        // ULEB128(5) = 0x05, then "hello" bytes
        bytes[0] shouldBe 5.toByte()
        bytes.drop(1).toByteArray() shouldBe "hello".toByteArray(Charsets.UTF_8)
    }

    @Test
    fun `serialize empty string`() {
        val s = BcsSerializer()
        s.serializeString("")
        s.toByteArray() shouldBe byteArrayOf(0)
    }

    @Test
    fun `serialize bytes`() {
        val s = BcsSerializer()
        s.serializeBytes(byteArrayOf(1, 2, 3))
        val bytes = s.toByteArray()
        bytes[0] shouldBe 3.toByte()
        bytes[1] shouldBe 1.toByte()
        bytes[2] shouldBe 2.toByte()
        bytes[3] shouldBe 3.toByte()
    }

    @Test
    fun `serialize fixed bytes has no length prefix`() {
        val s = BcsSerializer()
        s.serializeFixedBytes(byteArrayOf(0xAB.toByte(), 0xCD.toByte()))
        s.toByteArray() shouldBe byteArrayOf(0xAB.toByte(), 0xCD.toByte())
    }

    @Test
    fun `serialize ULEB128 single byte`() {
        val s = BcsSerializer()
        s.serializeUleb128(0u)
        s.toByteArray() shouldBe byteArrayOf(0)
    }

    @Test
    fun `serialize ULEB128 multi byte`() {
        val s = BcsSerializer()
        s.serializeUleb128(128u) // 0x80 -> needs 2 bytes
        s.toByteArray() shouldBe byteArrayOf(0x80.toByte(), 0x01)
    }

    @Test
    fun `serialize ULEB128 300`() {
        val s = BcsSerializer()
        s.serializeUleb128(300u) // 300 = 0x12C -> 0xAC 0x02
        s.toByteArray() shouldBe byteArrayOf(0xAC.toByte(), 0x02)
    }

    @Test
    fun `serialize sequence of bcs serializables`() {
        // Test sequence serialization using a simple wrapper
        data class U8Wrapper(val value: UByte) : BcsSerializable {
            override fun serialize(serializer: BcsSerializer) {
                serializer.serializeU8(value)
            }
        }

        val s = BcsSerializer()
        s.serializeSequence(listOf(U8Wrapper(1u), U8Wrapper(2u), U8Wrapper(3u)))
        s.toByteArray() shouldBe byteArrayOf(3, 1, 2, 3)
    }

    @Test
    fun `serialize option with value`() {
        data class U8Wrapper(val value: UByte) : BcsSerializable {
            override fun serialize(serializer: BcsSerializer) {
                serializer.serializeU8(value)
            }
        }

        val s = BcsSerializer()
        s.serializeOption(U8Wrapper(42u))
        s.toByteArray() shouldBe byteArrayOf(1, 42)
    }

    @Test
    fun `serialize option without value`() {
        val s = BcsSerializer()
        s.serializeOption<BcsSerializable>(null)
        s.toByteArray() shouldBe byteArrayOf(0)
    }

    @Test
    fun `serialize multiple values in sequence`() {
        val s = BcsSerializer()
        s.serializeBool(true)
        s.serializeU8(42u)
        s.serializeU32(1000u)
        s.serializeString("test")

        val bytes = s.toByteArray()
        bytes[0] shouldBe 1.toByte() // bool true
        bytes[1] shouldBe 42.toByte() // u8
        // u32 1000 = 0x000003E8 LE -> E8 03 00 00
        bytes[2] shouldBe 0xE8.toByte()
        bytes[3] shouldBe 0x03.toByte()
        bytes[4] shouldBe 0x00.toByte()
        bytes[5] shouldBe 0x00.toByte()
        // string "test" -> ULEB128(4) + bytes
        bytes[6] shouldBe 4.toByte()
    }
}
