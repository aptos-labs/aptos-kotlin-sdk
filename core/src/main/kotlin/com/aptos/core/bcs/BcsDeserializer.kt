package com.aptos.core.bcs

import com.aptos.core.error.BcsDeserializationException
import java.math.BigInteger

/**
 * BCS (Binary Canonical Serialization) deserializer.
 *
 * Decodes values from the BCS format used by the Aptos blockchain.
 * Reads sequentially from the input byte array, advancing an internal offset.
 * Check [remaining] to verify all bytes have been consumed after deserialization.
 *
 * @param input the BCS-encoded byte array to read from
 */
class BcsDeserializer(private val input: ByteArray) {
    private var offset: Int = 0

    /** The number of bytes remaining to be read. */
    val remaining: Int get() = input.size - offset

    /** Deserializes a boolean from a single byte (`0x00` = false, `0x01` = true). */
    fun deserializeBool(): Boolean {
        val byte = readByte()
        return when (byte.toInt()) {
            0 -> false
            1 -> true
            else -> throw BcsDeserializationException("Invalid boolean value: $byte")
        }
    }

    /** Deserializes an unsigned 8-bit integer from a single byte. */
    fun deserializeU8(): UByte = readByte().toUByte()

    /** Deserializes an unsigned 16-bit integer from 2 little-endian bytes. */
    fun deserializeU16(): UShort {
        ensureRemaining(2)
        val b0 = input[offset].toInt() and 0xFF
        val b1 = input[offset + 1].toInt() and 0xFF
        offset += 2
        return ((b1 shl 8) or b0).toUShort()
    }

    /** Deserializes an unsigned 32-bit integer from 4 little-endian bytes. */
    fun deserializeU32(): UInt {
        ensureRemaining(4)
        val b0 = input[offset].toLong() and 0xFF
        val b1 = input[offset + 1].toLong() and 0xFF
        val b2 = input[offset + 2].toLong() and 0xFF
        val b3 = input[offset + 3].toLong() and 0xFF
        offset += 4
        return (b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)).toUInt()
    }

    /** Deserializes an unsigned 64-bit integer from 8 little-endian bytes. */
    fun deserializeU64(): ULong {
        ensureRemaining(8)
        var value = 0uL
        for (i in 0 until 8) {
            value = value or ((input[offset + i].toULong() and 0xFFuL) shl (8 * i))
        }
        offset += 8
        return value
    }

    /** Deserializes an unsigned 128-bit integer from 16 little-endian bytes. */
    fun deserializeU128(): BigInteger {
        ensureRemaining(16)
        val value = littleEndianToBigInt(input, offset, 16)
        offset += 16
        return value
    }

    /** Deserializes an unsigned 256-bit integer from 32 little-endian bytes. */
    fun deserializeU256(): BigInteger {
        ensureRemaining(32)
        val value = littleEndianToBigInt(input, offset, 32)
        offset += 32
        return value
    }

    /** Deserializes a variable-length byte array (ULEB128 length prefix followed by bytes). */
    fun deserializeBytes(): ByteArray {
        val length = deserializeUleb128()
        return readBytes(length.toInt())
    }

    /** Deserializes a UTF-8 string (ULEB128 byte-length prefix followed by UTF-8 bytes). */
    fun deserializeString(): String = deserializeBytes().toString(Charsets.UTF_8)

    /** Reads exactly [length] raw bytes without a length prefix. */
    fun deserializeFixedBytes(length: Int): ByteArray = readBytes(length)

    /** Decodes a ULEB128-encoded unsigned 32-bit integer. */
    fun deserializeUleb128(): UInt {
        var value = 0L
        var shift = 0
        while (shift < 32) {
            val byte = readByte().toInt() and 0xFF
            value = value or ((byte.toLong() and 0x7F) shl shift)
            if (byte and 0x80 == 0) {
                if (value > UInt.MAX_VALUE.toLong()) {
                    throw BcsDeserializationException("ULEB128 value exceeds u32 range")
                }
                return value.toUInt()
            }
            shift += 7
        }
        throw BcsDeserializationException("ULEB128 too long for u32")
    }

    /** Deserializes a ULEB128-encoded sequence (vector) length. */
    fun deserializeSequenceLength(): Int = deserializeUleb128().toInt()

    /** Deserializes a ULEB128-encoded BCS enum/variant index. */
    fun deserializeVariantIndex(): UInt = deserializeUleb128()

    /** Deserializes an option tag: returns `true` for Some, `false` for None. */
    fun deserializeOptionTag(): Boolean = deserializeBool()

    private fun readByte(): Byte {
        ensureRemaining(1)
        return input[offset++]
    }

    private fun readBytes(count: Int): ByteArray {
        if (count < 0) {
            throw BcsDeserializationException("Negative byte count: $count")
        }
        ensureRemaining(count)
        val result = ByteArray(count)
        System.arraycopy(input, offset, result, 0, count)
        offset += count
        return result
    }

    private fun ensureRemaining(count: Int) {
        if (count < 0) {
            throw BcsDeserializationException("Negative byte count: $count")
        }
        if (count > remaining) {
            throw BcsDeserializationException(
                "Unexpected end of input: need $count bytes at offset $offset (input length: ${input.size})",
            )
        }
    }

    companion object {
        /**
         * Converts a full little-endian byte array to a non-negative [BigInteger].
         *
         * @param bytes the little-endian bytes
         * @return the corresponding non-negative [BigInteger]
         */
        fun littleEndianToBigInt(bytes: ByteArray): BigInteger = littleEndianToBigInt(bytes, 0, bytes.size)

        /**
         * Converts a slice of a little-endian byte array to a non-negative [BigInteger].
         *
         * @param bytes the source byte array
         * @param start the start offset within [bytes]
         * @param length the number of bytes to convert
         * @return the corresponding non-negative [BigInteger]
         */
        fun littleEndianToBigInt(bytes: ByteArray, start: Int, length: Int): BigInteger {
            // Reverse to big-endian and prepend 0x00 to ensure positive interpretation
            val bigEndian = ByteArray(length + 1)
            bigEndian[0] = 0
            for (i in 0 until length) {
                bigEndian[length - i] = bytes[start + i]
            }
            return BigInteger(bigEndian)
        }
    }
}
