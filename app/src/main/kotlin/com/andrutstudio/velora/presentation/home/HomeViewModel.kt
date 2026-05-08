package com.andrutstudio.velora.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import com.andrutstudio.velora.domain.repository.MarketRepository
import com.andrutstudio.velora.domain.repository.WalletRepository
import com.andrutstudio.velora.domain.usecase.RefreshBalancesUseCase
import com.andrutstudio.velora.presentation.navigation.Screen
import com.andrutstudio.velora.presentation.widget.WidgetUpdater
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val blockchainRepository: BlockchainRepository,
    private val refreshBalances: RefreshBalancesUseCase,
    private val marketRepository: MarketRepository,
    private val networkProvider: com.andrutstudio.velora.data.rpc.NetworkProvider,
    private val rpcService: com.andrutstudio.velora.data.rpc.PactusRpcService,
    private val widgetUpdater: WidgetUpdater,
    private val balanceAlertDao: com.andrutstudio.velora.data.local.db.BalanceAlertDao,
) : ViewModel() {

    data class State(
        val wallet: Wallet? = null,
        val wallets: List<Wallet> = emptyList(),
        val balances: Map<String, Amount> = emptyMap(),
        val pacPriceUsd: Double? = null,
        val priceHistory: List<Double> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val showAddAccountSheet: Boolean = false,
        val isAddingAccount: Boolean = false,
        val showUnlockDialog: Boolean = false,
        val isUnlocking: Boolean = false,
        val unlockError: String? = null,
        val renamingAddress: String? = null,
        val renameLabel: String = "",
        // Multi-wallet
        val showWalletPickerSheet: Boolean = false,
        val showAddWalletSheet: Boolean = false,
        val isCreatingWallet: Boolean = false,
        val createWalletError: String? = null,
        // Delete confirmations
        val pendingDeleteAccountAddress: String? = null,
        val pendingDeleteWalletId: String? = null,
        // Biometric
        val isBiometricEnabled: Boolean = false,
        val isBiometricAvailable: Boolean = false,
        val pendingActionAfterUnlock: PendingAction? = null,
        val showAlertSheetForAddress: String? = null,
        val alerts: Map<String, com.andrutstudio.velora.data.local.db.BalanceAlertEntity> = emptyMap(),
        val isOffline: Boolean = false,
    ) {
        val totalBalance: Amount
            get() = balances.values.fold(Amount.ZERO) { acc, v -> acc + v }
    }

    enum class PendingAction { ADD_ACCOUNT }

    sealed interface Effect {
        data class AddressCopied(val address: String) : Effect
        data class NavigateTo(val route: String) : Effect
        data class ShowError(val message: String) : Effect
        data class ShowMessage(val message: String) : Effect
        data object RequestBiometricUnlock : Effect
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeWallet()
        observeAlerts()
        viewModelScope.launch {
            networkProvider.findFastestNode(rpcService)
            fetchMarketPrice()
        }
        checkAutoBiometric()
        startBalancePolling()
        blockchainRepository.triggerSync()
    }

    private fun startBalancePolling() {
        viewModelScope.launch {
            while (true) {
                val wallet = _state.value.wallet
                if (wallet != null) {
                    fetchBalances(wallet, isRefreshing = false)
                    fetchMarketPrice()
                }
                delay(30_000) // Poll every 30 seconds while in foreground
            }
        }
    }

    private fun observeAlerts() {
        viewModelScope.launch {
            balanceAlertDao.getAllAlerts().collect { alertList ->
                _state.update { it.copy(alerts = alertList.associateBy { it.address }) }
            }
        }
    }

    fun onShowAlertSheet(address: String) {
        _state.update { it.copy(showAlertSheetForAddress = address) }
    }

    fun onDismissAlertSheet() {
        _state.update { it.copy(showAlertSheetForAddress = null) }
    }

    fun onSaveAlert(thresholdNanoPac: Long, enabled: Boolean, type: com.andrutstudio.velora.data.local.db.AlertType) {
        val address = _state.value.showAlertSheetForAddress ?: return
        viewModelScope.launch {
            balanceAlertDao.insertAlert(
                com.andrutstudio.velora.data.local.db.BalanceAlertEntity(
                    address = address,
                    thresholdNanoPac = thresholdNanoPac,
                    isEnabled = enabled,
                    type = type
                )
            )
            onDismissAlertSheet()
            _effect.send(Effect.ShowMessage("Balance alert saved"))
        }
    }

    fun onDeleteAlert() {
        val address = _state.value.showAlertSheetForAddress ?: return
        val alert = _state.value.alerts[address] ?: return
        viewModelScope.launch {
            balanceAlertDao.deleteAlert(alert)
            onDismissAlertSheet()
            _effect.send(Effect.ShowMessage("Balance alert removed"))
        }
    }

    private fun checkAutoBiometric() {
        viewModelScope.launch {
            // Wait for observeWallet to get initial state
            state.first { it.wallet != null || it.wallets.isNotEmpty() || !it.isLoading }
            if (walletRepository.isBiometricEnabled() && !walletRepository.isUnlocked()) {
                _effect.send(Effect.RequestBiometricUnlock)
            }
        }
    }

    private fun fetchMarketPrice() {
        viewModelScope.launch {
            runCatching { marketRepository.getPacPriceUsd() }
                .onSuccess { price -> _state.update { it.copy(pacPriceUsd = price) } }

            runCatching { marketRepository.getPriceHistory() }
                .onSuccess { history -> _state.update { it.copy(priceHistory = history) } }
        }
    }

    private fun observeWallet() {
        combine(
            walletRepository.observeWallet(),
            walletRepository.observeWallets(),
        ) { active, all -> active to all }
            .onEach { (active, all) ->
                _state.update { 
                    it.copy(
                        wallet = active, 
                        wallets = all, 
                        isLoading = active == null && all.isEmpty(),
                        isBiometricEnabled = walletRepository.isBiometricEnabled()
                    ) 
                }
                if (active != null && !walletRepository.isUnlocked() && walletRepository.isBiometricEnabled()) {
                    _effect.send(Effect.RequestBiometricUnlock)
                }
                if (active != null) {
                    fetchBalances(active, isRefreshing = false)
                    fetchMarketPrice()
                } else _state.update { it.copy(balances = emptyMap()) }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            val wallet = _state.value.wallet ?: return@launch
            fetchBalances(wallet, isRefreshing = true)
            fetchMarketPrice()
        }
    }

    fun onAddressCopied(address: String) {
        viewModelScope.launch { _effect.send(Effect.AddressCopied(address)) }
    }

    fun navigateTo(route: String) {
        viewModelScope.launch { _effect.send(Effect.NavigateTo(route)) }
    }

    fun onShowAddAccountSheet() {
        if (walletRepository.isUnlocked()) {
            _state.update { it.copy(showAddAccountSheet = true) }
        } else if (_state.value.isBiometricEnabled) {
            _state.update { it.copy(pendingActionAfterUnlock = PendingAction.ADD_ACCOUNT) }
            viewModelScope.launch { _effect.send(Effect.RequestBiometricUnlock) }
        } else {
            _state.update { it.copy(showUnlockDialog = true, unlockError = null, pendingActionAfterUnlock = PendingAction.ADD_ACCOUNT) }
        }
    }

    fun onBiometricUnlockSuccess(password: String) {
        onUnlock(password)
    }

    fun onBiometricUnlockTriggered() {
        viewModelScope.launch { _effect.send(Effect.RequestBiometricUnlock) }
    }

    fun onDismissAddAccountSheet() {
        _state.update { it.copy(showAddAccountSheet = false) }
    }

    fun onShowRename(address: String, currentLabel: String) {
        _state.update { it.copy(renamingAddress = address, renameLabel = currentLabel) }
    }

    fun onRenameLabelChange(value: String) {
        _state.update { it.copy(renameLabel = value) }
    }

    fun onDismissRename() {
        _state.update { it.copy(renamingAddress = null, renameLabel = "") }
    }

    fun onConfirmRename() {
        val address = _state.value.renamingAddress ?: return
        val label = _state.value.renameLabel.trim().ifEmpty { return }
        viewModelScope.launch {
            try {
                walletRepository.renameAccount(address, label)
            } catch (_: Exception) {}
            _state.update { it.copy(renamingAddress = null, renameLabel = "") }
        }
    }

    fun onDismissUnlock() {
        _state.update { it.copy(showUnlockDialog = false, unlockError = null) }
    }

    fun onUnlock(password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUnlocking = true, unlockError = null) }
            try {
                val success = walletRepository.unlockWallet(password)
                if (success) {
                    val pendingAction = _state.value.pendingActionAfterUnlock
                    _state.update { 
                        it.copy(
                            isUnlocking = false, 
                            showUnlockDialog = false, 
                            showAddAccountSheet = pendingAction == PendingAction.ADD_ACCOUNT,
                            pendingActionAfterUnlock = null
                        ) 
                    }
                } else {
                    _state.update { it.copy(isUnlocking = false, unlockError = "Incorrect password") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isUnlocking = false, unlockError = e.message ?: "Unlock failed") }
            }
        }
    }

    // ── Delete account ────────────────────────────────────────────────────────

    fun onDeleteAccount(address: String) {
        val wallet = _state.value.wallet ?: return
        if (wallet.accounts.size <= 1) {
            viewModelScope.launch {
                _effect.send(Effect.ShowError("Cannot delete the only account in this wallet"))
            }
            return
        }
        _state.update { it.copy(pendingDeleteAccountAddress = address) }
    }

    fun onDismissDeleteAccount() {
        _state.update { it.copy(pendingDeleteAccountAddress = null) }
    }

    fun onConfirmDeleteAccount() {
        val address = _state.value.pendingDeleteAccountAddress ?: return
        _state.update { it.copy(pendingDeleteAccountAddress = null) }
        viewModelScope.launch {
            try {
                walletRepository.deleteAccount(address)
                _effect.send(Effect.ShowMessage("Account deleted"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(Effect.ShowError(e.message ?: "Failed to delete account"))
            }
        }
    }

    // ── Multi-wallet ──────────────────────────────────────────────────────────

    fun onWalletSelectorClick() {
        _state.update { it.copy(showWalletPickerSheet = true) }
    }

    fun onDismissWalletPicker() {
        _state.update { it.copy(showWalletPickerSheet = false) }
    }

    fun onSelectWallet(walletId: String) {
        if (walletId == _state.value.wallet?.id) {
            _state.update { it.copy(showWalletPickerSheet = false) }
            return
        }
        viewModelScope.launch {
            try {
                walletRepository.selectWallet(walletId)
                _state.update { it.copy(showWalletPickerSheet = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(Effect.ShowError(e.message ?: "Failed to switch wallet"))
            }
        }
    }

    fun onRequestDeleteWallet(walletId: String) {
        _state.update { it.copy(pendingDeleteWalletId = walletId) }
    }

    fun onDismissDeleteWallet() {
        _state.update { it.copy(pendingDeleteWalletId = null) }
    }

    fun onConfirmDeleteWallet() {
        val id = _state.value.pendingDeleteWalletId ?: return
        _state.update { it.copy(pendingDeleteWalletId = null) }
        viewModelScope.launch {
            try {
                walletRepository.deleteWallet(id)
                _effect.send(Effect.ShowMessage("Wallet deleted"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(Effect.ShowError(e.message ?: "Failed to delete wallet"))
            }
        }
    }

    fun onAddWalletClick() {
        viewModelScope.launch {
            _state.update { it.copy(showWalletPickerSheet = false) }
            _effect.send(Effect.NavigateTo(Screen.AddWalletOptions.route))
        }
    }

    fun onDismissAddWallet() {
        _state.update { it.copy(showAddWalletSheet = false, createWalletError = null) }
    }

    fun onCreateWallet(name: String, password: String) {
        val trimmedName = name.trim().ifEmpty { "Wallet ${_state.value.wallets.size + 1}" }
        if (password.length < 8) {
            _state.update { it.copy(createWalletError = "Password must be at least 8 characters") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCreatingWallet = true, createWalletError = null) }
            try {
                val mnemonic = walletRepository.generateMnemonic(12)
                walletRepository.createWallet(trimmedName, mnemonic, password)
                _state.update { it.copy(isCreatingWallet = false, showAddWalletSheet = false) }
                _effect.send(Effect.ShowMessage("Wallet \"$trimmedName\" created"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isCreatingWallet = false, createWalletError = e.message ?: "Failed to create wallet")
                }
            }
        }
    }

    fun onAddAccount(type: AccountType, label: String) {
        val currentWallet = _state.value.wallet ?: return
        val finalLabel = label.ifBlank {
            "Account ${currentWallet.accounts.size + 1}"
        }
        viewModelScope.launch {
            _state.update { it.copy(isAddingAccount = true) }
            try {
                walletRepository.addAccount(type, finalLabel)
                _state.update { it.copy(isAddingAccount = false, showAddAccountSheet = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isAddingAccount = false) }
                _effect.send(Effect.ShowError(e.message ?: "Failed to add account"))
            }
        }
    }

    private suspend fun fetchBalances(wallet: Wallet, isRefreshing: Boolean) {
        _state.update { it.copy(isRefreshing = isRefreshing) }
        try {
            val balances = refreshBalances(wallet)
            _state.update { it.copy(balances = balances, isRefreshing = false, error = null, isOffline = false) }
            widgetUpdater.update(
                totalBalanceNanoPac = balances.values.sumOf { it.nanoPac },
                walletName = wallet.name,
                networkName = wallet.network.displayName,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val isNetworkError = e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.net.SocketTimeoutException
            if (isNetworkError) {
                _state.update { it.copy(isRefreshing = false, isOffline = true) }
                _effect.send(Effect.ShowMessage("You are offline. Reconnecting..."))
            } else {
                _state.update { it.copy(isRefreshing = false, error = e.message) }
            }
        }
    }
}
