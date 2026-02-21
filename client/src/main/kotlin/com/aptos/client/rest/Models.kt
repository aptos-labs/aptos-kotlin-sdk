package com.aptos.client.rest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response from the `GET /` (ledger info) endpoint. */
@Serializable
data class LedgerInfo(
    @SerialName("chain_id") val chainId: Int,
    val epoch: String,
    @SerialName("ledger_version") val ledgerVersion: String,
    @SerialName("oldest_ledger_version") val oldestLedgerVersion: String,
    @SerialName("ledger_timestamp") val ledgerTimestamp: String,
    @SerialName("node_role") val nodeRole: String,
    @SerialName("oldest_block_height") val oldestBlockHeight: String,
    @SerialName("block_height") val blockHeight: String,
    @SerialName("git_hash") val gitHash: String? = null,
)

/** Response from the `GET /accounts/{address}` endpoint. */
@Serializable
data class AccountInfo(
    @SerialName("sequence_number") val sequenceNumber: String,
    @SerialName("authentication_key") val authenticationKey: String,
)

/** A typed resource stored at an account address. */
@Serializable
data class AccountResource(
    val type: String,
    val data: kotlinx.serialization.json.JsonObject,
)

/** Response from transaction query endpoints (may represent pending or committed transactions). */
@Serializable
data class TransactionResponse(
    val type: String? = null,
    val hash: String,
    val sender: String? = null,
    @SerialName("sequence_number") val sequenceNumber: String? = null,
    @SerialName("max_gas_amount") val maxGasAmount: String? = null,
    @SerialName("gas_unit_price") val gasUnitPrice: String? = null,
    @SerialName("expiration_timestamp_secs") val expirationTimestampSecs: String? = null,
    val payload: kotlinx.serialization.json.JsonObject? = null,
    val signature: kotlinx.serialization.json.JsonObject? = null,
    @SerialName("gas_used") val gasUsed: String? = null,
    val success: Boolean? = null,
    @SerialName("vm_status") val vmStatus: String? = null,
    val version: String? = null,
    val timestamp: String? = null,
)

/** Response from submitting a transaction (before it is committed). */
@Serializable
data class PendingTransaction(
    val hash: String,
    val sender: String,
    @SerialName("sequence_number") val sequenceNumber: String,
    @SerialName("max_gas_amount") val maxGasAmount: String,
    @SerialName("gas_unit_price") val gasUnitPrice: String,
    @SerialName("expiration_timestamp_secs") val expirationTimestampSecs: String,
)

/** Response from the `GET /estimate_gas_price` endpoint. */
@Serializable
data class GasEstimate(
    @SerialName("gas_estimate") val gasEstimate: Int,
    @SerialName("deprioritized_gas_estimate") val deprioritizedGasEstimate: Int? = null,
    @SerialName("prioritized_gas_estimate") val prioritizedGasEstimate: Int? = null,
)

/** Error response body from the Aptos REST API. */
@Serializable
data class ApiError(
    val message: String,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("vm_error_code") val vmErrorCode: Int? = null,
)

/** Request body for the `POST /view` endpoint. */
@Serializable
data class ViewRequest(
    val function: String,
    @SerialName("type_arguments") val typeArguments: List<String> = emptyList(),
    val arguments: List<String> = emptyList(),
)
