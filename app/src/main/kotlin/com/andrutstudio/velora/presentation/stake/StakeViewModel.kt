package com.andrutstudio.velora.presentation.stake

import com.andrutstudio.velora.presentation.components.formatPac
import com.andrutstudio.velora.presentation.components.uiText
import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.components.toUiText
import com.andrutstudio.velora.presentation.components.UiText
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
import com.andrutstudio.velora.domain.model.Delegation
import com.andrutstudio.velora.domain.model.DelegationRules
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.model.ValidatorInfo
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

    /** What the confirm sheet signs: a bond (plain or delegated) or the Stake Owner's unbond. */
    enum class Action { BOND, UNBOND }

    /** Delegation term: months from [DelegationRules.TERM_DAYS], a picked date or a typed block. */
    sealed interface Term {
        data class Months(val months: Int) : Term
        data class Date(val epochMillis: Long?) : Term
        data object Block : Term
    }

    data class State(
        val wallet: Wallet? = null,
        val accounts: List<Account> = emptyList(),
        val selectedAccount: Account? = null,
        val availableBalance: Amount = Amount.ZERO,
        val validatorAddress: String = "",
        // The node's view of the validator (null: never bonded, or not looked up yet)
        val validatorInfo: ValidatorInfo? = null,
        val validatorLookedUp: Boolean = false,
        val validatorMoniker: String = "",
        val height: Long = 0,
        val maxStakableAmount: Amount? = null,
        val validatorPublicKey: String = "",
        val amountText: String = "",
        val memo: String = "",
        val estimatedFee: Amount? = null,
        val feeText: String = "",
        val isFeeManual: Boolean = false,
        val isFeeLoading: Boolean = false,
        val validatorAddressError: UiText? = null,
        val amountError: UiText? = null,
        val feeError: UiText? = null,
        val isConfirmVisible: Boolean = false,
        val action: Action = Action.BOND,
        val isStaking: Boolean = false,
        val isLoading: Boolean = true,
        val password: String = "",
        val passwordError: UiText? = null,
        // Shown inside the confirm sheet; a snackbar would be hidden behind the modal sheet.
        val stakeError: UiText? = null,
        val isBiometricEnabled: Boolean = false,
        // PIP-49 delegated bond
        val delegate: Boolean = false,
        val sharePercentText: String = "20",
        val term: Term = Term.Months(6),
        val customBlockText: String = "",
        val delegationError: UiText? = null,
    ) {
        val network: Network get() = wallet?.network ?: Network.MAINNET

        val validatorStake: Amount? get() = validatorInfo?.stake

        val parsedAmount: Amount?
            get() = amountText.toDoubleOrNull()?.takeIf { it > 0 }?.let { Amount.fromPac(it) }

        /** A delegated bond always stakes the maximum, 1,000 PAC. */
        val stakeAmount: Amount?
            get() = if (delegate) Amount.fromNanoPac(DelegationRules.MAX_STAKE_NANO_PAC) else parsedAmount

        val parsedFee: Amount?
            get() = feeText.toDoubleOrNull()?.takeIf { it > 0 }?.let { Amount.fromPac(it) }

        val sharePercent: Double?
            get() = sharePercentText.replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..DelegationRules.MAX_SHARE_PERCENT }

        /** The delegation's last block, or null while it cannot be computed. */
        val expiry: Long?
            get() {
                if (height <= 0) return null
                val block = when (val t = term) {
                    is Term.Months -> DelegationRules.expiryForDays(height, DelegationRules.TERM_DAYS.getValue(t.months))
                    is Term.Date -> t.epochMillis?.let {
                        val days = Math.ceil((it - System.currentTimeMillis()) / 86_400_000.0).toInt()
                        if (days > 0) DelegationRules.expiryForDays(height, days) else null
                    }
                    Term.Block -> customBlockText.trim().toLongOrNull()
                }
                return block?.takeIf { it > height }
            }

        /** The validator is a delegation owned by one of this wallet's accounts. */
        val ownedDelegation: ValidatorInfo?
            get() = validatorInfo?.takeIf { v -> v.delegated && accounts.any { it.address == v.delegateOwner } }

        val canPreview: Boolean
            get() = validatorAddress.isNotBlank() && stakeAmount != null && !isStaking &&
                    (parsedFee != null || estimatedFee != null)
    }

    sealed interface Effect {
        data class ShowSuccess(val txId: String) : Effect
        data class ShowError(val message: UiText) : Effect
        data object NavigateBack : Effect
        data object RequestBiometricUnlock : Effect
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var feeJob: Job? = null
    private var validatorJob: Job? = null

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
        val trimmed = address.trim()
        _state.update {
            it.copy(
                validatorAddress = trimmed, validatorAddressError = null,
                validatorInfo = null, validatorLookedUp = false, validatorMoniker = "",
            )
        }
        lookUpValidator(trimmed)
    }

    // The node is the source of truth for the bond rules (known validator,
    // delegation); the explorer only adds the moniker
    private fun lookUpValidator(address: String) {
        validatorJob?.cancel()
        if (!DelegationRules.isValidatorAddress(address, _state.value.network)) return
        validatorJob = viewModelScope.launch {
            delay(500)
            try {
                val info = blockchainRepository.getValidator(address)
                val height = blockchainRepository.getBlockHeight()
                _state.update { it.copy(validatorInfo = info, validatorLookedUp = true, height = height) }
                val moniker = runCatching { blockchainRepository.getValidatorPeer(address).peer?.moniker }.getOrNull()
                if (!moniker.isNullOrBlank()) _state.update { it.copy(validatorMoniker = moniker) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(validatorInfo = null, validatorLookedUp = false) }
            }
        }
    }

    fun onValidatorPublicKeyChange(publicKey: String) {
        _state.update { it.copy(validatorPublicKey = publicKey.trim()) }
    }

    fun onAmountChange(amount: String) {
        _state.update { it.copy(amountText = amount, amountError = null) }
        scheduleFeeEstimate()
    }

    fun onDelegateChange(delegate: Boolean) {
        _state.update { it.copy(delegate = delegate, amountError = null, delegationError = null) }
        scheduleFeeEstimate()
    }

    fun onSharePercentChange(value: String) {
        _state.update { it.copy(sharePercentText = value, delegationError = null) }
    }

    fun onTermChange(term: Term) {
        _state.update { it.copy(term = term, delegationError = null) }
    }

    fun onCustomBlockChange(value: String) {
        _state.update { it.copy(customBlockText = value.filter(Char::isDigit), delegationError = null) }
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
        _state.update { it.copy(password = value, passwordError = null, stakeError = null) }
    }

    private fun scheduleFeeEstimate() {
        feeJob?.cancel()
        feeJob = viewModelScope.launch {
            delay(700)
            val amount = _state.value.stakeAmount ?: return@launch
            _state.update { it.copy(isFeeLoading = true, feeError = null) }
            try {
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
                        feeError = UiText.Res(R.string.error_fee_estimation),
                    )
                }
            }
        }
    }

    fun onPreview() {
        val current = _state.value
        var hasError = false

        if (!DelegationRules.isValidatorAddress(current.validatorAddress, current.network)) {
            _state.update { it.copy(validatorAddressError = UiText.Res(R.string.stake_error_invalid_validator)) }
            hasError = true
        } else if (current.validatorInfo?.isUnbonded == true) {
            _state.update { it.copy(validatorAddressError = UiText.Res(R.string.stake_error_unbonded_validator)) }
            hasError = true
        }

        val amount = current.stakeAmount
        if (amount == null) {
            _state.update { it.copy(amountError = UiText.Res(R.string.error_enter_valid_amount)) }
            hasError = true
        }

        if (current.delegate) {
            val error = when {
                current.sharePercent == null -> UiText.Res(R.string.stake_error_invalid_share)
                current.height <= 0 -> UiText.Res(R.string.stake_term_needs_validator)
                current.term == Term.Block && (current.customBlockText.toLongOrNull() ?: 0) <= current.height ->
                    uiText(R.string.stake_error_block_too_low, current.height.toString())
                current.expiry == null -> UiText.Res(R.string.stake_error_pick_term)
                (current.validatorInfo?.stake?.nanoPac ?: 0) > 0 -> UiText.Res(R.string.stake_error_delegate_needs_empty)
                else -> null
            }
            if (error != null) {
                _state.update { it.copy(delegationError = error) }
                hasError = true
            }
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

            // Check validator stake limit (1000 PAC)
            current.validatorStake?.let { currentStake ->
                if (!current.delegate && currentStake.nanoPac + amount.nanoPac > DelegationRules.MAX_STAKE_NANO_PAC) {
                    val remaining = Amount.fromNanoPac(DelegationRules.MAX_STAKE_NANO_PAC - currentStake.nanoPac)
                    _state.update { it.copy(
                        amountError = uiText(R.string.stake_max_error, formatPac(remaining)),
                        maxStakableAmount = remaining
                    ) }
                    return
                }
            }

            openConfirm(Action.BOND)
        }
    }

    /** The Stake Owner unbonds a delegated validator once the delegation has ended. */
    fun onUnbondOwned() {
        val owned = _state.value.ownedDelegation ?: return
        if (_state.value.height < owned.delegateExpiry) return
        openConfirm(Action.UNBOND)
    }

    private fun openConfirm(action: Action) {
        _state.update {
            it.copy(
                isConfirmVisible = true, action = action, password = "", passwordError = null,
                stakeError = null, maxStakableAmount = null,
            )
        }
    }

    fun onDismissConfirm() {
        if (_state.value.isStaking) return
        _state.update { it.copy(isConfirmVisible = false, password = "", passwordError = null, stakeError = null) }
    }

    fun onConfirmStake() {
        val current = _state.value
        if (current.isStaking) return
        if (current.password.isEmpty()) {
            if (current.isBiometricEnabled) {
                viewModelScope.launch { _effect.send(Effect.RequestBiometricUnlock) }
            } else {
                _state.update { it.copy(passwordError = UiText.Res(R.string.error_enter_wallet_password)) }
            }
            return
        }
        submit(current.password)
    }

    fun onBiometricUnlockSuccess(password: String) = submit(password)

    private fun submit(password: String) {
        val current = _state.value
        _state.update { it.copy(isStaking = true, passwordError = null, stakeError = null) }
        viewModelScope.launch {
            try {
                if (!walletRepository.unlockWallet(password)) {
                    _state.update { it.copy(isStaking = false, passwordError = UiText.Res(R.string.error_incorrect_password)) }
                    return@launch
                }
                val txId = if (current.action == Action.UNBOND) unbondOwned(current) else bond(current)
                _state.update { it.copy(isStaking = false, isConfirmVisible = false) }
                _effect.send(Effect.ShowSuccess(txId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isStaking = false, stakeError = e.toUiText(R.string.error_staking_failed)) }
            }
        }
    }

    private suspend fun bond(current: State): String {
        val from = checkNotNull(current.selectedAccount).address
        val amount = checkNotNull(current.stakeAmount)
        val fee = checkNotNull(current.parsedFee ?: current.estimatedFee)
        val delegation = if (current.delegate) {
            val share = DelegationRules.percentToShare(checkNotNull(current.sharePercent))
            DelegationRules.checkShare(share)
            val expiry = checkNotNull(current.expiry)
            check(expiry > blockchainRepository.getBlockHeight()) { "The delegation must end after the current block" }
            Delegation(owner = from, shareNanoPac = share, expiry = expiry)
        } else null
        val rawTx = blockchainRepository.buildBondTransaction(
            from = from,
            to = current.validatorAddress,
            stake = amount,
            // The node refuses a public key for a validator it already knows
            publicKey = current.validatorPublicKey.takeIf { it.isNotBlank() && current.validatorInfo == null },
            fee = fee,
            memo = current.memo.takeIf { it.isNotBlank() },
            delegation = delegation,
        )
        val signedHex = walletRepository.signRawTransaction(rawTx.hex, rawTx.signingMessageHex, from)
        return blockchainRepository.broadcastTransaction(signedHex)
    }

    private suspend fun unbondOwned(current: State): String {
        // Read the validator again: only its Stake Owner may unbond, after the expiry
        val info = checkNotNull(blockchainRepository.getValidator(current.validatorAddress)) { "Validator not found" }
        check(info.delegated && current.accounts.any { it.address == info.delegateOwner }) {
            "Only the Stake Owner of this validator can unbond it"
        }
        check(info.delegateExpiry <= blockchainRepository.getBlockHeight()) {
            "The delegation ends at block ${info.delegateExpiry}; unbonding opens after it"
        }
        val rawTx = blockchainRepository.buildUnbondTransaction(
            validator = current.validatorAddress,
            memo = current.memo.takeIf { it.isNotBlank() },
            delegateOwner = info.delegateOwner,
        )
        val signedHex = walletRepository.signRawTransaction(rawTx.hex, rawTx.signingMessageHex, info.delegateOwner)
        return blockchainRepository.broadcastTransaction(signedHex)
    }
}
