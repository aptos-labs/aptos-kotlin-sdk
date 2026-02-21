package com.aptos.core.bcs

import com.aptos.core.error.BcsSerializationException
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    fun serializeU8(value: UByte) {
        output.write(value.toInt())
    }

    fun serializeU16(value: UShort) {
        val buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(value.toShort())
        output.write(buf.array())
    }

    fun serializeU32(value: UInt) {
        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(value.toInt())
        output.write(buf.array())
    }

    fun serializeU64(value: ULong) {
        val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buf.putLong(value.toLong())
        output.write(buf.array())
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
        var remaining = value.toInt()
        while (true) {
            var byte = remaining and 0x7F
            remaining = remaining ushr 7
            if (remaining != 0) {
                byte = byte or 0x80
            }
            output.write(byte)
            if (remaining == 0) break
        }
    }

    /** Serializes a sequence (vector) length as ULEB128. */
    fun serializeSequenceLength(length: Int) {
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
