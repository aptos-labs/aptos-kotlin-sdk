package com.aptos.core.types

/**
 * Utility object for hex string encoding and decoding.
 *
 * Supports both `0x`-prefixed and plain hex strings. All encoding uses lowercase hex digits.
 */
object HexString {
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    /**
     * Encodes a byte array to a lowercase hex string without a prefix.
     *
     * @param bytes the bytes to encode
     * @return the hex-encoded string (e.g. `"0a1b2c"`)
     */
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

    /**
     * Encodes a byte array to a lowercase hex string with a `0x` prefix.
     *
     * @param bytes the bytes to encode
     * @return the hex-encoded string with prefix (e.g. `"0x0a1b2c"`)
     */
    @JvmStatic
    fun encodeWithPrefix(bytes: ByteArray): String = "0x${encode(bytes)}"

    /**
     * Decodes a hex string (with or without `0x`/`0X` prefix) to a byte array.
     *
     * @param hex the hex string to decode
     * @return the decoded byte array
     * @throws IllegalArgumentException if the string has odd length or invalid hex characters
     */
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
