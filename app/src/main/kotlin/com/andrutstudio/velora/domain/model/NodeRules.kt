package com.andrutstudio.velora.domain.model

import java.net.URI

/** A JSON-RPC node the user added next to the default nodes. */
data class CustomNode(val url: String, val name: String = "")

/** P2P network name reported by pactus.network.get_network_info, used to catch a node of the wrong network. */
val Network.p2pName: String
    get() = if (this == Network.MAINNET) "pactus" else "pactus-testnet"

object NodeRules {
    const val NAME_MAX = 40
    const val BEHIND_WARN = 10L
    const val PINNED_RETRY_MS = 60_000L

    /** Android blocks cleartext traffic, so a custom node must use HTTPS. */
    fun isSafeRpcUrl(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }

    /**
     * Order of nodes for one call: the pinned node first unless it just failed, then the
     * fastest healthy node, then the rest. A failed node is skipped until the next speed test.
     */
    fun orderUrls(
        all: List<String>,
        pinned: String?,
        pinnedDown: Boolean,
        fastest: String?,
        failed: Set<String>,
        shuffle: (List<String>) -> List<String> = { it.shuffled() },
    ): List<String> {
        val available = all.filter { it !in failed }.ifEmpty { all }
        val ordered = if (fastest != null && fastest in available) {
            listOf(fastest) + available.filter { it != fastest }
        } else {
            shuffle(available)
        }
        if (pinned == null || pinned !in all || pinnedDown) return ordered
        return listOf(pinned) + ordered.filter { it != pinned }
    }

    /**
     * Before the node list there was one custom URL per network that replaced the default
     * nodes. It becomes a pinned custom node, so it is still used first but now has failover.
     */
    fun migrateLegacy(legacyUrl: String?, nodes: List<CustomNode>, pinned: String?): Pair<List<CustomNode>, String?> {
        val url = legacyUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return nodes to pinned
        val list = if (nodes.any { it.url == url }) nodes else nodes + CustomNode(url)
        return list to (pinned ?: url)
    }

    /** The node the wallet talks to now: the pinned one while it answers, else the fastest. */
    fun inUse(healthy: List<Pair<String, Long>>, pinned: String?): String? =
        healthy.firstOrNull { it.first == pinned }?.first ?: healthy.minByOrNull { it.second }?.first
}
