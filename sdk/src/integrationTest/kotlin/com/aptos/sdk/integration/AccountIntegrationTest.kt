package com.aptos.sdk.integration

import com.aptos.core.account.Ed25519Account
import com.aptos.sdk.Aptos
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Integration tests that run against the Aptos testnet.
 * These tests require network access and are NOT run in CI.
 *
 * Run: `./gradlew :sdk:integrationTest`
 */
@Tag("integration")
class AccountIntegrationTest {

    @Test
    fun `create and fund account on testnet`() = runTest {
        val aptos = Aptos.testnet()
        try {
            val account = Ed25519Account.generate()

            // Fund the account
            aptos.fundAccount(account.address, 100_000_000uL)

            // Verify balance
            val balance = aptos.getBalance(account.address)
            balance shouldNotBe 0uL

            // Verify account info
            val accountInfo = aptos.getAccount(account.address)
            accountInfo.sequenceNumber shouldBe "0"
        } finally {
            aptos.close()
        }
    }

    @Test
    fun `get ledger info from testnet`() = runTest {
        val aptos = Aptos.testnet()
        try {
            val info = aptos.getLedgerInfo()
            info.chainId shouldBe 2
            info.nodeRole shouldNotBe null
        } finally {
            aptos.close()
        }
    }
}
