package com.andrutstudio.velora.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrutstudio.velora.R
import com.andrutstudio.velora.data.rpc.NetworkProvider
import com.andrutstudio.velora.data.rpc.NodeHealth
import com.andrutstudio.velora.data.rpc.NodeProber
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.model.NodeRules
import com.andrutstudio.velora.domain.model.p2pName
import com.andrutstudio.velora.presentation.components.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Node RPC list in Network settings, the same as the browser extension's. */
@HiltViewModel
class NodeListViewModel @Inject constructor(
    private val networkProvider: NetworkProvider,
    private val prober: NodeProber,
) : ViewModel() {

    data class NodeRow(
        val url: String,
        val name: String,
        val custom: Boolean,
        val health: NodeHealth? = null, // null while checking
    )

    data class AddForm(
        val url: String = "",
        val name: String = "",
        val result: NodeHealth? = null,
        val isTesting: Boolean = false,
        val error: UiText? = null,
    ) {
        fun wrongNetwork(expected: String): Boolean =
            result?.ok == true && result.networkName.isNotEmpty() && result.networkName != expected
    }

    data class State(
        val network: Network = Network.MAINNET,
        val nodes: List<NodeRow> = emptyList(),
        val pinnedUrl: String? = null,
        val inUse: String? = null,
        val isChecking: Boolean = false,
        val addForm: AddForm? = null,
    ) {
        val topHeight: Long get() = nodes.maxOfOrNull { it.health?.height ?: 0L } ?: 0L
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var probeJob: Job? = null

    init {
        viewModelScope.launch {
            networkProvider.networkFlow.distinctUntilChanged().collect { network ->
                _state.update { it.copy(network = network, addForm = null) }
                refresh()
            }
        }
    }

    /** Reloads the list and measures every node again (Test all). */
    fun refresh() {
        probeJob?.cancel()
        probeJob = viewModelScope.launch {
            val network = _state.value.network
            val custom = networkProvider.customNodesFlow(network).first()
            val urls = networkProvider.allUrls(network)
            val pinned = networkProvider.pinnedUrl(network)
            val defaults = NetworkProvider.defaultUrls(network)
            val rows = urls.map { url ->
                NodeRow(url, custom.firstOrNull { it.url == url }?.name.orEmpty(), url !in defaults)
            }
            _state.update { it.copy(nodes = rows, pinnedUrl = pinned, inUse = null, isChecking = true) }

            val checked = rows.map { row -> async { row.copy(health = prober.probe(row.url)) } }.awaitAll()
            val healthy = checked.mapNotNull { row -> row.health?.takeIf { it.ok }?.let { row.url to (it.latencyMs ?: 0L) } }
            _state.update {
                it.copy(nodes = checked, inUse = NodeRules.inUse(healthy, pinned), isChecking = false)
            }
        }
    }

    fun onAutoMode() = setPinned(null)

    // Pinning starts from the node in use; the user can pick another in the list
    fun onPinnedMode() {
        val s = _state.value
        setPinned(s.inUse ?: s.nodes.firstOrNull()?.url ?: return)
    }

    fun onPin(url: String) = setPinned(url)

    private fun setPinned(url: String?) {
        viewModelScope.launch {
            networkProvider.setPinnedNode(_state.value.network, url)
            _state.update { s ->
                val healthy = s.nodes.mapNotNull { row -> row.health?.takeIf { it.ok }?.let { row.url to (it.latencyMs ?: 0L) } }
                s.copy(pinnedUrl = url, inUse = if (s.isChecking) s.inUse else NodeRules.inUse(healthy, url))
            }
        }
    }

    fun onDelete(url: String) {
        viewModelScope.launch {
            networkProvider.removeCustomNode(_state.value.network, url)
            _state.update { s ->
                s.copy(
                    nodes = s.nodes.filter { it.url != url },
                    pinnedUrl = s.pinnedUrl.takeIf { it != url },
                    inUse = s.inUse.takeIf { it != url },
                )
            }
            if (_state.value.inUse == null) refresh()
        }
    }

    // ── Add custom node ──────────────────────────────────────────────────────

    fun onOpenAdd() = _state.update { it.copy(addForm = AddForm()) }

    fun onCloseAdd() = _state.update { it.copy(addForm = null) }

    fun onAddUrlChange(value: String) = updateForm { it.copy(url = value, result = null, error = null) }

    fun onAddNameChange(value: String) = updateForm { it.copy(name = value.take(NodeRules.NAME_MAX)) }

    fun onTestConnection() {
        val form = _state.value.addForm ?: return
        val url = form.url.trim()
        validate(url)?.let { error -> updateForm { it.copy(error = error, result = null) }; return }
        updateForm { it.copy(isTesting = true, error = null, result = null) }
        viewModelScope.launch {
            val result = prober.probe(url)
            updateForm { it.copy(isTesting = false, result = result) }
        }
    }

    fun onSaveNode() {
        val s = _state.value
        val form = s.addForm ?: return
        val url = form.url.trim()
        if (form.result?.ok != true || form.wrongNetwork(s.network.p2pName)) return
        validate(url)?.let { error -> updateForm { it.copy(error = error) }; return }
        viewModelScope.launch {
            networkProvider.addCustomNode(s.network, url, form.name)
            _state.update { it.copy(addForm = null) }
            refresh()
        }
    }

    private fun validate(url: String): UiText? = when {
        !NodeRules.isSafeRpcUrl(url) -> UiText.Res(R.string.node_error_https)
        _state.value.nodes.any { it.url == url } -> UiText.Res(R.string.node_error_duplicate)
        else -> null
    }

    private fun updateForm(change: (AddForm) -> AddForm) =
        _state.update { s -> s.addForm?.let { s.copy(addForm = change(it)) } ?: s }
}
