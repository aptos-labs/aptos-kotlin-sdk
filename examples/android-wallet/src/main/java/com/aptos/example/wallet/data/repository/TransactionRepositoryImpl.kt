package com.aptos.example.wallet.data.repository

import com.aptos.core.account.Ed25519Account
import com.aptos.core.types.AccountAddress
import com.aptos.example.wallet.data.storage.SecureStorage
import com.aptos.example.wallet.domain.model.Network
import com.aptos.example.wallet.domain.model.SendResult
import com.aptos.example.wallet.domain.model.TransactionItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val secureStorage: SecureStorage,
    private val aptosProvider: AptosProvider,
) : TransactionRepository {

    override suspend fun sendApt(toAddress: String, amountOctas: ULong, network: Network): SendResult = try {
        val hex = requireNotNull(secureStorage.getPrivateKeyHex()) { "No account found" }
        val sender = Ed25519Account.fromPrivateKeyHex(hex)
        val to = AccountAddress.fromHexRelaxed(toAddress)
        val aptos = aptosProvider.get(network)
        val signedTxn = aptos.transfer(sender, to, amountOctas)
        val pending = aptos.submitTransaction(signedTxn)
        aptos.waitForTransaction(pending.hash)
        SendResult.Success(pending.hash)
    } catch (e: Exception) {
        SendResult.Error(e.message ?: "Unknown error")
    }

    override suspend fun getTransactions(network: Network, limit: Int): List<TransactionItem> {
        val hex = requireNotNull(secureStorage.getPrivateKeyHex()) { "No account found" }
        val account = Ed25519Account.fromPrivateKeyHex(hex)
        val aptos = aptosProvider.get(network)
        val responses = aptos.getAccountTransactions(account.address, limit = limit)
        return responses.map { txn ->
            TransactionItem(
                hash = txn.hash,
                type = txn.type,
                success = txn.success ?: false,
                timestamp = txn.timestamp,
                gasUsed = txn.gasUsed,
                version = txn.version,
            )
        }
    }

    override suspend fun fundFromFaucet(network: Network) {
        val hex = requireNotNull(secureStorage.getPrivateKeyHex()) { "No account found" }
        val account = Ed25519Account.fromPrivateKeyHex(hex)
        val aptos = aptosProvider.get(network)
        aptos.fundAccount(account.address)
    }
}
