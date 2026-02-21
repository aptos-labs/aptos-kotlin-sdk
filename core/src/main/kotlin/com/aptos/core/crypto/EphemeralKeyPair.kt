package com.aptos.core.crypto

import com.aptos.core.types.HexString
import java.security.SecureRandom

/**
 * An ephemeral Ed25519 key pair used for keyless (OIDC) account operations.
 *
 * Contains the underlying Ed25519 key pair, an expiration timestamp, a nonce,
 * and a random blinder used in the OIDC flow.
 *
 * @property privateKey the ephemeral Ed25519 private key
 * @property publicKey the ephemeral Ed25519 public key
 * @property expirationDateSecs Unix timestamp (seconds) when this key pair expires
 * @property nonce the nonce used in the OIDC authorization request
 * @property blinder 31 random bytes used for blinding
 */
class EphemeralKeyPair private constructor(
    val privateKey: Ed25519.PrivateKey,
    val publicKey: Ed25519.PublicKey,
    val expirationDateSecs: Long,
    val nonce: String,
    val blinder: ByteArray,
) {
    /** Returns true if this key pair has expired based on current system time. */
    fun isExpired(): Boolean = System.currentTimeMillis() / 1000 >= expirationDateSecs

    /** Signs the [message] with the ephemeral private key. */
    fun sign(message: ByteArray): Ed25519.Signature {
        check(!isExpired()) { "EphemeralKeyPair has expired" }
        return privateKey.sign(message)
    }

    /** Returns the public key bytes. */
    fun publicKeyBytes(): ByteArray = publicKey.data

    override fun toString(): String = "EphemeralKeyPair(nonce=$nonce, expires=$expirationDateSecs)"

    companion object {
        private const val BLINDER_LENGTH = 31

        /**
         * Generates a new ephemeral key pair with the given expiration.
         *
         * @param expirationDateSecs Unix timestamp (seconds) for expiration
         */
        @JvmStatic
        fun generate(expirationDateSecs: Long): EphemeralKeyPair {
            val privateKey = Ed25519.PrivateKey.generate()
            val publicKey = privateKey.publicKey()
            val random = SecureRandom()
            val blinder = ByteArray(BLINDER_LENGTH)
            random.nextBytes(blinder)

            // Compute nonce from public key, expiration, and blinder
            val nonceInput = publicKey.data + longToBytes(expirationDateSecs) + blinder
            val nonceHash = Hashing.sha3256(nonceInput)
            val nonce = HexString.encode(nonceHash)

            return EphemeralKeyPair(privateKey, publicKey, expirationDateSecs, nonce, blinder)
        }

        /**
         * Restores an ephemeral key pair from its components.
         */
        @JvmStatic
        fun fromPrivateKey(
            privateKey: Ed25519.PrivateKey,
            expirationDateSecs: Long,
            blinder: ByteArray,
        ): EphemeralKeyPair {
            require(blinder.size == BLINDER_LENGTH) {
                "Blinder must be $BLINDER_LENGTH bytes, got ${blinder.size}"
            }
            val publicKey = privateKey.publicKey()
            val nonceInput = publicKey.data + longToBytes(expirationDateSecs) + blinder
            val nonceHash = Hashing.sha3256(nonceInput)
            val nonce = HexString.encode(nonceHash)
            return EphemeralKeyPair(privateKey, publicKey, expirationDateSecs, nonce, blinder)
        }

        private fun longToBytes(value: Long): ByteArray {
            val bytes = ByteArray(8)
            for (i in 0 until 8) {
                bytes[7 - i] = (value shr (i * 8) and 0xFF).toByte()
            }
            return bytes
        }
    }
}
