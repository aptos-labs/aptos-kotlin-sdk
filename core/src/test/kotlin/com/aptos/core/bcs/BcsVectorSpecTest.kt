package com.aptos.core.bcs

import com.aptos.core.error.BcsDeserializationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Additional BCS spec-based tests covering bytes, strings, options, vectors,
 * and u8/u64 values from the official aptos-sdk-specs test vectors.
 */
class BcsVectorSpecTest {
    // -- u8 spec vectors --

    @ParameterizedTest(name = "u8 serialize: {0} -> {1}")
    @MethodSource("u8Vectors")
    fun `serialize u8 matches spec vector`(value: Int, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeU8(value.toUByte())
        serializer.toByteArray() shouldBe expectedBytes
    }

    @ParameterizedTest(name = "u8 deserialize: {1} -> {0}")
    @MethodSource("u8Vectors")
    fun `deserialize u8 matches spec vector`(expectedValue: Int, bytes: ByteArray) {
        val deserializer = BcsDeserializer(bytes)
        deserializer.deserializeU8() shouldBe expectedValue.toUByte()
    }

    // -- u64 spec vectors with specific values --

    @Test
    fun `u64 zero is 8 zero bytes`() {
        val s = BcsSerializer()
        s.serializeU64(0uL)
        s.toByteArray() shouldBe ByteArray(8)
    }

    @Test
    fun `u64 one is 0x01 followed by 7 zero bytes`() {
        val s = BcsSerializer()
        s.serializeU64(1uL)
        s.toByteArray() shouldBe byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    }

    @Test
    fun `u64 1000000 matches spec`() {
        val s = BcsSerializer()
        s.serializeU64(1_000_000uL)
        s.toByteArray() shouldBe byteArrayOf(0x40, 0x42, 0x0F, 0x00, 0x00, 0x00, 0x00, 0x00)
    }

    @Test
    fun `u64 100000000 (1 APT in octas) matches spec`() {
        val s = BcsSerializer()
        s.serializeU64(100_000_000uL)
        s.toByteArray() shouldBe byteArrayOf(0x00.toByte(), 0xE1.toByte(), 0xF5.toByte(), 0x05, 0x00, 0x00, 0x00, 0x00)
    }

    @Test
    fun `u64 max matches spec`() {
        val s = BcsSerializer()
        s.serializeU64(ULong.MAX_VALUE)
        s.toByteArray() shouldBe ByteArray(8) { 0xFF.toByte() }
    }

    // -- Bytes spec vectors --

    @Test
    fun `empty bytes serializes as 0x00`() {
        val s = BcsSerializer()
        s.serializeBytes(byteArrayOf())
        s.toByteArray() shouldBe byteArrayOf(0x00)
    }

    @Test
    fun `single byte 42 serializes as 0x01 0x2a`() {
        val s = BcsSerializer()
        s.serializeBytes(byteArrayOf(42))
        s.toByteArray() shouldBe byteArrayOf(0x01, 0x2a)
    }

    @Test
    fun `three bytes 1 2 3 serializes as 0x03 0x01 0x02 0x03`() {
        val s = BcsSerializer()
        s.serializeBytes(byteArrayOf(1, 2, 3))
        s.toByteArray() shouldBe byteArrayOf(0x03, 0x01, 0x02, 0x03)
    }

    @Test
    fun `empty bytes deserializes correctly`() {
        val d = BcsDeserializer(byteArrayOf(0x00))
        d.deserializeBytes() shouldBe byteArrayOf()
    }

    @Test
    fun `single byte 42 deserializes correctly`() {
        val d = BcsDeserializer(byteArrayOf(0x01, 0x2a))
        d.deserializeBytes() shouldBe byteArrayOf(42)
    }

    // -- String spec vectors --

    @Test
    fun `hello world string matches spec`() {
        val s = BcsSerializer()
        s.serializeString("hello world")
        s.toByteArray() shouldBe
            byteArrayOf(
                0x0b,
                'h'.code.toByte(),
                'e'.code.toByte(),
                'l'.code.toByte(),
                'l'.code.toByte(),
                'o'.code.toByte(),
                ' '.code.toByte(),
                'w'.code.toByte(),
                'o'.code.toByte(),
                'r'.code.toByte(),
                'l'.code.toByte(),
                'd'.code.toByte(),
            )
    }

    @Test
    fun `hello world string deserializes correctly`() {
        val bytes =
            byteArrayOf(
                0x0b,
                'h'.code.toByte(),
                'e'.code.toByte(),
                'l'.code.toByte(),
                'l'.code.toByte(),
                'o'.code.toByte(),
                ' '.code.toByte(),
                'w'.code.toByte(),
                'o'.code.toByte(),
                'r'.code.toByte(),
                'l'.code.toByte(),
                'd'.code.toByte(),
            )
        val d = BcsDeserializer(bytes)
        d.deserializeString() shouldBe "hello world"
    }

    // -- Option spec vectors --

    @Test
    fun `option none serializes as 0x00`() {
        val s = BcsSerializer()
        s.serializeOptionTag(false)
        s.toByteArray() shouldBe byteArrayOf(0x00)
    }

    @Test
    fun `option some u64 42 serializes as 0x01 plus LE u64`() {
        val s = BcsSerializer()
        s.serializeOptionTag(true)
        s.serializeU64(42uL)
        s.toByteArray() shouldBe
            byteArrayOf(
                0x01,
                0x2a,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
            )
    }

    @Test
    fun `option some bool true serializes as 0x01 0x01`() {
        val s = BcsSerializer()
        s.serializeOptionTag(true)
        s.serializeBool(true)
        s.toByteArray() shouldBe byteArrayOf(0x01, 0x01)
    }

    // -- Vector spec vectors --

    @Test
    fun `empty vector of u8 serializes as 0x00`() {
        val s = BcsSerializer()
        s.serializeSequenceLength(0)
        s.toByteArray() shouldBe byteArrayOf(0x00)
    }

    @Test
    fun `vector of u8 (1,2,3,4,5) matches spec`() {
        val s = BcsSerializer()
        s.serializeSequenceLength(5)
        for (b in listOf(1, 2, 3, 4, 5)) {
            s.serializeU8(b.toUByte())
        }
        s.toByteArray() shouldBe byteArrayOf(0x05, 0x01, 0x02, 0x03, 0x04, 0x05)
    }

    @Test
    fun `vector of u64 (1,2) matches spec`() {
        val s = BcsSerializer()
        s.serializeSequenceLength(2)
        s.serializeU64(1uL)
        s.serializeU64(2uL)
        s.toByteArray() shouldBe
            byteArrayOf(
                0x02,
                0x01,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x02,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
            )
    }

    @Test
    fun `vector of bool (true, false, true) matches spec`() {
        val s = BcsSerializer()
        s.serializeSequenceLength(3)
        s.serializeBool(true)
        s.serializeBool(false)
        s.serializeBool(true)
        s.toByteArray() shouldBe byteArrayOf(0x03, 0x01, 0x00, 0x01)
    }

    // -- Additional ULEB128 vectors --

    @Test
    fun `ULEB128 256 encodes as 0x80 0x02`() {
        val s = BcsSerializer()
        s.serializeUleb128(256u)
        s.toByteArray() shouldBe byteArrayOf(0x80.toByte(), 0x02)
    }

    @Test
    fun `ULEB128 2097151 encodes as 0xFF 0xFF 0x7F`() {
        val s = BcsSerializer()
        s.serializeUleb128(2097151u)
        s.toByteArray() shouldBe byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x7F)
    }

    @Test
    fun `ULEB128 2097152 encodes as 0x80 0x80 0x80 0x01`() {
        val s = BcsSerializer()
        s.serializeUleb128(2097152u)
        s.toByteArray() shouldBe byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x01)
    }

    // -- Struct serialization: fields in declaration order --

    @Test
    fun `struct with u8 and u64 serializes fields in order`() {
        // Simulates struct { a: u8, b: u64 } with a=1, b=100
        val s = BcsSerializer()
        s.serializeU8(1u)
        s.serializeU64(100uL)
        s.toByteArray() shouldBe
            byteArrayOf(
                0x01,
                0x64,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
            )
    }

    @Test
    fun `struct with string and u64 serializes fields in order`() {
        // Simulates struct { name: String, value: u64 } with name="test", value=42
        val s = BcsSerializer()
        s.serializeString("test")
        s.serializeU64(42uL)
        s.toByteArray() shouldBe
            byteArrayOf(
                0x04,
                't'.code.toByte(),
                'e'.code.toByte(),
                's'.code.toByte(),
                't'.code.toByte(),
                0x2a,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
            )
    }

    // -- Enum serialization: variant index + variant data --

    @Test
    fun `enum variant 0 with no data serializes as 0x00`() {
        val s = BcsSerializer()
        s.serializeVariantIndex(0u)
        s.toByteArray() shouldBe byteArrayOf(0x00)
    }

    @Test
    fun `enum variant 1 with u64 42 matches spec`() {
        val s = BcsSerializer()
        s.serializeVariantIndex(1u)
        s.serializeU64(42uL)
        s.toByteArray() shouldBe
            byteArrayOf(
                0x01,
                0x2a,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
            )
    }

    @Test
    fun `enum variant 2 with string hello matches spec`() {
        val s = BcsSerializer()
        s.serializeVariantIndex(2u)
        s.serializeString("hello")
        s.toByteArray() shouldBe
            byteArrayOf(
                0x02,
                0x05,
                'h'.code.toByte(),
                'e'.code.toByte(),
                'l'.code.toByte(),
                'l'.code.toByte(),
                'o'.code.toByte(),
            )
    }

    // -- Deserialization error: truncated vector --

    @Test
    fun `truncated vector deserialization fails`() {
        // Length prefix says 5, but only 3 bytes of data follow
        val input = byteArrayOf(0x05, 0x01, 0x02, 0x03)
        val d = BcsDeserializer(input)
        val length = d.deserializeUleb128()
        length shouldBe 5u
        shouldThrow<BcsDeserializationException> {
            // Try to read 5 bytes but only 3 remain
            d.deserializeFixedBytes(5)
        }
    }

    companion object {
        @JvmStatic
        fun u8Vectors(): Stream<Arguments> = Stream.of(
            Arguments.of(0, byteArrayOf(0x00)),
            Arguments.of(1, byteArrayOf(0x01)),
            Arguments.of(127, byteArrayOf(0x7F)),
            Arguments.of(128, byteArrayOf(0x80.toByte())),
            Arguments.of(255, byteArrayOf(0xFF.toByte())),
        )
    }
}
