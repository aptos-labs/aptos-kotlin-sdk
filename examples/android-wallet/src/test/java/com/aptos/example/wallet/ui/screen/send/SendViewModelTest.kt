package com.aptos.example.wallet.ui.screen.send

import com.aptos.example.wallet.data.preferences.NetworkPreferences
import com.aptos.example.wallet.data.repository.TransactionRepository
import com.aptos.example.wallet.domain.model.Network
import com.aptos.example.wallet.domain.model.SendResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
class SendViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transactionRepository = mockk<TransactionRepository>()
    private val networkPreferences = mockk<NetworkPreferences>()
    private lateinit var viewModel: SendViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { networkPreferences.selectedNetwork } returns flowOf(Network.TESTNET)
        viewModel = SendViewModel(transactionRepository, networkPreferences)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateRecipient validates address format`() {
        viewModel.updateRecipient("0x1")
        viewModel.uiState.value.isAddressValid shouldBe true

        viewModel.updateRecipient("not-valid-hex-!@#\$%")
        viewModel.uiState.value.isAddressValid shouldBe false
    }

    @Test
    fun `updateRecipient with full address is valid`() {
        viewModel.updateRecipient("0x" + "a".repeat(64))
        viewModel.uiState.value.isAddressValid shouldBe true
    }

    @Test
    fun `send with invalid address shows error`() {
        viewModel.updateRecipient("not-valid-hex-!@#\$%")
        viewModel.updateAmount("1.0")
        viewModel.send()

        viewModel.uiState.value.error shouldBe "Invalid recipient address"
    }

    @Test
    fun `send with invalid amount shows error`() {
        viewModel.updateRecipient("0x1")
        viewModel.updateAmount("abc")
        viewModel.send()

        viewModel.uiState.value.error shouldBe "Invalid amount"
    }

    @Test
    fun `send with zero amount shows error`() {
        viewModel.updateRecipient("0x1")
        viewModel.updateAmount("0")
        viewModel.send()

        viewModel.uiState.value.error shouldBe "Invalid amount"
    }

    @Test
    fun `successful send returns Success result`() = runTest(testDispatcher) {
        val expectedHash = "0xhash123"
        coEvery {
            transactionRepository.sendApt("0x1", 100_000_000uL, Network.TESTNET)
        } returns SendResult.Success(expectedHash)

        viewModel.updateRecipient("0x1")
        viewModel.updateAmount("1.0")
        viewModel.send()
        advanceUntilIdle()

        val result = viewModel.uiState.value.result
        result.shouldBeInstanceOf<SendResult.Success>()
        result.hash shouldBe expectedHash
    }

    @Test
    fun `failed send returns Error result`() = runTest(testDispatcher) {
        coEvery {
            transactionRepository.sendApt("0x1", 100_000_000uL, Network.TESTNET)
        } returns SendResult.Error("Insufficient balance")

        viewModel.updateRecipient("0x1")
        viewModel.updateAmount("1.0")
        viewModel.send()
        advanceUntilIdle()

        val result = viewModel.uiState.value.result
        result.shouldBeInstanceOf<SendResult.Error>()
        result.message shouldBe "Insufficient balance"
    }

    @Test
    fun `APT to octas conversion is correct`() = runTest(testDispatcher) {
        coEvery {
            transactionRepository.sendApt("0x1", 250_000_000uL, Network.TESTNET)
        } returns SendResult.Success("hash")

        viewModel.updateRecipient("0x1")
        viewModel.updateAmount("2.5")
        viewModel.send()
        advanceUntilIdle()

        viewModel.uiState.value.result.shouldBeInstanceOf<SendResult.Success>()
    }
}
