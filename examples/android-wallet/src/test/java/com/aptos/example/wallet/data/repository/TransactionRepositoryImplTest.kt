package com.aptos.example.wallet.data.repository

import com.aptos.client.rest.TransactionResponse
import com.aptos.core.transaction.SignedTransaction
import com.aptos.example.wallet.data.storage.SecureStorage
import com.aptos.example.wallet.domain.model.Network
import com.aptos.example.wallet.domain.model.SendResult
import com.aptos.sdk.Aptos
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionRepositoryImplTest {

    private val secureStorage = mockk<SecureStorage>()
    private val aptosProvider = mockk<AptosProvider>()
    private val mockAptos = mockk<Aptos>()
    private lateinit var repository: TransactionRepositoryImpl

    private val testPrivateKeyHex = "0x" + "ab".repeat(32)

    @BeforeEach
    fun setup() {
        repository = TransactionRepositoryImpl(secureStorage, aptosProvider)
        every { aptosProvider.get(any()) } returns mockAptos
        every { secureStorage.getPrivateKeyHex() } returns testPrivateKeyHex
    }

    @Test
    fun `sendApt success returns hash`() = runTest {
        val signedTxn = mockk<SignedTransaction>()
        coEvery { mockAptos.transfer(any(), any(), any(), any(), any()) } returns signedTxn
        coEvery { mockAptos.submitTransaction(signedTxn) } returns mockk {
            every { hash } returns "0xtxhash"
        }
        coEvery { mockAptos.waitForTransaction("0xtxhash", any(), any()) } returns mockk()

        val result = repository.sendApt("0x1", 100_000_000uL, Network.TESTNET)

        result.shouldBeInstanceOf<SendResult.Success>()
        result.hash shouldBe "0xtxhash"
    }

    @Test
    fun `sendApt failure returns error`() = runTest {
        coEvery { mockAptos.transfer(any(), any(), any(), any(), any()) } throws RuntimeException("Insufficient funds")

        val result = repository.sendApt("0x1", 100_000_000uL, Network.TESTNET)

        result.shouldBeInstanceOf<SendResult.Error>()
        result.message shouldBe "Insufficient funds"
    }

    @Test
    fun `getTransactions maps responses to domain models`() = runTest {
        val responses = listOf(
            TransactionResponse(
                hash = "0xhash1",
                type = "user_transaction",
                success = true,
                timestamp = "1234567890",
                gasUsed = "100",
                version = "42",
            ),
        )
        coEvery { mockAptos.getAccountTransactions(any(), limit = 25) } returns responses

        val result = repository.getTransactions(Network.TESTNET)

        result shouldHaveSize 1
        result[0].hash shouldBe "0xhash1"
        result[0].success shouldBe true
        result[0].gasUsed shouldBe "100"
    }

    @Test
    fun `fundFromFaucet calls SDK`() = runTest {
        coEvery { mockAptos.fundAccount(any(), any()) } returns Unit

        repository.fundFromFaucet(Network.TESTNET)

        coVerify { mockAptos.fundAccount(any()) }
    }
}
