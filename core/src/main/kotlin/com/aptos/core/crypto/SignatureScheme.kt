package com.aptos.core.crypto

/**
 * Enumeration of signature schemes supported by the Aptos blockchain.
 *
 * Each scheme has an identifier byte used in authentication key derivation
 * (appended after the public key bytes before SHA3-256 hashing).
 * Note that some schemes share the same identifier (e.g. [SECP256K1] and [MULTI_ED25519]
 * both use `0x01`) because the public key format already disambiguates them.
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
         * Returns the first [SignatureScheme] matching the given identifier byte.
         *
         * Since some schemes share the same [id], this returns the first match in declaration order.
         *
         * @param id the scheme identifier byte
         * @return the first matching scheme
         * @throws NoSuchElementException if no scheme matches
         */
        @JvmStatic
        fun fromId(id: Byte): SignatureScheme = entries.first { it.id == id }
    }
}
