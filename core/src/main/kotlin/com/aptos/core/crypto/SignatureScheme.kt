package com.aptos.core.crypto

/**
 * Enumeration of supported signature schemes.
 */
enum class SignatureScheme(val id: Byte) {
    ED25519(0x00),
    SECP256K1(0x01),
    MULTI_ED25519(0x01),
    MULTI_KEY(0x03),
    KEYLESS(0x05),
    ;

    companion object {
        @JvmStatic
        fun fromId(id: Byte): SignatureScheme = entries.first { it.id == id }
    }
}
