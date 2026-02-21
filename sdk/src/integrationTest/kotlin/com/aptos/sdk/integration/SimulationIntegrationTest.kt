package com.aptos.sdk.integration

import com.aptos.core.account.Ed25519Account
import com.aptos.sdk.Aptos
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
class SimulationIntegrationTest {

    @Test
    fun `simulate transaction on testnet`() = runTest {
        val aptos = Aptos.testnet()
        try {
            val sender = Ed25519Account.generate()
            val receiver = Ed25519Account.generate()

            aptos.fundAccount(sender.address, 100_000_000uL)

            val signedTxn = aptos.transfer(sender, receiver.address, 1_000_000uL)
            val results = aptos.simulateTransaction(signedTxn)

            results.isNotEmpty() shouldBe true
            results[0].gasUsed shouldNotBe null
        } finally {
            aptos.close()
        }
    }
}
