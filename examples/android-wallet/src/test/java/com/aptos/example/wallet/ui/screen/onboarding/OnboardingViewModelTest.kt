package com.aptos.example.wallet.ui.screen.onboarding

import com.aptos.example.wallet.data.repository.AccountRepository
import com.aptos.example.wallet.domain.model.WalletAccount
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val accountRepository = mockk<AccountRepository>()
    private lateinit var viewModel: OnboardingViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OnboardingViewModel(accountRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is CHOICE mode`() {
        viewModel.uiState.value.mode shouldBe OnboardingMode.CHOICE
    }

    @Test
    fun `selectCreate creates account and shows mnemonic`() = runTest(testDispatcher) {
        val wallet = WalletAccount("0xabc", 0uL)
        coEvery { accountRepository.createAccount() } returns wallet
        every { accountRepository.getMnemonicPhrase() } returns
            "word1 word2 word3 word4 word5 word6 word7 word8 word9 word10 word11 word12"

        viewModel.selectCreate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        state.mode shouldBe OnboardingMode.CREATE
        state.accountCreated shouldBe true
        state.generatedMnemonic shouldBe "word1 word2 word3 word4 word5 word6 word7 word8 word9 word10 word11 word12"
        state.isLoading shouldBe false
    }

    @Test
    fun `selectImport transitions to IMPORT mode`() {
        viewModel.selectImport()

        viewModel.uiState.value.mode shouldBe OnboardingMode.IMPORT
    }

    @Test
    fun `confirmImport with valid phrase creates account`() = runTest(testDispatcher) {
        val wallet = WalletAccount("0xdef", 0uL)
        coEvery { accountRepository.importAccount(any()) } returns wallet

        viewModel.selectImport()
        viewModel.updateMnemonicInput("word1 word2 word3 word4 word5 word6 word7 word8 word9 word10 word11 word12")
        viewModel.confirmImport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        state.accountCreated shouldBe true
        state.isLoading shouldBe false
    }

    @Test
    fun `confirmImport with invalid phrase shows error`() = runTest(testDispatcher) {
        coEvery { accountRepository.importAccount(any()) } throws IllegalArgumentException("Invalid mnemonic")

        viewModel.selectImport()
        viewModel.updateMnemonicInput("bad phrase")
        viewModel.confirmImport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        state.error shouldBe "Invalid mnemonic"
        state.accountCreated shouldBe false
    }

    @Test
    fun `confirmImport with blank phrase shows error`() {
        viewModel.selectImport()
        viewModel.updateMnemonicInput("   ")
        viewModel.confirmImport()

        viewModel.uiState.value.error shouldBe "Please enter a mnemonic phrase"
    }

    @Test
    fun `goBack resets state`() {
        viewModel.selectImport()
        viewModel.updateMnemonicInput("some input")
        viewModel.goBack()

        viewModel.uiState.value shouldBe OnboardingUiState()
    }
}
