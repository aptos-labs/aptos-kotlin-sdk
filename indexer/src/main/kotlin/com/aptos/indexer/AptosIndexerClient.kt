package com.aptos.indexer

import com.aptos.core.error.ApiException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Aptos Indexer GraphQL client for querying indexed blockchain data.
 *
 * Provides high-level methods for common queries (tokens, collections, transactions, events)
 * as well as a generic [query] method for custom GraphQL operations.
 *
 * This is an opt-in module — users add the `:indexer` dependency separately.
 */
class AptosIndexerClient(val config: IndexerConfig, engine: HttpClientEngine? = null) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient: HttpClient = HttpClient(engine ?: CIO.create()) {
        install(ContentNegotiation) {
            json(this@AptosIndexerClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMs
            connectTimeoutMillis = config.timeoutMs
        }
    }

    /**
     * Executes a raw GraphQL query and returns the response body as a string.
     */
    suspend fun query(graphqlQuery: String, variables: JsonObject? = null): String {
        val request = GraphQLRequest(query = graphqlQuery, variables = variables)
        val response = httpClient.post(config.indexerUrl) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw ApiException(
                message = "Indexer error: ${response.bodyAsText()}",
                statusCode = response.status.value,
            )
        }
        return response.bodyAsText()
    }

    /** Fetches tokens owned by the given address. */
    suspend fun getAccountTokens(ownerAddress: String, offset: Int? = null, limit: Int? = null): List<Token> {
        val variables = buildJsonObject {
            put("owner_address", ownerAddress)
            offset?.let { put("offset", it) }
            limit?.let { put("limit", it) }
        }
        val responseText = query(Queries.GET_ACCOUNT_TOKENS, variables)
        val response = json.decodeFromString<GraphQLResponse<TokensData>>(responseText)
        checkErrors(response)
        return response.data?.tokens ?: emptyList()
    }

    /** Fetches NFT collections, optionally filtered by creator address. */
    suspend fun getCollections(
        creatorAddress: String? = null,
        offset: Int? = null,
        limit: Int? = null,
    ): List<Collection> {
        val variables = buildJsonObject {
            creatorAddress?.let { put("creator_address", it) }
            offset?.let { put("offset", it) }
            limit?.let { put("limit", it) }
        }
        val responseText = query(Queries.GET_COLLECTIONS, variables)
        val response = json.decodeFromString<GraphQLResponse<CollectionsData>>(responseText)
        checkErrors(response)
        return response.data?.collections ?: emptyList()
    }

    /** Fetches user transactions with payload data for the given sender. */
    suspend fun getAccountTransactionsWithPayload(
        sender: String,
        offset: Int? = null,
        limit: Int? = null,
    ): List<IndexerTransaction> {
        val variables = buildJsonObject {
            put("sender", sender)
            offset?.let { put("offset", it) }
            limit?.let { put("limit", it) }
        }
        val responseText = query(Queries.GET_ACCOUNT_TRANSACTIONS, variables)
        val response = json.decodeFromString<GraphQLResponse<TransactionsData>>(responseText)
        checkErrors(response)
        return response.data?.transactions ?: emptyList()
    }

    /** Fetches events, optionally filtered by account address and event type. */
    suspend fun getEvents(
        accountAddress: String? = null,
        type: String? = null,
        offset: Int? = null,
        limit: Int? = null,
    ): List<IndexerEvent> {
        val variables = buildJsonObject {
            accountAddress?.let { put("account_address", it) }
            type?.let { put("type", it) }
            offset?.let { put("offset", it) }
            limit?.let { put("limit", it) }
        }
        val responseText = query(Queries.GET_EVENTS, variables)
        val response = json.decodeFromString<GraphQLResponse<EventsData>>(responseText)
        checkErrors(response)
        return response.data?.events ?: emptyList()
    }

    private fun <T> checkErrors(response: GraphQLResponse<T>) {
        val errors = response.errors
        if (!errors.isNullOrEmpty()) {
            throw ApiException("GraphQL errors: ${errors.joinToString { it.message }}")
        }
    }

    fun close() {
        httpClient.close()
    }
}
