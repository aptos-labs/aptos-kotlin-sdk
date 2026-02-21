package com.aptos.core.account

import com.aptos.core.crypto.AuthenticationKey
import com.aptos.core.crypto.Ed25519
import com.aptos.core.crypto.SignatureScheme
import com.aptos.core.types.AccountAddress

/**
 * An Aptos account backed by an Ed25519 key pair.
 *
 * Use the companion object factories to create instances:
 * - [generate] creates a new random account
 * - [fromPrivateKey] / [fromPrivateKeyHex] restores from a known key
 * - [fromMnemonic] derives from a BIP-39 mnemonic via SLIP-0010
 */
class Ed25519Account private constructor(
    val privateKey: Ed25519.PrivateKey,
    val publicKey: Ed25519.PublicKey,
    override val address: AccountAddress,
) : Account {

    override val publicKeyBytes: ByteArray get() = publicKey.data
    override val scheme: SignatureScheme get() = SignatureScheme.ED25519
    override val authenticationKey: AuthenticationKey by lazy {
        AuthenticationKey.fromEd25519(publicKey)
    }

    override fun sign(message: ByteArray): ByteArray = privateKey.sign(message).data

    /** Signs [message] and returns a typed [Ed25519.Signature] (rather than raw bytes). */
    fun signEd25519(message: ByteArray): Ed25519.Signature = privateKey.sign(message)

    companion object {
        @JvmStatic
        fun generate(): Ed25519Account {
            val privateKey = Ed25519.PrivateKey.generate()
            return fromPrivateKey(privateKey)
        }

        @JvmStatic
        fun fromPrivateKey(privateKey: Ed25519.PrivateKey): Ed25519Account {
            val publicKey = privateKey.publicKey()
            val authKey = AuthenticationKey.fromEd25519(publicKey)
            return Ed25519Account(privateKey, publicKey, authKey.derivedAddress())
        }

        @JvmStatic
        fun fromPrivateKeyHex(hex: String): Ed25519Account {
            return fromPrivateKey(Ed25519.PrivateKey.fromHex(hex))
        }

        /**
         * Derives an Ed25519 account from a BIP-39 [mnemonic] using SLIP-0010 derivation.
         *
         * @param path the derivation path (default: `m/44'/637'/0'/0'/0'`)
         */
        @JvmStatic
        fun fromMnemonic(
            mnemonic: Mnemonic,
            path: DerivationPath = DerivationPath.DEFAULT_APTOS,
        ): Ed25519Account {
            val seed = mnemonic.toSeed()
            val derivedKey = Slip0010.deriveEd25519(seed, path)
            return fromPrivateKey(Ed25519.PrivateKey(derivedKey))
        }
    }
}
