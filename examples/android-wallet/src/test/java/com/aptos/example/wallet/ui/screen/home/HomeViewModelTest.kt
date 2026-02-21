package com.aptos.example.wallet.ui.screen.home

import com.aptos.example.wallet.data.preferences.NetworkPreferences
import com.aptos.example.wallet.data.repository.AccountRepository
import com.aptos.example.wallet.data.repository.TransactionRepository
import com.aptos.example.wallet.domain.model.Network
import com.aptos.example.wallet.domain.model.WalletAccount
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val accountRepository = mockk<AccountRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val networkPreferences = mockk<NetworkPreferences>()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads account for selected network`() = runTest(testDispatcher) {
        val wallet = WalletAccount("0xabc", 100_000_000uL)
        every { networkPreferences.selectedNetwork } returns flowOf(Network.TESTNET)
        coEvery { accountRepository.getAccount(Network.TESTNET) } returns wallet

        val viewModel = HomeViewModel(accountRepository, transactionRepository, networkPreferences)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        state.account shouldBe wallet
        state.network shouldBe Network.TESTNET
        state.isLoading shouldBe false
    }

    @Test
    fun `refresh updates balance`() = runTest(testDispatcher) {
        val wallet = WalletAccount("0xabc", 100_000_000uL)
        every { networkPreferences.selectedNetwork } returns flowOf(Network.TESTNET)
        coEvery { accountRepository.getAccount(Network.TESTNET) } returns wallet
        coEvery { accountRepository.refreshBalance(Network.TESTNET) } returns 200_000_000uL

        val viewModel = HomeViewModel(accountRepository, transactionRepository, networkPreferences)
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.uiState.value.account?.balanceOctas shouldBe 200_000_000uL
    }

    @Test
    fun `requestFaucet funds account and refreshes balance`() = runTest(testDispatcher) {
        val wallet = WalletAccount("0xabc", 0uL)
        every { networkPreferences.selectedNetwork } returns flowOf(Network.TESTNET)
        coEvery { accountRepository.getAccount(Network.TESTNET) } returns wallet
        coEvery { transactionRepository.fundFromFaucet(Network.TESTNET) } returns Unit
        coEvery { accountRepository.refreshBalance(Network.TESTNET) } returns 100_000_000uL

        val viewModel = HomeViewModel(accountRepository, transactionRepository, networkPreferences)
        advanceUntilIdle()
        viewModel.requestFaucet()
        advanceUntilIdle()

        viewModel.uiState.value.account?.balanceOctas shouldBe 100_000_000uL
        viewModel.uiState.value.isFunding shouldBe false
    }

    @Test
    fun `error during load is captured`() = runTest(testDispatcher) {
        every { networkPreferences.selectedNetwork } returns flowOf(Network.TESTNET)
        coEvery { accountRepository.getAccount(Network.TESTNET) } throws RuntimeException("Network error")

        val viewModel = HomeViewModel(accountRepository, transactionRepository, networkPreferences)
        advanceUntilIdle()

        viewModel.uiState.value.error shouldBe "Network error"
    }
}
