package com.aptos.example.wallet.data.repository

import com.aptos.core.account.Ed25519Account
import com.aptos.core.account.Mnemonic
import com.aptos.example.wallet.data.storage.SecureStorage
import com.aptos.example.wallet.domain.model.Network
import com.aptos.example.wallet.domain.model.WalletAccount
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val secureStorage: SecureStorage,
    private val aptosProvider: AptosProvider,
) : AccountRepository {

    override fun hasAccount(): Boolean = secureStorage.hasAccount()

    override suspend fun createAccount(): WalletAccount {
        val mnemonic = Mnemonic.generate(12)
        val account = Ed25519Account.fromMnemonic(mnemonic)
        secureStorage.saveMnemonic(mnemonic.phrase())
        secureStorage.savePrivateKeyHex(account.privateKey.toHex())
        return WalletAccount(
            address = account.address.toHex(),
            balanceOctas = 0uL,
        )
    }

    override suspend fun importAccount(phrase: String): WalletAccount {
        val mnemonic = Mnemonic.fromPhrase(phrase.trim())
        val account = Ed25519Account.fromMnemonic(mnemonic)
        secureStorage.saveMnemonic(mnemonic.phrase())
        secureStorage.savePrivateKeyHex(account.privateKey.toHex())
        return WalletAccount(
            address = account.address.toHex(),
            balanceOctas = 0uL,
        )
    }

    override suspend fun getAccount(network: Network): WalletAccount {
        val hex = requireNotNull(secureStorage.getPrivateKeyHex()) { "No account found" }
        val account = Ed25519Account.fromPrivateKeyHex(hex)
        val aptos = aptosProvider.get(network)
        val balance = try {
            aptos.getBalance(account.address)
        } catch (_: Exception) {
            0uL
        }
        return WalletAccount(
            address = account.address.toHex(),
            balanceOctas = balance,
        )
    }

    override suspend fun refreshBalance(network: Network): ULong {
        val hex = requireNotNull(secureStorage.getPrivateKeyHex()) { "No account found" }
        val account = Ed25519Account.fromPrivateKeyHex(hex)
        val aptos = aptosProvider.get(network)
        return aptos.getBalance(account.address)
    }

    override fun getMnemonicPhrase(): String? = secureStorage.getMnemonic()

    override fun deleteAccount() {
        secureStorage.clear()
    }
}
