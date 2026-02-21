package com.aptos.indexer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** GraphQL request body. */
@Serializable
data class GraphQLRequest(val query: String, val variables: JsonObject? = null)

/** GraphQL response wrapper. */
@Serializable
data class GraphQLResponse<T>(val data: T? = null, val errors: List<GraphQLError>? = null)

/** GraphQL error entry. */
@Serializable
data class GraphQLError(
    val message: String,
    val locations: List<GraphQLErrorLocation>? = null,
    val path: List<String>? = null,
)

/** GraphQL error location. */
@Serializable
data class GraphQLErrorLocation(val line: Int, val column: Int)

/** Indexer token data. */
@Serializable
data class Token(
    @SerialName("token_data_id") val tokenDataId: String? = null,
    @SerialName("token_name") val tokenName: String? = null,
    @SerialName("collection_name") val collectionName: String? = null,
    @SerialName("creator_address") val creatorAddress: String? = null,
    val amount: String? = null,
    @SerialName("token_uri") val tokenUri: String? = null,
    @SerialName("token_properties") val tokenProperties: JsonObject? = null,
)

/** Indexer collection data. */
@Serializable
data class Collection(
    @SerialName("collection_id") val collectionId: String? = null,
    @SerialName("collection_name") val collectionName: String? = null,
    @SerialName("creator_address") val creatorAddress: String? = null,
    val description: String? = null,
    val uri: String? = null,
    @SerialName("max_supply") val maxSupply: String? = null,
    @SerialName("current_supply") val currentSupply: String? = null,
)

/** Indexer transaction with payload data. */
@Serializable
data class IndexerTransaction(
    val version: String? = null,
    val hash: String? = null,
    val sender: String? = null,
    @SerialName("sequence_number") val sequenceNumber: String? = null,
    val success: Boolean? = null,
    @SerialName("vm_status") val vmStatus: String? = null,
    @SerialName("gas_used") val gasUsed: String? = null,
    val timestamp: String? = null,
    val payload: JsonObject? = null,
)

/** Indexer event data. */
@Serializable
data class IndexerEvent(
    @SerialName("account_address") val accountAddress: String? = null,
    @SerialName("sequence_number") val sequenceNumber: String? = null,
    val type: String? = null,
    val data: JsonObject? = null,
    @SerialName("transaction_version") val transactionVersion: String? = null,
)

// --- Response wrapper types for GraphQL deserialization ---

@Serializable
internal data class TokensData(@SerialName("current_token_ownerships_v2") val tokens: List<Token>)

@Serializable
internal data class CollectionsData(@SerialName("current_collections_v2") val collections: List<Collection>)

@Serializable
internal data class TransactionsData(@SerialName("user_transactions") val transactions: List<IndexerTransaction>)

@Serializable
internal data class EventsData(val events: List<IndexerEvent>)
