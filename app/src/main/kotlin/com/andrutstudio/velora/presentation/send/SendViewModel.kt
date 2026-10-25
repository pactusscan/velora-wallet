package com.andrutstudio.velora.presentation.send

import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.components.toUiText
import com.andrutstudio.velora.presentation.components.UiText
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
import com.andrutstudio.velora.domain.repository.AddressBookRepository
import com.andrutstudio.velora.domain.repository.SavedMemoRepository
import com.andrutstudio.velora.data.crypto.PactusAddress
import com.andrutstudio.velora.data.local.db.AddressBookEntity
import com.andrutstudio.velora.data.local.db.SavedMemoEntity
import com.andrutstudio.velora.domain.usecase.SendTransactionUseCase
import com.andrutstudio.velora.presentation.components.formatPacInput
import javax.inject.Inject

@HiltViewModel
class SendViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val blockchainRepository: BlockchainRepository,
    private val addressBookRepository: AddressBookRepository,
    private val savedMemoRepository: SavedMemoRepository,
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
        val toAddressError: UiText? = null,
        val amountError: UiText? = null,
        val feeError: UiText? = null,
        val isConfirmVisible: Boolean = false,
        val isSending: Boolean = false,
        val isLoading: Boolean = true,
        val password: String = "",
        val passwordError: UiText? = null,
        // Shown inside the confirm sheet; a snackbar would be hidden behind the modal sheet.
        val sendError: UiText? = null,
        val isBiometricEnabled: Boolean = false,
        val addressBook: List<AddressBookEntity> = emptyList(),
        val isAddressBookVisible: Boolean = false,
        val savedMemos: List<SavedMemoEntity> = emptyList(),
        val isSavedMemosVisible: Boolean = false,
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
        data class ShowError(val message: UiText) : Effect
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
        observeAddressBook()
        observeSavedMemos()
    }

    private fun observeAddressBook() {
        viewModelScope.launch {
            addressBookRepository.getAddresses().collect { list ->
                _state.update { it.copy(addressBook = list) }
            }
        }
    }

    private fun observeSavedMemos() {
        viewModelScope.launch {
            savedMemoRepository.getMemos().collect { list ->
                _state.update { it.copy(savedMemos = list) }
            }
        }
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
        _state.update { it.copy(password = value, passwordError = null, sendError = null) }
    }

    fun onAddressBookClick() {
        _state.update { it.copy(isAddressBookVisible = true) }
    }

    fun onDismissAddressBook() {
        _state.update { it.copy(isAddressBookVisible = false) }
    }

    fun onSelectAddress(address: String) {
        _state.update { it.copy(toAddress = address, isAddressBookVisible = false, toAddressError = null) }
    }

    fun onAddAddress(label: String, address: String) {
        viewModelScope.launch {
            addressBookRepository.addAddress(label, address)
        }
    }

    fun onDeleteAddress(entity: AddressBookEntity) {
        viewModelScope.launch {
            addressBookRepository.deleteAddress(entity)
        }
    }

    fun onSavedMemosClick() {
        _state.update { it.copy(isSavedMemosVisible = true) }
    }

    fun onDismissSavedMemos() {
        _state.update { it.copy(isSavedMemosVisible = false) }
    }

    fun onSelectMemo(text: String) {
        _state.update { it.copy(memo = text, isSavedMemosVisible = false) }
    }

    fun onAddMemo(text: String) {
        viewModelScope.launch {
            savedMemoRepository.addMemo(text)
        }
    }

    fun onDeleteMemo(entity: SavedMemoEntity) {
        viewModelScope.launch {
            savedMemoRepository.deleteMemo(entity)
        }
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
                        feeError = UiText.Res(R.string.error_fee_estimation),
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
            _state.update { it.copy(amountError = UiText.Res(R.string.error_enter_valid_amount)) }
            hasError = true
        }

        val fee = current.parsedFee
        if (fee == null) {
            if (!current.isFeeLoading) {
                _state.update { it.copy(feeError = UiText.Res(R.string.error_enter_fee)) }
                hasError = true
            }
        }

        if (!hasError && amount != null && fee != null) {
            val total = amount.nanoPac + fee.nanoPac
            if (total > current.availableBalance.nanoPac) {
                _state.update { it.copy(amountError = UiText.Res(R.string.error_insufficient_balance)) }
                return
            }
            _state.update { it.copy(isConfirmVisible = true, password = "", passwordError = null, sendError = null) }
        }
    }

    fun onDismissConfirm() {
        if (_state.value.isSending) return
        _state.update { it.copy(isConfirmVisible = false, password = "", passwordError = null, sendError = null) }
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
                _state.update { it.copy(passwordError = UiText.Res(R.string.error_enter_wallet_password)) }
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
        _state.update { it.copy(isSending = true, passwordError = null, sendError = null) }
        viewModelScope.launch {
            try {
                val unlocked = walletRepository.unlockWallet(password)
                if (!unlocked) {
                    _state.update { it.copy(isSending = false, passwordError = UiText.Res(R.string.error_incorrect_password)) }
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
                _state.update { it.copy(isSending = false, sendError = e.toUiText(R.string.error_transaction_failed)) }
            }
        }
    }

    private fun validateRecipient(address: String, from: String?): UiText? {
        if (address.isBlank()) return UiText.Res(R.string.send_error_recipient_required)
        if (!isValidAccountAddress(address)) return UiText.Res(R.string.send_error_invalid_address)
        if (from != null && address.equals(from, ignoreCase = false)) {
            return UiText.Res(R.string.send_error_own_address)
        }
        return null
    }

    // Accepts pc1p / pc1z / pc1r / pc1y with full bech32m checksum validation.
    private fun isValidAccountAddress(address: String): Boolean =
        PactusAddress.isValidAccountAddress(address)
}


