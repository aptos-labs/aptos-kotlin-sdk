package com.aptos.core.bcs

import com.aptos.core.error.BcsSerializationException
import java.io.ByteArrayOutputStream
import java.math.BigInteger

/**
 * BCS (Binary Canonical Serialization) serializer.
 *
 * Encodes values into the BCS format used by the Aptos blockchain.
 * All multi-byte integers are encoded in little-endian byte order.
 * Sequences are length-prefixed with ULEB128, and structs are serialized
 * field-by-field in declaration order.
 *
 * Typical usage:
 * ```kotlin
 * val s = BcsSerializer()
 * s.serializeU64(42uL)
 * s.serializeString("hello")
 * val bytes = s.toByteArray()
 * ```
 *
 * @param initialCapacity the initial buffer capacity in bytes (default 256)
 */
class BcsSerializer(initialCapacity: Int = 256) {
    private val output = ByteArrayOutputStream(initialCapacity)

    /** Returns the serialized bytes accumulated so far. */
    fun toByteArray(): ByteArray = output.toByteArray()

    /** Serializes a boolean as a single byte: `0x00` for false, `0x01` for true. */
    fun serializeBool(value: Boolean) {
        output.write(if (value) 1 else 0)
    }

    /** Serializes an unsigned 8-bit integer as a single byte. */
    fun serializeU8(value: UByte) {
        output.write(value.toInt())
    }

    /** Serializes an unsigned 16-bit integer in little-endian byte order (2 bytes). */
    fun serializeU16(value: UShort) {
        val v = value.toInt()
        output.write(v and 0xFF)
        output.write((v ushr 8) and 0xFF)
    }

    /** Serializes an unsigned 32-bit integer in little-endian byte order (4 bytes). */
    fun serializeU32(value: UInt) {
        output.write((value and 0xFFu).toInt())
        output.write(((value shr 8) and 0xFFu).toInt())
        output.write(((value shr 16) and 0xFFu).toInt())
        output.write(((value shr 24) and 0xFFu).toInt())
    }

    /** Serializes an unsigned 64-bit integer in little-endian byte order (8 bytes). */
    fun serializeU64(value: ULong) {
        output.write((value and 0xFFuL).toInt())
        output.write(((value shr 8) and 0xFFuL).toInt())
        output.write(((value shr 16) and 0xFFuL).toInt())
        output.write(((value shr 24) and 0xFFuL).toInt())
        output.write(((value shr 32) and 0xFFuL).toInt())
        output.write(((value shr 40) and 0xFFuL).toInt())
        output.write(((value shr 48) and 0xFFuL).toInt())
        output.write(((value shr 56) and 0xFFuL).toInt())
    }

    /** Serializes a 128-bit unsigned integer in little-endian byte order (16 bytes). */
    fun serializeU128(value: BigInteger) {
        validateUnsigned(value, 128)
        val bytes = bigIntToLittleEndian(value, 16)
        output.write(bytes)
    }

    /** Serializes a 256-bit unsigned integer in little-endian byte order (32 bytes). */
    fun serializeU256(value: BigInteger) {
        validateUnsigned(value, 256)
        val bytes = bigIntToLittleEndian(value, 32)
        output.write(bytes)
    }

    /** Serializes a variable-length byte array with a ULEB128 length prefix. */
    fun serializeBytes(value: ByteArray) {
        serializeUleb128(value.size.toUInt())
        output.write(value)
    }

    /** Serializes a UTF-8 string with a ULEB128 byte-length prefix. */
    fun serializeString(value: String) {
        serializeBytes(value.toByteArray(Charsets.UTF_8))
    }

    /** Writes raw bytes without a length prefix (used for fixed-size fields like addresses). */
    fun serializeFixedBytes(value: ByteArray) {
        output.write(value)
    }

    /** Encodes an unsigned 32-bit integer using ULEB128 variable-length encoding. */
    fun serializeUleb128(value: UInt) {
        var remaining = value.toLong() and 0xFFFF_FFFFL
        while (true) {
            var byte = (remaining and 0x7F).toInt()
            remaining = remaining ushr 7
            if (remaining != 0L) {
                byte = byte or 0x80
            }
            output.write(byte)
            if (remaining == 0L) break
        }
    }

    /** Serializes a sequence (vector) length as ULEB128. */
    fun serializeSequenceLength(length: Int) {
        require(length >= 0) { "Sequence length must be non-negative: $length" }
        serializeUleb128(length.toUInt())
    }

    /** Serializes a BCS enum/variant index as ULEB128. */
    fun serializeVariantIndex(index: UInt) {
        serializeUleb128(index)
    }

    /** Serializes an option tag: `true` means Some (0x01), `false` means None (0x00). */
    fun serializeOptionTag(hasValue: Boolean) {
        serializeBool(hasValue)
    }

    /** Serializes a list of [BcsSerializable] items with a ULEB128 length prefix. */
    fun <T : BcsSerializable> serializeSequence(items: List<T>) {
        serializeSequenceLength(items.size)
        items.forEach { it.serialize(this) }
    }

    /** Serializes an optional value: `null` writes None, non-null writes Some + value. */
    fun <T : BcsSerializable> serializeOption(value: T?) {
        if (value != null) {
            serializeOptionTag(true)
            value.serialize(this)
        } else {
            serializeOptionTag(false)
        }
    }

    private fun validateUnsigned(value: BigInteger, bits: Int) {
        if (value.signum() < 0) {
            throw BcsSerializationException("u$bits value must not be negative: $value")
        }
        if (value.bitLength() > bits) {
            throw BcsSerializationException("u$bits value exceeds $bits bits: $value")
        }
    }

    companion object {
        /**
         * Converts a non-negative [BigInteger] to a fixed-size little-endian byte array.
         *
         * @param value the non-negative integer to convert
         * @param length the desired output length in bytes
         * @return little-endian byte array, zero-padded on the right if needed
         */
        fun bigIntToLittleEndian(value: BigInteger, length: Int): ByteArray {
            val result = ByteArray(length)
            val bigEndian = value.toByteArray()
            // BigInteger.toByteArray() is big-endian with possible leading sign byte
            val startIndex = if (bigEndian.size > length && bigEndian[0] == 0.toByte()) 1 else 0
            val copyLen = minOf(bigEndian.size - startIndex, length)
            // Reverse into little-endian
            for (i in 0 until copyLen) {
                result[i] = bigEndian[bigEndian.size - 1 - i]
            }
            return result
        }
    }
}
