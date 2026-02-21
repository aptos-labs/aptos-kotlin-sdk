package com.aptos.example.wallet.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aptos.example.wallet.data.preferences.NetworkPreferences
import com.aptos.example.wallet.data.repository.AccountRepository
import com.aptos.example.wallet.data.repository.TransactionRepository
import com.aptos.example.wallet.domain.model.Network
import com.aptos.example.wallet.domain.model.WalletAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val account: WalletAccount? = null,
    val network: Network = Network.TESTNET,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isFunding: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val networkPreferences: NetworkPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkPreferences.selectedNetwork.collect { network ->
                _uiState.update { it.copy(network = network) }
                loadAccount(network)
            }
        }
    }

    private suspend fun loadAccount(network: Network) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val account = accountRepository.getAccount(network)
            _uiState.update { it.copy(account = account, isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val balance = accountRepository.refreshBalance(_uiState.value.network)
                _uiState.update { state ->
                    state.copy(
                        account = state.account?.copy(balanceOctas = balance),
                        isRefreshing = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, error = e.message) }
            }
        }
    }

    fun requestFaucet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFunding = true, error = null) }
            try {
                transactionRepository.fundFromFaucet(_uiState.value.network)
                val balance = accountRepository.refreshBalance(_uiState.value.network)
                _uiState.update { state ->
                    state.copy(
                        account = state.account?.copy(balanceOctas = balance),
                        isFunding = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFunding = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
