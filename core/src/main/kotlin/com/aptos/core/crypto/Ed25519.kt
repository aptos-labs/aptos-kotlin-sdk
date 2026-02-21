package com.aptos.core.crypto

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.CryptoException
import com.aptos.core.types.HexString
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom

object Ed25519 {

    const val PRIVATE_KEY_LENGTH = 32
    const val PUBLIC_KEY_LENGTH = 32
    const val SIGNATURE_LENGTH = 64

    data class PrivateKey(val data: ByteArray) {
        init {
            require(data.size == PRIVATE_KEY_LENGTH) {
                "Ed25519 private key must be $PRIVATE_KEY_LENGTH bytes, got ${data.size}"
            }
        }

        fun publicKey(): PublicKey {
            val params = Ed25519PrivateKeyParameters(data, 0)
            return PublicKey(params.generatePublicKey().encoded)
        }

        fun sign(message: ByteArray): Signature {
            val params = Ed25519PrivateKeyParameters(data, 0)
            val signer = Ed25519Signer()
            signer.init(true, params)
            signer.update(message, 0, message.size)
            return Signature(signer.generateSignature())
        }

        fun toHex(): String = HexString.encodeWithPrefix(data)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PrivateKey) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
        override fun toString(): String = "Ed25519PrivateKey(***)"

        companion object {
            @JvmStatic
            fun generate(): PrivateKey {
                val random = SecureRandom()
                val params = Ed25519PrivateKeyParameters(random)
                return PrivateKey(params.encoded)
            }

            @JvmStatic
            fun fromHex(hex: String): PrivateKey = PrivateKey(HexString.decode(hex))

            @JvmStatic
            fun fromSeed(seed: ByteArray): PrivateKey {
                require(seed.size >= PRIVATE_KEY_LENGTH) {
                    "Seed must be at least $PRIVATE_KEY_LENGTH bytes"
                }
                return PrivateKey(seed.copyOfRange(0, PRIVATE_KEY_LENGTH))
            }
        }
    }

    data class PublicKey(val data: ByteArray) : BcsSerializable {
        init {
            require(data.size == PUBLIC_KEY_LENGTH) {
                "Ed25519 public key must be $PUBLIC_KEY_LENGTH bytes, got ${data.size}"
            }
        }

        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeBytes(data)
        }

        fun verify(message: ByteArray, signature: Signature): Boolean {
            return try {
                val params = Ed25519PublicKeyParameters(data, 0)
                val verifier = Ed25519Signer()
                verifier.init(false, params)
                verifier.update(message, 0, message.size)
                verifier.verifySignature(signature.data)
            } catch (e: Exception) {
                throw CryptoException("Ed25519 verification failed", e)
            }
        }

        fun toHex(): String = HexString.encodeWithPrefix(data)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PublicKey) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
        override fun toString(): String = "Ed25519PublicKey(${toHex()})"

        companion object {
            @JvmStatic
            fun fromHex(hex: String): PublicKey = PublicKey(HexString.decode(hex))
        }
    }

    data class Signature(val data: ByteArray) : BcsSerializable {
        init {
            require(data.size == SIGNATURE_LENGTH) {
                "Ed25519 signature must be $SIGNATURE_LENGTH bytes, got ${data.size}"
            }
        }

        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeBytes(data)
        }

        fun toHex(): String = HexString.encodeWithPrefix(data)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Signature) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
        override fun toString(): String = "Ed25519Signature(${toHex()})"

        companion object {
            @JvmStatic
            fun fromHex(hex: String): Signature = Signature(HexString.decode(hex))
        }
    }
}
