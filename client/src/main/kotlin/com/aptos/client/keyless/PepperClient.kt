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
 * Client for the Aptos pepper service used in keyless account creation.
 *
 * The pepper service provides a deterministic blinding factor (pepper) for a given
 * OIDC identity, enabling privacy-preserving address derivation.
 */
class PepperClient(
    private val pepperServiceUrl: String,
    engine: HttpClientEngine? = null,
    private val timeoutMs: Long = 30_000,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient: HttpClient = HttpClient(engine ?: CIO.create()) {
        install(ContentNegotiation) {
            json(this@PepperClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
        }
    }

    /**
     * Fetches the pepper for the given OIDC identity.
     *
     * @param jwt the JWT token from the identity provider
     * @param ephemeralPublicKey the hex-encoded ephemeral public key
     * @param uidKey the claim key used as the unique identifier (default: "sub")
     * @return the pepper bytes
     */
    suspend fun getPepper(jwt: String, ephemeralPublicKey: String, uidKey: String = "sub"): ByteArray {
        val response = httpClient.post("${pepperServiceUrl.trimEnd('/')}/v0/pepper") {
            contentType(ContentType.Application.Json)
            setBody(PepperRequest(jwt, ephemeralPublicKey, uidKey))
        }
        if (!response.status.isSuccess()) {
            throw ApiException(
                message = "Pepper service error: ${response.bodyAsText()}",
                statusCode = response.status.value,
            )
        }
        val result = response.body<PepperResponse>()
        return HexString.decode(result.pepper)
    }

    fun close() {
        httpClient.close()
    }

    @Serializable
    internal data class PepperRequest(
        val jwt: String,
        @SerialName("epk") val ephemeralPublicKey: String,
        @SerialName("uid_key") val uidKey: String,
    )

    @Serializable
    internal data class PepperResponse(val pepper: String)
}
