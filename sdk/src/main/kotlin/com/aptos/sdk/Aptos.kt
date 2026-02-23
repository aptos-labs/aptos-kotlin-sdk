package com.aptos.sdk

import com.aptos.client.config.AptosConfig
import com.aptos.client.faucet.FaucetClient
import com.aptos.client.keyless.PepperClient
import com.aptos.client.keyless.ProverClient
import com.aptos.client.rest.AccountInfo
import com.aptos.client.rest.AccountResource
import com.aptos.client.rest.AptosRestClient
import com.aptos.client.rest.EventResponse
import com.aptos.client.rest.GasEstimate
import com.aptos.client.rest.LedgerInfo
import com.aptos.client.rest.PendingTransaction
import com.aptos.client.rest.SimulationResult
import com.aptos.client.rest.TransactionResponse
import com.aptos.core.account.Account
import com.aptos.core.account.KeylessAccount
import com.aptos.core.crypto.EphemeralKeyPair
import com.aptos.core.crypto.Keyless
import com.aptos.core.transaction.SignedTransaction
import com.aptos.core.transaction.TransactionBuilder
import com.aptos.core.transaction.TransactionPayload
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId
import com.aptos.core.types.HexString
import io.ktor.client.engine.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray

/**
 * Main entry point for the Aptos Kotlin SDK.
 *
 * Composes REST client, faucet client, and transaction utilities into a single facade.
 * Use the static factories [testnet], [mainnet], [devnet], [localnet] for quick setup,
 * or [builder] for custom configuration.
 *
 * All network methods are `suspend` functions. Java callers can use the `*Blocking` variants.
 *
 * ```kotlin
 * val aptos = Aptos.testnet()
 * val balance = aptos.getBalance(address)
 * aptos.close()
 * ```
 */
class Aptos private constructor(
    val config: AptosConfig,
    private val restClient: AptosRestClient,
    private val faucetClient: FaucetClient?,
) {
    constructor(config: AptosConfig) : this(
        config = config,
        restClient = AptosRestClient(config),
        faucetClient = if (config.faucetUrl != null) FaucetClient(config) else null,
    )

    constructor(config: AptosConfig, engine: HttpClientEngine) : this(
        config = config,
        restClient = AptosRestClient(config, engine),
        faucetClient = if (config.faucetUrl != null) FaucetClient(config, engine) else null,
    )

    // --- Account ---

    /**
     * Fetches on-chain account information, including the sequence number and authentication key.
     *
     * @param address the account address to look up
     * @return account metadata from the REST API
     */
    suspend fun getAccount(address: AccountAddress): AccountInfo = restClient.getAccount(address)

    /**
     * Fetches all resources stored under an account.
     *
     * @param address the account whose resources to retrieve
     * @return list of all account resources
     */
    suspend fun getAccountResources(address: AccountAddress): List<AccountResource> =
        restClient.getAccountResources(address)

    /**
     * Fetches a single resource by type from an account.
     *
     * @param address the account address
     * @param resourceType the fully-qualified Move resource type (e.g. `"0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>"`)
     * @return the requested account resource
     */
    suspend fun getAccountResource(address: AccountAddress, resourceType: String): AccountResource =
        restClient.getAccountResource(address, resourceType)

    // --- Balance ---

    /**
     * Returns the APT balance (in octas) for the given account.
     *
     * @param address the account address
     * @return balance in octas (1 APT = 100,000,000 octas)
     */
    suspend fun getBalance(address: AccountAddress): ULong = restClient.getBalance(address)

    /** Blocking variant of [getBalance] for Java interop. */
    @JvmName("getBalanceSync")
    fun getBalanceBlocking(address: AccountAddress): ULong = runBlocking { getBalance(address) }

    // --- Ledger ---

    /**
     * Fetches current ledger information, including chain ID, epoch, and block height.
     *
     * @return current ledger metadata
     */
    suspend fun getLedgerInfo(): LedgerInfo = restClient.getLedgerInfo()

    /** Blocking variant of [getLedgerInfo] for Java interop. */
    @JvmName("getLedgerInfoSync")
    fun getLedgerInfoBlocking(): LedgerInfo = runBlocking { getLedgerInfo() }

    // --- Gas ---

    /**
     * Estimates the current gas unit price.
     *
     * @return gas price estimate with standard, prioritized, and deprioritized values
     */
    suspend fun estimateGasPrice(): GasEstimate = restClient.estimateGasPrice()

    // --- Faucet ---

    /**
     * Funds an account via the faucet (testnet/devnet/localnet only).
     *
     * @param address the account to fund
     * @param amount the amount of octas to request (default: 100,000,000 = 1 APT)
     * @throws IllegalStateException if no faucet URL is configured for the current network
     */
    suspend fun fundAccount(address: AccountAddress, amount: ULong = 100_000_000uL) {
        val faucet = checkNotNull(faucetClient) { "Faucet not available for this network configuration" }
        faucet.fundAccount(address, amount)
    }

    /** Blocking variant of [fundAccount] for Java interop. */
    @JvmName("fundAccountSync")
    fun fundAccountBlocking(address: AccountAddress, amount: ULong = 100_000_000uL) =
        runBlocking { fundAccount(address, amount) }

    // --- Simulation ---

    /**
     * Simulates a signed transaction without submitting it to the blockchain.
     *
     * @param signedTxn the signed transaction to simulate
     * @return simulation results including gas used and execution status
     */
    suspend fun simulateTransaction(signedTxn: SignedTransaction): List<SimulationResult> =
        restClient.simulateTransaction(signedTxn)

    /** Blocking variant of [simulateTransaction] for Java interop. */
    @JvmName("simulateTransactionSync")
    fun simulateTransactionBlocking(signedTxn: SignedTransaction): List<SimulationResult> =
        runBlocking { simulateTransaction(signedTxn) }

    // --- Account Transactions ---

    /**
     * Fetches transactions sent by an account.
     *
     * @param address the sender address
     * @param start optional starting sequence number for pagination
     * @param limit optional maximum number of transactions to return
     * @return list of transactions sent by the account
     */
    suspend fun getAccountTransactions(
        address: AccountAddress,
        start: Long? = null,
        limit: Int? = null,
    ): List<TransactionResponse> = restClient.getAccountTransactions(address, start, limit)

    /** Blocking variant of [getAccountTransactions] for Java interop. */
    @JvmName("getAccountTransactionsSync")
    fun getAccountTransactionsBlocking(
        address: AccountAddress,
        start: Long? = null,
        limit: Int? = null,
    ): List<TransactionResponse> = runBlocking { getAccountTransactions(address, start, limit) }

    // --- Events ---

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
    ): List<EventResponse> = restClient.getEvents(address, eventHandle, fieldName, start, limit)

    /** Blocking variant of [getEvents] for Java interop. */
    @JvmName("getEventsSync")
    fun getEventsBlocking(
        address: AccountAddress,
        eventHandle: String,
        fieldName: String,
        start: Long? = null,
        limit: Int? = null,
    ): List<EventResponse> = runBlocking { getEvents(address, eventHandle, fieldName, start, limit) }

    // --- Transactions ---

    /**
     * Submits a signed transaction to the blockchain.
     *
     * @param signedTxn the BCS-encoded signed transaction
     * @return pending transaction with its hash for status tracking
     */
    suspend fun submitTransaction(signedTxn: SignedTransaction): PendingTransaction =
        restClient.submitTransaction(signedTxn)

    /**
     * Polls until a transaction is committed or the timeout is reached.
     *
     * @param hash the transaction hash to wait for
     * @param timeoutMs maximum time to wait in milliseconds (default: 30,000)
     * @param pollIntervalMs polling interval in milliseconds (default: 1,000)
     * @return the committed transaction response
     * @throws com.aptos.core.error.ApiException if the transaction is not found within the timeout
     */
    suspend fun waitForTransaction(
        hash: String,
        timeoutMs: Long = 30_000,
        pollIntervalMs: Long = 1_000,
    ): TransactionResponse = restClient.waitForTransaction(hash, timeoutMs, pollIntervalMs)

    /**
     * Fetches a committed transaction by its hash.
     *
     * @param hash the transaction hash (hex-encoded with `0x` prefix)
     * @return the transaction response
     */
    suspend fun getTransactionByHash(hash: String): TransactionResponse = restClient.getTransactionByHash(hash)

    /**
     * Fetches a committed transaction by its ledger version.
     *
     * @param version the ledger version
     * @return the transaction response
     */
    suspend fun getTransactionByVersion(version: ULong): TransactionResponse =
        restClient.getTransactionByVersion(version)

    /** Blocking variant of [getTransactionByVersion] for Java interop. */
    @JvmName("getTransactionByVersionSync")
    fun getTransactionByVersionBlocking(version: ULong): TransactionResponse = runBlocking {
        getTransactionByVersion(version)
    }

    // --- View ---

    /**
     * Executes a Move view function and returns the result.
     *
     * View functions are read-only and do not modify on-chain state.
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
    ): JsonArray = restClient.view(function, typeArguments, arguments)

    // --- High-Level Convenience ---

    /**
     * Builds and signs an APT transfer transaction.
     *
     * Automatically fetches the sender's sequence number and chain ID.
     * The returned [SignedTransaction] can be submitted via [submitTransaction].
     */
    suspend fun transfer(
        sender: Account,
        to: AccountAddress,
        amount: ULong,
        maxGasAmount: ULong = 200_000uL,
        gasUnitPrice: ULong = 100uL,
    ): SignedTransaction {
        val accountInfo = getAccount(sender.address)
        val chainId = config.chainId ?: ChainId(getLedgerInfo().chainId.toUByte())

        val payload = TransactionPayload.EntryFunction.aptTransfer(to, amount)
        val rawTxn = TransactionBuilder.builder()
            .sender(sender.address)
            .sequenceNumber(accountInfo.sequenceNumber.toULong())
            .payload(payload)
            .maxGasAmount(maxGasAmount)
            .gasUnitPrice(gasUnitPrice)
            .chainId(chainId)
            .build()

        return TransactionBuilder.signTransaction(sender, rawTxn)
    }

    // --- Keyless ---

    /**
     * Creates a keyless account from a JWT and ephemeral key pair.
     *
     * Contacts the pepper and prover services to obtain the pepper and ZK proof.
     *
     * @param jwt the JWT token from the OIDC provider
     * @param ephemeralKeyPair the ephemeral key pair used during the OIDC flow
     * @param uidKey the JWT claim used as the unique identifier (default: "sub")
     */
    suspend fun createKeylessAccount(
        jwt: String,
        ephemeralKeyPair: EphemeralKeyPair,
        uidKey: String = "sub",
    ): KeylessAccount {
        val pepperUrl = checkNotNull(config.pepperServiceUrl) { "Pepper service URL not configured" }
        val proverUrl = checkNotNull(config.proverServiceUrl) { "Prover service URL not configured" }

        val claims = Keyless.parseJwt(jwt)
        val epkHex = HexString.encodeWithPrefix(ephemeralKeyPair.publicKeyBytes())

        val pepperClient = PepperClient(pepperUrl)
        val pepper = try {
            pepperClient.getPepper(jwt, epkHex, uidKey)
        } finally {
            pepperClient.close()
        }

        val proverClient = ProverClient(proverUrl)
        val proof = try {
            proverClient.getProof(
                jwt = jwt,
                ephemeralPublicKey = epkHex,
                expirationDateSecs = ephemeralKeyPair.expirationDateSecs,
                pepper = HexString.encodeWithPrefix(pepper),
                uidKey = uidKey,
            )
        } finally {
            proverClient.close()
        }

        val uidVal = if (uidKey == "sub") claims.sub else claims.sub
        val keylessPublicKey = Keyless.PublicKey(
            iss = claims.iss,
            aud = claims.aud,
            uidKey = uidKey,
            uidVal = uidVal,
            pepper = pepper,
        )

        return KeylessAccount.create(ephemeralKeyPair, keylessPublicKey, proof, jwt)
    }

    /** Closes the underlying HTTP clients and releases resources. */
    fun close() {
        restClient.close()
        faucetClient?.close()
    }

    companion object {
        @JvmStatic
        fun testnet(): Aptos = Aptos(AptosConfig.testnet())

        @JvmStatic
        fun mainnet(): Aptos = Aptos(AptosConfig.mainnet())

        @JvmStatic
        fun devnet(): Aptos = Aptos(AptosConfig.devnet())

        @JvmStatic
        fun localnet(): Aptos = Aptos(AptosConfig.localnet())

        @JvmStatic
        fun builder(): AptosBuilder = AptosBuilder()
    }
}
