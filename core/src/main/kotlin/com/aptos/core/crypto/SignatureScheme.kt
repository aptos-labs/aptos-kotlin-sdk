package com.aptos.core.crypto

/**
 * Enumeration of supported signature schemes.
 */
enum class SignatureScheme(val id: Byte) {
    ED25519(0x00),
    SECP256K1(0x01),
    ;

    companion object {
        @JvmStatic
        fun fromId(id: Byte): SignatureScheme = entries.first { it.id == id }
    }
}
