package com.andrutstudio.velora.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.andrutstudio.velora.data.crypto.Bech32m
import com.andrutstudio.velora.data.local.WrongPasswordException
import com.andrutstudio.velora.data.rpc.NetworkProvider
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.domain.repository.WalletRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val networkProvider: NetworkProvider,
) : ViewModel() {

    data class State(
        val wallet: Wallet? = null,
        val isBiometricEnabled: Boolean = false,
        // Change-password form
        val oldPassword: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val passwordError: String? = null,
        val isPasswordLoading: Boolean = false,
        // Backup form
        val backupPassword: String = "",
        val backupError: String? = null,
        val isBackupLoading: Boolean = false,
        // Network
        val isNetworkLoading: Boolean = false,
        val showUnlockForNetwork: Boolean = false,
        val pendingNetwork: Network? = null,
        val isUnlocking: Boolean = false,
        val unlockError: String? = null,
        // Custom RPC URLs
        val customRpcMainnet: String = "",
        val customRpcTestnet: String = "",
        val isCustomRpcSaving: Boolean = false,
        // Rename wallet
        val isRenamingWallet: Boolean = false,
        val renameName: String = "",
        // Biometric
        val showUnlockForBiometric: Boolean = false,
        val biometricAction: BiometricAction? = null,
    )

    enum class BiometricAction { REVEAL_MNEMONIC, SWITCH_NETWORK }

    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
        data object PasswordChanged : Effect
        data class MnemonicRevealed(val mnemonic: String) : Effect
        data object WalletReset : Effect
        data class RequestBiometricEnrollment(val password: String) : Effect
        data object RequestBiometricUnlock : Effect
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            walletRepository.observeWallet().filterNotNull().first().let { wallet ->
                _state.update {
                    it.copy(
                        wallet = wallet,
                        isBiometricEnabled = walletRepository.isBiometricEnabled(),
                    )
                }
            }
            // Keep wallet state updated
            walletRepository.observeWallet().collect { wallet ->
                _state.update { it.copy(wallet = wallet) }
            }
        }

        viewModelScope.launch {
            networkProvider.customRpcFlow(Network.MAINNET).collect { url ->
                _state.update { it.copy(customRpcMainnet = url ?: "") }
            }
        }

        viewModelScope.launch {
            networkProvider.customRpcFlow(Network.TESTNET).collect { url ->
                _state.update { it.copy(customRpcTestnet = url ?: "") }
            }
        }
    }

    // ── Security ──────────────────────────────────────────────────────────────

    fun onOldPasswordChange(value: String) {
        _state.update { it.copy(oldPassword = value, passwordError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _state.update { it.copy(newPassword = value, passwordError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _state.update { it.copy(confirmPassword = value, passwordError = null) }
    }

    fun onChangePassword() {
        val s = _state.value
        val error = when {
            s.oldPassword.isBlank() -> "Enter your current password"
            s.newPassword.length < 8 -> "New password must be at least 8 characters"
            s.newPassword != s.confirmPassword -> "Passwords do not match"
            s.newPassword == s.oldPassword -> "New password must be different"
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(passwordError = error) }
            return
        }

        _state.update { it.copy(isPasswordLoading = true, passwordError = null) }
        viewModelScope.launch {
            runCatching {
                walletRepository.changePassword(s.oldPassword, s.newPassword)
            }.onSuccess {
                _state.update {
                    it.copy(
                        isPasswordLoading = false,
                        oldPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                    )
                }
                _effect.send(Effect.PasswordChanged)
                _effect.send(Effect.ShowSnackbar("Password changed successfully"))
            }.onFailure { e ->
                val msg = if (e is WrongPasswordException) "Current password is incorrect"
                else e.message ?: "Failed to change password"
                _state.update { it.copy(isPasswordLoading = false, passwordError = msg) }
            }
        }
    }

    fun onToggleBiometric(enabled: Boolean) {
        if (enabled) {
            _state.update { it.copy(showUnlockForBiometric = true, unlockError = null) }
        } else {
            walletRepository.setBiometricEnabled(false)
            _state.update { it.copy(isBiometricEnabled = false) }
        }
    }

    fun onDismissBiometricUnlock() {
        _state.update { it.copy(showUnlockForBiometric = false, unlockError = null) }
    }

    fun onConfirmBiometricUnlock(password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUnlocking = true, unlockError = null) }
            try {
                val success = walletRepository.unlockWallet(password)
                if (success) {
                    _state.update { it.copy(isUnlocking = false, showUnlockForBiometric = false) }
                    _effect.send(Effect.RequestBiometricEnrollment(password))
                } else {
                    _state.update { it.copy(isUnlocking = false, unlockError = "Incorrect password") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isUnlocking = false, unlockError = e.message ?: "Failed to enable biometric") }
            }
        }
    }

    fun onBiometricEnrollmentSuccess() {
        viewModelScope.launch {
            walletRepository.setBiometricEnabled(true)
            _state.update { it.copy(isBiometricEnabled = true) }
            _effect.send(Effect.ShowSnackbar("Biometric unlock enabled"))
        }
    }

    fun onBiometricUnlockTriggered() {
        viewModelScope.launch { _effect.send(Effect.RequestBiometricUnlock) }
    }

    fun onBiometricUnlockSuccess(password: String) {
        viewModelScope.launch {
            val success = walletRepository.unlockWallet(password)
            if (success) {
                val action = _state.value.biometricAction
                _state.update { it.copy(biometricAction = null) }
                
                when (action) {
                    BiometricAction.SWITCH_NETWORK -> {
                        val pending = _state.value.pendingNetwork
                        _state.update { it.copy(showUnlockForNetwork = false, pendingNetwork = null) }
                        pending?.let { doSwitchNetwork(it) }
                    }
                    BiometricAction.REVEAL_MNEMONIC -> {
                        runCatching { walletRepository.revealMnemonic(password) }
                            .onSuccess { revealed ->
                                val finalValue = if (_state.value.wallet?.isPrivateKeyImport == true) {
                                    try {
                                        val bytes = revealed.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                                        val network = _state.value.wallet?.network ?: Network.MAINNET
                                        val hrp = if (network == Network.MAINNET) "secret" else "tsecret"
                                        val type = _state.value.wallet?.accounts?.firstOrNull()?.type ?: AccountType.ED25519
                                        val typeByte = if (type == AccountType.BLS) 1.toByte() else 3.toByte()
                                        Bech32m.encodeWithType(hrp, typeByte, bytes).uppercase()
                                    } catch (e: Exception) {
                                        revealed.uppercase()
                                    }
                                } else {
                                    revealed
                                }
                                _effect.send(Effect.MnemonicRevealed(finalValue))
                            }
                    }
                    null -> Unit
                }
            }
        }
    }

    // ── Network ───────────────────────────────────────────────────────────────

    fun onSwitchNetwork(network: Network) {
        if (_state.value.wallet?.network == network) return
        if (!walletRepository.isUnlocked()) {
            if (walletRepository.isBiometricEnabled()) {
                _state.update { it.copy(pendingNetwork = network, biometricAction = BiometricAction.SWITCH_NETWORK) }
                viewModelScope.launch { _effect.send(Effect.RequestBiometricUnlock) }
            } else {
                _state.update { it.copy(showUnlockForNetwork = true, pendingNetwork = network, unlockError = null) }
            }
            return
        }
        doSwitchNetwork(network)
    }

    fun onDismissUnlock() {
        _state.update { it.copy(showUnlockForNetwork = false, pendingNetwork = null, unlockError = null) }
    }

    fun onUnlock(password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUnlocking = true, unlockError = null) }
            try {
                val success = walletRepository.unlockWallet(password)
                if (success) {
                    val pending = _state.value.pendingNetwork
                    _state.update { it.copy(isUnlocking = false, showUnlockForNetwork = false, pendingNetwork = null) }
                    pending?.let { doSwitchNetwork(it) }
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

    private fun doSwitchNetwork(network: Network) {
        _state.update { it.copy(isNetworkLoading = true) }
        viewModelScope.launch {
            runCatching { walletRepository.setNetwork(network) }
                .onSuccess {
                    networkProvider.setNetwork(network)
                    _state.update { it.copy(isNetworkLoading = false) }
                    _effect.send(Effect.ShowSnackbar("Switched to ${network.displayName}"))
                }
                .onFailure { e ->
                    _state.update { it.copy(isNetworkLoading = false) }
                    _effect.send(Effect.ShowSnackbar(e.message ?: "Network switch failed"))
                }
        }
    }

    fun onCustomRpcChange(network: Network, url: String) {
        if (network == Network.MAINNET) {
            _state.update { it.copy(customRpcMainnet = url) }
        } else {
            _state.update { it.copy(customRpcTestnet = url) }
        }
    }

    fun onSaveCustomRpc(network: Network) {
        val url = if (network == Network.MAINNET) _state.value.customRpcMainnet
        else _state.value.customRpcTestnet
        _state.update { it.copy(isCustomRpcSaving = true) }
        viewModelScope.launch {
            networkProvider.setCustomRpcUrl(network, url.takeIf { it.isNotBlank() })
            _state.update { it.copy(isCustomRpcSaving = false) }
            val msg = if (url.isBlank()) "Reverted to default endpoints"
            else "Custom RPC URL saved"
            _effect.send(Effect.ShowSnackbar(msg))
        }
    }

    // ── Backup ────────────────────────────────────────────────────────────────

    fun onBackupPasswordChange(value: String) {
        _state.update { it.copy(backupPassword = value, backupError = null) }
    }

    fun onRevealMnemonic() {
        val password = _state.value.backupPassword
        if (password.isBlank()) {
            if (walletRepository.isBiometricEnabled()) {
                _state.update { it.copy(biometricAction = BiometricAction.REVEAL_MNEMONIC) }
                viewModelScope.launch { _effect.send(Effect.RequestBiometricUnlock) }
            } else {
                _state.update { it.copy(backupError = "Enter your password") }
            }
            return
        }
        _state.update { it.copy(isBackupLoading = true, backupError = null) }
        viewModelScope.launch {
            runCatching { walletRepository.revealMnemonic(password) }
                .onSuccess { revealed ->
                    val finalValue = if (_state.value.wallet?.isPrivateKeyImport == true) {
                        try {
                            val bytes = revealed.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                            val network = _state.value.wallet?.network ?: Network.MAINNET
                            val hrp = if (network == Network.MAINNET) "secret" else "tsecret"
                            val type = _state.value.wallet?.accounts?.firstOrNull()?.type ?: AccountType.ED25519
                            val typeByte = if (type == AccountType.BLS) 1.toByte() else 3.toByte()
                            Bech32m.encodeWithType(hrp, typeByte, bytes).uppercase()
                        } catch (e: Exception) {
                            revealed.uppercase()
                        }
                    } else {
                        revealed
                    }
                    _state.update { it.copy(isBackupLoading = false, backupPassword = "") }
                    _effect.send(Effect.MnemonicRevealed(finalValue))
                }
                .onFailure { e ->
                    val msg = if (e is WrongPasswordException) "Incorrect password"
                    else e.message ?: "Failed to decrypt phrase"
                    _state.update { it.copy(isBackupLoading = false, backupError = msg) }
                }
        }
    }

    fun onResetWallet() {
        viewModelScope.launch {
            walletRepository.resetWallet()
            _effect.send(Effect.WalletReset)
        }
    }

    fun onClearBackupForm() {
        _state.update { it.copy(backupPassword = "", backupError = null) }
    }

    // ── Wallet Rename ────────────────────────────────────────────────────────

    fun onShowRenameWallet() {
        val currentName = _state.value.wallet?.name ?: ""
        _state.update { it.copy(isRenamingWallet = true, renameName = currentName) }
    }

    fun onDismissRenameWallet() {
        _state.update { it.copy(isRenamingWallet = false, renameName = "") }
    }

    fun onRenameNameChange(value: String) {
        _state.update { it.copy(renameName = value) }
    }

    fun onConfirmRenameWallet() {
        val wallet = _state.value.wallet ?: return
        val newName = _state.value.renameName.trim()
        if (newName.isBlank()) return

        viewModelScope.launch {
            walletRepository.renameWallet(wallet.id, newName)
            _state.update { it.copy(isRenamingWallet = false, renameName = "") }
            _effect.send(Effect.ShowSnackbar("Wallet renamed to $newName"))
        }
    }
}
