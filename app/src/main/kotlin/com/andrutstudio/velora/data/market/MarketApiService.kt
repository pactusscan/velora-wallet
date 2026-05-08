package com.andrutstudio.velora.data.market

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

interface MarketApiService {
    @GET("api/v1/market")
    suspend fun getMarket(): MarketResponse

    @GET("api/v1/market/history")
    suspend fun getMarketHistory(): MarketHistoryResponse
}

data class MarketResponse(
    @SerializedName("price") val price: Double,
)

data class MarketHistoryResponse(
    @SerializedName("data") val data: List<PricePoint>,
    @SerializedName("days") val days: Int,
)

data class PricePoint(
    @SerializedName("time") val time: Long,
    @SerializedName("price") val price: Double,
)
