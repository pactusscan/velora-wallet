package com.andrutstudio.velora.presentation.anchor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.AnchorInfo
import com.andrutstudio.velora.domain.model.AnchorRequest
import com.andrutstudio.velora.domain.model.AnchorRules
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import com.andrutstudio.velora.domain.repository.WalletRepository
import com.andrutstudio.velora.domain.usecase.SendAnchorUseCase
import com.andrutstudio.velora.presentation.components.UiText
import com.andrutstudio.velora.presentation.components.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
import javax.inject.Inject

// The state comes from the node, never from past transactions.
@HiltViewModel
class AnchorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val blockchainRepository: BlockchainRepository,
    private val sendAnchor: SendAnchorUseCase,
) : ViewModel() {

    enum class Mode { VIEW, PUBLISH }

    data class State(
        val account: Account? = null,
        val canSign: Boolean = false,
        val isLoading: Boolean = true,
        val loadError: Boolean = false,
        val anchor: AnchorInfo? = null,
        val balance: Amount = Amount.ZERO,
        val mode: Mode = Mode.VIEW,
        val rootHash: String = "",
        val manifestUri: String = "",
        val anchorType: Int = 0,
        val memo: String = "",
        // PIP-57: the total the anchor should hold, in PAC
        val targetText: String = "1",
        val rootError: UiText? = null,
        val uriError: UiText? = null,
        val memoError: UiText? = null,
        val isPreparing: Boolean = false,
        val formError: UiText? = null,
        // Non-null while the confirm sheet is open
        val pending: AnchorRequest? = null,
        val password: String = "",
        val passwordError: UiText? = null,
        val sendError: UiText? = null,
        val isSending: Boolean = false,
        val isBiometricEnabled: Boolean = false,
    ) {
        val lockedNow: Amount get() = anchor?.lockedDeposit ?: Amount.ZERO

        /** What this transaction adds to reach the target, or null for an invalid target. */
        val deposit: Amount?
            get() = AnchorRules.parsePac(targetText)
                ?.let { Amount.fromNanoPac(AnchorRules.depositToAdd(it, lockedNow.nanoPac)) }
    }

    sealed interface Effect {
        data object Sent : Effect
        data object RequestBiometricUnlock : Effect
    }

    private val address: String = checkNotNull(savedStateHandle["address"])

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            val wallet = walletRepository.observeWallet().filterNotNull().first()
            _state.update {
                it.copy(
                    account = wallet.accounts.find { account -> account.address == address },
                    canSign = !wallet.isWatchOnly,
                    isBiometricEnabled = walletRepository.isBiometricEnabled(),
                )
            }
        }
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, loadError = false) }
        viewModelScope.launch {
            try {
                val anchor = blockchainRepository.getAnchor(address)
                val balance = blockchainRepository.getBalance(address)
                _state.update { it.copy(anchor = anchor, balance = balance, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, loadError = true) }
            }
        }
    }

    fun onStartPublish() {
        _state.update {
            it.copy(
                mode = Mode.PUBLISH, rootHash = "", manifestUri = "", anchorType = 0, memo = "", targetText = "1",
                rootError = null, uriError = null, memoError = null, formError = null,
            )
        }
    }

    fun onCancelPublish() {
        _state.update { it.copy(mode = Mode.VIEW) }
    }

    fun onRootHashChange(value: String) {
        _state.update { it.copy(rootHash = value, rootError = null, formError = null) }
    }

    fun onManifestUriChange(value: String) {
        _state.update { it.copy(manifestUri = value, uriError = null, formError = null) }
    }

    fun onMemoChange(value: String) {
        _state.update { it.copy(memo = value, memoError = null, formError = null) }
    }

    fun onTargetChange(value: String) {
        _state.update { it.copy(targetText = value, formError = null) }
    }

    fun onAnchorTypeChange(value: Int) {
        _state.update { it.copy(anchorType = value) }
    }

    fun onReviewPublish() {
        val current = _state.value
        val root = AnchorRules.normalizeRoot(current.rootHash)
        val uri = current.manifestUri.trim()
        val memo = current.memo.trim()
        _state.update {
            it.copy(
                rootError = if (root == null) UiText.Res(R.string.anchor_error_root) else null,
                uriError = if (AnchorRules.isValidUri(uri)) null else UiText.Res(R.string.anchor_error_uri),
                memoError = if (AnchorRules.isValidMemo(memo)) null else UiText.Res(R.string.anchor_error_memo),
            )
        }
        if (root == null || !AnchorRules.isValidUri(uri) || !AnchorRules.isValidMemo(memo)) return
        val deposit = current.deposit ?: run {
            _state.update { it.copy(formError = UiText.Res(R.string.anchor_error_target)) }
            return
        }
        prepare(
            AnchorRequest(
                sender = address,
                isDelete = false,
                rootHash = root,
                manifestUri = uri,
                anchorType = current.anchorType,
                deposit = deposit,
                memo = memo,
            ),
        )
    }

    fun onReviewDelete() = prepare(AnchorRequest(sender = address, isDelete = true))

    private fun prepare(request: AnchorRequest) {
        _state.update { it.copy(isPreparing = true, formError = null) }
        viewModelScope.launch {
            try {
                val fee = sendAnchor.estimateFee(request.deposit)
                if (request.isDelete) {
                    // The fee comes out of the refund, so the locked deposit must cover it
                    val locked = blockchainRepository.getAnchor(address)?.lockedDeposit?.nanoPac ?: 0L
                    if (!AnchorRules.canDelete(locked, fee.nanoPac)) {
                        _state.update { it.copy(isPreparing = false, formError = UiText.Res(R.string.anchor_error_delete_fee)) }
                        return@launch
                    }
                }
                val balance = blockchainRepository.getBalance(address)
                if (request.deposit.nanoPac + fee.nanoPac > balance.nanoPac) {
                    _state.update {
                        it.copy(isPreparing = false, formError = UiText.Res(R.string.error_insufficient_balance))
                    }
                    return@launch
                }
                _state.update {
                    it.copy(
                        isPreparing = false,
                        pending = request.copy(fee = fee),
                        password = "",
                        passwordError = null,
                        sendError = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isPreparing = false, formError = UiText.Res(R.string.error_fee_estimation))
                }
            }
        }
    }

    fun onDismissConfirm() {
        if (_state.value.isSending) return
        _state.update { it.copy(pending = null, password = "") }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, passwordError = null, sendError = null) }
    }

    fun onConfirm() {
        val current = _state.value
        if (current.isSending) return
        when {
            current.password.isNotEmpty() -> send(current.password)
            current.isBiometricEnabled -> viewModelScope.launch { _effect.send(Effect.RequestBiometricUnlock) }
            else -> _state.update { it.copy(passwordError = UiText.Res(R.string.error_enter_wallet_password)) }
        }
    }

    fun onBiometricUnlockSuccess(password: String) = send(password)

    private fun send(password: String) {
        val request = _state.value.pending ?: return
        _state.update { it.copy(isSending = true, passwordError = null, sendError = null) }
        viewModelScope.launch {
            try {
                if (!walletRepository.unlockWallet(password)) {
                    _state.update {
                        it.copy(isSending = false, passwordError = UiText.Res(R.string.error_incorrect_password))
                    }
                    return@launch
                }
                sendAnchor(request)
                _state.update { it.copy(isSending = false, pending = null, password = "", mode = Mode.VIEW) }
                _effect.send(Effect.Sent)
                delay(BLOCK_MS)
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSending = false, sendError = e.toUiText(R.string.error_transaction_failed)) }
            }
        }
    }

    private companion object {
        const val BLOCK_MS = 12_000L
    }
}
