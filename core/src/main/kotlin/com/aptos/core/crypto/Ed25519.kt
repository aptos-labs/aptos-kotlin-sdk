package com.aptos.core.crypto

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.CryptoException
import com.aptos.core.types.HexString
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom

/**
 * Ed25519 cryptographic operations for the Aptos blockchain.
 *
 * Provides key generation, signing, and verification using the Ed25519 curve
 * via Bouncy Castle. All keys and signatures are represented as byte arrays.
 */
object Ed25519 {
    /** Length of an Ed25519 private key in bytes. */
    const val PRIVATE_KEY_LENGTH = 32

    /** Length of an Ed25519 public key in bytes. */
    const val PUBLIC_KEY_LENGTH = 32

    /** Length of an Ed25519 signature in bytes. */
    const val SIGNATURE_LENGTH = 64

    /**
     * An Ed25519 private key (32 bytes).
     *
     * Use [generate] to create a new random key, [fromHex] to restore from hex,
     * or [fromSeed] to derive from a longer seed.
     *
     * @property data the raw 32-byte private key
     */
    data class PrivateKey(val data: ByteArray) {
        init {
            require(data.size == PRIVATE_KEY_LENGTH) {
                "Ed25519 private key must be $PRIVATE_KEY_LENGTH bytes, got ${data.size}"
            }
        }

        /** Derives the corresponding [PublicKey] from this private key. */
        fun publicKey(): PublicKey {
            val params = Ed25519PrivateKeyParameters(data, 0)
            return PublicKey(params.generatePublicKey().encoded)
        }

        /** Signs the given [message] and returns the 64-byte Ed25519 signature. */
        fun sign(message: ByteArray): Signature {
            val params = Ed25519PrivateKeyParameters(data, 0)
            val signer = Ed25519Signer()
            signer.init(true, params)
            signer.update(message, 0, message.size)
            return Signature(signer.generateSignature())
        }

        /** Returns the hex-encoded private key with `0x` prefix. */
        fun toHex(): String = HexString.encodeWithPrefix(data)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PrivateKey) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()

        override fun toString(): String = "Ed25519PrivateKey(***)"

        companion object {
            /** Generates a new random Ed25519 private key using [SecureRandom]. */
            @JvmStatic
            fun generate(): PrivateKey {
                val random = SecureRandom()
                val params = Ed25519PrivateKeyParameters(random)
                return PrivateKey(params.encoded)
            }

            /** Parses an Ed25519 private key from a hex-encoded string (with or without `0x` prefix). */
            @JvmStatic
            fun fromHex(hex: String): PrivateKey = PrivateKey(HexString.decode(hex))

            /**
             * Derives an Ed25519 private key from a seed by taking the first 32 bytes.
             *
             * @param seed a byte array of at least 32 bytes (e.g. from SLIP-0010 derivation)
             */
            @JvmStatic
            fun fromSeed(seed: ByteArray): PrivateKey {
                require(seed.size >= PRIVATE_KEY_LENGTH) {
                    "Seed must be at least $PRIVATE_KEY_LENGTH bytes"
                }
                return PrivateKey(seed.copyOfRange(0, PRIVATE_KEY_LENGTH))
            }
        }
    }

    /**
     * An Ed25519 public key (32 bytes).
     *
     * @property data the raw 32-byte public key
     */
    data class PublicKey(val data: ByteArray) : BcsSerializable {
        init {
            require(data.size == PUBLIC_KEY_LENGTH) {
                "Ed25519 public key must be $PUBLIC_KEY_LENGTH bytes, got ${data.size}"
            }
        }

        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeBytes(data)
        }

        /**
         * Verifies that [signature] is valid for the given [message] under this public key.
         *
         * @throws CryptoException if verification encounters an error
         */
        fun verify(message: ByteArray, signature: Signature): Boolean = try {
            val params = Ed25519PublicKeyParameters(data, 0)
            val verifier = Ed25519Signer()
            verifier.init(false, params)
            verifier.update(message, 0, message.size)
            verifier.verifySignature(signature.data)
        } catch (e: Exception) {
            throw CryptoException("Ed25519 verification failed", e)
        }

        /** Returns the hex-encoded public key with `0x` prefix. */
        fun toHex(): String = HexString.encodeWithPrefix(data)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PublicKey) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()

        override fun toString(): String = "Ed25519PublicKey(${toHex()})"

        companion object {
            /** Parses an Ed25519 public key from a hex-encoded string (with or without `0x` prefix). */
            @JvmStatic
            fun fromHex(hex: String): PublicKey = PublicKey(HexString.decode(hex))
        }
    }

    /**
     * An Ed25519 signature (64 bytes).
     *
     * @property data the raw 64-byte signature
     */
    data class Signature(val data: ByteArray) : BcsSerializable {
        init {
            require(data.size == SIGNATURE_LENGTH) {
                "Ed25519 signature must be $SIGNATURE_LENGTH bytes, got ${data.size}"
            }
        }

        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeBytes(data)
        }

        /** Returns the hex-encoded signature with `0x` prefix. */
        fun toHex(): String = HexString.encodeWithPrefix(data)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Signature) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()

        override fun toString(): String = "Ed25519Signature(${toHex()})"

        companion object {
            /** Parses an Ed25519 signature from a hex-encoded string (with or without `0x` prefix). */
            @JvmStatic
            fun fromHex(hex: String): Signature = Signature(HexString.decode(hex))
        }
    }
}
