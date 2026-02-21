package com.aptos.example.wallet.data.repository

import com.aptos.example.wallet.domain.model.Network
import com.aptos.example.wallet.domain.model.WalletAccount

interface AccountRepository {
    fun hasAccount(): Boolean
    suspend fun createAccount(): WalletAccount
    suspend fun importAccount(phrase: String): WalletAccount
    suspend fun getAccount(network: Network): WalletAccount
    suspend fun refreshBalance(network: Network): ULong
    fun getMnemonicPhrase(): String?
    fun deleteAccount()
}
