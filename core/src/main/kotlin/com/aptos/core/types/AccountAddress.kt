package com.aptos.core.types

import com.aptos.core.bcs.BcsDeserializer
import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.AccountAddressParseException

/**
 * Represents a 32-byte Aptos account address.
 *
 * Addresses are always stored as exactly [LENGTH] (32) bytes internally.
 * Short-form hex strings (e.g. `"0x1"`) are zero-padded on construction.
 *
 * Use [fromHex] for strict parsing (only allows short form for special addresses 0x0-0xf)
 * or [fromHexRelaxed] for lenient parsing (zero-pads any length).
 *
 * @property data the raw 32-byte address
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

    /** Returns the full 66-character hex representation (0x + 64 hex chars). */
    fun toHex(): String = "0x${HexString.encode(data)}"

    /** Returns the shortest hex representation with leading zeros trimmed (e.g. `"0x1"`). */
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

        /**
         * Parses an address from a hex string using strict rules.
         *
         * Short form (e.g. `"0x1"`) is only accepted for special addresses `0x0` through `0xf`.
         * All other addresses must use the full 64-character hex format.
         *
         * @param hex the hex string, optionally prefixed with `0x`
         * @return the parsed [AccountAddress]
         * @throws AccountAddressParseException if the input is invalid or not in the expected format
         */
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

        /**
         * Parses an address from a hex string with lenient zero-padding.
         *
         * Accepts any hex string up to 64 characters and left-pads with zeros.
         * For example, `"0xab"` becomes the 32-byte address `0x00...00ab`.
         *
         * @param hex the hex string, optionally prefixed with `0x`
         * @return the parsed [AccountAddress]
         * @throws AccountAddressParseException if the input contains invalid hex characters or is too long
         */
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

        /** Deserializes an [AccountAddress] from BCS (reads exactly 32 bytes). */
        @JvmStatic
        fun fromBcs(deserializer: BcsDeserializer): AccountAddress {
            return AccountAddress(deserializer.deserializeFixedBytes(LENGTH))
        }

        /** Returns `true` if [hex] is a valid strict-form address string. */
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
