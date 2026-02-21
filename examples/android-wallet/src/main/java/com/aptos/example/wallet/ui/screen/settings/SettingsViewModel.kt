package com.aptos.example.wallet.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aptos.example.wallet.data.preferences.NetworkPreferences
import com.aptos.example.wallet.data.repository.AccountRepository
import com.aptos.example.wallet.data.repository.AptosProvider
import com.aptos.example.wallet.domain.model.Network
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val network: Network = Network.TESTNET,
    val showMnemonic: Boolean = false,
    val mnemonic: String? = null,
    val showDeleteConfirm: Boolean = false,
    val walletDeleted: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val networkPreferences: NetworkPreferences,
    private val accountRepository: AccountRepository,
    private val aptosProvider: AptosProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkPreferences.selectedNetwork.collect { network ->
                _uiState.update { it.copy(network = network) }
            }
        }
    }

    fun setNetwork(network: Network) {
        viewModelScope.launch {
            networkPreferences.setNetwork(network)
            aptosProvider.invalidate()
        }
    }

    fun showMnemonic() {
        val phrase = accountRepository.getMnemonicPhrase()
        _uiState.update { it.copy(showMnemonic = true, mnemonic = phrase) }
    }

    fun hideMnemonic() {
        _uiState.update { it.copy(showMnemonic = false, mnemonic = null) }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun deleteWallet() {
        accountRepository.deleteAccount()
        aptosProvider.invalidate()
        _uiState.update { it.copy(showDeleteConfirm = false, walletDeleted = true) }
    }
}
