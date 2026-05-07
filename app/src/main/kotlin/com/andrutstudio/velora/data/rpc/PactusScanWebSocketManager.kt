package com.andrutstudio.velora.data.rpc

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PactusScanWebSocketManager @Inject constructor(
    private val client: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _newBlockEvent = MutableSharedFlow<Unit>(replay = 0)
    val newBlockEvent = _newBlockEvent.asSharedFlow()

    fun connect() {
        if (webSocket != null) return

        val request = Request.Builder()
            .url("wss://api.pactusscan.com/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // PactusScan WS usually sends a simple message or JSON when a block is found
                // Assuming "new_block" or similar event trigger
                if (text.contains("new_block", ignoreCase = true) || text.contains("block", ignoreCase = true)) {
                    scope.launch {
                        _newBlockEvent.emit(Unit)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("PactusScanWS", "Closing: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("PactusScanWS", "Error: ${t.message}")
                webSocket.close(1001, null)
                this@PactusScanWebSocketManager.webSocket = null
                // Reconnect after delay
                scope.launch {
                    delay(5000)
                    connect()
                }
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User logout/App background")
        webSocket = null
    }
}
