package com.andrutstudio.velora.data.rpc

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PactusScanApiService {
    @GET("api/v1/account/{address}/txs")
    suspend fun getTransactions(
        @Path("address") address: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PactusScanTxListResponse

    @GET("api/v1/transaction/{hash}")
    suspend fun getTransaction(
        @Path("hash") hash: String
    ): PactusScanTxResponse
}

data class PactusScanTxListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("txs") val txs: List<PactusScanTxResponse>
)

data class PactusScanTxResponse(
    @SerializedName("hash") val id: String,
    @SerializedName("payload_type") val payloadType: Int,
    @SerializedName("sender") val sender: String?,
    @SerializedName("receiver") val receiver: String?,
    @SerializedName("amount") val amountNanoPac: Long,
    @SerializedName("fee") val feeNanoPac: Long,
    @SerializedName("memo") val memo: String?,
    @SerializedName("block_time") val blockTime: Long,
    @SerializedName("block_height") val blockHeight: Long,
    @SerializedName("direction") val direction: Int // 0=self, 1=in, 2=out
)
