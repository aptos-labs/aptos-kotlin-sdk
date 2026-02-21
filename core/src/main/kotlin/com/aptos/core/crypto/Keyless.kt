package com.aptos.core.crypto

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.CryptoException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * Keyless (OIDC) cryptographic types for the Aptos blockchain.
 *
 * Enables account creation and authentication using OpenID Connect identity providers
 * (Google, Apple, etc.) without requiring users to manage private keys directly.
 */
object Keyless {
    /**
     * Parsed JWT claims relevant to keyless authentication.
     */
    data class JwtClaims(
        val iss: String,
        val aud: String,
        val sub: String,
        val nonce: String? = null,
        val uidKey: String = "sub",
        val uidVal: String = sub,
    )

    /**
     * Parses a JWT token and extracts the payload claims.
     *
     * @param token a JWT token in the format header.payload.signature
     * @throws CryptoException if the token is malformed
     */
    @JvmStatic
    fun parseJwt(token: String): JwtClaims {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw CryptoException("Invalid JWT: expected 3 parts, got ${parts.size}")
        }
        val payloadJson = try {
            val decoded = Base64.getUrlDecoder().decode(parts[1])
            String(decoded, Charsets.UTF_8)
        } catch (e: Exception) {
            throw CryptoException("Invalid JWT payload encoding", e)
        }

        val json = Json { ignoreUnknownKeys = true }
        val obj = try {
            json.parseToJsonElement(payloadJson).jsonObject
        } catch (e: Exception) {
            throw CryptoException("Invalid JWT payload JSON", e)
        }

        fun claim(name: String): String = obj[name]?.jsonPrimitive?.content
            ?: throw CryptoException("JWT missing '$name' claim")

        return JwtClaims(
            iss = claim("iss"),
            aud = claim("aud"),
            sub = claim("sub"),
            nonce = obj["nonce"]?.jsonPrimitive?.content,
        )
    }

    /**
     * A keyless public key derived from OIDC identity provider claims.
     *
     * Auth key: SHA3-256(SHA3-256(iss) || SHA3-256(aud) || SHA3-256(uidKey + ":" + uidVal) || pepper || 0x05)
     */
    data class PublicKey(
        val iss: String,
        val aud: String,
        val uidKey: String,
        val uidVal: String,
        val pepper: ByteArray,
    ) : BcsSerializable {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeString(iss)
            serializer.serializeString(aud)
            serializer.serializeString(uidKey)
            serializer.serializeString(uidVal)
            serializer.serializeBytes(pepper)
        }

        /** Derives the authentication key for this keyless public key. */
        fun authKey(): AuthenticationKey {
            val issHash = Hashing.sha3256(iss.toByteArray(Charsets.UTF_8))
            val audHash = Hashing.sha3256(aud.toByteArray(Charsets.UTF_8))
            val uidHash = Hashing.sha3256("$uidKey:$uidVal".toByteArray(Charsets.UTF_8))
            return AuthenticationKey.fromKeyless(issHash, audHash, uidHash, pepper)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PublicKey) return false
            return iss == other.iss &&
                aud == other.aud &&
                uidKey == other.uidKey &&
                uidVal == other.uidVal &&
                pepper.contentEquals(other.pepper)
        }

        override fun hashCode(): Int {
            var result = iss.hashCode()
            result = 31 * result + aud.hashCode()
            result = 31 * result + uidKey.hashCode()
            result = 31 * result + uidVal.hashCode()
            result = 31 * result + pepper.contentHashCode()
            return result
        }
    }
}
