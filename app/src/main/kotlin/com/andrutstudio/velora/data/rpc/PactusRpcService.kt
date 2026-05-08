package com.andrutstudio.velora.data.rpc

import com.google.gson.JsonElement
import com.andrutstudio.velora.data.rpc.model.RpcRequest
import com.andrutstudio.velora.data.rpc.model.RpcResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface PactusRpcService {
    @POST
    suspend fun call(@Url url: String, @Body request: RpcRequest): RpcResponse<JsonElement>
}
