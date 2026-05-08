package com.andrutstudio.velora.data.rpc.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class RpcRequest(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: Any?,
    val id: Int = 1,
)

data class RpcResponse<T>(
    val jsonrpc: String,
    val result: T?,
    val error: RpcError?,
    val id: Int,
)

data class RpcError(
    val code: Int,
    val message: String,
)
