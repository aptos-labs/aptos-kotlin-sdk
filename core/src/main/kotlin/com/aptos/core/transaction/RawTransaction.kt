package com.aptos.core.transaction

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.crypto.Hashing
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId

/**
 * Represents an unsigned (raw) Aptos transaction ready to be signed.
 *
 * BCS serialization follows the spec field order: sender, sequence_number, payload,
 * max_gas_amount, gas_unit_price, expiration_timestamp_secs, chain_id.
 *
 * Use [signingMessage] to produce the bytes that should be signed by an account.
 *
 * @property sender the account address sending the transaction
 * @property sequenceNumber the sender's next sequence number
 * @property payload the transaction payload (entry function call, script, etc.)
 * @property maxGasAmount maximum gas units the sender is willing to pay
 * @property gasUnitPrice price per gas unit in octas
 * @property expirationTimestampSecs Unix timestamp after which the transaction expires
 * @property chainId the chain ID to prevent cross-chain replay
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

    /** Returns the BCS-encoded bytes of this raw transaction (without signing prefix). */
    fun toBcs(): ByteArray {
        val serializer = BcsSerializer()
        serialize(serializer)
        return serializer.toByteArray()
    }
}
