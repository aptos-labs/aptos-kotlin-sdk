package com.aptos.example.wallet.ui.screen.transactions

import com.aptos.example.wallet.data.preferences.NetworkPreferences
import com.aptos.example.wallet.data.repository.TransactionRepository
import com.aptos.example.wallet.domain.model.Network
import com.aptos.example.wallet.domain.model.TransactionItem
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transactionRepository = mockk<TransactionRepository>()
    private val networkPreferences = mockk<NetworkPreferences>()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { networkPreferences.selectedNetwork } returns flowOf(Network.TESTNET)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads transactions`() = runTest(testDispatcher) {
        val txns = listOf(
            TransactionItem("0xhash1", "user_transaction", true, "1234567890", "100", "1"),
            TransactionItem("0xhash2", "user_transaction", false, "1234567891", "200", "2"),
        )
        coEvery { transactionRepository.getTransactions(Network.TESTNET) } returns txns

        val viewModel = TransactionsViewModel(transactionRepository, networkPreferences)
        advanceUntilIdle()

        viewModel.uiState.value.transactions shouldHaveSize 2
        viewModel.uiState.value.isLoading shouldBe false
    }

    @Test
    fun `empty transaction list`() = runTest(testDispatcher) {
        coEvery { transactionRepository.getTransactions(Network.TESTNET) } returns emptyList()

        val viewModel = TransactionsViewModel(transactionRepository, networkPreferences)
        advanceUntilIdle()

        viewModel.uiState.value.transactions.shouldBeEmpty()
    }

    @Test
    fun `error during load is captured`() = runTest(testDispatcher) {
        coEvery { transactionRepository.getTransactions(Network.TESTNET) } throws RuntimeException("Network error")

        val viewModel = TransactionsViewModel(transactionRepository, networkPreferences)
        advanceUntilIdle()

        viewModel.uiState.value.error shouldBe "Network error"
    }

    @Test
    fun `refresh reloads transactions`() = runTest(testDispatcher) {
        coEvery { transactionRepository.getTransactions(Network.TESTNET) } returns emptyList()

        val viewModel = TransactionsViewModel(transactionRepository, networkPreferences)
        advanceUntilIdle()

        val txns = listOf(
            TransactionItem("0xhash1", "user_transaction", true, null, null, null),
        )
        coEvery { transactionRepository.getTransactions(Network.TESTNET) } returns txns

        viewModel.refresh()
        advanceUntilIdle()

        viewModel.uiState.value.transactions shouldHaveSize 1
        viewModel.uiState.value.isRefreshing shouldBe false
    }
}
