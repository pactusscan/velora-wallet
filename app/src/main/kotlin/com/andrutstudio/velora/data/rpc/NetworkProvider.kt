package com.andrutstudio.velora.data.rpc

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.andrutstudio.velora.data.rpc.model.RpcRequest
import com.andrutstudio.velora.domain.model.CustomNode
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.model.NodeRules
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_NETWORK = stringPreferencesKey("selected_network")

// Before the node list: one custom URL per network that replaced the default nodes
private val KEY_LEGACY_RPC_MAINNET = stringPreferencesKey("custom_rpc_mainnet")
private val KEY_LEGACY_RPC_TESTNET = stringPreferencesKey("custom_rpc_testnet")

private fun legacyKey(network: Network) = if (network == Network.MAINNET) KEY_LEGACY_RPC_MAINNET else KEY_LEGACY_RPC_TESTNET
private fun nodesKey(network: Network) = stringPreferencesKey("custom_nodes_${network.name.lowercase()}")
private fun pinnedKey(network: Network) = stringPreferencesKey("pinned_node_${network.name.lowercase()}")

@Singleton
class NetworkProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson,
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

        fun defaultUrls(network: Network): List<String> = if (network == Network.MAINNET) MAINNET_URLS else TESTNET_URLS

        // PactusScan API and live feed per network (testnet: api-testnet, the
        // old api.phoenix host does not resolve)
        fun scanApiHost(network: Network): String =
            if (network == Network.MAINNET) "api.pactusscan.com" else "api-testnet.pactusscan.com"

        fun scanWsUrl(network: Network): String = "wss://${scanApiHost(network)}/ws"
    }

    private val nodeListType = object : TypeToken<List<StoredNode>>() {}.type

    val networkFlow: Flow<Network> = dataStore.data.map { prefs ->
        prefs[KEY_NETWORK]?.let { runCatching { Network.valueOf(it) }.getOrNull() } ?: Network.MAINNET
    }

    var current: Network = Network.MAINNET
        private set

    private var preferredUrl: String? = null
    private val failedUrls: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // A pinned node that failed is skipped until this time, then tried first again
    private val downUntil = ConcurrentHashMap<String, Long>()

    @Volatile
    private var migrated = false

    suspend fun findFastestNode(service: PactusRpcService) = coroutineScope {
        val urls = allUrls(current)
        val checkRequest = RpcRequest(method = "pactus.blockchain.get_blockchain_info", params = emptyMap<String, Any>())

        val results = urls.map { url ->
            async {
                val start = System.currentTimeMillis()
                val success = withTimeoutOrNull(2000) {
                    // A node that refuses calls still answers HTTP 200
                    runCatching { service.call(url, checkRequest).error == null }.getOrDefault(false)
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
        downUntil[url] = System.currentTimeMillis() + NodeRules.PINNED_RETRY_MS
        if (url == preferredUrl) preferredUrl = null
    }

    suspend fun setNetwork(network: Network) {
        current = network
        dataStore.edit { it[KEY_NETWORK] = network.name }
    }

    fun customNodesFlow(network: Network): Flow<List<CustomNode>> =
        dataStore.data.map { prefs -> decodeNodes(prefs[nodesKey(network)]) }

    fun pinnedNodeFlow(network: Network): Flow<String?> =
        dataStore.data.map { prefs -> prefs[pinnedKey(network)]?.takeIf { it.isNotBlank() } }

    /** Default nodes first, then the user's custom nodes. */
    suspend fun allUrls(network: Network): List<String> {
        migrateLegacy()
        return defaultUrls(network) + customNodesFlow(network).first().map { it.url }
    }

    suspend fun pinnedUrl(network: Network): String? {
        migrateLegacy()
        return pinnedNodeFlow(network).first()
    }

    /** Callers check the URL, the network and duplicates first (see NodeListViewModel). */
    suspend fun addCustomNode(network: Network, url: String, name: String) {
        require(NodeRules.isSafeRpcUrl(url)) { "Custom RPC nodes must use HTTPS" }
        migrateLegacy()
        dataStore.edit { prefs ->
            val list = decodeNodes(prefs[nodesKey(network)])
            if (url in defaultUrls(network) || list.any { it.url == url }) return@edit
            prefs[nodesKey(network)] = encodeNodes(list + CustomNode(url, name.trim().take(NodeRules.NAME_MAX)))
        }
    }

    suspend fun removeCustomNode(network: Network, url: String) {
        migrateLegacy()
        dataStore.edit { prefs ->
            prefs[nodesKey(network)] = encodeNodes(decodeNodes(prefs[nodesKey(network)]).filter { it.url != url })
            if (prefs[pinnedKey(network)] == url) prefs.remove(pinnedKey(network))
        }
        failedUrls.remove(url)
        if (preferredUrl == url) preferredUrl = null
    }

    /** null = Auto (fastest node). */
    suspend fun setPinnedNode(network: Network, url: String?) {
        if (url != null) require(url in allUrls(network)) { "Unknown node" }
        dataStore.edit { prefs ->
            if (url == null) prefs.remove(pinnedKey(network)) else prefs[pinnedKey(network)] = url
        }
        if (url != null) downUntil.remove(url)
    }

    suspend fun getCurrentUrls(): List<String> {
        val all = allUrls(current)
        val pinned = pinnedUrl(current)
        val pinnedDown = pinned != null && System.currentTimeMillis() < (downUntil[pinned] ?: 0L)
        return NodeRules.orderUrls(all, pinned, pinnedDown, preferredUrl, failedUrls)
    }

    private fun encodeNodes(nodes: List<CustomNode>): String = gson.toJson(nodes.map { StoredNode(it.url, it.name) })

    private fun decodeNodes(json: String?): List<CustomNode> =
        json?.let { runCatching { gson.fromJson<List<StoredNode>>(it, nodeListType) }.getOrNull() }
            ?.mapNotNull { node -> node.url?.takeIf { it.isNotBlank() }?.let { CustomNode(it, node.name.orEmpty()) } }
            .orEmpty()

    private suspend fun migrateLegacy() {
        if (migrated) return
        dataStore.edit { prefs ->
            for (network in Network.entries) {
                val legacy = prefs[legacyKey(network)]?.trim() ?: continue
                prefs.remove(legacyKey(network))
                // Plain http never worked here (cleartext is blocked), so it is dropped
                if (!NodeRules.isSafeRpcUrl(legacy)) continue
                if (legacy in defaultUrls(network)) {
                    if (prefs[pinnedKey(network)] == null) prefs[pinnedKey(network)] = legacy
                    continue
                }
                val (nodes, pinned) = NodeRules.migrateLegacy(
                    legacy,
                    decodeNodes(prefs[nodesKey(network)]),
                    prefs[pinnedKey(network)],
                )
                prefs[nodesKey(network)] = encodeNodes(nodes)
                pinned?.let { prefs[pinnedKey(network)] = it }
            }
        }
        migrated = true
    }
}

// Stored form of a custom node; data.rpc is kept by the R8 rules, so the JSON keys stay stable
private data class StoredNode(val url: String?, val name: String?)
