package com.andrutstudio.velora.presentation.send

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import com.andrutstudio.velora.domain.repository.WalletRepository
import com.andrutstudio.velora.domain.usecase.SendTransactionUseCase
import com.andrutstudio.velora.presentation.components.formatPacInput
import javax.inject.Inject

@HiltViewModel
class SendViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val blockchainRepository: BlockchainRepository,
    private val sendTransaction: SendTransactionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    data class State(
        val wallet: Wallet? = null,
        val selectedAccount: Account? = null,
        val availableBalance: Amount = Amount.ZERO,
        val toAddress: String = "",
        val amountText: String = "",
        val memo: String = "",
        val estimatedFee: Amount? = null,
        val feeText: String = "",
        val isFeeManual: Boolean = false,
        val isFeeLoading: Boolean = false,
        val toAddressError: String? = null,
        val amountError: String? = null,
        val feeError: String? = null,
        val isConfirmVisible: Boolean = false,
        val isSending: Boolean = false,
        val isLoading: Boolean = true,
        val password: String = "",
        val passwordError: String? = null,
        val isBiometricEnabled: Boolean = false,
    ) {
        val parsedAmount: Amount?
            get() = amountText.toDoubleOrNull()?.takeIf { it > 0 }?.let { Amount.fromPac(it) }

        val parsedFee: Amount?
            get() = feeText.toDoubleOrNull()?.takeIf { it > 0 }?.let { Amount.fromPac(it) }

        val canPreview: Boolean
            get() = toAddress.isNotBlank() && parsedAmount != null && !isSending &&
                    (parsedFee != null || estimatedFee != null)
    }

    sealed interface Effect {
        data class TxSuccess(val txId: String) : Effect
        data class ShowError(val message: String) : Effect
        data object NavigateBack : Effect
        data object RequestBiometricUnlock : Effect
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var feeJob: Job? = null

    init {
        val prefilledTo = savedStateHandle.get<String>("to").orEmpty()
        val prefilledAmount = savedStateHandle.get<String>("amount").orEmpty()
        if (prefilledTo.isNotEmpty() || prefilledAmount.isNotEmpty()) {
            _state.update { it.copy(toAddress = prefilledTo, amountText = prefilledAmount) }
        }
        loadWallet()
    }

    private fun loadWallet() {
        viewModelScope.launch {
            val wallet = walletRepository.observeWallet().filterNotNull().first()
            val firstAccount = wallet.accounts.firstOrNull()
            _state.update { 
                it.copy(
                    wallet = wallet, 
                    selectedAccount = firstAccount, 
                    isLoading = false,
                    isBiometricEnabled = walletRepository.isBiometricEnabled()
                ) 
            }
            firstAccount?.let { fetchBalance(it) }
            if (_state.value.amountText.isNotEmpty()) scheduleFeeEstimate()
        }
    }

    fun onSelectAccount(account: Account) {
        _state.update { it.copy(selectedAccount = account, toAddressError = null) }
        fetchBalance(account)
    }

    private fun fetchBalance(account: Account) {
        viewModelScope.launch {
            try {
                val balance = blockchainRepository.getBalance(account.address)
                _state.update { it.copy(availableBalance = balance) }
            } catch (_: Exception) {}
        }
    }

    fun onToAddressChange(value: String) {
        _state.update { it.copy(toAddress = value.trim(), toAddressError = null) }
    }

    fun onAmountChange(value: String) {
        _state.update { it.copy(amountText = value, amountError = null) }
        scheduleFeeEstimate()
    }

    fun onMaxAmount() {
        val balance = _state.value.availableBalance
        val fee = _state.value.parsedFee ?: _state.value.estimatedFee ?: Amount.ZERO
        val max = if (balance.nanoPac > fee.nanoPac) Amount(balance.nanoPac - fee.nanoPac) else Amount.ZERO
        _state.update { it.copy(amountText = formatPacInput(max), amountError = null) }
        scheduleFeeEstimate()
    }

    fun onFeeChange(value: String) {
        _state.update { it.copy(feeText = value, isFeeManual = true, feeError = null) }
    }

    fun onResetFee() {
        val estimated = _state.value.estimatedFee
        _state.update {
            it.copy(
                feeText = if (estimated != null) formatPacInput(estimated) else "",
                isFeeManual = false,
                feeError = null,
            )
        }
    }

    fun onMemoChange(value: String) {
        _state.update { it.copy(memo = value) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, passwordError = null) }
    }

    private fun scheduleFeeEstimate() {
        feeJob?.cancel()
        feeJob = viewModelScope.launch {
            delay(700)
            val amount = _state.value.parsedAmount ?: return@launch
            _state.update { it.copy(isFeeLoading = true, feeError = null) }
            try {
                val fee = sendTransaction.estimateFee(amount)
                _state.update { current ->
                    current.copy(
                        estimatedFee = fee,
                        isFeeLoading = false,
                        feeText = if (!current.isFeeManual || current.feeText.isEmpty()) formatPacInput(fee) else current.feeText,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isFeeLoading = false,
                        feeError = "Estimation failed — enter fee manually",
                    )
                }
            }
        }
    }

    fun onPreview() {
        val current = _state.value
        var hasError = false

        val from = current.selectedAccount?.address
        val addressError = validateRecipient(current.toAddress, from)
        if (addressError != null) {
            _state.update { it.copy(toAddressError = addressError) }
            hasError = true
        }

        val amount = current.parsedAmount
        if (amount == null) {
            _state.update { it.copy(amountError = "Enter a valid amount") }
            hasError = true
        }

        val fee = current.parsedFee
        if (fee == null) {
            if (!current.isFeeLoading) {
                _state.update { it.copy(feeError = "Enter a fee amount") }
                hasError = true
            }
        }

        if (!hasError && amount != null && fee != null) {
            val total = amount.nanoPac + fee.nanoPac
            if (total > current.availableBalance.nanoPac) {
                _state.update { it.copy(amountError = "Insufficient balance for amount + fee") }
                return
            }
            _state.update { it.copy(isConfirmVisible = true, password = "", passwordError = null) }
        }
    }

    fun onDismissConfirm() {
        if (_state.value.isSending) return
        _state.update { it.copy(isConfirmVisible = false, password = "", passwordError = null) }
    }

    fun onConfirmSend() {
        val current = _state.value
        if (current.isSending) return

        val from = current.selectedAccount?.address ?: return
        val amount = current.parsedAmount ?: return
        val fee = current.parsedFee ?: current.estimatedFee ?: return
        val password = current.password

        if (password.isEmpty()) {
            if (current.isBiometricEnabled) {
                viewModelScope.launch { _effect.send(Effect.RequestBiometricUnlock) }
            } else {
                _state.update { it.copy(passwordError = "Enter your wallet password") }
            }
            return
        }

        doSend(from, current.toAddress, amount, fee, current.memo, password)
    }

    fun onBiometricUnlockSuccess(password: String) {
        val current = _state.value
        val from = current.selectedAccount?.address ?: return
        val amount = current.parsedAmount ?: return
        val fee = current.parsedFee ?: current.estimatedFee ?: return
        
        doSend(from, current.toAddress, amount, fee, current.memo, password)
    }

    private fun doSend(from: String, to: String, amount: Amount, fee: Amount, memo: String, password: String) {
        _state.update { it.copy(isSending = true, passwordError = null) }
        viewModelScope.launch {
            try {
                val unlocked = walletRepository.unlockWallet(password)
                if (!unlocked) {
                    _state.update { it.copy(isSending = false, passwordError = "Incorrect password") }
                    return@launch
                }
                val txId = sendTransaction(
                    from = from,
                    to = to,
                    amount = amount,
                    fee = fee,
                    memo = memo,
                )
                _state.update {
                    it.copy(
                        isSending = false,
                        isConfirmVisible = false,
                        password = "",
                    )
                }
                _effect.send(Effect.TxSuccess(txId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSending = false) }
                _effect.send(Effect.ShowError(e.message ?: "Transaction failed"))
            }
        }
    }

    private fun validateRecipient(address: String, from: String?): String? {
        if (address.isBlank()) return "Recipient address is required"
        if (!isValidAccountAddress(address)) return "Invalid Pactus address (e.g. pc1r...)"
        if (from != null && address.equals(from, ignoreCase = false)) {
            return "Cannot send to your own address"
        }
        return null
    }

    /**
     * Pactus addresses are bech32-encoded.
     * pc1r... = Account (Ed25519)
     * pc1p... = Validator (BLS)
     * pc1z... = Account (BLS)
     */
    private fun isValidAccountAddress(address: String): Boolean {
        val lower = address.lowercase()
        if (lower.length !in 41..63) return false
        if (!lower.startsWith("pc1r") && !lower.startsWith("pc1z") && !lower.startsWith("pc1p")) return false
        val charset = "qpzry9x8gf2tvdw0s3jn54khce6mua7l".toSet()
        return lower.drop(4).all { it in charset }
    }
}


