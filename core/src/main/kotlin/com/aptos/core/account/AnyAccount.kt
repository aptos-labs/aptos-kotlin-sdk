package com.aptos.core.account

import com.aptos.core.crypto.AuthenticationKey
import com.aptos.core.crypto.SignatureScheme
import com.aptos.core.types.AccountAddress

/**
 * Sealed class wrapper for runtime polymorphism across account types.
 */
sealed class AnyAccount : Account {
    data class Ed25519(val account: Ed25519Account) : AnyAccount() {
        override val address: AccountAddress get() = account.address
        override val publicKeyBytes: ByteArray get() = account.publicKeyBytes
        override val scheme: SignatureScheme get() = account.scheme
        override val authenticationKey: AuthenticationKey get() = account.authenticationKey

        override fun sign(message: ByteArray): ByteArray = account.sign(message)
    }

    data class Secp256k1(val account: Secp256k1Account) : AnyAccount() {
        override val address: AccountAddress get() = account.address
        override val publicKeyBytes: ByteArray get() = account.publicKeyBytes
        override val scheme: SignatureScheme get() = account.scheme
        override val authenticationKey: AuthenticationKey get() = account.authenticationKey

        override fun sign(message: ByteArray): ByteArray = account.sign(message)
    }

    data class MultiEd25519(val account: MultiEd25519Account) : AnyAccount() {
        override val address: AccountAddress get() = account.address
        override val publicKeyBytes: ByteArray get() = account.publicKeyBytes
        override val scheme: SignatureScheme get() = account.scheme
        override val authenticationKey: AuthenticationKey get() = account.authenticationKey

        override fun sign(message: ByteArray): ByteArray = account.sign(message)
    }

    data class Keyless(val account: KeylessAccount) : AnyAccount() {
        override val address: AccountAddress get() = account.address
        override val publicKeyBytes: ByteArray get() = account.publicKeyBytes
        override val scheme: SignatureScheme get() = account.scheme
        override val authenticationKey: AuthenticationKey get() = account.authenticationKey

        override fun sign(message: ByteArray): ByteArray = account.sign(message)
    }

    companion object {
        @JvmStatic
        fun from(account: Account): AnyAccount = when (account) {
            is Ed25519Account -> Ed25519(account)
            is Secp256k1Account -> Secp256k1(account)
            is MultiEd25519Account -> MultiEd25519(account)
            is KeylessAccount -> Keyless(account)
            is AnyAccount -> account
            else -> throw IllegalArgumentException("Unsupported account type: ${account::class}")
        }
    }
}
