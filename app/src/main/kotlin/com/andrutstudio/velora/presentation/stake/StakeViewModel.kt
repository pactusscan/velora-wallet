package com.andrutstudio.velora.presentation.stake

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
import com.andrutstudio.velora.presentation.components.formatPacInput
import javax.inject.Inject

@HiltViewModel
class StakeViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val blockchainRepository: BlockchainRepository,
) : ViewModel() {

    data class State(
        val wallet: Wallet? = null,
        val accounts: List<Account> = emptyList(),
        val selectedAccount: Account? = null,
        val availableBalance: Amount = Amount.ZERO,
        val validatorAddress: String = "",
        val validatorPublicKey: String = "",
        val amountText: String = "",
        val memo: String = "",
        val estimatedFee: Amount? = null,
        val feeText: String = "",
        val isFeeManual: Boolean = false,
        val isFeeLoading: Boolean = false,
        val validatorAddressError: String? = null,
        val amountError: String? = null,
        val feeError: String? = null,
        val isConfirmVisible: Boolean = false,
        val isStaking: Boolean = false,
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
            get() = validatorAddress.isNotBlank() && parsedAmount != null && !isStaking &&
                    (parsedFee != null || estimatedFee != null)
    }

    sealed interface Effect {
        data class ShowSuccess(val txId: String) : Effect
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
        loadWallet()
    }

    private fun loadWallet() {
        viewModelScope.launch {
            val wallet = walletRepository.observeWallet().filterNotNull().first()
            val firstAccount = wallet.accounts.firstOrNull()
            _state.update { 
                it.copy(
                    wallet = wallet,
                    accounts = wallet.accounts,
                    selectedAccount = firstAccount, 
                    isLoading = false,
                    isBiometricEnabled = walletRepository.isBiometricEnabled()
                ) 
            }
            firstAccount?.let { fetchBalance(it) }
        }
    }

    fun onSelectAccount(account: Account) {
        _state.update { it.copy(selectedAccount = account, validatorAddressError = null) }
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

    fun onValidatorAddressChange(address: String) {
        _state.update { it.copy(validatorAddress = address.trim(), validatorAddressError = null) }
    }

    fun onValidatorPublicKeyChange(publicKey: String) {
        _state.update { it.copy(validatorPublicKey = publicKey.trim()) }
    }

    fun onAmountChange(amount: String) {
        _state.update { it.copy(amountText = amount, amountError = null) }
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

    fun onMemoChange(memo: String) {
        _state.update { it.copy(memo = memo) }
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
                // Bond transactions usually have same fee logic as transfers in Pactus?
                // rpc.calculateFee(..., payload_type=2) could be used if RpcCaller supported it.
                // For now use general calculateFee.
                val fee = blockchainRepository.calculateFee(amount)
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

        if (current.validatorAddress.isBlank() || !current.validatorAddress.startsWith("pc1p")) {
            _state.update { it.copy(validatorAddressError = "Invalid validator address (pc1p...)") }
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
        if (_state.value.isStaking) return
        _state.update { it.copy(isConfirmVisible = false, password = "", passwordError = null) }
    }

    fun onConfirmStake() {
        val current = _state.value
        if (current.isStaking) return

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

        doStake(from, current.validatorAddress, current.validatorPublicKey, amount, fee, current.memo, password)
    }

    fun onBiometricUnlockSuccess(password: String) {
        val current = _state.value
        val from = current.selectedAccount?.address ?: return
        val amount = current.parsedAmount ?: return
        val fee = current.parsedFee ?: current.estimatedFee ?: return
        
        doStake(from, current.validatorAddress, current.validatorPublicKey, amount, fee, current.memo, password)
    }

    private fun doStake(from: String, to: String, pubKey: String, amount: Amount, fee: Amount, memo: String, password: String) {
        _state.update { it.copy(isStaking = true, passwordError = null) }
        viewModelScope.launch {
            try {
                val unlocked = walletRepository.unlockWallet(password)
                if (!unlocked) {
                    _state.update { it.copy(isStaking = false, passwordError = "Incorrect password") }
                    return@launch
                }
                
                val rawTx = blockchainRepository.buildBondTransaction(
                    from = from,
                    to = to,
                    stake = amount,
                    publicKey = pubKey.takeIf { it.isNotBlank() },
                    fee = fee,
                    memo = memo.takeIf { it.isNotBlank() }
                )

                val signedHex = walletRepository.signRawTransaction(
                    rawHex = rawTx.hex,
                    signingMessageHex = rawTx.signingMessageHex,
                    fromAddress = from,
                )
                val txId = blockchainRepository.broadcastTransaction(signedHex)

                _state.update { it.copy(isStaking = false, isConfirmVisible = false) }
                _effect.send(Effect.ShowSuccess(txId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isStaking = false) }
                _effect.send(Effect.ShowError(e.message ?: "Staking failed"))
            }
        }
    }
}


