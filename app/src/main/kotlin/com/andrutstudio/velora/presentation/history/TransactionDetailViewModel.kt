package com.andrutstudio.velora.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.andrutstudio.velora.domain.model.Transaction
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import com.andrutstudio.velora.domain.repository.WalletRepository
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val blockchainRepository: BlockchainRepository,
    private val walletRepository: WalletRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    data class State(
        val transaction: Transaction? = null,
        val isLoading: Boolean = true,
        val error: String? = null,
        val myAddresses: Set<String> = emptySet(),
        val dateDisplayMode: DateDisplayMode = DateDisplayMode.LOCAL,
    ) {
        val isIncoming: Boolean
            get() = transaction?.direction == 1 || (transaction?.to != null &&
                myAddresses.contains(transaction.to) &&
                !myAddresses.contains(transaction.from))
    }

    enum class DateDisplayMode { LOCAL, UTC, UNIX }

    private val txId: String = savedStateHandle.get<String>("txId") ?: ""

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        loadAddresses()
        loadTransaction()
    }

    private fun loadAddresses() {
        viewModelScope.launch {
            walletRepository.observeWallet().collect { wallet ->
                val addresses = wallet?.accounts?.map { it.address }?.toSet() ?: emptySet()
                _state.update { it.copy(myAddresses = addresses) }
            }
        }
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            runCatching { blockchainRepository.getTransaction(txId) }
                .onSuccess { tx -> _state.update { it.copy(transaction = tx, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun onToggleDateMode() {
        _state.update { 
            val nextMode = when (it.dateDisplayMode) {
                DateDisplayMode.LOCAL -> DateDisplayMode.UTC
                DateDisplayMode.UTC -> DateDisplayMode.UNIX
                DateDisplayMode.UNIX -> DateDisplayMode.LOCAL
            }
            it.copy(dateDisplayMode = nextMode)
        }
    }
}
