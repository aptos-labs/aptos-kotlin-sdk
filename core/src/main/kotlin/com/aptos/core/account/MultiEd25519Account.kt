package com.aptos.core.account

import com.aptos.core.crypto.AuthenticationKey
import com.aptos.core.crypto.Ed25519
import com.aptos.core.crypto.MultiEd25519
import com.aptos.core.crypto.SignatureScheme
import com.aptos.core.types.AccountAddress

/**
 * An Aptos account backed by an N-of-M Ed25519 multi-signature key.
 *
 * Holds a list of [Ed25519Account] signers along with their indices in the full public key set
 * and the complete [MultiEd25519.PublicKey] (which includes all M public keys and the threshold).
 *
 * @property signers the Ed25519 accounts that will sign (must meet threshold)
 * @property signerIndices the index of each signer in the full public key list
 * @property multiPublicKey the full N-of-M public key
 */
class MultiEd25519Account private constructor(
    val signers: List<Ed25519Account>,
    val signerIndices: List<Int>,
    val multiPublicKey: MultiEd25519.PublicKey,
    override val address: AccountAddress,
) : Account {
    override val publicKeyBytes: ByteArray get() = multiPublicKey.toBytes()
    override val scheme: SignatureScheme get() = SignatureScheme.MULTI_ED25519
    override val authenticationKey: AuthenticationKey by lazy {
        multiPublicKey.authKey()
    }

    /**
     * Signs the [message] with all available signers and returns the concatenated
     * signature bytes (sig1 || sig2 || ... || sigK || 4-byte bitmap).
     */
    override fun sign(message: ByteArray): ByteArray {
        val indexedSigs = signers.zip(signerIndices).map { (signer, index) ->
            MultiEd25519.IndexedSignature(index, signer.signEd25519(message))
        }
        val bitmap = MultiEd25519.Bitmap.fromIndices(signerIndices.sorted(), multiPublicKey.keys.size)
        return MultiEd25519.Signature(indexedSigs, bitmap).toBytes()
    }

    /** Signs the [message] and returns a typed [MultiEd25519.Signature]. */
    fun signMultiEd25519(message: ByteArray): MultiEd25519.Signature {
        val indexedSigs = signers.zip(signerIndices).map { (signer, index) ->
            MultiEd25519.IndexedSignature(index, signer.signEd25519(message))
        }
        val bitmap = MultiEd25519.Bitmap.fromIndices(signerIndices.sorted(), multiPublicKey.keys.size)
        return MultiEd25519.Signature(indexedSigs, bitmap)
    }

    companion object {
        /**
         * Creates a MultiEd25519Account from the given signers and their indices
         * within the full public key set.
         *
         * @param signers the Ed25519 accounts that will participate in signing
         * @param signerIndices the index of each signer in the full public key list
         * @param allPublicKeys the full ordered list of all M public keys
         * @param threshold the minimum number of signatures required
         */
        @JvmStatic
        fun create(
            signers: List<Ed25519Account>,
            signerIndices: List<Int>,
            allPublicKeys: List<Ed25519.PublicKey>,
            threshold: UByte,
        ): MultiEd25519Account {
            require(signers.size == signerIndices.size) {
                "signers and signerIndices must have the same size"
            }
            require(signers.size >= threshold.toInt()) {
                "Need at least $threshold signers, got ${signers.size}"
            }
            val multiPk = MultiEd25519.PublicKey(allPublicKeys, threshold)
            val authKey = multiPk.authKey()
            return MultiEd25519Account(signers, signerIndices, multiPk, authKey.derivedAddress())
        }
    }
}
