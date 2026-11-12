package com.andrutstudio.velora.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeRulesTest {
    private val all = listOf("https://a/jsonrpc", "https://b/jsonrpc", "https://c/jsonrpc")
    private val keepOrder: (List<String>) -> List<String> = { it }

    @Test
    fun `only https urls with a host are accepted`() {
        assertTrue(NodeRules.isSafeRpcUrl("https://node.example.com/jsonrpc"))
        assertTrue(NodeRules.isSafeRpcUrl("  HTTPS://node.example.com:8545  "))
        assertFalse(NodeRules.isSafeRpcUrl("http://node.example.com/jsonrpc"))
        assertFalse(NodeRules.isSafeRpcUrl("http://127.0.0.1:8545"))
        assertFalse(NodeRules.isSafeRpcUrl("https:///jsonrpc"))
        assertFalse(NodeRules.isSafeRpcUrl("node.example.com"))
        assertFalse(NodeRules.isSafeRpcUrl("javascript:alert(1)"))
    }

    @Test
    fun `fastest node goes first in auto mode`() {
        val urls = NodeRules.orderUrls(all, null, false, "https://b/jsonrpc", emptySet(), keepOrder)
        assertEquals(listOf("https://b/jsonrpc", "https://a/jsonrpc", "https://c/jsonrpc"), urls)
    }

    @Test
    fun `pinned node goes first and the others stay as failover`() {
        val urls = NodeRules.orderUrls(all, "https://c/jsonrpc", false, "https://b/jsonrpc", emptySet(), keepOrder)
        assertEquals(listOf("https://c/jsonrpc", "https://b/jsonrpc", "https://a/jsonrpc"), urls)
    }

    @Test
    fun `a pinned node that just failed is skipped`() {
        val urls = NodeRules.orderUrls(all, "https://c/jsonrpc", true, "https://b/jsonrpc", setOf("https://c/jsonrpc"), keepOrder)
        assertEquals(listOf("https://b/jsonrpc", "https://a/jsonrpc"), urls)
    }

    @Test
    fun `a pinned url outside the node list is ignored`() {
        val urls = NodeRules.orderUrls(all, "https://evil/jsonrpc", false, null, emptySet(), keepOrder)
        assertEquals(all, urls)
    }

    @Test
    fun `failed nodes are skipped unless all failed`() {
        assertEquals(listOf("https://c/jsonrpc"), NodeRules.orderUrls(all, null, false, null, setOf("https://a/jsonrpc", "https://b/jsonrpc"), keepOrder))
        assertEquals(all, NodeRules.orderUrls(all, null, false, null, all.toSet(), keepOrder))
    }

    @Test
    fun `legacy custom url becomes a pinned custom node`() {
        val (nodes, pinned) = NodeRules.migrateLegacy("https://mine/jsonrpc", emptyList(), null)
        assertEquals(listOf(CustomNode("https://mine/jsonrpc")), nodes)
        assertEquals("https://mine/jsonrpc", pinned)
    }

    @Test
    fun `migration keeps an existing pin and does not duplicate`() {
        val existing = listOf(CustomNode("https://mine/jsonrpc", "Home"))
        val (nodes, pinned) = NodeRules.migrateLegacy("https://mine/jsonrpc", existing, "https://a/jsonrpc")
        assertEquals(existing, nodes)
        assertEquals("https://a/jsonrpc", pinned)
        assertEquals(existing to "https://a/jsonrpc", NodeRules.migrateLegacy(null, existing, "https://a/jsonrpc"))
    }

    @Test
    fun `node in use is the pinned one while it answers, else the fastest`() {
        val healthy = listOf("https://a/jsonrpc" to 300L, "https://b/jsonrpc" to 90L)
        assertEquals("https://a/jsonrpc", NodeRules.inUse(healthy, "https://a/jsonrpc"))
        assertEquals("https://b/jsonrpc", NodeRules.inUse(healthy, "https://c/jsonrpc"))
        assertEquals("https://b/jsonrpc", NodeRules.inUse(healthy, null))
        assertEquals(null, NodeRules.inUse(emptyList(), null))
    }

    @Test
    fun `network names match the node's get_network_info`() {
        assertEquals("pactus", Network.MAINNET.p2pName)
        assertEquals("pactus-testnet", Network.TESTNET.p2pName)
    }
}
