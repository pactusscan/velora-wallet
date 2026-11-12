package com.andrutstudio.velora.data.rpc

import com.andrutstudio.velora.data.rpc.model.RpcRequest
import com.andrutstudio.velora.data.rpc.model.RpcResponse
import com.google.gson.JsonElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

enum class ProbeError { UNREACHABLE, LOGIN, REFUSED }

/** One-off health of a node for the node list: latency, last block and the P2P network it serves. */
data class NodeHealth(
    val ok: Boolean,
    val latencyMs: Long? = null,
    val height: Long? = null,
    val networkName: String = "", // empty when the node does not report it
    val error: ProbeError? = null,
)

@Singleton
class NodeProber @Inject constructor(private val service: PactusRpcService) {

    suspend fun probe(url: String): NodeHealth {
        val start = System.currentTimeMillis()
        val info = call(url, "pactus.blockchain.get_blockchain_info")
            ?: return NodeHealth(ok = false, error = ProbeError.UNREACHABLE)
        val latencyMs = System.currentTimeMillis() - start
        info.error?.let {
            return NodeHealth(ok = false, latencyMs = latencyMs, error = if (it.isPrivateNode()) ProbeError.LOGIN else ProbeError.REFUSED)
        }
        val height = runCatching { info.result?.asJsonObject?.get("last_block_height")?.asLong }.getOrNull() ?: 0L

        // Older or locked-down nodes may not answer this; the name is then unknown
        val networkName = call(url, "pactus.network.get_network_info")?.result
            ?.let { runCatching { it.asJsonObject.get("network_name")?.asString }.getOrNull() }
            .orEmpty()
        return NodeHealth(ok = true, latencyMs = latencyMs, height = height, networkName = networkName)
    }

    private suspend fun call(url: String, method: String): RpcResponse<JsonElement>? =
        withTimeoutOrNull(PROBE_TIMEOUT_MS) {
            try {
                service.call(url, RpcRequest(method = method, params = emptyMap<String, Any>()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }

    private companion object {
        const val PROBE_TIMEOUT_MS = 5_000L
    }
}
