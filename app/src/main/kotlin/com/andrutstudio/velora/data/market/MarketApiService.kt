package com.andrutstudio.velora.data.market

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

interface MarketApiService {
    @GET("api/v1/market")
    suspend fun getMarket(): MarketResponse
}

data class MarketResponse(
    @SerializedName("price") val price: Double,
)
