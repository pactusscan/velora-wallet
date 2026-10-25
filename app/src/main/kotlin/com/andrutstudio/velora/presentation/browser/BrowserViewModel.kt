package com.andrutstudio.velora.presentation.browser

import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.components.toUiText
import com.andrutstudio.velora.presentation.components.UiText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.andrutstudio.velora.data.local.db.BookmarkEntity
import com.andrutstudio.velora.data.local.db.BrowserDao
import com.andrutstudio.velora.data.local.db.BrowserHistoryEntity
import com.andrutstudio.velora.data.crypto.SignGuard
import com.andrutstudio.velora.data.local.db.ConnectedSiteEntity
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.repository.WalletRepository
import com.andrutstudio.velora.domain.usecase.SendTransactionUseCase
import com.andrutstudio.velora.domain.usecase.SignerPick
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val sendTransaction: SendTransactionUseCase,
    private val browserDao: BrowserDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    data class State(
        val url: String = HOME_URL,
        val urlBarText: String = HOME_URL,
        val pageTitle: String = "",
        val isLoading: Boolean = false,
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false,
        val isBookmarked: Boolean = false,
        val loadingProgress: Int = 0,
        val accounts: List<Account> = emptyList(),
        val network: Network = Network.MAINNET,
        val history: List<BrowserHistoryEntity> = emptyList(),
        val bookmarks: List<BookmarkEntity> = emptyList(),
        val isDrawerOpen: Boolean = false,
        val pendingConnectRequest: PendingConnectRequest? = null,
        val pendingSignRequest: PendingSignRequest? = null,
        val pendingSignMessageRequest: PendingSignMessageRequest? = null,
        val isSigning: Boolean = false,
        // Signing needs the unlocked wallet. When it is still locked, confirming a
        // sign sheet opens the unlock dialog and resumes the signing afterwards.
        val showUnlockDialog: Boolean = false,
        val isUnlocking: Boolean = false,
        val unlockError: UiText? = null,
        val isBiometricEnabled: Boolean = false,
    )

    data class PendingConnectRequest(
        val requestId: String,
        val host: String,
        val address: String,
    )

    // needsConnect mirrors the extension: a sign request from a host the user
    // has not approved yet becomes a combined connect + sign approval, and the
    // host is granted only after the user confirms.
    data class PendingSignRequest(
        val requestId: String,
        val host: String,
        val needsConnect: Boolean,
        val fromAddress: String,
        val to: String,
        val amountNanoPac: Long,
        val feeNanoPac: Long,
        val memo: String,
    )

    // fromAddress is the account that will sign. When the dApp named it
    // (signMessage(message, { address })) signerFixed is true and the sheet
    // shows it without a picker; otherwise signerOptions lists the accounts the
    // user can choose from.
    data class PendingSignMessageRequest(
        val requestId: String,
        val host: String,
        val needsConnect: Boolean,
        val fromAddress: String,
        val message: String,
        val signerFixed: Boolean = false,
        val signerOptions: List<Account> = emptyList(),
    )

    sealed interface Effect {
        data class LoadUrl(val url: String) : Effect
        data object GoBack : Effect
        data object GoForward : Effect
        data object Reload : Effect
        data class OpenExternal(val url: String) : Effect
        data class ResolveConnect(val requestId: String) : Effect
        data class ResolveSign(val requestId: String, val txId: String) : Effect
        data class RejectSign(val requestId: String, val error: String) : Effect
        data class ShowSnackbar(val message: UiText) : Effect
        data object RequestBiometricUnlock : Effect
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // The exact request the user confirmed before the unlock dialog opened.
    // Signing resumes after unlock only if that same request is still pending.
    private var approvedBeforeUnlock: Any? = null

    init {
        val initialUrl = savedStateHandle.get<String>("url").orEmpty()
        val startUrl = initialUrl.ifBlank { HOME_URL }
        val displayUrl = if (startUrl == HOME_URL) "" else startUrl
        _state.update { it.copy(url = startUrl, urlBarText = displayUrl) }

        viewModelScope.launch {
            walletRepository.observeWallet().filterNotNull().first().let { wallet ->
                _state.update {
                    it.copy(
                        accounts = wallet.accounts,
                        network = wallet.network,
                        isBiometricEnabled = walletRepository.isBiometricEnabled(),
                    )
                }
            }
        }

        viewModelScope.launch {
            browserDao.observeHistory().collect { history ->
                _state.update { it.copy(history = history) }
            }
        }

        viewModelScope.launch {
            browserDao.observeBookmarks().collect { bookmarks ->
                if (bookmarks.isEmpty()) seedPresetBookmarks()
                _state.update { it.copy(bookmarks = bookmarks) }
            }
        }

        viewModelScope.launch { _effect.send(Effect.LoadUrl(startUrl)) }
    }

    private suspend fun seedPresetBookmarks() {
        listOf(
            BookmarkEntity(url = "https://pactus.org", title = "Pactus", isPreset = true),
            BookmarkEntity(url = "https://pactusscan.com", title = "Pactus Explorer", isPreset = true),
            BookmarkEntity(url = "https://wrapto.app", title = "Wrapto", isPreset = true),
            BookmarkEntity(url = "https://docs.pactus.org", title = "Pactus Docs", isPreset = true),
        ).forEach { browserDao.insertBookmark(it) }
    }

    fun onUrlBarChanged(text: String) {
        _state.update { it.copy(urlBarText = text) }
    }

    fun onUrlSubmitted(text: String) {
        val url = sanitizeUrl(text)
        _state.update { it.copy(urlBarText = url) }
        viewModelScope.launch { _effect.send(Effect.LoadUrl(url)) }
    }

    fun onPageStarted(url: String) {
        val displayUrl = if (url == HOME_URL) "" else url
        _state.update { it.copy(url = url, urlBarText = displayUrl, isLoading = true, loadingProgress = 0) }
    }

    fun onPageFinished(url: String, title: String) {
        _state.update { it.copy(url = url, isLoading = false, pageTitle = title, loadingProgress = 100) }
        viewModelScope.launch {
            browserDao.insertHistory(BrowserHistoryEntity(url = url, title = title.ifBlank { url }))
            _state.update { it.copy(isBookmarked = browserDao.isBookmarked(url)) }
        }
    }

    fun onProgressChanged(progress: Int) {
        _state.update { it.copy(loadingProgress = progress) }
    }

    fun onNavStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _state.update { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    fun onBackClicked() { viewModelScope.launch { _effect.send(Effect.GoBack) } }
    fun onForwardClicked() { viewModelScope.launch { _effect.send(Effect.GoForward) } }
    fun onReloadClicked() { viewModelScope.launch { _effect.send(Effect.Reload) } }

    fun onToggleBookmark() {
        val url = _state.value.url
        val title = _state.value.pageTitle.ifBlank { url }
        viewModelScope.launch {
            if (_state.value.isBookmarked) {
                val bookmark = _state.value.bookmarks.find { it.url == url } ?: return@launch
                browserDao.deleteBookmark(bookmark)
                _state.update { it.copy(isBookmarked = false) }
            } else {
                browserDao.insertBookmark(BookmarkEntity(url = url, title = title))
                _state.update { it.copy(isBookmarked = true) }
                _effect.send(Effect.ShowSnackbar(UiText.Res(R.string.browser_bookmarked)))
            }
        }
    }

    fun onToggleDrawer() {
        _state.update { it.copy(isDrawerOpen = !it.isDrawerOpen) }
    }

    fun onCloseDrawer() {
        _state.update { it.copy(isDrawerOpen = false) }
    }

    fun onBookmarkClicked(url: String) {
        _state.update { it.copy(isDrawerOpen = false, urlBarText = url) }
        viewModelScope.launch { _effect.send(Effect.LoadUrl(url)) }
    }

    fun onHistoryItemClicked(url: String) {
        _state.update { it.copy(isDrawerOpen = false, urlBarText = url) }
        viewModelScope.launch { _effect.send(Effect.LoadUrl(url)) }
    }

    fun onClearHistory() {
        viewModelScope.launch { browserDao.clearHistory() }
    }

    fun onExternalLinkRequested(url: String) {
        viewModelScope.launch { _effect.send(Effect.OpenExternal(url)) }
    }

    // Called from JS bridge (may be on any thread — MutableStateFlow + Channel are thread-safe)
    fun getAccountsJson(): String {
        val accounts = _state.value.accounts
        return buildString {
            append("[")
            accounts.forEachIndexed { i, acc ->
                if (i > 0) append(",")
                append("""{"address":"${acc.address}","type":"${acc.type.name}","label":"${acc.label}"}""")
            }
            append("]")
        }
    }

    fun getNetworkName(): String {
        return _state.value.network.name.lowercase()
    }

    // dApp asked to connect (window.pactus.connect()). Hosts the user already
    // approved (Settings > Connected sites) are auto-approved; anyone else gets
    // the approval sheet. Approving hands the accounts back to
    // the page and remembers the host.
    fun onConnectRequest(requestId: String) {
        val address = _state.value.accounts.firstOrNull()?.address ?: run {
            viewModelScope.launch { _effect.send(Effect.RejectSign(requestId, "No account available")) }
            return
        }
        val host = currentHost()
        viewModelScope.launch {
            if (host.isNotBlank() && browserDao.isSiteConnected(host)) {
                _effect.send(Effect.ResolveConnect(requestId))
            } else {
                _state.update {
                    it.copy(pendingConnectRequest = PendingConnectRequest(requestId, host, address))
                }
            }
        }
    }

    private fun currentHost(): String =
        runCatching { java.net.URI(_state.value.url).host }.getOrNull() ?: _state.value.url

    fun onConfirmConnect() {
        val req = _state.value.pendingConnectRequest ?: return
        _state.update { it.copy(pendingConnectRequest = null) }
        viewModelScope.launch {
            if (req.host.isNotBlank()) browserDao.insertConnectedSite(ConnectedSiteEntity(host = req.host))
            _effect.send(Effect.ResolveConnect(req.requestId))
        }
    }

    fun onCancelConnect() {
        val req = _state.value.pendingConnectRequest ?: return
        _state.update { it.copy(pendingConnectRequest = null) }
        viewModelScope.launch { _effect.send(Effect.RejectSign(req.requestId, "User rejected")) }
    }

    fun onSignRequest(requestId: String, to: String, amountNanoPac: String, feeNanoPac: String, memo: String) {
        val fromAddress = _state.value.accounts.firstOrNull()?.address ?: run {
            viewModelScope.launch { _effect.send(Effect.RejectSign(requestId, "No account available")) }
            return
        }
        val amount = amountNanoPac.toLongOrNull() ?: 0L
        val fee = feeNanoPac.toLongOrNull() ?: 0L
        val host = currentHost()
        viewModelScope.launch {
            val connected = host.isNotBlank() && browserDao.isSiteConnected(host)
            if (hasPendingSignRequest()) {
                _effect.send(Effect.RejectSign(requestId, ERR_REQUEST_PENDING))
                return@launch
            }
            _state.update {
                it.copy(
                    pendingSignRequest = PendingSignRequest(
                        requestId = requestId,
                        host = host,
                        needsConnect = !connected,
                        fromAddress = fromAddress,
                        to = to,
                        amountNanoPac = amount,
                        feeNanoPac = fee,
                        memo = memo,
                    )
                )
            }
        }
    }

    fun onSignMessageRequest(requestId: String, message: String, address: String) {
        // Refuse non-text messages: they could be transaction bytes smuggled
        // through the (prefix-less) message signer to bypass the transaction
        // approval flow.
        if (!SignGuard.isSignableMessage(message)) {
            viewModelScope.launch {
                _effect.send(Effect.RejectSign(requestId, "Message contains non-text characters and cannot be signed"))
            }
            return
        }
        val accounts = _state.value.accounts
        val candidates = SignerPick.candidates(accounts)
        if (candidates.isEmpty()) {
            viewModelScope.launch { _effect.send(Effect.RejectSign(requestId, "No account available")) }
            return
        }
        val requested = address.trim()
        val host = currentHost()
        viewModelScope.launch {
            val connected = host.isNotBlank() && browserDao.isSiteConnected(host)
            // A named address must belong to this wallet; never fall back to
            // another key. A connected site already sees the accounts, so it is
            // told right away; an unconnected one only learns after the user
            // responds, so it cannot probe which addresses the wallet holds.
            val holdsRequested = requested.isEmpty() || SignerPick.requestedSignerIndex(accounts, requested) >= 0
            if (!holdsRequested && connected) {
                _effect.send(Effect.RejectSign(requestId, "No account available"))
                return@launch
            }
            if (hasPendingSignRequest()) {
                _effect.send(Effect.RejectSign(requestId, ERR_REQUEST_PENDING))
                return@launch
            }
            _state.update {
                it.copy(
                    pendingSignMessageRequest = PendingSignMessageRequest(
                        requestId = requestId,
                        host = host,
                        needsConnect = !connected,
                        fromAddress = requested.ifEmpty { candidates.first().address },
                        message = message,
                        signerFixed = requested.isNotEmpty(),
                        signerOptions = if (requested.isEmpty()) candidates else emptyList(),
                    )
                )
            }
        }
    }

    fun onSelectMessageSigner(address: String) {
        _state.update { state ->
            val req = state.pendingSignMessageRequest
            if (req == null || req.signerFixed || req.signerOptions.none { it.address == address }) {
                state
            } else {
                state.copy(pendingSignMessageRequest = req.copy(fromAddress = address))
            }
        }
    }

    fun onConfirmSign() {
        val req = _state.value.pendingSignRequest ?: return
        if (!walletRepository.isUnlocked()) {
            requestUnlock(req)
            return
        }
        _state.update { it.copy(isSigning = true) }
        viewModelScope.launch {
            if (req.needsConnect && req.host.isNotBlank()) {
                browserDao.insertConnectedSite(ConnectedSiteEntity(host = req.host))
            }
            runCatching {
                sendTransaction(
                    from = req.fromAddress,
                    to = req.to,
                    amount = Amount.fromNanoPac(req.amountNanoPac),
                    fee = Amount.fromNanoPac(req.feeNanoPac),
                    memo = req.memo.takeIf { it.isNotBlank() },
                )
            }.onSuccess { txId ->
                _state.update { it.copy(isSigning = false, pendingSignRequest = null) }
                _effect.send(Effect.ResolveSign(req.requestId, txId))
            }.onFailure { e ->
                _state.update { it.copy(isSigning = false, pendingSignRequest = null) }
                _effect.send(Effect.RejectSign(req.requestId, e.message ?: "Transaction failed"))
                _effect.send(Effect.ShowSnackbar(e.toUiText(R.string.error_transaction_failed)))
            }
        }
    }

    fun onConfirmSignMessage() {
        val req = _state.value.pendingSignMessageRequest ?: return
        if (!walletRepository.isUnlocked()) {
            requestUnlock(req)
            return
        }
        _state.update { it.copy(isSigning = true) }
        viewModelScope.launch {
            if (req.needsConnect && req.host.isNotBlank()) {
                browserDao.insertConnectedSite(ConnectedSiteEntity(host = req.host))
            }
            val accounts = _state.value.accounts
            val index = SignerPick.resolveSignerIndex(
                accounts = accounts,
                requested = if (req.signerFixed) req.fromAddress else "",
                chosen = accounts.indexOfFirst { it.address == req.fromAddress }.takeIf { it >= 0 },
            )
            val signer = accounts.getOrNull(index) ?: run {
                _state.update { it.copy(isSigning = false, pendingSignMessageRequest = null) }
                _effect.send(Effect.RejectSign(req.requestId, "No account available"))
                return@launch
            }
            runCatching {
                walletRepository.signMessage(req.message, signer.address)
            }.onSuccess { signature ->
                _state.update { it.copy(isSigning = false, pendingSignMessageRequest = null) }
                _effect.send(Effect.ResolveSign(req.requestId, signature))
            }.onFailure { e ->
                _state.update { it.copy(isSigning = false, pendingSignMessageRequest = null) }
                _effect.send(Effect.RejectSign(req.requestId, e.message ?: "Signing failed"))
                _effect.send(Effect.ShowSnackbar(e.toUiText(R.string.error_signing_failed)))
            }
        }
    }

    fun onCancelSign() {
        val req = _state.value.pendingSignRequest
        val msgReq = _state.value.pendingSignMessageRequest
        approvedBeforeUnlock = null
        
        _state.update {
            it.copy(
                pendingSignRequest = null,
                pendingSignMessageRequest = null,
                showUnlockDialog = false,
                unlockError = null,
            )
        }
        
        val id = req?.requestId ?: msgReq?.requestId ?: return
        viewModelScope.launch { _effect.send(Effect.RejectSign(id, "User rejected")) }
    }

    // Only one sign request at a time: a page must not be able to swap the
    // request under the user's finger or behind the unlock dialog.
    private fun hasPendingSignRequest(): Boolean =
        _state.value.pendingSignRequest != null || _state.value.pendingSignMessageRequest != null

    private fun requestUnlock(approved: Any) {
        approvedBeforeUnlock = approved
        _state.update { it.copy(showUnlockDialog = true, unlockError = null) }
    }

    // The unlock dialog's dismiss action is labelled "Reject", so leaving it
    // rejects the dApp request as well instead of leaving a half-approved sheet.
    fun onDismissUnlock() {
        if (_state.value.isUnlocking) return
        onCancelSign()
    }

    fun onBiometricUnlockTriggered() {
        viewModelScope.launch { _effect.send(Effect.RequestBiometricUnlock) }
    }

    fun onBiometricUnlockSuccess(password: String) {
        onUnlock(password)
    }

    fun onUnlock(password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUnlocking = true, unlockError = null) }
            try {
                if (!walletRepository.unlockWallet(password)) {
                    _state.update { it.copy(isUnlocking = false, unlockError = UiText.Res(R.string.error_incorrect_password)) }
                    return@launch
                }
                _state.update { it.copy(isUnlocking = false, showUnlockDialog = false) }
                // Resume only the exact request the user confirmed; if anything
                // changed, the sheet stays open and the user confirms again.
                val approved = approvedBeforeUnlock
                approvedBeforeUnlock = null
                val current = _state.value
                when {
                    approved == null -> Unit
                    approved == current.pendingSignRequest -> onConfirmSign()
                    approved == current.pendingSignMessageRequest -> onConfirmSignMessage()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isUnlocking = false, unlockError = e.toUiText(R.string.error_unlock_failed)) }
            }
        }
    }

    private fun sanitizeUrl(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
        }
    }

    companion object {
        const val HOME_URL = "about:blank"
        const val ERR_REQUEST_PENDING = "Another request is pending"
    }
}
