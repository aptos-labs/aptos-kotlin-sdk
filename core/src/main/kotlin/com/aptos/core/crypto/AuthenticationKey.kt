package com.aptos.core.crypto

import com.aptos.core.types.AccountAddress
import com.aptos.core.types.HexString

/**
 * A 32-byte authentication key derived from a public key and signature scheme.
 *
 * Computed as `SHA3-256(public_key_bytes || scheme_id_byte)`. The authentication key
 * is used to derive the initial account address and to verify transaction authenticators.
 *
 * @property data the raw 32-byte authentication key
 */
data class AuthenticationKey(val data: ByteArray) {
    init {
        require(data.size == LENGTH) {
            "AuthenticationKey must be $LENGTH bytes, got ${data.size}"
        }
    }

    /** Derives the [AccountAddress] from this authentication key (they share the same 32 bytes). */
    fun derivedAddress(): AccountAddress = AccountAddress(data.copyOf())

    fun toHex(): String = HexString.encodeWithPrefix(data)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuthenticationKey) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int = data.contentHashCode()

    override fun toString(): String = toHex()

    companion object {
        const val LENGTH = 32

        /** Computes `SHA3-256(publicKeyBytes || scheme.id)`. */
        @JvmStatic
        fun fromPublicKey(publicKeyBytes: ByteArray, scheme: SignatureScheme): AuthenticationKey {
            val input = publicKeyBytes + byteArrayOf(scheme.id)
            val hash = Hashing.sha3256(input)
            return AuthenticationKey(hash)
        }

        /** Derives the authentication key for an Ed25519 public key (scheme id 0x00). */
        @JvmStatic
        fun fromEd25519(publicKey: Ed25519.PublicKey): AuthenticationKey =
            fromPublicKey(publicKey.data, SignatureScheme.ED25519)

        /** Derives the authentication key for a Secp256k1 public key (scheme id 0x01). */
        @JvmStatic
        fun fromSecp256k1(publicKey: Secp256k1.PublicKey): AuthenticationKey =
            fromPublicKey(publicKey.data, SignatureScheme.SECP256K1)
    }
}
