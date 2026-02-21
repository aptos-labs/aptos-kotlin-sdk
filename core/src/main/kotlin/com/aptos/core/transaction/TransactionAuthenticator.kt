package com.aptos.core.transaction

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.crypto.Ed25519
import com.aptos.core.crypto.MultiEd25519
import com.aptos.core.crypto.MultiKey
import com.aptos.core.types.AccountAddress

/**
 * Sealed class for account-level authenticators used within transaction authenticators.
 *
 * BCS variant indices: Ed25519=0, MultiEd25519=1, SingleKey=2, MultiKey=3.
 */
sealed class AccountAuthenticator : BcsSerializable {
    /** Ed25519 account authenticator (variant index 0). */
    data class Ed25519Auth(val publicKey: Ed25519.PublicKey, val signature: Ed25519.Signature) :
        AccountAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(0u) // Ed25519 = 0
            publicKey.serialize(serializer)
            signature.serialize(serializer)
        }
    }

    /** MultiEd25519 account authenticator (variant index 1). */
    data class MultiEd25519Auth(val publicKey: MultiEd25519.PublicKey, val signature: MultiEd25519.Signature) :
        AccountAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(1u) // MultiEd25519 = 1
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

    /** MultiKey account authenticator (variant index 3). */
    data class MultiKeyAuth(val publicKey: MultiKey.PublicKey, val signature: MultiKey.Signature) :
        AccountAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(3u) // MultiKey = 3
            publicKey.serialize(serializer)
            signature.serialize(serializer)
        }
    }
}

/**
 * Sealed class for top-level transaction authenticators attached to [SignedTransaction].
 *
 * BCS variant indices: Ed25519=0, MultiEd25519=1, MultiAgent=2, FeePayer=3, SingleSender=4.
 */
sealed class TransactionAuthenticator : BcsSerializable {
    /** Ed25519 transaction authenticator (variant index 0) -- legacy format. */
    data class Ed25519Auth(val publicKey: Ed25519.PublicKey, val signature: Ed25519.Signature) :
        TransactionAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(0u)
            publicKey.serialize(serializer)
            signature.serialize(serializer)
        }
    }

    /** MultiEd25519 transaction authenticator (variant index 1). */
    data class MultiEd25519Auth(val publicKey: MultiEd25519.PublicKey, val signature: MultiEd25519.Signature) :
        TransactionAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(1u)
            publicKey.serialize(serializer)
            signature.serialize(serializer)
        }
    }

    /** Multi-agent transaction authenticator (variant index 2). */
    data class MultiAgent(
        val sender: AccountAuthenticator,
        val secondarySignerAddresses: List<AccountAddress>,
        val secondarySigners: List<AccountAuthenticator>,
    ) : TransactionAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(2u)
            sender.serialize(serializer)
            serializer.serializeSequenceLength(secondarySignerAddresses.size)
            secondarySignerAddresses.forEach { it.serialize(serializer) }
            serializer.serializeSequenceLength(secondarySigners.size)
            secondarySigners.forEach { it.serialize(serializer) }
        }
    }

    /** Fee payer transaction authenticator (variant index 3). */
    data class FeePayer(
        val sender: AccountAuthenticator,
        val secondarySignerAddresses: List<AccountAddress>,
        val secondarySigners: List<AccountAuthenticator>,
        val feePayerAddress: AccountAddress,
        val feePayerAuth: AccountAuthenticator,
    ) : TransactionAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(3u)
            sender.serialize(serializer)
            serializer.serializeSequenceLength(secondarySignerAddresses.size)
            secondarySignerAddresses.forEach { it.serialize(serializer) }
            serializer.serializeSequenceLength(secondarySigners.size)
            secondarySigners.forEach { it.serialize(serializer) }
            feePayerAddress.serialize(serializer)
            feePayerAuth.serialize(serializer)
        }
    }

    /** SingleSender transaction authenticator (variant index 4) -- used for all key types. */
    data class SingleSender(val accountAuthenticator: AccountAuthenticator) : TransactionAuthenticator() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(4u)
            accountAuthenticator.serialize(serializer)
        }
    }
}
