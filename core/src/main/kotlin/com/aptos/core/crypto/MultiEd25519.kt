package com.aptos.core.crypto

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.CryptoException
import com.aptos.core.types.HexString

/**
 * N-of-M Ed25519 multi-signature scheme with a maximum of 32 keys.
 *
 * The public key is the concatenation of all Ed25519 public keys plus a threshold byte.
 * The signature is the concatenation of the collected Ed25519 signatures plus a 4-byte bitmap.
 */
object MultiEd25519 {
    /** Maximum number of Ed25519 public keys in a multi-sig. */
    const val MAX_KEYS = 32

    /** Length of the signer bitmap in bytes. */
    const val BITMAP_LENGTH = 4

    /**
     * An N-of-M multi-Ed25519 public key.
     *
     * BCS format: `serializeBytes(pk1 || pk2 || ... || pkN || threshold)`
     *
     * @property keys the list of Ed25519 public keys (max [MAX_KEYS])
     * @property threshold the minimum number of signatures required
     */
    data class PublicKey(val keys: List<Ed25519.PublicKey>, val threshold: UByte) : BcsSerializable {
        init {
            require(keys.isNotEmpty()) { "MultiEd25519 public key must have at least one key" }
            require(keys.size <= MAX_KEYS) {
                "MultiEd25519 supports at most $MAX_KEYS keys, got ${keys.size}"
            }
            require(threshold.toInt() in 1..keys.size) {
                "Threshold must be between 1 and ${keys.size}, got $threshold"
            }
        }

        /** Returns the raw bytes: pk1 || pk2 || ... || pkN || threshold. */
        fun toBytes(): ByteArray {
            val result = ByteArray(keys.size * Ed25519.PUBLIC_KEY_LENGTH + 1)
            keys.forEachIndexed { i, key ->
                System.arraycopy(key.data, 0, result, i * Ed25519.PUBLIC_KEY_LENGTH, Ed25519.PUBLIC_KEY_LENGTH)
            }
            result[result.size - 1] = threshold.toByte()
            return result
        }

        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeBytes(toBytes())
        }

        /** Derives the authentication key: SHA3-256(pk1 || ... || pkN || threshold || 0x01). */
        fun authKey(): AuthenticationKey {
            val input = toBytes() + byteArrayOf(SignatureScheme.MULTI_ED25519.id)
            return AuthenticationKey(Hashing.sha3256(input))
        }

        /** Returns the hex-encoded multi-sig public key with `0x` prefix. */
        fun toHex(): String = HexString.encodeWithPrefix(toBytes())

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PublicKey) return false
            return keys == other.keys && threshold == other.threshold
        }

        override fun hashCode(): Int = 31 * keys.hashCode() + threshold.hashCode()
    }

    /**
     * An indexed Ed25519 signature with its position in the public key list.
     */
    data class IndexedSignature(val index: Int, val signature: Ed25519.Signature) {
        init {
            require(index in 0 until MAX_KEYS) {
                "Signature index must be in 0..${MAX_KEYS - 1}, got $index"
            }
        }
    }

    /**
     * A 4-byte bitmap indicating which signers have provided signatures.
     * Bit i is set at position (31 - i) in MSB-first order.
     */
    data class Bitmap(val bits: Int) {
        /** Returns the 4-byte big-endian bitmap. */
        fun toBytes(): ByteArray = byteArrayOf(
            (bits ushr 24 and 0xFF).toByte(),
            (bits ushr 16 and 0xFF).toByte(),
            (bits ushr 8 and 0xFF).toByte(),
            (bits and 0xFF).toByte(),
        )

        companion object {
            /**
             * Creates a bitmap from a sorted list of unique signer indices.
             *
             * @param indices the signer indices (must be sorted, unique, and < numKeys)
             * @param numKeys the total number of public keys
             */
            @JvmStatic
            fun fromIndices(indices: List<Int>, numKeys: Int): Bitmap {
                require(indices == indices.sorted()) { "Indices must be sorted" }
                require(indices == indices.distinct()) { "Indices must be unique" }
                var bits = 0
                for (index in indices) {
                    require(index in 0 until numKeys) {
                        "Index $index out of range for $numKeys keys"
                    }
                    bits = bits or (1 shl (31 - index))
                }
                return Bitmap(bits)
            }
        }
    }

    /**
     * A multi-Ed25519 signature containing individual signatures and a bitmap.
     *
     * BCS format: `serializeBytes(sig1 || sig2 || ... || sigK || 4-byte-bitmap)`
     */
    data class Signature(val signatures: List<IndexedSignature>, val bitmap: Bitmap) : BcsSerializable {
        init {
            require(signatures.isNotEmpty()) { "MultiEd25519 signature must have at least one signature" }
        }

        /** Returns the raw bytes: sig1 || sig2 || ... || sigK || bitmap. */
        fun toBytes(): ByteArray {
            val sorted = signatures.sortedBy { it.index }
            val result = ByteArray(sorted.size * Ed25519.SIGNATURE_LENGTH + BITMAP_LENGTH)
            sorted.forEachIndexed { i, indexedSig ->
                System.arraycopy(
                    indexedSig.signature.data,
                    0,
                    result,
                    i * Ed25519.SIGNATURE_LENGTH,
                    Ed25519.SIGNATURE_LENGTH,
                )
            }
            val bitmapBytes = bitmap.toBytes()
            System.arraycopy(bitmapBytes, 0, result, sorted.size * Ed25519.SIGNATURE_LENGTH, BITMAP_LENGTH)
            return result
        }

        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeBytes(toBytes())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Signature) return false
            return signatures == other.signatures && bitmap == other.bitmap
        }

        override fun hashCode(): Int = 31 * signatures.hashCode() + bitmap.hashCode()
    }

    /**
     * Verifies a multi-signature against a message using the given public key.
     *
     * @throws CryptoException if verification encounters an error
     */
    @JvmStatic
    fun verify(publicKey: PublicKey, message: ByteArray, signature: Signature): Boolean {
        if (signature.signatures.size < publicKey.threshold.toInt()) return false
        return try {
            signature.signatures.all { indexed ->
                if (indexed.index >= publicKey.keys.size) return false
                publicKey.keys[indexed.index].verify(message, indexed.signature)
            }
        } catch (e: Exception) {
            throw CryptoException("MultiEd25519 verification failed", e)
        }
    }
}
