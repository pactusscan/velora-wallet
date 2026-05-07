package com.andrutstudio.velora.data.rpc

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.andrutstudio.velora.domain.model.Network
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_NETWORK = stringPreferencesKey("selected_network")
private val KEY_CUSTOM_RPC_MAINNET = stringPreferencesKey("custom_rpc_mainnet")
private val KEY_CUSTOM_RPC_TESTNET = stringPreferencesKey("custom_rpc_testnet")

@Singleton
class NetworkProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val MAINNET_URLS = listOf(
            "https://bootstrap1.pactus.org/jsonrpc",
            "https://bootstrap2.pactus.org/jsonrpc",
            "https://bootstrap3.pactus.org/jsonrpc",
            "https://bootstrap4.pactus.org/jsonrpc",
        )
        val TESTNET_URLS = listOf(
            "https://testnet1.pactus.org/jsonrpc",
            "https://testnet2.pactus.org/jsonrpc",
            "https://testnet3.pactus.org/jsonrpc",
            "https://testnet4.pactus.org/jsonrpc",
        )
    }

    val networkFlow: Flow<Network> = dataStore.data.map { prefs ->
        prefs[KEY_NETWORK]?.let { runCatching { Network.valueOf(it) }.getOrNull() } ?: Network.MAINNET
    }

    var current: Network = Network.MAINNET
        private set

    suspend fun setNetwork(network: Network) {
        current = network
        dataStore.edit { it[KEY_NETWORK] = network.name }
    }

    fun customRpcFlow(network: Network): Flow<String?> {
        val key = if (network == Network.MAINNET) KEY_CUSTOM_RPC_MAINNET else KEY_CUSTOM_RPC_TESTNET
        return dataStore.data.map { prefs -> prefs[key]?.takeIf { it.isNotBlank() } }
    }

    suspend fun setCustomRpcUrl(network: Network, url: String?) {
        val key = if (network == Network.MAINNET) KEY_CUSTOM_RPC_MAINNET else KEY_CUSTOM_RPC_TESTNET
        dataStore.edit { prefs ->
            if (url.isNullOrBlank()) prefs.remove(key) else prefs[key] = url
        }
    }

    suspend fun getCurrentUrls(): List<String> {
        val prefs = dataStore.data.first()
        val key = if (current == Network.MAINNET) KEY_CUSTOM_RPC_MAINNET else KEY_CUSTOM_RPC_TESTNET
        val custom = prefs[key]?.takeIf { it.isNotBlank() }
        return if (custom != null) listOf(custom)
        else if (current == Network.MAINNET) MAINNET_URLS else TESTNET_URLS
    }
}
