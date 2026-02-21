package com.aptos.client.faucet

import com.aptos.client.config.AptosConfig
import com.aptos.core.error.ApiException
import com.aptos.core.types.AccountAddress
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

/**
 * Client for the Aptos faucet API (testnet/devnet/localnet).
 */
class FaucetClient(
    val config: AptosConfig,
    engine: HttpClientEngine? = null,
) {
    private val httpClient: HttpClient = HttpClient(engine ?: CIO.create()) {
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMs
            connectTimeoutMillis = config.timeoutMs
        }
    }

    private val faucetUrl: String = config.faucetUrl?.trimEnd('/')
        ?: throw ApiException("Faucet URL not configured for this network")

    suspend fun fundAccount(address: AccountAddress, amount: ULong = 100_000_000uL) {
        val response = httpClient.post("$faucetUrl/fund") {
            contentType(ContentType.Application.Json)
            setBody("""{"address":"${address.toHex()}","amount":$amount}""")
        }

        if (!response.status.isSuccess()) {
            // Try legacy endpoint format
            val legacyResponse = httpClient.post("$faucetUrl/mint") {
                parameter("address", address.toHex())
                parameter("amount", amount.toString())
            }
            if (!legacyResponse.status.isSuccess()) {
                throw ApiException(
                    "Faucet request failed: ${response.bodyAsText()}",
                    statusCode = response.status.value,
                )
            }
        }
    }

    @JvmName("fundAccountSync")
    fun fundAccountBlocking(address: AccountAddress, amount: ULong = 100_000_000uL) =
        runBlocking { fundAccount(address, amount) }

    fun close() {
        httpClient.close()
    }
}
