package com.aptos.sdk.integration

import com.aptos.core.account.Ed25519Account
import com.aptos.sdk.Aptos
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
class TransferIntegrationTest {

    @Test
    fun `fund transfer and verify on testnet`() = runTest {
        val aptos = Aptos.testnet()
        try {
            val sender = Ed25519Account.generate()
            val receiver = Ed25519Account.generate()

            // Fund sender
            aptos.fundAccount(sender.address, 100_000_000uL)

            // Transfer
            val transferAmount = 1_000_000uL
            val signedTxn = aptos.transfer(sender, receiver.address, transferAmount)
            val pending = aptos.submitTransaction(signedTxn)

            // Wait for confirmation
            val confirmed = aptos.waitForTransaction(pending.hash)
            confirmed.success shouldBe true

            // Verify receiver balance
            val receiverBalance = aptos.getBalance(receiver.address)
            receiverBalance shouldBe transferAmount
        } finally {
            aptos.close()
        }
    }

    @Test
    fun `get account transactions after transfer`() = runTest {
        val aptos = Aptos.testnet()
        try {
            val sender = Ed25519Account.generate()
            val receiver = Ed25519Account.generate()

            aptos.fundAccount(sender.address, 100_000_000uL)
            val signedTxn = aptos.transfer(sender, receiver.address, 1_000_000uL)
            val pending = aptos.submitTransaction(signedTxn)
            aptos.waitForTransaction(pending.hash)

            val txns = aptos.getAccountTransactions(sender.address)
            txns.isNotEmpty() shouldBe true
        } finally {
            aptos.close()
        }
    }
}
