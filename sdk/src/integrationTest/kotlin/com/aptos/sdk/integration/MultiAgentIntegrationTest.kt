package com.aptos.sdk.integration

import com.aptos.core.account.Ed25519Account
import com.aptos.core.transaction.TransactionBuilder
import com.aptos.core.transaction.TransactionPayload
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId
import com.aptos.sdk.Aptos
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
class MultiAgentIntegrationTest {

    @Test
    fun `build and sign multi-agent transaction`() = runTest {
        val aptos = Aptos.testnet()
        try {
            val sender = Ed25519Account.generate()
            val secondary = Ed25519Account.generate()

            // Fund accounts
            aptos.fundAccount(sender.address, 100_000_000uL)
            aptos.fundAccount(secondary.address, 100_000_000uL)

            // Build multi-agent transaction
            val accountInfo = aptos.getAccount(sender.address)
            val signedTxn = TransactionBuilder.builder()
                .sender(sender.address)
                .sequenceNumber(accountInfo.sequenceNumber.toULong())
                .payload(TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1000uL))
                .chainId(ChainId.TESTNET)
                .secondarySigners(listOf(secondary))
                .sign(sender)

            signedTxn.authenticator.shouldBeInstanceOf<com.aptos.core.transaction.TransactionAuthenticator.MultiAgent>()
        } finally {
            aptos.close()
        }
    }
}
