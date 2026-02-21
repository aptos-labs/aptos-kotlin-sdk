package com.aptos.example.wallet.ui.screen.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aptos.core.types.AccountAddress
import com.aptos.example.wallet.data.preferences.NetworkPreferences
import com.aptos.example.wallet.data.repository.TransactionRepository
import com.aptos.example.wallet.domain.model.SendResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SendUiState(
    val recipientAddress: String = "",
    val amountApt: String = "",
    val isAddressValid: Boolean = false,
    val isSending: Boolean = false,
    val result: SendResult? = null,
    val error: String? = null,
)

@HiltViewModel
class SendViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val networkPreferences: NetworkPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    fun updateRecipient(address: String) {
        val valid = try {
            AccountAddress.fromHexRelaxed(address)
            address.isNotBlank()
        } catch (_: Exception) {
            false
        }
        _uiState.update { it.copy(recipientAddress = address, isAddressValid = valid, error = null) }
    }

    fun updateAmount(amount: String) {
        _uiState.update { it.copy(amountApt = amount, error = null) }
    }

    fun send() {
        val state = _uiState.value
        if (!state.isAddressValid) {
            _uiState.update { it.copy(error = "Invalid recipient address") }
            return
        }

        val aptAmount = state.amountApt.toDoubleOrNull()
        if (aptAmount == null || aptAmount <= 0) {
            _uiState.update { it.copy(error = "Invalid amount") }
            return
        }

        val amountOctas = (aptAmount * 100_000_000).toULong()

        _uiState.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            val network = networkPreferences.selectedNetwork.first()
            val result = transactionRepository.sendApt(state.recipientAddress, amountOctas, network)
            _uiState.update { it.copy(isSending = false, result = result) }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
