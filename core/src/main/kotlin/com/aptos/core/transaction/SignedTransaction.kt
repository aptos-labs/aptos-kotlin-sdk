package com.aptos.core.transaction

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.crypto.Hashing

/**
 * A signed transaction ready for submission to the Aptos blockchain.
 */
data class SignedTransaction(
    val rawTransaction: RawTransaction,
    val authenticator: TransactionAuthenticator,
) : BcsSerializable {

    override fun serialize(serializer: BcsSerializer) {
        rawTransaction.serialize(serializer)
        authenticator.serialize(serializer)
    }

    /**
     * Returns the BCS bytes for submission to the REST API.
     */
    fun toSubmitBytes(): ByteArray {
        val serializer = BcsSerializer()
        serialize(serializer)
        return serializer.toByteArray()
    }

    /**
     * Returns the transaction hash (SHA3-256 of prefix + BCS(SignedTransaction)).
     */
    fun hash(): ByteArray {
        // Hash = SHA3-256(APTOS::Transaction prefix || variant_index(0=UserTransaction) || BCS(SignedTransaction))
        val serializer = BcsSerializer()
        serializer.serializeVariantIndex(0u) // UserTransaction variant = 0
        serialize(serializer)
        val payload = serializer.toByteArray()

        val digest = org.bouncycastle.jcajce.provider.digest.SHA3.Digest256()
        digest.update(Hashing.TRANSACTION_PREFIX)
        digest.update(payload)
        return digest.digest()
    }
}
