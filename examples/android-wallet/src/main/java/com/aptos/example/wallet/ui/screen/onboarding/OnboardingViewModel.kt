package com.aptos.example.wallet.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aptos.example.wallet.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingMode { CHOICE, CREATE, IMPORT }

data class OnboardingUiState(
    val mode: OnboardingMode = OnboardingMode.CHOICE,
    val generatedMnemonic: String? = null,
    val mnemonicInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val accountCreated: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val accountRepository: AccountRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectCreate() {
        _uiState.update { it.copy(mode = OnboardingMode.CREATE, isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val account = accountRepository.createAccount()
                val mnemonic = accountRepository.getMnemonicPhrase()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generatedMnemonic = mnemonic,
                        accountCreated = true,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectImport() {
        _uiState.update { it.copy(mode = OnboardingMode.IMPORT, error = null) }
    }

    fun updateMnemonicInput(input: String) {
        _uiState.update { it.copy(mnemonicInput = input, error = null) }
    }

    fun confirmImport() {
        val phrase = _uiState.value.mnemonicInput.trim()
        if (phrase.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a mnemonic phrase") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                accountRepository.importAccount(phrase)
                _uiState.update { it.copy(isLoading = false, accountCreated = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun goBack() {
        _uiState.update { OnboardingUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
