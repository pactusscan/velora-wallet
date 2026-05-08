package com.andrutstudio.velora.data.rpc

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import com.andrutstudio.velora.data.rpc.model.RpcRequest
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

    private var preferredUrl: String? = null
    private val failedUrls = mutableSetOf<String>()

    suspend fun findFastestNode(service: PactusRpcService) = coroutineScope {
        val urls = if (current == Network.MAINNET) MAINNET_URLS else TESTNET_URLS
        val checkRequest = RpcRequest(method = "pactus.blockchain.get_blockchain_info", params = emptyMap<String, Any>())

        val results = urls.map { url ->
            async {
                val start = System.currentTimeMillis()
                val success = withTimeoutOrNull(2000) {
                    runCatching { service.call(url, checkRequest) }.isSuccess
                } ?: false
                if (success) url to (System.currentTimeMillis() - start) else url to Long.MAX_VALUE
            }
        }.awaitAll()

        preferredUrl = results.filter { it.second < Long.MAX_VALUE }
            .minByOrNull { it.second }?.first ?: urls.first()
        failedUrls.clear()
    }

    fun reportFailure(url: String) {
        failedUrls.add(url)
        if (url == preferredUrl) preferredUrl = null
    }

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
        
        if (custom != null) return listOf(custom)

        val baseUrls = if (current == Network.MAINNET) MAINNET_URLS else TESTNET_URLS
        val available = baseUrls.filter { it !in failedUrls }
        
        return if (preferredUrl != null && preferredUrl in available) {
            listOf(preferredUrl!!) + available.filter { it != preferredUrl }
        } else {
            available.ifEmpty { baseUrls }.shuffled()
        }
    }
}
