package com.aptos.core.crypto

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer

/**
 * Heterogeneous multi-signature scheme supporting mixed key types (Ed25519 + Secp256k1).
 *
 * Uses scheme byte 0x03 for authentication key derivation.
 */
object MultiKey {
    /** BCS variant index for Ed25519 key/signature types. */
    const val KEY_TYPE_ED25519: UByte = 0u

    /** BCS variant index for Secp256k1 key/signature types. */
    const val KEY_TYPE_SECP256K1: UByte = 1u

    /**
     * A public key of any supported type, tagged with its key type.
     *
     * BCS: variant_index(type) || bytes
     */
    data class AnyPublicKey(val type: UByte, val keyBytes: ByteArray) : BcsSerializable {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(type.toUInt())
            serializer.serializeBytes(keyBytes)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AnyPublicKey) return false
            return type == other.type && keyBytes.contentEquals(other.keyBytes)
        }

        override fun hashCode(): Int = 31 * type.hashCode() + keyBytes.contentHashCode()

        companion object {
            /** Wraps an [Ed25519.PublicKey] as an [AnyPublicKey]. */
            @JvmStatic
            fun ed25519(publicKey: Ed25519.PublicKey): AnyPublicKey = AnyPublicKey(KEY_TYPE_ED25519, publicKey.data)

            /** Wraps a [Secp256k1.PublicKey] as an [AnyPublicKey]. */
            @JvmStatic
            fun secp256k1(publicKey: Secp256k1.PublicKey): AnyPublicKey =
                AnyPublicKey(KEY_TYPE_SECP256K1, publicKey.data)
        }
    }

    /**
     * A multi-key public key composed of heterogeneous key types.
     *
     * BCS: ULEB128(N) || pk1.serialize() || ... || pkN.serialize() || u8(sigsRequired)
     * Auth key: SHA3-256(BCS(PublicKey) || 0x03)
     */
    data class PublicKey(val keys: List<AnyPublicKey>, val sigsRequired: UByte) : BcsSerializable {
        init {
            require(keys.isNotEmpty()) { "MultiKey must have at least one key" }
            require(keys.size <= MultiEd25519.MAX_KEYS) {
                "MultiKey supports at most ${MultiEd25519.MAX_KEYS} keys, got ${keys.size}"
            }
            require(sigsRequired.toInt() in 1..keys.size) {
                "sigsRequired must be between 1 and ${keys.size}, got $sigsRequired"
            }
        }

        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeSequenceLength(keys.size)
            keys.forEach { it.serialize(serializer) }
            serializer.serializeU8(sigsRequired)
        }

        /** Derives the authentication key: SHA3-256(BCS(this) || 0x03). */
        fun authKey(): AuthenticationKey = AuthenticationKey.fromMultiKey(this)
    }

    /**
     * A signature of any supported type, tagged with its signature type.
     *
     * BCS: variant_index(type) || bytes
     */
    data class AnySignature(val type: UByte, val sigBytes: ByteArray) : BcsSerializable {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(type.toUInt())
            serializer.serializeBytes(sigBytes)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AnySignature) return false
            return type == other.type && sigBytes.contentEquals(other.sigBytes)
        }

        override fun hashCode(): Int = 31 * type.hashCode() + sigBytes.contentHashCode()

        companion object {
            /** Wraps an [Ed25519.Signature] as an [AnySignature]. */
            @JvmStatic
            fun ed25519(signature: Ed25519.Signature): AnySignature = AnySignature(KEY_TYPE_ED25519, signature.data)

            /** Wraps a [Secp256k1.Signature] as an [AnySignature]. */
            @JvmStatic
            fun secp256k1(signature: Secp256k1.Signature): AnySignature =
                AnySignature(KEY_TYPE_SECP256K1, signature.data)
        }
    }

    /**
     * A multi-key signature containing individual typed signatures and a bitmap.
     *
     * BCS: ULEB128(N) || sig1.serialize() || ... || sigK.serialize() || bitmap(4 bytes)
     */
    data class Signature(val signatures: List<AnySignature>, val bitmap: MultiEd25519.Bitmap) : BcsSerializable {
        init {
            require(signatures.isNotEmpty()) { "MultiKey signature must have at least one signature" }
        }

        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeSequenceLength(signatures.size)
            signatures.forEach { it.serialize(serializer) }
            serializer.serializeFixedBytes(bitmap.toBytes())
        }
    }
}
