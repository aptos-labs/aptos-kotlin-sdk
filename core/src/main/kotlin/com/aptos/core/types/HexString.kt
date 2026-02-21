package com.aptos.core.types

/**
 * Utility object for hex string encoding and decoding.
 */
object HexString {
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    @JvmStatic
    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            sb.append(HEX_CHARS[i shr 4])
            sb.append(HEX_CHARS[i and 0x0F])
        }
        return sb.toString()
    }

    @JvmStatic
    fun encodeWithPrefix(bytes: ByteArray): String = "0x${encode(bytes)}"

    @JvmStatic
    fun decode(hex: String): ByteArray {
        val stripped = if (hex.startsWith("0x") || hex.startsWith("0X")) hex.substring(2) else hex
        require(stripped.length % 2 == 0) { "Hex string must have even length, got ${stripped.length}" }
        return ByteArray(stripped.length / 2) { i ->
            val high = hexCharToInt(stripped[i * 2])
            val low = hexCharToInt(stripped[i * 2 + 1])
            ((high shl 4) or low).toByte()
        }
    }

    private fun hexCharToInt(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> throw IllegalArgumentException("Invalid hex character: '$c'")
    }
}
