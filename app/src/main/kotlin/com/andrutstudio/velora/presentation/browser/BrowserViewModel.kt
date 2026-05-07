package com.andrutstudio.velora.presentation.browser

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.launch
import com.andrutstudio.velora.data.local.db.BookmarkEntity
import com.andrutstudio.velora.data.local.db.BrowserDao
import com.andrutstudio.velora.data.local.db.BrowserHistoryEntity
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.repository.WalletRepository
import com.andrutstudio.velora.domain.usecase.SendTransactionUseCase
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
        val pendingSignRequest: PendingSignRequest? = null,
        val isSigning: Boolean = false,
    )

    data class PendingSignRequest(
        val requestId: String,
        val fromAddress: String,
        val to: String,
        val amountNanoPac: Long,
        val feeNanoPac: Long,
        val memo: String,
    )

    sealed interface Effect {
        data class LoadUrl(val url: String) : Effect
        data object GoBack : Effect
        data object GoForward : Effect
        data object Reload : Effect
        data class OpenExternal(val url: String) : Effect
        data class ResolveSign(val requestId: String, val txId: String) : Effect
        data class RejectSign(val requestId: String, val error: String) : Effect
        data class ShowSnackbar(val message: String) : Effect
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        val initialUrl = savedStateHandle.get<String>("url").orEmpty()
        val startUrl = initialUrl.ifBlank { HOME_URL }
        val displayUrl = if (startUrl == HOME_URL) "" else startUrl
        _state.update { it.copy(url = startUrl, urlBarText = displayUrl) }

        viewModelScope.launch {
            walletRepository.observeWallet().filterNotNull().first().let { wallet ->
                _state.update { it.copy(accounts = wallet.accounts, network = wallet.network) }
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
                _effect.send(Effect.ShowSnackbar("Bookmarked"))
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

    fun onSignRequest(requestId: String, to: String, amountNanoPac: String, feeNanoPac: String, memo: String) {
        val fromAddress = _state.value.accounts.firstOrNull()?.address ?: run {
            viewModelScope.launch { _effect.send(Effect.RejectSign(requestId, "No account available")) }
            return
        }
        val amount = amountNanoPac.toLongOrNull() ?: 0L
        val fee = feeNanoPac.toLongOrNull() ?: 0L
        _state.update {
            it.copy(
                pendingSignRequest = PendingSignRequest(
                    requestId = requestId,
                    fromAddress = fromAddress,
                    to = to,
                    amountNanoPac = amount,
                    feeNanoPac = fee,
                    memo = memo,
                )
            )
        }
    }

    fun onConfirmSign() {
        val req = _state.value.pendingSignRequest ?: return
        _state.update { it.copy(isSigning = true) }
        viewModelScope.launch {
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
                _effect.send(Effect.ShowSnackbar(e.message ?: "Transaction failed"))
            }
        }
    }

    fun onCancelSign() {
        val req = _state.value.pendingSignRequest ?: return
        _state.update { it.copy(pendingSignRequest = null) }
        viewModelScope.launch { _effect.send(Effect.RejectSign(req.requestId, "User rejected")) }
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
    }
}
