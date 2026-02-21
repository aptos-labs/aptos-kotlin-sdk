package com.aptos.example.wallet.data.repository

import com.aptos.example.wallet.domain.model.Network
import com.aptos.example.wallet.domain.model.SendResult
import com.aptos.example.wallet.domain.model.TransactionItem

interface TransactionRepository {
    suspend fun sendApt(toAddress: String, amountOctas: ULong, network: Network): SendResult
    suspend fun getTransactions(network: Network, limit: Int = 25): List<TransactionItem>
    suspend fun fundFromFaucet(network: Network)
}
