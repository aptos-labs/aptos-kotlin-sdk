package com.aptos.sdk

import com.aptos.client.config.AptosConfig
import com.aptos.client.faucet.FaucetClient
import com.aptos.client.rest.AccountInfo
import com.aptos.client.rest.AccountResource
import com.aptos.client.rest.AptosRestClient
import com.aptos.client.rest.GasEstimate
import com.aptos.client.rest.LedgerInfo
import com.aptos.client.rest.PendingTransaction
import com.aptos.client.rest.TransactionResponse
import com.aptos.core.account.Account
import com.aptos.core.transaction.SignedTransaction
import com.aptos.core.transaction.TransactionBuilder
import com.aptos.core.transaction.TransactionPayload
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId
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

    internal constructor(config: AptosConfig, engine: HttpClientEngine) : this(
        config = config,
        restClient = AptosRestClient(config, engine),
        faucetClient = null,
    )

    // --- Account ---

    suspend fun getAccount(address: AccountAddress): AccountInfo = restClient.getAccount(address)

    suspend fun getAccountResources(address: AccountAddress): List<AccountResource> =
        restClient.getAccountResources(address)

    suspend fun getAccountResource(address: AccountAddress, resourceType: String): AccountResource =
        restClient.getAccountResource(address, resourceType)

    // --- Balance ---

    suspend fun getBalance(address: AccountAddress): ULong = restClient.getBalance(address)

    @JvmName("getBalanceSync")
    fun getBalanceBlocking(address: AccountAddress): ULong = runBlocking { getBalance(address) }

    // --- Ledger ---

    suspend fun getLedgerInfo(): LedgerInfo = restClient.getLedgerInfo()

    @JvmName("getLedgerInfoSync")
    fun getLedgerInfoBlocking(): LedgerInfo = runBlocking { getLedgerInfo() }

    // --- Gas ---

    suspend fun estimateGasPrice(): GasEstimate = restClient.estimateGasPrice()

    // --- Faucet ---

    suspend fun fundAccount(address: AccountAddress, amount: ULong = 100_000_000uL) {
        val faucet = checkNotNull(faucetClient) { "Faucet not available for this network configuration" }
        faucet.fundAccount(address, amount)
    }

    @JvmName("fundAccountSync")
    fun fundAccountBlocking(address: AccountAddress, amount: ULong = 100_000_000uL) =
        runBlocking { fundAccount(address, amount) }

    // --- Transactions ---

    suspend fun submitTransaction(signedTxn: SignedTransaction): PendingTransaction =
        restClient.submitTransaction(signedTxn)

    suspend fun waitForTransaction(
        hash: String,
        timeoutMs: Long = 30_000,
        pollIntervalMs: Long = 1_000,
    ): TransactionResponse = restClient.waitForTransaction(hash, timeoutMs, pollIntervalMs)

    suspend fun getTransactionByHash(hash: String): TransactionResponse = restClient.getTransactionByHash(hash)

    // --- View ---

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
