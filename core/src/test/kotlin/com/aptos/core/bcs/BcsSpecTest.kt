package com.aptos.core.bcs

import com.aptos.core.error.BcsDeserializationException
import com.aptos.core.error.BcsSerializationException
import com.aptos.core.types.AccountAddress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger
import java.util.stream.Stream

/**
 * Comprehensive spec-based tests for the BCS (Binary Canonical Serialization)
 * implementation, validated against the official aptos-sdk-specs test vectors.
 *
 * See: https://github.com/aptos-foundation/aptos-sdk-specs
 */
class BcsSpecTest {
    // ---------------------------------------------------------------
    // ULEB128 test vectors from the spec
    //
    // | value  | bytes                  |
    // | 0      | [0x00]                 |
    // | 1      | [0x01]                 |
    // | 127    | [0x7f]                 |
    // | 128    | [0x80, 0x01]           |
    // | 255    | [0xff, 0x01]           |
    // | 16383  | [0xff, 0x7f]           |
    // | 16384  | [0x80, 0x80, 0x01]     |
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "ULEB128 serialize: {0} -> {1}")
    @MethodSource("uleb128Vectors")
    fun `serialize ULEB128 matches spec vector`(value: Long, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeUleb128(value.toUInt())
        serializer.toByteArray() shouldBe expectedBytes
    }

    @ParameterizedTest(name = "ULEB128 deserialize: {1} -> {0}")
    @MethodSource("uleb128Vectors")
    fun `deserialize ULEB128 matches spec vector`(expectedValue: Long, bytes: ByteArray) {
        val deserializer = BcsDeserializer(bytes)
        deserializer.deserializeUleb128() shouldBe expectedValue.toUInt()
    }

    @ParameterizedTest(name = "ULEB128 roundtrip: {0}")
    @MethodSource("uleb128Vectors")
    fun `ULEB128 roundtrip matches spec vector`(value: Long, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeUleb128(value.toUInt())
        val serialized = serializer.toByteArray()
        serialized shouldBe expectedBytes

        val deserializer = BcsDeserializer(serialized)
        deserializer.deserializeUleb128() shouldBe value.toUInt()
        deserializer.remaining shouldBe 0
    }

    // ---------------------------------------------------------------
    // u16 test vectors from the spec
    //
    // | value  | bytes          |
    // | 0x00   | [0x00, 0x00]   |
    // | 0x1234 | [0x34, 0x12]   |
    // | 65535  | [0xFF, 0xFF]   |
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "u16 serialize: {0} -> {1}")
    @MethodSource("u16Vectors")
    fun `serialize u16 matches spec vector`(value: Int, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeU16(value.toUShort())
        serializer.toByteArray() shouldBe expectedBytes
    }

    @ParameterizedTest(name = "u16 deserialize: {1} -> {0}")
    @MethodSource("u16Vectors")
    fun `deserialize u16 matches spec vector`(expectedValue: Int, bytes: ByteArray) {
        val deserializer = BcsDeserializer(bytes)
        deserializer.deserializeU16() shouldBe expectedValue.toUShort()
    }

    @ParameterizedTest(name = "u16 roundtrip: {0}")
    @MethodSource("u16Vectors")
    fun `u16 roundtrip matches spec vector`(value: Int, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeU16(value.toUShort())
        val serialized = serializer.toByteArray()
        serialized shouldBe expectedBytes

        val deserializer = BcsDeserializer(serialized)
        deserializer.deserializeU16() shouldBe value.toUShort()
        deserializer.remaining shouldBe 0
    }

    // ---------------------------------------------------------------
    // u32 test vectors from the spec
    //
    // | value      | bytes                      |
    // | 0          | [0x00, 0x00, 0x00, 0x00]   |
    // | 0x12345678 | [0x78, 0x56, 0x34, 0x12]   |
    // | 0xFFFFFFFF | [0xFF, 0xFF, 0xFF, 0xFF]   |
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "u32 serialize: {0} -> {1}")
    @MethodSource("u32Vectors")
    fun `serialize u32 matches spec vector`(value: Long, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeU32(value.toUInt())
        serializer.toByteArray() shouldBe expectedBytes
    }

    @ParameterizedTest(name = "u32 deserialize: {1} -> {0}")
    @MethodSource("u32Vectors")
    fun `deserialize u32 matches spec vector`(expectedValue: Long, bytes: ByteArray) {
        val deserializer = BcsDeserializer(bytes)
        deserializer.deserializeU32() shouldBe expectedValue.toUInt()
    }

    @ParameterizedTest(name = "u32 roundtrip: {0}")
    @MethodSource("u32Vectors")
    fun `u32 roundtrip matches spec vector`(value: Long, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeU32(value.toUInt())
        val serialized = serializer.toByteArray()
        serialized shouldBe expectedBytes

        val deserializer = BcsDeserializer(serialized)
        deserializer.deserializeU32() shouldBe value.toUInt()
        deserializer.remaining shouldBe 0
    }

    // ---------------------------------------------------------------
    // u64 test vectors from the spec
    //
    // | value                | bytes                                              |
    // | 0                    | [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]  |
    // | 0x123456789abcdef0   | [0xf0, 0xde, 0xbc, 0x9a, 0x78, 0x56, 0x34, 0x12]  |
    // | 0xFFFFFFFFFFFFFFFF   | [0xFF x 8]                                         |
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "u64 serialize: {0}")
    @MethodSource("u64Vectors")
    fun `serialize u64 matches spec vector`(valueHex: String, expectedBytes: ByteArray) {
        val value = BigInteger(valueHex, 16).toLong().toULong()
        val serializer = BcsSerializer()
        serializer.serializeU64(value)
        serializer.toByteArray() shouldBe expectedBytes
    }

    @ParameterizedTest(name = "u64 deserialize: {0}")
    @MethodSource("u64Vectors")
    fun `deserialize u64 matches spec vector`(valueHex: String, bytes: ByteArray) {
        val expected = BigInteger(valueHex, 16).toLong().toULong()
        val deserializer = BcsDeserializer(bytes)
        deserializer.deserializeU64() shouldBe expected
    }

    @ParameterizedTest(name = "u64 roundtrip: {0}")
    @MethodSource("u64Vectors")
    fun `u64 roundtrip matches spec vector`(valueHex: String, expectedBytes: ByteArray) {
        val value = BigInteger(valueHex, 16).toLong().toULong()
        val serializer = BcsSerializer()
        serializer.serializeU64(value)
        val serialized = serializer.toByteArray()
        serialized shouldBe expectedBytes

        val deserializer = BcsDeserializer(serialized)
        deserializer.deserializeU64() shouldBe value
        deserializer.remaining shouldBe 0
    }

    // ---------------------------------------------------------------
    // u128 test vectors from the spec
    //
    // | value                                  | bytes (little-endian)              |
    // | 0x000102030405060708090A0B0C0D0E0F     | [0x0F..0x00]                       |
    // | 0                                      | [0x00 x 16]                        |
    // | 2^128 - 1                              | [0xFF x 16]                        |
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "u128 serialize: {0}")
    @MethodSource("u128Vectors")
    fun `serialize u128 matches spec vector`(value: BigInteger, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeU128(value)
        serializer.toByteArray() shouldBe expectedBytes
    }

    @ParameterizedTest(name = "u128 deserialize: {0}")
    @MethodSource("u128Vectors")
    fun `deserialize u128 matches spec vector`(expectedValue: BigInteger, bytes: ByteArray) {
        val deserializer = BcsDeserializer(bytes)
        deserializer.deserializeU128() shouldBe expectedValue
    }

    @ParameterizedTest(name = "u128 roundtrip: {0}")
    @MethodSource("u128Vectors")
    fun `u128 roundtrip matches spec vector`(value: BigInteger, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeU128(value)
        val serialized = serializer.toByteArray()
        serialized shouldBe expectedBytes

        val deserializer = BcsDeserializer(serialized)
        deserializer.deserializeU128() shouldBe value
        deserializer.remaining shouldBe 0
    }

    // ---------------------------------------------------------------
    // u256 test vectors from the spec
    //
    // | value (64 hex chars)                                            | bytes              |
    // | 0x000102030405060708090A0B0C0D0E0F101112131415161718191A1B...1F | reversed           |
    // | 0                                                               | [0x00 x 32]        |
    // | 2^256 - 1                                                       | [0xFF x 32]        |
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "u256 serialize: {0}")
    @MethodSource("u256Vectors")
    fun `serialize u256 matches spec vector`(value: BigInteger, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeU256(value)
        serializer.toByteArray() shouldBe expectedBytes
    }

    @ParameterizedTest(name = "u256 deserialize: {0}")
    @MethodSource("u256Vectors")
    fun `deserialize u256 matches spec vector`(expectedValue: BigInteger, bytes: ByteArray) {
        val deserializer = BcsDeserializer(bytes)
        deserializer.deserializeU256() shouldBe expectedValue
    }

    @ParameterizedTest(name = "u256 roundtrip: {0}")
    @MethodSource("u256Vectors")
    fun `u256 roundtrip matches spec vector`(value: BigInteger, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeU256(value)
        val serialized = serializer.toByteArray()
        serialized shouldBe expectedBytes

        val deserializer = BcsDeserializer(serialized)
        deserializer.deserializeU256() shouldBe value
        deserializer.remaining shouldBe 0
    }

    // ---------------------------------------------------------------
    // String test vectors from the spec
    //
    // | string   | bytes                              |
    // | "hello"  | [0x05, h, e, l, l, o]              |
    // | ""       | [0x00]                              |
    // | "hello"  | [0x06, h, <c3><a9>, l, l, o]       |
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "string serialize: \"{0}\"")
    @MethodSource("stringVectors")
    fun `serialize string matches spec vector`(value: String, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeString(value)
        serializer.toByteArray() shouldBe expectedBytes
    }

    @ParameterizedTest(name = "string deserialize: \"{0}\"")
    @MethodSource("stringVectors")
    fun `deserialize string matches spec vector`(expectedValue: String, bytes: ByteArray) {
        val deserializer = BcsDeserializer(bytes)
        deserializer.deserializeString() shouldBe expectedValue
    }

    @ParameterizedTest(name = "string roundtrip: \"{0}\"")
    @MethodSource("stringVectors")
    fun `string roundtrip matches spec vector`(value: String, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeString(value)
        val serialized = serializer.toByteArray()
        serialized shouldBe expectedBytes

        val deserializer = BcsDeserializer(serialized)
        deserializer.deserializeString() shouldBe value
        deserializer.remaining shouldBe 0
    }

    // ---------------------------------------------------------------
    // Boolean test vectors from the spec
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "bool serialize: {0}")
    @MethodSource("boolVectors")
    fun `serialize bool matches spec vector`(value: Boolean, expectedBytes: ByteArray) {
        val serializer = BcsSerializer()
        serializer.serializeBool(value)
        serializer.toByteArray() shouldBe expectedBytes
    }

    @ParameterizedTest(name = "bool deserialize: {0}")
    @MethodSource("boolVectors")
    fun `deserialize bool matches spec vector`(expectedValue: Boolean, bytes: ByteArray) {
        val deserializer = BcsDeserializer(bytes)
        deserializer.deserializeBool() shouldBe expectedValue
    }

    // ---------------------------------------------------------------
    // AccountAddress serialization test vectors from the spec
    //
    // "0x1" -> 32 bytes, byte 31 = 0x01, rest zeros
    // ---------------------------------------------------------------

    @Test
    fun `serialize AccountAddress 0x1 matches spec vector`() {
        val address = AccountAddress.fromHex("0x1")
        val serializer = BcsSerializer()
        address.serialize(serializer)
        val bytes = serializer.toByteArray()

        // AccountAddress is always 32 bytes, no length prefix (fixed bytes)
        bytes.size shouldBe 32

        // First 31 bytes should be zero
        for (i in 0 until 31) {
            bytes[i] shouldBe 0x00.toByte()
        }

        // Last byte (byte index 31) should be 0x01
        bytes[31] shouldBe 0x01.toByte()
    }

    @Test
    fun `deserialize AccountAddress 0x1 matches spec vector`() {
        val input = ByteArray(32)
        input[31] = 0x01
        val deserializer = BcsDeserializer(input)
        val address = AccountAddress.fromBcs(deserializer)
        address shouldBe AccountAddress.fromHex("0x1")
        deserializer.remaining shouldBe 0
    }

    @Test
    fun `serialize AccountAddress zero matches spec vector`() {
        val address = AccountAddress.ZERO
        val serializer = BcsSerializer()
        address.serialize(serializer)
        val bytes = serializer.toByteArray()

        bytes.size shouldBe 32
        bytes shouldBe ByteArray(32)
    }

    @Test
    fun `AccountAddress roundtrip serialization`() {
        val original = AccountAddress.fromHex("0x1")
        val serializer = BcsSerializer()
        original.serialize(serializer)
        val bytes = serializer.toByteArray()

        val deserializer = BcsDeserializer(bytes)
        val restored = AccountAddress.fromBcs(deserializer)
        restored shouldBe original
        deserializer.remaining shouldBe 0
    }

    @Test
    fun `serialize AccountAddress with full hex matches spec vector`() {
        val hex = "0x000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
        val address = AccountAddress.fromHexRelaxed(hex)
        val serializer = BcsSerializer()
        address.serialize(serializer)
        val bytes = serializer.toByteArray()

        bytes.size shouldBe 32
        // Serialized as fixed bytes in the exact order of the address data
        for (i in 0 until 32) {
            bytes[i] shouldBe i.toByte()
        }
    }

    // ---------------------------------------------------------------
    // Error cases from the spec
    //
    // Deserialize [0x01, 0x02] as u64 -> fail
    // Deserialize [0x02] as boolean -> fail
    // ---------------------------------------------------------------

    @Test
    fun `deserialize u64 from too few bytes should fail`() {
        // Spec: Deserialize [0x01, 0x02] as u64 -> fail
        val input = byteArrayOf(0x01, 0x02)
        val deserializer = BcsDeserializer(input)
        shouldThrow<BcsDeserializationException> {
            deserializer.deserializeU64()
        }
    }

    @Test
    fun `deserialize boolean from invalid byte 0x02 should fail`() {
        // Spec: Deserialize [0x02] as boolean -> fail
        val input = byteArrayOf(0x02)
        val deserializer = BcsDeserializer(input)
        shouldThrow<BcsDeserializationException> {
            deserializer.deserializeBool()
        }
    }

    @Test
    fun `deserialize boolean from byte 0xFF should fail`() {
        val input = byteArrayOf(0xFF.toByte())
        val deserializer = BcsDeserializer(input)
        shouldThrow<BcsDeserializationException> {
            deserializer.deserializeBool()
        }
    }

    @Test
    fun `deserialize u32 from too few bytes should fail`() {
        val input = byteArrayOf(0x01, 0x02)
        val deserializer = BcsDeserializer(input)
        shouldThrow<BcsDeserializationException> {
            deserializer.deserializeU32()
        }
    }

    @Test
    fun `deserialize u16 from too few bytes should fail`() {
        val input = byteArrayOf(0x01)
        val deserializer = BcsDeserializer(input)
        shouldThrow<BcsDeserializationException> {
            deserializer.deserializeU16()
        }
    }

    @Test
    fun `deserialize u128 from too few bytes should fail`() {
        val input = ByteArray(8) { 0x01 }
        val deserializer = BcsDeserializer(input)
        shouldThrow<BcsDeserializationException> {
            deserializer.deserializeU128()
        }
    }

    @Test
    fun `deserialize u256 from too few bytes should fail`() {
        val input = ByteArray(16) { 0x01 }
        val deserializer = BcsDeserializer(input)
        shouldThrow<BcsDeserializationException> {
            deserializer.deserializeU256()
        }
    }

    @Test
    fun `deserialize from empty input should fail`() {
        val deserializer = BcsDeserializer(byteArrayOf())
        shouldThrow<BcsDeserializationException> {
            deserializer.deserializeU8()
        }
    }

    @Test
    fun `deserialize string with insufficient data for length prefix should fail`() {
        // Length prefix says 5 bytes, but only 2 bytes of data follow
        val input = byteArrayOf(0x05, 0x68, 0x65)
        val deserializer = BcsDeserializer(input)
        shouldThrow<BcsDeserializationException> {
            deserializer.deserializeString()
        }
    }

    @Test
    fun `serialize u128 with negative value should fail`() {
        val serializer = BcsSerializer()
        shouldThrow<BcsSerializationException> {
            serializer.serializeU128(BigInteger.valueOf(-1))
        }
    }

    @Test
    fun `serialize u128 with value exceeding 128 bits should fail`() {
        val serializer = BcsSerializer()
        val tooLarge = BigInteger.TWO.pow(128)
        shouldThrow<BcsSerializationException> {
            serializer.serializeU128(tooLarge)
        }
    }

    @Test
    fun `serialize u256 with negative value should fail`() {
        val serializer = BcsSerializer()
        shouldThrow<BcsSerializationException> {
            serializer.serializeU256(BigInteger.valueOf(-1))
        }
    }

    @Test
    fun `serialize u256 with value exceeding 256 bits should fail`() {
        val serializer = BcsSerializer()
        val tooLarge = BigInteger.TWO.pow(256)
        shouldThrow<BcsSerializationException> {
            serializer.serializeU256(tooLarge)
        }
    }

    // ---------------------------------------------------------------
    // Additional spec conformance: boundary values
    // ---------------------------------------------------------------

    @Test
    fun `u8 zero serializes correctly`() {
        val serializer = BcsSerializer()
        serializer.serializeU8(0u)
        serializer.toByteArray() shouldBe byteArrayOf(0x00)
    }

    @Test
    fun `u8 max serializes correctly`() {
        val serializer = BcsSerializer()
        serializer.serializeU8(0xFFu)
        serializer.toByteArray() shouldBe byteArrayOf(0xFF.toByte())
    }

    @Test
    fun `u64 zero serializes correctly`() {
        val serializer = BcsSerializer()
        serializer.serializeU64(0uL)
        serializer.toByteArray() shouldBe ByteArray(8)
    }

    @Test
    fun `u128 zero serializes correctly`() {
        val serializer = BcsSerializer()
        serializer.serializeU128(BigInteger.ZERO)
        serializer.toByteArray() shouldBe ByteArray(16)
    }

    @Test
    fun `u128 max serializes correctly`() {
        val max128 = BigInteger.TWO.pow(128).subtract(BigInteger.ONE)
        val serializer = BcsSerializer()
        serializer.serializeU128(max128)
        serializer.toByteArray() shouldBe ByteArray(16) { 0xFF.toByte() }
    }

    @Test
    fun `u256 zero serializes correctly`() {
        val serializer = BcsSerializer()
        serializer.serializeU256(BigInteger.ZERO)
        serializer.toByteArray() shouldBe ByteArray(32)
    }

    @Test
    fun `u256 max serializes correctly`() {
        val max256 = BigInteger.TWO.pow(256).subtract(BigInteger.ONE)
        val serializer = BcsSerializer()
        serializer.serializeU256(max256)
        serializer.toByteArray() shouldBe ByteArray(32) { 0xFF.toByte() }
    }

    // ---------------------------------------------------------------
    // Spec conformance: sequence (vector) length prefix uses ULEB128
    // ---------------------------------------------------------------

    @Test
    fun `sequence length is encoded as ULEB128`() {
        val serializer = BcsSerializer()
        serializer.serializeSequenceLength(128)
        // 128 as ULEB128 = [0x80, 0x01]
        serializer.toByteArray() shouldBe byteArrayOf(0x80.toByte(), 0x01)
    }

    @Test
    fun `variant index is encoded as ULEB128`() {
        val serializer = BcsSerializer()
        serializer.serializeVariantIndex(128u)
        serializer.toByteArray() shouldBe byteArrayOf(0x80.toByte(), 0x01)
    }

    // ---------------------------------------------------------------
    // Spec conformance: option encoding
    // ---------------------------------------------------------------

    @Test
    fun `option none serializes as single zero byte`() {
        val serializer = BcsSerializer()
        serializer.serializeOptionTag(false)
        serializer.toByteArray() shouldBe byteArrayOf(0x00)
    }

    @Test
    fun `option some serializes with leading 0x01`() {
        val serializer = BcsSerializer()
        serializer.serializeOptionTag(true)
        serializer.toByteArray() shouldBe byteArrayOf(0x01)
    }

    // ---------------------------------------------------------------
    // Spec conformance: composite roundtrip
    // ---------------------------------------------------------------

    @Test
    fun `full composite roundtrip preserves all values`() {
        val serializer = BcsSerializer()
        serializer.serializeBool(false)
        serializer.serializeBool(true)
        serializer.serializeU8(0xABu)
        serializer.serializeU16(0x1234u)
        serializer.serializeU32(0x12345678u)
        serializer.serializeU64(0x123456789abcdef0uL)
        serializer.serializeU128(BigInteger("000102030405060708090A0B0C0D0E0F", 16))
        serializer.serializeString("hello")
        serializer.serializeString("")
        serializer.serializeString("h\u00E9llo")

        val bytes = serializer.toByteArray()
        val deserializer = BcsDeserializer(bytes)

        deserializer.deserializeBool() shouldBe false
        deserializer.deserializeBool() shouldBe true
        deserializer.deserializeU8() shouldBe 0xABu.toUByte()
        deserializer.deserializeU16() shouldBe 0x1234u.toUShort()
        deserializer.deserializeU32() shouldBe 0x12345678u
        deserializer.deserializeU64() shouldBe 0x123456789abcdef0uL
        deserializer.deserializeU128() shouldBe BigInteger("000102030405060708090A0B0C0D0E0F", 16)
        deserializer.deserializeString() shouldBe "hello"
        deserializer.deserializeString() shouldBe ""
        deserializer.deserializeString() shouldBe "h\u00E9llo"
        deserializer.remaining shouldBe 0
    }

    // ---------------------------------------------------------------
    // Test vector providers
    // ---------------------------------------------------------------

    companion object {
        /**
         * ULEB128 test vectors from the aptos-sdk-specs.
         *
         * Values are passed as Long to avoid JUnit 5 parameter resolution
         * issues with Kotlin unsigned inline classes.
         */
        @JvmStatic
        fun uleb128Vectors(): Stream<Arguments> = Stream.of(
            Arguments.of(0L, byteArrayOf(0x00)),
            Arguments.of(1L, byteArrayOf(0x01)),
            Arguments.of(127L, byteArrayOf(0x7F)),
            Arguments.of(128L, byteArrayOf(0x80.toByte(), 0x01)),
            Arguments.of(255L, byteArrayOf(0xFF.toByte(), 0x01)),
            Arguments.of(16383L, byteArrayOf(0xFF.toByte(), 0x7F)),
            Arguments.of(16384L, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x01)),
        )

        /**
         * u16 test vectors from the aptos-sdk-specs (little-endian encoding).
         *
         * Values are passed as Int to avoid JUnit 5 parameter resolution
         * issues with Kotlin unsigned inline classes.
         */
        @JvmStatic
        fun u16Vectors(): Stream<Arguments> = Stream.of(
            Arguments.of(0x0000, byteArrayOf(0x00, 0x00)),
            Arguments.of(0x1234, byteArrayOf(0x34, 0x12)),
            Arguments.of(65535, byteArrayOf(0xFF.toByte(), 0xFF.toByte())),
        )

        /**
         * u32 test vectors from the aptos-sdk-specs (little-endian encoding).
         *
         * Values are passed as Long to support the full u32 range (0 to 0xFFFFFFFF).
         */
        @JvmStatic
        fun u32Vectors(): Stream<Arguments> = Stream.of(
            Arguments.of(0L, byteArrayOf(0x00, 0x00, 0x00, 0x00)),
            Arguments.of(0x12345678L, byteArrayOf(0x78, 0x56, 0x34, 0x12)),
            Arguments.of(0xFFFFFFFFL, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())),
        )

        /**
         * u64 test vectors from the aptos-sdk-specs (little-endian encoding).
         *
         * Values are passed as hex strings since ULong cannot be used as JUnit
         * parameterized test arguments directly.
         */
        @JvmStatic
        fun u64Vectors(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "0",
                ByteArray(8),
            ),
            Arguments.of(
                "123456789abcdef0",
                byteArrayOf(
                    0xf0.toByte(),
                    0xde.toByte(),
                    0xbc.toByte(),
                    0x9a.toByte(),
                    0x78,
                    0x56,
                    0x34,
                    0x12,
                ),
            ),
            Arguments.of(
                "FFFFFFFFFFFFFFFF",
                ByteArray(8) { 0xFF.toByte() },
            ),
        )

        /**
         * u128 test vectors from the aptos-sdk-specs (little-endian encoding).
         */
        @JvmStatic
        fun u128Vectors(): Stream<Arguments> = Stream.of(
            // 0x000102030405060708090A0B0C0D0E0F -> reversed byte order
            Arguments.of(
                BigInteger("000102030405060708090A0B0C0D0E0F", 16),
                byteArrayOf(
                    0x0F,
                    0x0E,
                    0x0D,
                    0x0C,
                    0x0B,
                    0x0A,
                    0x09,
                    0x08,
                    0x07,
                    0x06,
                    0x05,
                    0x04,
                    0x03,
                    0x02,
                    0x01,
                    0x00,
                ),
            ),
            // Zero
            Arguments.of(
                BigInteger.ZERO,
                ByteArray(16),
            ),
            // Max u128 = 2^128 - 1
            Arguments.of(
                BigInteger.TWO.pow(128).subtract(BigInteger.ONE),
                ByteArray(16) { 0xFF.toByte() },
            ),
        )

        /**
         * u256 test vectors from the aptos-sdk-specs (little-endian encoding).
         */
        @JvmStatic
        fun u256Vectors(): Stream<Arguments> = Stream.of(
            // 0x000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F
            // -> reversed byte order
            Arguments.of(
                BigInteger("000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F", 16),
                byteArrayOf(
                    0x1F,
                    0x1E,
                    0x1D,
                    0x1C,
                    0x1B,
                    0x1A,
                    0x19,
                    0x18,
                    0x17,
                    0x16,
                    0x15,
                    0x14,
                    0x13,
                    0x12,
                    0x11,
                    0x10,
                    0x0F,
                    0x0E,
                    0x0D,
                    0x0C,
                    0x0B,
                    0x0A,
                    0x09,
                    0x08,
                    0x07,
                    0x06,
                    0x05,
                    0x04,
                    0x03,
                    0x02,
                    0x01,
                    0x00,
                ),
            ),
            // Zero
            Arguments.of(
                BigInteger.ZERO,
                ByteArray(32),
            ),
            // Max u256 = 2^256 - 1
            Arguments.of(
                BigInteger.TWO.pow(256).subtract(BigInteger.ONE),
                ByteArray(32) { 0xFF.toByte() },
            ),
        )

        /**
         * String test vectors from the aptos-sdk-specs.
         * Strings are encoded as ULEB128(length_in_bytes) followed by UTF-8 bytes.
         */
        @JvmStatic
        fun stringVectors(): Stream<Arguments> = Stream.of(
            // "hello" -> [0x05, 'h', 'e', 'l', 'l', 'o']
            Arguments.of(
                "hello",
                byteArrayOf(
                    0x05,
                    'h'.code.toByte(),
                    'e'.code.toByte(),
                    'l'.code.toByte(),
                    'l'.code.toByte(),
                    'o'.code.toByte(),
                ),
            ),
            // "" -> [0x00]
            Arguments.of(
                "",
                byteArrayOf(0x00),
            ),
            // "hello" with e-acute (U+00E9): UTF-8 encodes as 0xC3 0xA9 -> 6 bytes total
            // length prefix is 0x06 (6 bytes in UTF-8)
            Arguments.of(
                "h\u00E9llo",
                byteArrayOf(
                    0x06,
                    'h'.code.toByte(),
                    0xC3.toByte(),
                    0xA9.toByte(), // e-acute in UTF-8
                    'l'.code.toByte(),
                    'l'.code.toByte(),
                    'o'.code.toByte(),
                ),
            ),
        )

        /**
         * Boolean test vectors.
         */
        @JvmStatic
        fun boolVectors(): Stream<Arguments> = Stream.of(
            Arguments.of(false, byteArrayOf(0x00)),
            Arguments.of(true, byteArrayOf(0x01)),
        )
    }
}
