package com.aptos.core.account

import com.aptos.core.crypto.AuthenticationKey
import com.aptos.core.crypto.Secp256k1
import com.aptos.core.crypto.SignatureScheme
import com.aptos.core.types.AccountAddress

/**
 * An Aptos account backed by a Secp256k1 key pair.
 */
class Secp256k1Account private constructor(
    val privateKey: Secp256k1.PrivateKey,
    val publicKey: Secp256k1.PublicKey,
    override val address: AccountAddress,
) : Account {

    override val publicKeyBytes: ByteArray get() = publicKey.data
    override val scheme: SignatureScheme get() = SignatureScheme.SECP256K1
    override val authenticationKey: AuthenticationKey by lazy {
        AuthenticationKey.fromSecp256k1(publicKey)
    }

    override fun sign(message: ByteArray): ByteArray = privateKey.sign(message).data

    fun signSecp256k1(message: ByteArray): Secp256k1.Signature = privateKey.sign(message)

    companion object {
        @JvmStatic
        fun generate(): Secp256k1Account {
            val privateKey = Secp256k1.PrivateKey.generate()
            return fromPrivateKey(privateKey)
        }

        @JvmStatic
        fun fromPrivateKey(privateKey: Secp256k1.PrivateKey): Secp256k1Account {
            val publicKey = privateKey.publicKey()
            val authKey = AuthenticationKey.fromSecp256k1(publicKey)
            return Secp256k1Account(privateKey, publicKey, authKey.derivedAddress())
        }

        @JvmStatic
        fun fromPrivateKeyHex(hex: String): Secp256k1Account {
            return fromPrivateKey(Secp256k1.PrivateKey.fromHex(hex))
        }

        @JvmStatic
        fun fromMnemonic(
            mnemonic: Mnemonic,
            path: DerivationPath = DerivationPath.DEFAULT_APTOS,
        ): Secp256k1Account {
            val seed = mnemonic.toSeed()
            val derivedKey = Bip32.deriveSecp256k1(seed, path)
            return fromPrivateKey(Secp256k1.PrivateKey(derivedKey))
        }
    }
}
