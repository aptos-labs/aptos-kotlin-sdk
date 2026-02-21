package com.aptos.core.transaction

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.crypto.Hashing
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId

/**
 * Represents an unsigned (raw) Aptos transaction.
 * Field order for BCS: sender, sequence_number, payload, max_gas_amount, gas_unit_price,
 * expiration_timestamp_secs, chain_id.
 */
data class RawTransaction(
    val sender: AccountAddress,
    val sequenceNumber: ULong,
    val payload: TransactionPayload,
    val maxGasAmount: ULong,
    val gasUnitPrice: ULong,
    val expirationTimestampSecs: ULong,
    val chainId: ChainId,
) : BcsSerializable {

    override fun serialize(serializer: BcsSerializer) {
        sender.serialize(serializer)
        serializer.serializeU64(sequenceNumber)
        payload.serialize(serializer)
        serializer.serializeU64(maxGasAmount)
        serializer.serializeU64(gasUnitPrice)
        serializer.serializeU64(expirationTimestampSecs)
        chainId.serialize(serializer)
    }

    /**
     * Returns the signing message: SHA3-256("APTOS::RawTransaction") || BCS(raw_txn)
     */
    fun signingMessage(): ByteArray {
        val serializer = BcsSerializer()
        serialize(serializer)
        return Hashing.RAW_TRANSACTION_PREFIX + serializer.toByteArray()
    }

    fun toBcs(): ByteArray {
        val serializer = BcsSerializer()
        serialize(serializer)
        return serializer.toByteArray()
    }
}
