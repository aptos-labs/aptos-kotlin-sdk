package com.aptos.core.types

import com.aptos.core.bcs.BcsDeserializer
import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.AccountAddressParseException

/**
 * Represents a 32-byte Aptos account address.
 */
data class AccountAddress(val data: ByteArray) : BcsSerializable, Comparable<AccountAddress> {

    init {
        require(data.size == LENGTH) {
            "AccountAddress must be exactly $LENGTH bytes, got ${data.size}"
        }
    }

    override fun serialize(serializer: BcsSerializer) {
        serializer.serializeFixedBytes(data)
    }

    fun toHex(): String = "0x${HexString.encode(data)}"

    fun toShortString(): String {
        val hex = HexString.encode(data)
        val trimmed = hex.trimStart('0')
        return "0x${trimmed.ifEmpty { "0" }}"
    }

    override fun compareTo(other: AccountAddress): Int {
        for (i in data.indices) {
            val cmp = (data[i].toInt() and 0xFF).compareTo(other.data[i].toInt() and 0xFF)
            if (cmp != 0) return cmp
        }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountAddress) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int = data.contentHashCode()

    override fun toString(): String = toHex()

    companion object {
        const val LENGTH = 32

        @JvmStatic
        val ZERO = AccountAddress(ByteArray(LENGTH))

        @JvmStatic
        val ONE = fromHexRelaxed("0x1")

        @JvmStatic
        val THREE = fromHexRelaxed("0x3")

        @JvmStatic
        val FOUR = fromHexRelaxed("0x4")

        @JvmStatic
        fun fromHex(hex: String): AccountAddress {
            val stripped = if (hex.startsWith("0x") || hex.startsWith("0X")) hex.substring(2) else hex
            if (stripped.isEmpty()) {
                throw AccountAddressParseException("Address string is empty")
            }
            if (stripped.length > LENGTH * 2) {
                throw AccountAddressParseException(
                    "Hex string too long: ${stripped.length} chars (max ${LENGTH * 2})"
                )
            }
            // Strict: must be either LONG form (64 chars) or SHORT form (1-4 chars for special addresses 0x0-0xf)
            if (stripped.length != LENGTH * 2) {
                // Short form allowed only for special addresses (0x0 through 0xf)
                if (stripped.length > 4) {
                    throw AccountAddressParseException(
                        "Hex string is not in long form (expected ${LENGTH * 2} chars): 0x$stripped"
                    )
                }
                val value = stripped.toLongOrNull(16)
                    ?: throw AccountAddressParseException("Invalid hex characters in: 0x$stripped")
                if (value > 15) {
                    throw AccountAddressParseException(
                        "Short form only allowed for special addresses 0x0 through 0xf, got: 0x$stripped"
                    )
                }
            }
            return fromHexRelaxed("0x$stripped")
        }

        @JvmStatic
        fun fromHexRelaxed(hex: String): AccountAddress {
            val stripped = if (hex.startsWith("0x") || hex.startsWith("0X")) hex.substring(2) else hex
            if (stripped.isEmpty()) {
                throw AccountAddressParseException("Address string is empty")
            }
            if (stripped.length > LENGTH * 2) {
                throw AccountAddressParseException(
                    "Hex string too long: ${stripped.length} chars (max ${LENGTH * 2})"
                )
            }
            val padded = stripped.padStart(LENGTH * 2, '0')
            return try {
                AccountAddress(HexString.decode(padded))
            } catch (e: IllegalArgumentException) {
                throw AccountAddressParseException("Invalid hex: $hex", e)
            }
        }

        @JvmStatic
        fun fromBcs(deserializer: BcsDeserializer): AccountAddress {
            return AccountAddress(deserializer.deserializeFixedBytes(LENGTH))
        }

        @JvmStatic
        fun isValid(hex: String): Boolean {
            return try {
                fromHex(hex)
                true
            } catch (_: AccountAddressParseException) {
                false
            }
        }
    }
}
