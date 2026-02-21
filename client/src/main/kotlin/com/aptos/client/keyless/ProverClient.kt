package com.aptos.client.keyless

import com.aptos.core.error.ApiException
import com.aptos.core.types.HexString
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Client for the Aptos prover service used in keyless account creation.
 *
 * The prover service generates a zero-knowledge proof linking the ephemeral key pair
 * to the OIDC identity without revealing the JWT.
 */
class ProverClient(
    private val proverServiceUrl: String,
    engine: HttpClientEngine? = null,
    private val timeoutMs: Long = 60_000,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient: HttpClient = HttpClient(engine ?: CIO.create()) {
        install(ContentNegotiation) {
            json(this@ProverClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
        }
    }

    /**
     * Obtains a ZK proof from the prover service.
     *
     * @param jwt the JWT token from the identity provider
     * @param ephemeralPublicKey the hex-encoded ephemeral public key
     * @param expirationDateSecs the ephemeral key pair expiration timestamp
     * @param pepper the hex-encoded pepper from the pepper service
     * @param uidKey the claim key used as the unique identifier (default: "sub")
     * @return the proof bytes
     */
    suspend fun getProof(
        jwt: String,
        ephemeralPublicKey: String,
        expirationDateSecs: Long,
        pepper: String,
        uidKey: String = "sub",
    ): ByteArray {
        val response = httpClient.post("${proverServiceUrl.trimEnd('/')}/v0/prove") {
            contentType(ContentType.Application.Json)
            setBody(
                ProverRequest(
                    jwt = jwt,
                    ephemeralPublicKey = ephemeralPublicKey,
                    expirationDateSecs = expirationDateSecs,
                    pepper = pepper,
                    uidKey = uidKey,
                ),
            )
        }
        if (!response.status.isSuccess()) {
            throw ApiException(
                message = "Prover service error: ${response.bodyAsText()}",
                statusCode = response.status.value,
            )
        }
        val result = response.body<ProverResponse>()
        return HexString.decode(result.proof)
    }

    fun close() {
        httpClient.close()
    }

    @Serializable
    internal data class ProverRequest(
        val jwt: String,
        @SerialName("epk") val ephemeralPublicKey: String,
        @SerialName("exp_date_secs") val expirationDateSecs: Long,
        val pepper: String,
        @SerialName("uid_key") val uidKey: String,
    )

    @Serializable
    internal data class ProverResponse(val proof: String)
}
