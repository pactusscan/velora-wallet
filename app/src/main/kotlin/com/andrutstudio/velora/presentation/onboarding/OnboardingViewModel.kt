package com.andrutstudio.velora.presentation.onboarding

import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.components.toUiText
import com.andrutstudio.velora.presentation.components.uiText
import com.andrutstudio.velora.presentation.components.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.andrutstudio.velora.domain.repository.WalletRepository
import com.andrutstudio.velora.domain.usecase.CreateWalletUseCase
import com.andrutstudio.velora.domain.usecase.GenerateMnemonicUseCase
import com.andrutstudio.velora.domain.usecase.ValidateMnemonicUseCase
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val generateMnemonic: GenerateMnemonicUseCase,
    private val validateMnemonic: ValidateMnemonicUseCase,
    private val createWallet: CreateWalletUseCase,
    private val walletRepository: WalletRepository,
    private val keyManager: com.andrutstudio.velora.data.crypto.PactusKeyManager,
    private val networkProvider: com.andrutstudio.velora.data.rpc.NetworkProvider,
) : ViewModel() {

    data class State(
        val walletName: String = "",
        val mnemonic: List<String> = emptyList(),
        val isRestoring: Boolean = false,
        val isPrivateKeyImport: Boolean = false,
        val isWatchOnly: Boolean = false,
        val isTestnet: Boolean = false,
        val activeNetwork: com.andrutstudio.velora.domain.model.Network = com.andrutstudio.velora.domain.model.Network.MAINNET,
        val importedAccountType: com.andrutstudio.velora.domain.model.AccountType = com.andrutstudio.velora.domain.model.AccountType.ED25519,
        val privateKeyInput: String = "",
        val privateKeyHex: String = "",
        val watchAddress: String = "",
        val privateKeyError: UiText? = null,
        val watchAddressError: UiText? = null,
        val restoreWords: List<String> = List(12) { "" },
        val mnemonicText: String = "",
        val wordCount: Int = 12,
        val isLoading: Boolean = false,
        val nameError: UiText? = null,
        val mnemonicError: UiText? = null,
        val passwordError: UiText? = null,
    )

    sealed interface Effect {
        data object NavigateToBackup : Effect
        data object NavigateToSecurity : Effect
        data object NavigateToHome : Effect
        data class RequestBiometricEnrollment(val password: String) : Effect
        data class ShowError(val message: UiText) : Effect
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        _state.update { it.copy(activeNetwork = networkProvider.current) }
    }

    // ── Create flow ──────────────────────────────────────────────────────────

    fun setWalletName(name: String) {
        _state.update { it.copy(walletName = name, nameError = null) }
    }

    fun startCreate() {
        _state.update { it.copy(isRestoring = false) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val words = generateMnemonic(12).split(" ")
            _state.update { it.copy(mnemonic = words, isLoading = false) }
            _effect.send(Effect.NavigateToBackup)
        }
    }

    fun proceedToVerify() {
        viewModelScope.launch { _effect.send(Effect.NavigateToSecurity) }
    }


    // ── Private key import flow ───────────────────────────────────────────────

    fun startPrivateKeyImport() {
        _state.update { it.copy(isPrivateKeyImport = true, isRestoring = false, privateKeyInput = "", privateKeyHex = "", privateKeyError = null) }
    }

    fun setPrivateKeyHex(input: String) {
        _state.update { it.copy(privateKeyInput = input, privateKeyError = null) }
    }

    fun submitPrivateKey() {
        val input = _state.value.privateKeyInput.trim()
        val currentNetwork = _state.value.activeNetwork

        val hex = if (input.startsWith("secret1", ignoreCase = true) || input.startsWith("tsecret1", ignoreCase = true)) {
            try {
                val (hrp, type, bytes) = com.andrutstudio.velora.data.crypto.Bech32m.decode(input)
                
                // Pactus Private Key Types: 1 = BLS, 3 = Ed25519
                val accountType = when(type.toInt()) {
                    1 -> {
                        _state.update { it.copy(privateKeyError = UiText.Res(R.string.import_error_bls_soon)) }
                        return
                    }
                    3 -> com.andrutstudio.velora.domain.model.AccountType.ED25519
                    else -> {
                        _state.update { it.copy(privateKeyError = UiText.Res(R.string.import_error_key_unsupported)) }
                        return
                    }
                }

                // Validate that the key matches the current network
                if (currentNetwork == com.andrutstudio.velora.domain.model.Network.MAINNET && hrp != "secret") {
                    _state.update { it.copy(privateKeyError = UiText.Res(R.string.import_error_testnet_key)) }
                    return
                }
                if (currentNetwork == com.andrutstudio.velora.domain.model.Network.TESTNET && hrp != "tsecret") {
                    _state.update { it.copy(privateKeyError = UiText.Res(R.string.import_error_mainnet_key)) }
                    return
                }

                _state.update { it.copy(importedAccountType = accountType) }
                bytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                _state.update { it.copy(privateKeyError = UiText.Res(R.string.import_error_key_format)) }
                return
            }
        } else {
            val h = input.removePrefix("0x")
            if (!h.matches(Regex("[0-9a-fA-F]{64}"))) {
                _state.update { it.copy(privateKeyError = UiText.Res(R.string.import_error_key_empty)) }
                return
            }
            _state.update { it.copy(importedAccountType = com.andrutstudio.velora.domain.model.AccountType.ED25519) }
            h
        }

        _state.update { it.copy(privateKeyHex = hex, isPrivateKeyImport = true) }

        // Security check: Check if the derived address already exists in any existing wallet
        viewModelScope.launch {
            val address = try {
                val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                keyManager.addressFromPrivateKey(bytes, currentNetwork, _state.value.importedAccountType)
            } catch (e: Exception) {
                null
            }

            if (address != null) {
                val exists = walletRepository.observeWallets().first().any { wallet ->
                    wallet.accounts.any { it.address == address }
                }
                if (exists) {
                    _state.update { it.copy(privateKeyError = UiText.Res(R.string.import_error_already_imported)) }
                    return@launch
                }
            }

            _effect.send(Effect.NavigateToSecurity)
        }
    }

    // ── Watch wallet flow ────────────────────────────────────────────────────

    fun startWatchWallet() {
        _state.update { it.copy(isWatchOnly = true, isPrivateKeyImport = false, isRestoring = false, watchAddress = "", watchAddressError = null) }
    }

    fun setWatchAddress(address: String) {
        _state.update { it.copy(watchAddress = address.trim(), watchAddressError = null) }
    }

    fun submitWatchWallet() {
        val address = _state.value.watchAddress.trim()
        if (address.isBlank()) {
            _state.update { it.copy(watchAddressError = UiText.Res(R.string.watch_error_invalid_address)) }
            return
        }
        // Basic Pactus address validation (pc1...)
        if (!address.startsWith("pc1")) {
            _state.update { it.copy(watchAddressError = UiText.Res(R.string.watch_error_address_prefix)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                walletRepository.watchWallet(
                    name = _state.value.walletName.ifBlank { "Watched Wallet" },
                    address = address
                )
            }.onSuccess {
                _effect.send(Effect.NavigateToHome)
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, watchAddressError = e.toUiText(R.string.watch_error_invalid_address)) }
            }
        }
    }

    // ── Restore flow ─────────────────────────────────────────────────────────

    fun startRestore() {
        _state.update { it.copy(isRestoring = true, restoreWords = List(12) { "" }, mnemonicText = "", wordCount = 12) }
    }

    fun setMnemonicText(text: String) {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val count = if (words.size > 12) 24 else 12
        val padded = List(count) { i -> words.getOrElse(i) { "" } }
        _state.update {
            it.copy(
                mnemonicText = text,
                wordCount = count,
                restoreWords = padded,
                mnemonicError = null,
            )
        }
    }

    fun setWordCount(count: Int) {
        require(count == 12 || count == 24)
        _state.update { it.copy(wordCount = count, restoreWords = List(count) { "" }, mnemonicError = null) }
    }

    fun setRestoreWord(index: Int, word: String) {
        val currentWords = _state.value.restoreWords.toMutableList()
        if (index >= currentWords.size) return

        // Check if user pasted a full phrase (contains spaces)
        if (word.trim().contains(" ")) {
            val pastedWords = word.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            pastedWords.forEachIndexed { i, w ->
                if (index + i < currentWords.size) {
                    currentWords[index + i] = w.lowercase()
                }
            }
        } else {
            currentWords[index] = word.trim().lowercase()
        }

        _state.update { it.copy(restoreWords = currentWords, mnemonicError = null) }
    }

    fun submitRestore() {
        val words = _state.value.restoreWords
        if (words.any { it.isBlank() }) {
            _state.update { it.copy(mnemonicError = uiText(R.string.restore_error_fill_words, _state.value.wordCount)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val mnemonic = words.joinToString(" ")
            val valid = validateMnemonic(mnemonic)
            if (valid) {
                _state.update { it.copy(mnemonic = words, isLoading = false, mnemonicError = null, isRestoring = true) }
                _effect.send(Effect.NavigateToSecurity)
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        mnemonicError = UiText.Res(R.string.restore_error_invalid_phrase),
                    )
                }
            }
        }
    }

    // ── Security setup ───────────────────────────────────────────────────────

    fun finishSetup(password: String, confirmPassword: String, enableBiometric: Boolean) {
        val state = _state.value

        if (password.length < 8) {
            _state.update { it.copy(passwordError = UiText.Res(R.string.error_password_min_length)) }
            return
        }
        if (password != confirmPassword) {
            _state.update { it.copy(passwordError = UiText.Res(R.string.security_setup_passwords_mismatch)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, passwordError = null) }
            runCatching {
                if (state.isPrivateKeyImport) {
                    walletRepository.importPrivateKey(
                        name = state.walletName.ifBlank { "Imported Wallet" },
                        privateKeyHex = state.privateKeyHex,
                        password = password,
                        type = state.importedAccountType,
                    )
                } else {
                    createWallet(
                        name = state.walletName.ifBlank { "My Wallet" },
                        mnemonic = state.mnemonic.joinToString(" "),
                        password = password,
                        isRestore = state.isRestoring,
                    )
                }
                
                // Save biometric preference initially as false, will be set to true on success
                walletRepository.setBiometricEnabled(false)

                if (enableBiometric) {
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(Effect.RequestBiometricEnrollment(password))
                } else {
                    clearSensitiveData()
                    _effect.send(Effect.NavigateToHome)
                }
            }.onFailure { e ->
                android.util.Log.e("Onboarding", "Setup failed", e)
                _state.update { it.copy(isLoading = false) }
                _effect.send(Effect.ShowError(e.toUiText(R.string.error_create_wallet)))
            }
        }
    }

    fun onBiometricEnrollmentSuccess() {
        viewModelScope.launch {
            walletRepository.setBiometricEnabled(true)
            clearSensitiveData()
            _effect.send(Effect.NavigateToHome)
        }
    }

    fun onBiometricEnrollmentError(error: String) {
        viewModelScope.launch {
            // Even if biometric fails, the wallet is already created, so we go home
            // but keep biometric disabled
            walletRepository.setBiometricEnabled(false)
            clearSensitiveData()
            _effect.send(Effect.NavigateToHome)
        }
    }

    private fun clearSensitiveData() {
        _state.update {
            it.copy(
                mnemonic = emptyList(),
                restoreWords = emptyList(),
                mnemonicText = "",
                isLoading = false,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearSensitiveData()
    }
}
