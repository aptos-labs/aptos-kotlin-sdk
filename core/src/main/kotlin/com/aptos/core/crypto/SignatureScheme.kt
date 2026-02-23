package com.aptos.core.crypto

/**
 * Enumeration of signature schemes supported by the Aptos blockchain.
 *
 * Each scheme has a unique identifier byte used in authentication key derivation
 * (appended after the public key bytes before SHA3-256 hashing).
 *
 * @property id the scheme identifier byte used in authentication key derivation
 */
enum class SignatureScheme(val id: Byte) {
    ED25519(0x00),
    SECP256K1(0x01),
    MULTI_ED25519(0x01),
    MULTI_KEY(0x03),
    KEYLESS(0x05),
    ;

    companion object {
        /**
         * Returns the [SignatureScheme] matching the given identifier byte.
         *
         * @param id the scheme identifier byte
         * @return the matching scheme
         * @throws NoSuchElementException if no scheme matches
         */
        @JvmStatic
        fun fromId(id: Byte): SignatureScheme = entries.first { it.id == id }
    }
}
