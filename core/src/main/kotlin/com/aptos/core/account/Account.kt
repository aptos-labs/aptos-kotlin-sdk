package com.aptos.core.account

import com.aptos.core.crypto.AuthenticationKey
import com.aptos.core.crypto.SignatureScheme
import com.aptos.core.types.AccountAddress

/**
 * Interface representing an Aptos account that can sign transactions.
 *
 * Implemented by [com.aptos.core.account.Ed25519Account] and
 * [com.aptos.core.account.Secp256k1Account].
 */
interface Account {
    /** The on-chain address derived from this account's authentication key. */
    val address: AccountAddress

    /** The raw public key bytes used for signature verification. */
    val publicKeyBytes: ByteArray

    /** The signature scheme (Ed25519 or Secp256k1). */
    val scheme: SignatureScheme

    /** The authentication key derived from the public key and scheme. */
    val authenticationKey: AuthenticationKey

    /** Signs the given [message] bytes and returns the raw signature bytes. */
    fun sign(message: ByteArray): ByteArray

    /** Signs raw transaction bytes (defaults to [sign]). */
    fun signTransaction(rawTxnBytes: ByteArray): ByteArray = sign(rawTxnBytes)
}
