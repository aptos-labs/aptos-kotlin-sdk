package com.aptos.core.transaction

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.crypto.Ed25519
import com.aptos.core.crypto.Secp256k1

/**
 * Sealed class for account-level authenticators used within transaction authenticators.
 *
 * BCS variant indices: Ed25519=0, SingleKey=2.
 */
sealed class AccountAuthenticator : BcsSerializable {

    /** Ed25519 account authenticator (variant index 0). */
    data class Ed25519Auth(
        val publicKey: Ed25519.PublicKey,
        val signature: Ed25519.Signature,
    ) : AccountAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(0u) // Ed25519 = 0
            publicKey.serialize(serializer)
            signature.serialize(serializer)
        }
    }

    /** SingleKey account authenticator for non-Ed25519 schemes (variant index 2). */
    data class SingleKey(
        val publicKeyType: UByte,
        val publicKeyBytes: ByteArray,
        val signatureType: UByte,
        val signatureBytes: ByteArray,
    ) : AccountAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(2u) // SingleKey = 2
            // AnyPublicKey: variant index + bytes
            serializer.serializeVariantIndex(publicKeyType.toUInt())
            serializer.serializeBytes(publicKeyBytes)
            // AnySignature: variant index + bytes
            serializer.serializeVariantIndex(signatureType.toUInt())
            serializer.serializeBytes(signatureBytes)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SingleKey) return false
            return publicKeyType == other.publicKeyType &&
                publicKeyBytes.contentEquals(other.publicKeyBytes) &&
                signatureType == other.signatureType &&
                signatureBytes.contentEquals(other.signatureBytes)
        }

        override fun hashCode(): Int {
            var result = publicKeyType.hashCode()
            result = 31 * result + publicKeyBytes.contentHashCode()
            result = 31 * result + signatureType.hashCode()
            result = 31 * result + signatureBytes.contentHashCode()
            return result
        }
    }
}

/**
 * Sealed class for top-level transaction authenticators attached to [SignedTransaction].
 *
 * BCS variant indices: Ed25519=0, SingleSender=4.
 */
sealed class TransactionAuthenticator : BcsSerializable {

    /** Ed25519 transaction authenticator (variant index 0) -- legacy format. */
    data class Ed25519Auth(
        val publicKey: Ed25519.PublicKey,
        val signature: Ed25519.Signature,
    ) : TransactionAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(0u)
            publicKey.serialize(serializer)
            signature.serialize(serializer)
        }
    }

    /** SingleSender transaction authenticator (variant index 4) -- used for all key types. */
    data class SingleSender(
        val accountAuthenticator: AccountAuthenticator,
    ) : TransactionAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(4u)
            accountAuthenticator.serialize(serializer)
        }
    }
}
