package com.aptos.core.account

import com.aptos.core.crypto.AuthenticationKey
import com.aptos.core.crypto.EphemeralKeyPair
import com.aptos.core.crypto.Keyless
import com.aptos.core.crypto.SignatureScheme
import com.aptos.core.types.AccountAddress

/**
 * An Aptos account backed by a keyless (OIDC) identity.
 *
 * Uses an ephemeral key pair for signing, combined with a zero-knowledge proof
 * that links the ephemeral key to the OIDC identity without revealing the JWT.
 *
 * @property ephemeralKeyPair the ephemeral Ed25519 key pair for signing
 * @property keylessPublicKey the keyless public key derived from OIDC claims
 * @property proof the zero-knowledge proof bytes from the prover service
 * @property jwt the original JWT token
 */
class KeylessAccount private constructor(
    val ephemeralKeyPair: EphemeralKeyPair,
    val keylessPublicKey: Keyless.PublicKey,
    val proof: ByteArray,
    val jwt: String,
    override val address: AccountAddress,
) : Account {
    override val publicKeyBytes: ByteArray
        get() {
            val serializer = com.aptos.core.bcs.BcsSerializer()
            keylessPublicKey.serialize(serializer)
            return serializer.toByteArray()
        }

    override val scheme: SignatureScheme get() = SignatureScheme.KEYLESS
    override val authenticationKey: AuthenticationKey by lazy {
        keylessPublicKey.authKey()
    }

    /**
     * Signs the [message] with the ephemeral key pair.
     * Checks that the ephemeral key pair has not expired.
     */
    override fun sign(message: ByteArray): ByteArray {
        check(!ephemeralKeyPair.isExpired()) { "Ephemeral key pair has expired" }
        return ephemeralKeyPair.sign(message).data
    }

    companion object {
        /**
         * Creates a keyless account from the given components.
         *
         * @param ephemeralKeyPair the ephemeral key pair used for signing
         * @param keylessPublicKey the keyless public key
         * @param proof the ZK proof from the prover service
         * @param jwt the original JWT token
         */
        @JvmStatic
        fun create(
            ephemeralKeyPair: EphemeralKeyPair,
            keylessPublicKey: Keyless.PublicKey,
            proof: ByteArray,
            jwt: String,
        ): KeylessAccount {
            val authKey = keylessPublicKey.authKey()
            return KeylessAccount(ephemeralKeyPair, keylessPublicKey, proof, jwt, authKey.derivedAddress())
        }
    }
}
