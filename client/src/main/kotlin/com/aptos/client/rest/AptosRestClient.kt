package com.aptos.client.rest

import com.aptos.client.config.AptosConfig
import com.aptos.client.retry.RetryPolicy
import com.aptos.core.error.ApiException
import com.aptos.core.transaction.SignedTransaction
import com.aptos.core.types.AccountAddress
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Aptos REST API client with coroutine-based async operations.
 *
 * Provides suspend functions for all Aptos node REST endpoints including account info,
 * resource queries, transaction submission, gas estimation, and view functions.
 * All retryable operations use exponential backoff per the configured [AptosConfig.retryConfig].
 *
 * @param config the network and retry configuration
 * @param engine optional Ktor [HttpClientEngine] (defaults to CIO; use MockEngine for testing)
 */
class AptosRestClient(val config: AptosConfig, engine: HttpClientEngine? = null) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    internal val httpClient: HttpClient = HttpClient(engine ?: CIO.create()) {
        install(ContentNegotiation) {
            json(this@AptosRestClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMs
            connectTimeoutMillis = config.timeoutMs
        }
    }

    private val baseUrl = config.nodeUrl.trimEnd('/')

    // --- Ledger ---

    /**
     * Fetches current ledger information from the node.
     *
     * @return ledger metadata including chain ID, epoch, and block height
     */
    suspend fun getLedgerInfo(): LedgerInfo = retryable {
        val response = httpClient.get(baseUrl)
        handleResponse(response)
        response.body()
    }

    /** Blocking variant of [getLedgerInfo] for Java interop. */
    @JvmName("getLedgerInfoSync")
    fun getLedgerInfoBlocking(): LedgerInfo = runBlocking { getLedgerInfo() }

    // --- Account ---

    /**
     * Fetches on-chain account information.
     *
     * @param address the account address to look up
     * @return account metadata including sequence number and authentication key
     */
    suspend fun getAccount(address: AccountAddress): AccountInfo = retryable {
        val response = httpClient.get("$baseUrl/accounts/${address.toHex()}")
        handleResponse(response)
        response.body()
    }

    /** Blocking variant of [getAccount] for Java interop. */
    @JvmName("getAccountSync")
    fun getAccountBlocking(address: AccountAddress): AccountInfo = runBlocking { getAccount(address) }

    /**
     * Fetches all resources stored under an account.
     *
     * @param address the account whose resources to retrieve
     * @return list of all resources held by the account
     */
    suspend fun getAccountResources(address: AccountAddress): List<AccountResource> = retryable {
        val response = httpClient.get("$baseUrl/accounts/${address.toHex()}/resources")
        handleResponse(response)
        response.body()
    }

    /**
     * Fetches a specific resource by type from an account.
     *
     * @param address the account address
     * @param resourceType the fully-qualified Move resource type
     * @return the requested resource
     */
    suspend fun getAccountResource(address: AccountAddress, resourceType: String): AccountResource = retryable {
        val response = httpClient.get("$baseUrl/accounts/${address.toHex()}/resource/$resourceType")
        handleResponse(response)
        response.body()
    }

    // --- Transactions ---

    /**
     * Fetches a committed transaction by its hash.
     *
     * @param hash the transaction hash (hex-encoded with `0x` prefix)
     * @return the transaction response
     */
    suspend fun getTransactionByHash(hash: String): TransactionResponse = retryable {
        val response = httpClient.get("$baseUrl/transactions/by_hash/$hash")
        handleResponse(response)
        response.body()
    }

    /**
     * Fetches a committed transaction by its ledger version.
     *
     * @param version the ledger version number
     * @return the transaction response
     */
    suspend fun getTransactionByVersion(version: ULong): TransactionResponse = retryable {
        val response = httpClient.get("$baseUrl/transactions/by_version/$version")
        handleResponse(response)
        response.body()
    }

    /** Blocking variant of [getTransactionByVersion] for Java interop. */
    @JvmName("getTransactionByVersionSync")
    fun getTransactionByVersionBlocking(version: ULong): TransactionResponse = runBlocking {
        getTransactionByVersion(version)
    }

    /** Submits a BCS-encoded signed transaction and returns the pending transaction info. */
    suspend fun submitTransaction(signedTxn: SignedTransaction): PendingTransaction = retryable {
        val bcsBytes = signedTxn.toSubmitBytes()
        val response = httpClient.post("$baseUrl/transactions") {
            contentType(ContentType("application", "x.aptos.signed_transaction+bcs"))
            setBody(bcsBytes)
        }
        handleResponse(response)
        response.body()
    }

    /**
     * Polls until the transaction is confirmed or the timeout is reached.
     *
     * @throws ApiException if the transaction fails or times out
     */
    suspend fun waitForTransaction(
        hash: String,
        timeoutMs: Long = 30_000,
        pollIntervalMs: Long = 1_000,
    ): TransactionResponse {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val txn = pollTransaction(hash)
            if (txn != null) return txn
            delay(pollIntervalMs)
        }
        throw ApiException("Transaction $hash timed out after ${timeoutMs}ms")
    }

    private suspend fun pollTransaction(hash: String): TransactionResponse? {
        val txn =
            try {
                getTransactionByHash(hash)
            } catch (e: ApiException) {
                return when (e.statusCode) {
                    404, 429 -> null // Not found yet / currently rate limited
                    else -> throw e
                }
            }
        if (txn.type == "pending_transaction") return null
        if (txn.success == true) return txn
        throw ApiException("Transaction failed: ${txn.vmStatus}", errorCode = txn.vmStatus)
    }

    /** Simulates a signed transaction without executing it. */
    suspend fun simulateTransaction(signedTxn: SignedTransaction): List<SimulationResult> = retryable {
        val bcsBytes = signedTxn.toSubmitBytes()
        val response = httpClient.post("$baseUrl/transactions/simulate") {
            contentType(ContentType("application", "x.aptos.signed_transaction+bcs"))
            setBody(bcsBytes)
        }
        handleResponse(response)
        response.body()
    }

    /** Blocking variant of [simulateTransaction] for Java interop. */
    @JvmName("simulateTransactionSync")
    fun simulateTransactionBlocking(signedTxn: SignedTransaction): List<SimulationResult> =
        runBlocking { simulateTransaction(signedTxn) }

    /**
     * Fetches transactions sent by the given account address.
     *
     * @param address the sender account address
     * @param start optional starting sequence number for pagination
     * @param limit optional maximum number of transactions to return
     * @return list of transactions originating from the account
     */
    suspend fun getAccountTransactions(
        address: AccountAddress,
        start: Long? = null,
        limit: Int? = null,
    ): List<TransactionResponse> = retryable {
        val response = httpClient.get("$baseUrl/accounts/${address.toHex()}/transactions") {
            start?.let { parameter("start", it.toString()) }
            limit?.let { parameter("limit", it.toString()) }
        }
        handleResponse(response)
        response.body()
    }

    /** Blocking variant of [getAccountTransactions] for Java interop. */
    @JvmName("getAccountTransactionsSync")
    fun getAccountTransactionsBlocking(
        address: AccountAddress,
        start: Long? = null,
        limit: Int? = null,
    ): List<TransactionResponse> = runBlocking { getAccountTransactions(address, start, limit) }

    /**
     * Fetches events from an event handle stored under an account.
     *
     * @param address the account address
     * @param eventHandle the fully-qualified event handle struct type
     * @param fieldName the event handle field name
     * @param start optional starting sequence number for pagination
     * @param limit optional maximum number of events to return
     * @return list of events matching the criteria
     */
    suspend fun getEvents(
        address: AccountAddress,
        eventHandle: String,
        fieldName: String,
        start: Long? = null,
        limit: Int? = null,
    ): List<EventResponse> = retryable {
        val response = httpClient.get(
            "$baseUrl/accounts/${address.toHex()}/events/$eventHandle/$fieldName",
        ) {
            start?.let { parameter("start", it.toString()) }
            limit?.let { parameter("limit", it.toString()) }
        }
        handleResponse(response)
        response.body()
    }

    /** Blocking variant of [getEvents] for Java interop. */
    @JvmName("getEventsSync")
    fun getEventsBlocking(
        address: AccountAddress,
        eventHandle: String,
        fieldName: String,
        start: Long? = null,
        limit: Int? = null,
    ): List<EventResponse> = runBlocking { getEvents(address, eventHandle, fieldName, start, limit) }

    // --- Gas ---

    /**
     * Estimates the current gas unit price from the node.
     *
     * @return gas price estimate with standard, prioritized, and deprioritized values
     */
    suspend fun estimateGasPrice(): GasEstimate = retryable {
        val response = httpClient.get("$baseUrl/estimate_gas_price")
        handleResponse(response)
        response.body()
    }

    // --- View ---

    /**
     * Executes a Move view function (read-only, no state changes).
     *
     * @param function the fully-qualified function name (e.g. `"0x1::coin::balance"`)
     * @param typeArguments Move type arguments for the function
     * @param arguments function arguments as JSON-encoded strings
     * @return the view function result as a JSON array
     */
    suspend fun view(
        function: String,
        typeArguments: List<String> = emptyList(),
        arguments: List<String> = emptyList(),
    ): JsonArray = retryable {
        val response = httpClient.post("$baseUrl/view") {
            contentType(ContentType.Application.Json)
            setBody(ViewRequest(function, typeArguments, arguments))
        }
        handleResponse(response)
        response.body()
    }

    // --- Balance ---

    /** Returns the APT balance (in octas) for the given [address]. */
    suspend fun getBalance(address: AccountAddress): ULong {
        val resource = getAccountResource(
            address,
            "0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>",
        )
        val coinData = resource.data["coin"]?.jsonObject
            ?: throw ApiException("Missing 'coin' in CoinStore resource")
        val valueStr = coinData["value"]?.jsonPrimitive?.content
            ?: throw ApiException("Missing 'value' in coin data")
        return valueStr.toULong()
    }

    /** Blocking variant of [getBalance] for Java interop. */
    @JvmName("getBalanceSync")
    fun getBalanceBlocking(address: AccountAddress): ULong = runBlocking { getBalance(address) }

    // --- Helpers ---

    private suspend fun <T> retryable(block: suspend () -> T): T = RetryPolicy.withRetry(config.retryConfig, block)

    private suspend fun handleResponse(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            val body = try {
                response.body<ApiError>()
            } catch (_: Exception) {
                null
            }
            val message = body?.message ?: "HTTP ${response.status.value}: ${response.bodyAsText()}"
            throw ApiException(
                message = message,
                statusCode = response.status.value,
                errorCode = body?.errorCode,
            )
        }
    }

    /** Closes the underlying HTTP client and releases resources. */
    fun close() {
        httpClient.close()
    }
}
