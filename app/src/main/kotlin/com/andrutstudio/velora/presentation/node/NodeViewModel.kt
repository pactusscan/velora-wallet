package com.andrutstudio.velora.presentation.node

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrutstudio.velora.R
import com.andrutstudio.velora.data.local.db.MonitoredNodeDao
import com.andrutstudio.velora.data.local.db.MonitoredNodeEntity
import com.andrutstudio.velora.data.rpc.PactusScanApiService
import com.andrutstudio.velora.data.rpc.PeerDetailResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NodeViewModel @Inject constructor(
    private val nodeDao: MonitoredNodeDao,
    private val scanApi: PactusScanApiService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    data class State(
        val nodes: List<String> = emptyList(),
        val peerData: Map<String, PeerLoadState> = emptyMap(),
        val showAddDialog: Boolean = false,
        val addInput: String = "",
        val addError: String? = null,
        val addLoading: Boolean = false,
    )

    sealed class PeerLoadState {
        data object Loading : PeerLoadState()
        data class Loaded(val response: PeerDetailResponse) : PeerLoadState()
        data class Error(val message: String) : PeerLoadState()
    }

    sealed class Event {
        data object OpenAdd : Event()
        data object CloseAdd : Event()
        data class InputChanged(val value: String) : Event()
        data object ConfirmAdd : Event()
        data class Remove(val address: String) : Event()
        data class Refresh(val address: String) : Event()
        data class MoveNode(val fromIndex: Int, val toIndex: Int) : Event()
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
    }

    private var currentNodes: List<MonitoredNodeEntity> = emptyList()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _effect = kotlinx.coroutines.channels.Channel<Effect>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        nodeDao.getAll()
            .onEach { entities ->
                currentNodes = entities
                val addresses = entities.map { it.validatorAddress }
                _state.update { it.copy(nodes = addresses) }
                addresses.forEach { addr ->
                    val current = _state.value.peerData[addr]
                    if (current == null || current is PeerLoadState.Error) {
                        loadPeer(addr)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.OpenAdd -> _state.update { it.copy(showAddDialog = true, addInput = "", addError = null) }
            Event.CloseAdd -> _state.update { it.copy(showAddDialog = false, addLoading = false) }
            is Event.InputChanged -> _state.update { it.copy(addInput = event.value, addError = null) }
            Event.ConfirmAdd -> confirmAdd()
            is Event.Remove -> removeNode(event.address)
            is Event.Refresh -> loadPeer(event.address)
            is Event.MoveNode -> moveNode(event.fromIndex, event.toIndex)
        }
    }

    private fun moveNode(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val nodes = currentNodes.toMutableList()
        val item = nodes.removeAt(fromIndex)
        nodes.add(toIndex, item)
        
        val updatedNodes = nodes.mapIndexed { index, entity ->
            entity.copy(displayOrder = index)
        }
        
        viewModelScope.launch {
            nodeDao.updateOrder(updatedNodes)
        }
    }

    private fun loadPeer(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(peerData = it.peerData + (id to PeerLoadState.Loading)) }
            runCatching {
                if (id.startsWith("pc1p")) {
                    scanApi.getValidatorPeer(id)
                } else {
                    scanApi.getPeerById(id)
                }
            }
                .onSuccess { resp ->
                    _state.update { it.copy(peerData = it.peerData + (id to PeerLoadState.Loaded(resp))) }
                    
                    // Update online status in DB
                    currentNodes.find { it.validatorAddress == id }?.let { node ->
                        if (!node.lastKnownOnline) {
                            viewModelScope.launch {
                                nodeDao.update(node.copy(lastKnownOnline = true))
                            }
                        }
                    }
                }
                .onFailure { err ->
                    val message = when (err) {
                        is java.net.ConnectException -> context.getString(R.string.node_error_connection_refused)
                        is java.net.UnknownHostException -> context.getString(R.string.node_error_dns)
                        is java.net.SocketTimeoutException -> context.getString(R.string.node_error_timeout)
                        is retrofit2.HttpException -> {
                            when (err.code()) {
                                502, 503, 504 -> context.getString(R.string.node_error_server_offline)
                                else -> context.getString(R.string.node_error_server_code, err.code())
                            }
                        }
                        else -> err.localizedMessage ?: context.getString(R.string.node_load_error)
                    }
                    _state.update { it.copy(peerData = it.peerData + (id to PeerLoadState.Error(message))) }
                    
                    // Update offline status in DB and notify
                    currentNodes.find { it.validatorAddress == id }?.let { node ->
                        if (node.lastKnownOnline) {
                            viewModelScope.launch {
                                nodeDao.update(node.copy(lastKnownOnline = false))
                                _effect.send(Effect.ShowSnackbar("Node ${node.validatorAddress.take(8)}... is offline"))
                            }
                        }
                    }
                }
        }
    }

    private fun confirmAdd() {
        val input = _state.value.addInput.trim()
        val isValidator = input.startsWith("pc1p")
        val isPeerId = input.length >= 30 && !input.contains(" ")

        if (!isValidator && !isPeerId) {
            _state.update { it.copy(addError = "Invalid input (must be a validator address or PeerID)") }
            return
        }

        if (_state.value.nodes.contains(input)) {
            _state.update { it.copy(addError = "Node already added") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(addLoading = true, addError = null) }
            runCatching {
                if (isValidator) {
                    scanApi.getValidatorPeer(input)
                } else {
                    scanApi.getPeerById(input)
                }
            }
                .onSuccess { resp ->
                    val nextOrder = (currentNodes.maxOfOrNull { it.displayOrder } ?: -1) + 1
                    nodeDao.insert(MonitoredNodeEntity(validatorAddress = input, displayOrder = nextOrder))
                    _state.update { it.copy(
                        showAddDialog = false,
                        addLoading = false,
                        peerData = it.peerData + (input to PeerLoadState.Loaded(resp))
                    ) }
                }
                .onFailure { err ->
                    val message = when (err) {
                        is java.net.ConnectException -> context.getString(R.string.node_error_daemon_down)
                        is retrofit2.HttpException -> context.getString(R.string.node_error_server_code, err.code())
                        else -> context.getString(R.string.node_error_not_found)
                    }
                    _state.update { it.copy(
                        addLoading = false,
                        addError = message
                    ) }
                }
        }
    }

    private fun removeNode(address: String) {
        viewModelScope.launch {
            nodeDao.delete(MonitoredNodeEntity(validatorAddress = address))
            _state.update { s -> s.copy(peerData = s.peerData - address) }
        }
    }
}
