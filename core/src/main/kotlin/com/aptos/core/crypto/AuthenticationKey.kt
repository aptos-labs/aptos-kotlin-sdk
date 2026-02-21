package com.aptos.core.crypto

import com.aptos.core.types.AccountAddress
import com.aptos.core.types.HexString

/**
 * Represents a 32-byte authentication key derived from a public key and scheme.
 * Computed as SHA3-256(pubkey_bytes || scheme_id).
 */
data class AuthenticationKey(val data: ByteArray) {

    init {
        require(data.size == LENGTH) {
            "AuthenticationKey must be $LENGTH bytes, got ${data.size}"
        }
    }

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

        @JvmStatic
        fun fromPublicKey(publicKeyBytes: ByteArray, scheme: SignatureScheme): AuthenticationKey {
            val input = publicKeyBytes + byteArrayOf(scheme.id)
            val hash = Hashing.sha3256(input)
            return AuthenticationKey(hash)
        }

        @JvmStatic
        fun fromEd25519(publicKey: Ed25519.PublicKey): AuthenticationKey {
            return fromPublicKey(publicKey.data, SignatureScheme.ED25519)
        }

        @JvmStatic
        fun fromSecp256k1(publicKey: Secp256k1.PublicKey): AuthenticationKey {
            return fromPublicKey(publicKey.data, SignatureScheme.SECP256K1)
        }
    }
}
