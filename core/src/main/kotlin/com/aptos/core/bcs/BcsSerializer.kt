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
 */
class BcsSerializer(initialCapacity: Int = 256) {
    private val output = ByteArrayOutputStream(initialCapacity)

    fun toByteArray(): ByteArray = output.toByteArray()

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

    fun serializeU128(value: BigInteger) {
        validateUnsigned(value, 128)
        val bytes = bigIntToLittleEndian(value, 16)
        output.write(bytes)
    }

    fun serializeU256(value: BigInteger) {
        validateUnsigned(value, 256)
        val bytes = bigIntToLittleEndian(value, 32)
        output.write(bytes)
    }

    fun serializeBytes(value: ByteArray) {
        serializeUleb128(value.size.toUInt())
        output.write(value)
    }

    fun serializeString(value: String) {
        serializeBytes(value.toByteArray(Charsets.UTF_8))
    }

    fun serializeFixedBytes(value: ByteArray) {
        output.write(value)
    }

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

    fun serializeSequenceLength(length: Int) {
        serializeUleb128(length.toUInt())
    }

    fun serializeVariantIndex(index: UInt) {
        serializeUleb128(index)
    }

    fun serializeOptionTag(hasValue: Boolean) {
        serializeBool(hasValue)
    }

    fun <T : BcsSerializable> serializeSequence(items: List<T>) {
        serializeSequenceLength(items.size)
        items.forEach { it.serialize(this) }
    }

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
