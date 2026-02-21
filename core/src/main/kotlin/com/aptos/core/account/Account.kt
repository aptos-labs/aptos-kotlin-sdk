package com.aptos.core.account

import com.aptos.core.crypto.AuthenticationKey
import com.aptos.core.crypto.SignatureScheme
import com.aptos.core.types.AccountAddress

/**
 * Interface representing an Aptos account that can sign transactions.
 */
interface Account {
    val address: AccountAddress
    val publicKeyBytes: ByteArray
    val scheme: SignatureScheme
    val authenticationKey: AuthenticationKey

    fun sign(message: ByteArray): ByteArray

    fun signTransaction(rawTxnBytes: ByteArray): ByteArray = sign(rawTxnBytes)
}
