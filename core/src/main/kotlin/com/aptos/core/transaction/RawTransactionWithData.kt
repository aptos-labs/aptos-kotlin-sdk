package com.aptos.core.transaction

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.crypto.Hashing
import com.aptos.core.types.AccountAddress

/**
 * Raw transaction variants that carry additional data beyond [RawTransaction].
 *
 * Used for multi-agent and fee-payer transactions where multiple accounts
 * are involved in the transaction.
 */
sealed class RawTransactionWithData : BcsSerializable {
    /**
     * Multi-agent transaction variant (variant index 0).
     *
     * Multiple secondary signers co-sign the same transaction alongside the primary sender.
     */
    data class MultiAgent(val rawTransaction: RawTransaction, val secondarySignerAddresses: List<AccountAddress>) :
        RawTransactionWithData() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(0u)
            rawTransaction.serialize(serializer)
            serializer.serializeSequenceLength(secondarySignerAddresses.size)
            secondarySignerAddresses.forEach { it.serialize(serializer) }
        }
    }

    /**
     * Fee payer transaction variant (variant index 1).
     *
     * A separate fee payer account pays the gas fees for the transaction.
     */
    data class FeePayer(
        val rawTransaction: RawTransaction,
        val secondarySignerAddresses: List<AccountAddress>,
        val feePayerAddress: AccountAddress,
    ) : RawTransactionWithData() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(1u)
            rawTransaction.serialize(serializer)
            serializer.serializeSequenceLength(secondarySignerAddresses.size)
            secondarySignerAddresses.forEach { it.serialize(serializer) }
            feePayerAddress.serialize(serializer)
        }
    }

    /**
     * Returns the signing message: SHA3-256("APTOS::RawTransactionWithData") || BCS(this).
     *
     * All parties (sender, secondary signers, fee payer) sign the same message.
     */
    fun signingMessage(): ByteArray {
        val serializer = BcsSerializer()
        serialize(serializer)
        return Hashing.RAW_TRANSACTION_WITH_DATA_PREFIX + serializer.toByteArray()
    }
}
