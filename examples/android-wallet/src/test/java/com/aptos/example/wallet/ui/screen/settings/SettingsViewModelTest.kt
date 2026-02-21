package com.aptos.example.wallet.ui.screen.settings

import com.aptos.example.wallet.data.preferences.NetworkPreferences
import com.aptos.example.wallet.data.repository.AccountRepository
import com.aptos.example.wallet.data.repository.AptosProvider
import com.aptos.example.wallet.domain.model.Network
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val networkPreferences = mockk<NetworkPreferences>()
    private val accountRepository = mockk<AccountRepository>()
    private val aptosProvider = mockk<AptosProvider>()
    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { networkPreferences.selectedNetwork } returns flowOf(Network.TESTNET)
        justRun { aptosProvider.invalidate() }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(networkPreferences, accountRepository, aptosProvider)

    @Test
    fun `init observes network`() = runTest(testDispatcher) {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.value.network shouldBe Network.TESTNET
    }

    @Test
    fun `setNetwork updates preferences and invalidates provider`() = runTest(testDispatcher) {
        coEvery { networkPreferences.setNetwork(Network.DEVNET) } returns Unit
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setNetwork(Network.DEVNET)
        advanceUntilIdle()

        coVerify { networkPreferences.setNetwork(Network.DEVNET) }
        verify { aptosProvider.invalidate() }
    }

    @Test
    fun `showMnemonic loads phrase`() = runTest(testDispatcher) {
        every { accountRepository.getMnemonicPhrase() } returns "word1 word2 word3"
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showMnemonic()

        viewModel.uiState.value.showMnemonic shouldBe true
        viewModel.uiState.value.mnemonic shouldBe "word1 word2 word3"
    }

    @Test
    fun `hideMnemonic clears state`() = runTest(testDispatcher) {
        every { accountRepository.getMnemonicPhrase() } returns "words"
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showMnemonic()
        viewModel.hideMnemonic()

        viewModel.uiState.value.showMnemonic shouldBe false
        viewModel.uiState.value.mnemonic shouldBe null
    }

    @Test
    fun `deleteWallet clears account and invalidates provider`() = runTest(testDispatcher) {
        justRun { accountRepository.deleteAccount() }
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showDeleteConfirmation()
        viewModel.deleteWallet()

        viewModel.uiState.value.walletDeleted shouldBe true
        verify { accountRepository.deleteAccount() }
        verify { aptosProvider.invalidate() }
    }
}
