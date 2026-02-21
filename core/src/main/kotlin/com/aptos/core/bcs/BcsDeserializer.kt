package com.aptos.core.bcs

import com.aptos.core.error.BcsDeserializationException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * BCS (Binary Canonical Serialization) deserializer.
 *
 * Decodes values from the BCS format used by the Aptos blockchain.
 */
class BcsDeserializer(private val input: ByteArray) {
    private var offset: Int = 0

    val remaining: Int get() = input.size - offset

    fun deserializeBool(): Boolean {
        val byte = readByte()
        return when (byte.toInt()) {
            0 -> false
            1 -> true
            else -> throw BcsDeserializationException("Invalid boolean value: $byte")
        }
    }

    fun deserializeU8(): UByte = readByte().toUByte()

    fun deserializeU16(): UShort {
        val bytes = readBytes(2)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short.toUShort()
    }

    fun deserializeU32(): UInt {
        val bytes = readBytes(4)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()
    }

    fun deserializeU64(): ULong {
        val bytes = readBytes(8)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).long.toULong()
    }

    fun deserializeU128(): BigInteger {
        val bytes = readBytes(16)
        return littleEndianToBigInt(bytes)
    }

    fun deserializeU256(): BigInteger {
        val bytes = readBytes(32)
        return littleEndianToBigInt(bytes)
    }

    fun deserializeBytes(): ByteArray {
        val length = deserializeUleb128()
        return readBytes(length.toInt())
    }

    fun deserializeString(): String {
        return deserializeBytes().toString(Charsets.UTF_8)
    }

    fun deserializeFixedBytes(length: Int): ByteArray {
        return readBytes(length)
    }

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

    fun deserializeSequenceLength(): Int = deserializeUleb128().toInt()

    fun deserializeVariantIndex(): UInt = deserializeUleb128()

    fun deserializeOptionTag(): Boolean = deserializeBool()

    private fun readByte(): Byte {
        if (offset >= input.size) {
            throw BcsDeserializationException(
                "Unexpected end of input at offset $offset (input length: ${input.size})"
            )
        }
        return input[offset++]
    }

    private fun readBytes(count: Int): ByteArray {
        if (offset + count > input.size) {
            throw BcsDeserializationException(
                "Unexpected end of input: need $count bytes at offset $offset (input length: ${input.size})"
            )
        }
        val result = input.copyOfRange(offset, offset + count)
        offset += count
        return result
    }

    companion object {
        fun littleEndianToBigInt(bytes: ByteArray): BigInteger {
            // Reverse to big-endian and prepend 0x00 to ensure positive interpretation
            val bigEndian = ByteArray(bytes.size + 1)
            bigEndian[0] = 0
            for (i in bytes.indices) {
                bigEndian[bytes.size - i] = bytes[i]
            }
            return BigInteger(bigEndian)
        }
    }
}
