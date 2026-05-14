package com.andrutstudio.velora.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.andrutstudio.velora.data.rpc.PactusScanWebSocketManager
import com.andrutstudio.velora.domain.model.Transaction
import com.andrutstudio.velora.domain.model.TransactionType
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import com.andrutstudio.velora.domain.repository.WalletRepository
import javax.inject.Inject

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val blockchainRepository: BlockchainRepository,
    private val webSocketManager: PactusScanWebSocketManager,
) : ViewModel() {

    enum class Filter { ALL, TRANSFER, BOND }

    data class State(
        val transactions: List<Transaction> = emptyList(),
        val filter: Filter = Filter.ALL,
        val selectedAddressFilter: String? = null,
        val isLoading: Boolean = true,
        val wallet: Wallet? = null,
    ) {
        val filtered: List<Transaction>
            get() {
                val typeFiltered = when (filter) {
                    Filter.ALL -> transactions
                    Filter.TRANSFER -> transactions.filter { it.type == TransactionType.TRANSFER }
                    Filter.BOND -> transactions.filter {
                        it.type == TransactionType.BOND || it.type == TransactionType.UNBOND
                    }
                }
                
                return if (selectedAddressFilter == null) {
                    typeFiltered
                } else {
                    typeFiltered.filter { it.involvedAddress == selectedAddressFilter }
                }
            }
    }

    sealed interface Effect {
        data class NavigateToDetail(val txId: String) : Effect
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeHistory()
        refreshHistory()
        setupRealtimeUpdates()
    }

    private fun setupRealtimeUpdates() {
        webSocketManager.connect()
        viewModelScope.launch {
            webSocketManager.newBlockEvent.collect {
                refreshHistory()
            }
        }
    }

    private fun refreshHistory() {
        viewModelScope.launch {
            try {
                val wallet = walletRepository.observeWallet().filterNotNull().first()
                wallet.accounts.forEach { account ->
                    blockchainRepository.getTransactionHistory(account.address, 1)
                }
            } catch (_: Exception) {}
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            walletRepository.observeWallet()
                .filterNotNull()
                .flatMapLatest { wallet ->
                    _state.update { it.copy(wallet = wallet) }
                    blockchainRepository.observeTransactionHistory(
                        wallet.accounts.map { it.address }
                    )
                }
                .collect { txs ->
                    _state.update { it.copy(transactions = txs, isLoading = false) }
                }
        }
    }

    fun setFilter(filter: Filter) {
        _state.update { it.copy(filter = filter) }
    }

    fun setAddressFilter(address: String?) {
        _state.update { it.copy(selectedAddressFilter = address) }
    }

    fun onTransactionClick(txId: String) {
        viewModelScope.launch { _effect.send(Effect.NavigateToDetail(txId)) }
    }
}
