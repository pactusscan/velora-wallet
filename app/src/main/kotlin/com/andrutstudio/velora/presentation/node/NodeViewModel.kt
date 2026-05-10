package com.andrutstudio.velora.presentation.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrutstudio.velora.data.local.db.MonitoredNodeDao
import com.andrutstudio.velora.data.local.db.MonitoredNodeEntity
import com.andrutstudio.velora.data.rpc.PactusScanApiService
import com.andrutstudio.velora.data.rpc.PeerDetailResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NodeViewModel @Inject constructor(
    private val nodeDao: MonitoredNodeDao,
    private val scanApi: PactusScanApiService,
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
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        nodeDao.getAll()
            .onEach { entities ->
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
        }
    }

    private fun loadPeer(address: String) {
        viewModelScope.launch {
            _state.update { it.copy(peerData = it.peerData + (address to PeerLoadState.Loading)) }
            runCatching { scanApi.getValidatorPeer(address) }
                .onSuccess { resp ->
                    _state.update { it.copy(peerData = it.peerData + (address to PeerLoadState.Loaded(resp))) }
                }
                .onFailure { err ->
                    _state.update { it.copy(peerData = it.peerData + (address to PeerLoadState.Error(err.message ?: "Failed"))) }
                }
        }
    }

    private fun confirmAdd() {
        val address = _state.value.addInput.trim()
        if (!address.startsWith("pc1p") || address.length < 20) {
            _state.update { it.copy(addError = "Invalid validator address (must start with pc1p)") }
            return
        }
        if (_state.value.nodes.contains(address)) {
            _state.update { it.copy(addError = "Node already added") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(addLoading = true, addError = null) }
            runCatching { scanApi.getValidatorPeer(address) }
                .onSuccess { resp ->
                    nodeDao.insert(MonitoredNodeEntity(validatorAddress = address))
                    _state.update { it.copy(
                        showAddDialog = false,
                        addLoading = false,
                        peerData = it.peerData + (address to PeerLoadState.Loaded(resp))
                    ) }
                }
                .onFailure {
                    _state.update { it.copy(
                        addLoading = false,
                        addError = "Node not found. Check the validator address."
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
