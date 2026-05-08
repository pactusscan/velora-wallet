package com.andrutstudio.velora.data.repository

import com.andrutstudio.velora.data.market.MarketApiService
import com.andrutstudio.velora.domain.repository.MarketRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepositoryImpl @Inject constructor(
    private val marketApiService: MarketApiService,
) : MarketRepository {
    override suspend fun getPacPriceUsd(): Double =
        marketApiService.getMarket().price

    override suspend fun getPriceHistory(): List<Double> =
        marketApiService.getMarketHistory().data.map { it.price }
}
